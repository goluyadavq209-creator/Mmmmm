package com.example.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class InspectedVideoInfo(
    val sourceType: String,
    val sourceUrl: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val durationSeconds: Int,
    val resolutionLabel: String,
    val previewFrames: List<Bitmap> = emptyList()
)

class YouTubeMetadataService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun extractYouTubeVideoId(url: String): String? {
        val trimmed = url.trim()
        val patterns = listOf(
            Regex("""(?:youtube\.com/watch\?.*?v=)([a-zA-Z0-9_-]{11})"""),
            Regex("""(?:youtu\.be/)([a-zA-Z0-9_-]{11})"""),
            Regex("""(?:youtube\.com/shorts/)([a-zA-Z0-9_-]{11})"""),
            Regex("""(?:youtube\.com/live/)([a-zA-Z0-9_-]{11})"""),
            Regex("""(?:youtube\.com/embed/)([a-zA-Z0-9_-]{11})""")
        )
        for (regex in patterns) {
            val match = regex.find(trimmed)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return null
    }

    suspend fun inspectYouTubeUrl(rawUrl: String): Result<InspectedVideoInfo> = withContext(Dispatchers.IO) {
        try {
            val trimmed = rawUrl.trim()
            val videoId = extractYouTubeVideoId(trimmed)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Please enter a valid YouTube video, Shorts, or youtu.be URL.")
                )

            val canonicalUrl = "https://www.youtube.com/watch?v=$videoId"
            val encoded = URLEncoder.encode(canonicalUrl, "UTF-8")
            val oembedUrl = "https://www.youtube.com/oembed?url=$encoded&format=json"

            val request = Request.Builder()
                .url(oembedUrl)
                .header("Accept", "application/json")
                .build()

            var title = "YouTube Video ($videoId)"
            var author = "YouTube Creator"
            var thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Could not validate YouTube video (HTTP ${response.code}). Ensure the video is public and embedding is enabled.")
                    )
                }
                val body = response.body?.string().orEmpty()
                if (body.isNotBlank()) {
                    val json = JSONObject(body)
                    title = json.optString("title", title).ifBlank { title }
                    author = json.optString("author_name", author).ifBlank { author }
                    thumbnailUrl = json.optString("thumbnail_url", thumbnailUrl).ifBlank { thumbnailUrl }
                }
            }

            val thumbBitmap = downloadThumbnailBitmap(thumbnailUrl)
            Result.success(
                InspectedVideoInfo(
                    sourceType = "YOUTUBE_URL",
                    sourceUrl = canonicalUrl,
                    title = title,
                    channelName = author,
                    thumbnailUrl = thumbnailUrl,
                    durationSeconds = 300,
                    resolutionLabel = "1920x1080 (YouTube HD)",
                    previewFrames = listOfNotNull(thumbBitmap)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "YouTube inspection failed", e)
            Result.failure(e)
        }
    }

    private fun downloadThumbnailBitmap(url: String): Bitmap? {
        return try {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    resp.body?.bytes()?.let { bytes ->
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Thumbnail download skipped: ${e.message}")
            null
        }
    }

    suspend fun inspectLocalVideo(uri: Uri): Result<InspectedVideoInfo> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 1920
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 1080
            val durationSec = (durationMs / 1000L).toInt().coerceAtLeast(1)

            val fileName = resolveDisplayName(uri) ?: "Authorized Video Clip"
            val cleanTitle = fileName.substringBeforeLast(".").ifBlank { "Authorized Video" }

            // Extract up to 6 evenly spaced keyframes scaled to <= 640px for fast multimodal Gemini analysis
            val frames = mutableListOf<Bitmap>()
            val sampleCount = if (durationMs >= 12_000L) 6 else 3
            for (i in 0 until sampleCount) {
                val timeUs = if (durationMs > 0L) {
                    ((durationMs * (i + 1)) / (sampleCount + 1)) * 1000L
                } else {
                    0L
                }
                val rawFrame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (rawFrame != null) {
                    frames.add(scaleDownBitmap(rawFrame, 640))
                }
            }

            Result.success(
                InspectedVideoInfo(
                    sourceType = "LOCAL_VIDEO",
                    sourceUrl = uri.toString(),
                    title = cleanTitle,
                    channelName = "Authorized Device Video",
                    thumbnailUrl = "",
                    durationSeconds = durationSec,
                    resolutionLabel = "${width}x${height}",
                    previewFrames = frames
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Local video inspection failed", e)
            Result.failure(e)
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleDownBitmap(source: Bitmap, maxDimension: Int): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= maxDimension && h <= maxDimension) return source
        val scale = maxDimension.toFloat() / maxOf(w, h).toFloat()
        val targetW = (w * scale).toInt().coerceAtLeast(1)
        val targetH = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    private companion object {
        const val TAG = "YouTubeMetadataService"
    }
}
