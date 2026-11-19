package com.example.data.service

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.data.model.GeneratedClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.resume

data class RenderedClipOutput(
    val localFilePath: String,
    val galleryUri: String?,
    val fileSizeBytes: Long
)

class VideoProcessorService(private val context: Context) {

    @OptIn(UnstableApi::class)
    suspend fun renderVerticalClip(
        clip: GeneratedClip,
        sourceUri: Uri,
        onProgress: (Int) -> Unit
    ): Result<RenderedClipOutput> {
        val outputDir = File(context.filesDir, "clips").apply { mkdirs() }
        val safeId = clip.id.ifBlank { System.currentTimeMillis().toString() }
        val outputFile = File(outputDir, "clipgenius_${safeId}.mp4")
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val targetWidth = if (clip.exportResolution == "720p") 720 else 1080
        val targetHeight = if (clip.exportResolution == "720p") 1280 else 1920

        val startMs = clip.startMs.coerceAtLeast(0L)
        val endMs = clip.endMs.coerceAtLeast(startMs + 2_000L)

        val renderResult = withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<Result<File>> { cont ->
                val mainHandler = Handler(Looper.getMainLooper())
                var isFinished = false

                val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()

                val inputMediaItem = MediaItem.Builder()
                    .setUri(sourceUri)
                    .setClippingConfiguration(clippingConfig)
                    .build()

                val videoEffects = mutableListOf<Effect>()
                // Real 9:16 vertical crop-and-scale presentation (1080x1920 or 720x1280)
                videoEffects.add(
                    Presentation.createForWidthAndHeight(
                        targetWidth,
                        targetHeight,
                        Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
                    )
                )
                // Dynamic zoom punch when enabled
                if (clip.dynamicZoomEnabled || clip.faceTrackingEnabled) {
                    videoEffects.add(
                        ScaleAndRotateTransformation.Builder()
                            .setScale(1.06f, 1.06f)
                            .build()
                    )
                }

                val editedMediaItem = EditedMediaItem.Builder(inputMediaItem)
                    .setEffects(Effects(emptyList(), videoEffects))
                    .build()

                val transformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            if (!isFinished) {
                                isFinished = true
                                onProgress(100)
                                cont.resume(Result.success(outputFile))
                            }
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            if (!isFinished) {
                                isFinished = true
                                Log.e(TAG, "Media3 Transformer export error", exportException)
                                cont.resume(Result.failure(exportException))
                            }
                        }
                    })
                    .build()

                val progressHolder = ProgressHolder()
                val pollRunnable = object : Runnable {
                    override fun run() {
                        if (isFinished) return
                        val state = transformer.getProgress(progressHolder)
                        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                            onProgress(progressHolder.progress.coerceIn(0, 99))
                        }
                        if (state != Transformer.PROGRESS_STATE_NOT_STARTED && !isFinished) {
                            mainHandler.postDelayed(this, 250L)
                        }
                    }
                }

                cont.invokeOnCancellation {
                    isFinished = true
                    mainHandler.removeCallbacks(pollRunnable)
                    mainHandler.post { transformer.cancel() }
                }

                try {
                    onProgress(5)
                    transformer.start(editedMediaItem, outputFile.absolutePath)
                    mainHandler.postDelayed(pollRunnable, 250L)
                } catch (e: Exception) {
                    isFinished = true
                    cont.resume(Result.failure(e))
                }
            }
        }

        return renderResult.fold(
            onSuccess = { renderedFile ->
                val galleryUri = saveMp4ToGallery(renderedFile, clip.viralTitle)
                Result.success(
                    RenderedClipOutput(
                        localFilePath = renderedFile.absolutePath,
                        galleryUri = galleryUri,
                        fileSizeBytes = renderedFile.length()
                    )
                )
            },
            onFailure = { Result.failure(it) }
        )
    }

    private suspend fun saveMp4ToGallery(file: File, clipTitle: String): String? =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null
            try {
                val sanitized = clipTitle
                    .replace(Regex("[^a-zA-Z0-9_\\- ]"), "")
                    .trim()
                    .take(40)
                    .ifBlank { "ClipGenius_Short" }
                val fileName = "${sanitized}_${System.currentTimeMillis()}.mp4"
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/ClipGenius")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext null

                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(file).use { input ->
                        input.copyTo(out)
                    }
                }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri.toString()
            } catch (e: Exception) {
                Log.w(TAG, "Gallery copy skipped: ${e.message}")
                null
            }
        }

    private companion object {
        const val TAG = "VideoProcessorService"
    }
}
