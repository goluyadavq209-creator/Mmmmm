package com.example.data.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.GeneratedClip
import com.example.data.model.SocialPlatform
import java.io.File

data class SocialLaunchOutcome(
    val launchedNativeApp: Boolean,
    val message: String,
    val clipboardCopied: Boolean
)

class SocialPublisherService(private val context: Context) {

    fun copyMetadataToClipboard(clip: GeneratedClip, platform: SocialPlatform): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
            val captionBody = when (platform) {
                SocialPlatform.YOUTUBE -> buildString {
                    appendLine(clip.viralTitle)
                    appendLine()
                    appendLine(clip.youtubeCaption.ifBlank { clip.seoDescription })
                    if (clip.hashtags.isNotEmpty()) {
                        appendLine()
                        append(clip.hashtags.joinToString(" "))
                    }
                }
                SocialPlatform.INSTAGRAM -> buildString {
                    appendLine(clip.instagramCaption.ifBlank { clip.viralTitle })
                    if (clip.hashtags.isNotEmpty()) {
                        appendLine()
                        append(clip.hashtags.joinToString(" "))
                    }
                }
            }
            clipboard.setPrimaryClip(ClipData.newPlainText("ClipGenius Metadata", captionBody))
            true
        } catch (e: Exception) {
            Log.w(TAG, "Clipboard copy failed: ${e.message}")
            false
        }
    }

    fun launchPlatformPublishShare(
        clip: GeneratedClip,
        platform: SocialPlatform
    ): SocialLaunchOutcome {
        val copied = copyMetadataToClipboard(clip, platform)
        val captionText = when (platform) {
            SocialPlatform.YOUTUBE -> "${clip.viralTitle}\n\n${clip.youtubeCaption}\n${clip.hashtags.joinToString(" ")}"
            SocialPlatform.INSTAGRAM -> "${clip.instagramCaption}\n${clip.hashtags.joinToString(" ")}"
        }

        val renderedFile = clip.exportedFilePath.takeIf { it.isNotBlank() }?.let { File(it) }
        val videoUri: Uri? = if (renderedFile != null && renderedFile.exists()) {
            try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    renderedFile
                )
            } catch (e: Exception) {
                Log.w(TAG, "FileProvider URI error: ${e.message}")
                null
            }
        } else null

        val targetPackage = when (platform) {
            SocialPlatform.YOUTUBE -> "com.google.android.youtube"
            SocialPlatform.INSTAGRAM -> "com.instagram.android"
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            if (videoUri != null) {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, videoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
            putExtra(Intent.EXTRA_SUBJECT, clip.viralTitle)
            putExtra(Intent.EXTRA_TEXT, captionText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val isTargetInstalled = isPackageInstalled(targetPackage)
        return try {
            if (isTargetInstalled) {
                shareIntent.setPackage(targetPackage)
                context.startActivity(shareIntent)
                SocialLaunchOutcome(
                    launchedNativeApp = true,
                    message = "Opened ${platform.displayName} ${platform.formatName} uploader. Viral title & hashtags copied to clipboard!",
                    clipboardCopied = copied
                )
            } else {
                val chooser = Intent.createChooser(
                    shareIntent,
                    "Publish to ${platform.displayName} ${platform.formatName}"
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                SocialLaunchOutcome(
                    launchedNativeApp = false,
                    message = "Opened Android Share Sheet for ${platform.displayName}. Metadata copied to clipboard!",
                    clipboardCopied = copied
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch publish intent", e)
            SocialLaunchOutcome(
                launchedNativeApp = false,
                message = "Metadata copied to clipboard. Unable to open share sheet: ${e.localizedMessage}",
                clipboardCopied = copied
            )
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (_: Exception) {
            false
        }
    }

    private companion object {
        const val TAG = "SocialPublisherService"
    }
}
