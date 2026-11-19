package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PlanTier(
    val displayName: String,
    val defaultDailyLimit: Int,
    val maxResolution: String,
    val maxFps: Int,
    val supportsAutoPublish: Boolean,
    val supportsBulkClipping: Boolean,
    val priceMonthlyLabel: String
) {
    FREE("Free", 3, "720p", 30, false, false, "$0 / mo"),
    PRO("Pro", 20, "1080p", 60, true, false, "$29 / mo"),
    PREMIUM("Premium", 100, "1080p", 60, true, true, "$79 / mo");

    companion object {
        fun fromRaw(raw: String?): PlanTier =
            entries.firstOrNull { it.name == raw } ?: FREE
    }
}

enum class SubtitleLanguage(val label: String, val badge: String) {
    ENGLISH("English", "EN"),
    HINDI("Hindi (हिंदी)", "HI"),
    HINGLISH("Hinglish", "HI-EN");

    companion object {
        fun fromRaw(raw: String?): SubtitleLanguage =
            entries.firstOrNull { it.name == raw } ?: ENGLISH
    }
}

enum class CaptionStyle(val label: String) {
    NEON_POP("Neon Pop"),
    BOLD_BOX("Bold Box"),
    MINIMAL_GLASS("Minimal Glass"),
    KARAOKE_GLOW("Karaoke Glow");

    companion object {
        fun fromRaw(raw: String?): CaptionStyle =
            entries.firstOrNull { it.name == raw } ?: NEON_POP
    }
}

enum class TransitionStyle(val label: String) {
    SMOOTH_CUT("Smooth Cut"),
    ZOOM_PUNCH("Zoom Punch"),
    WHIP_PAN("Whip Pan"),
    FADE_GLOW("Fade Glow");

    companion object {
        fun fromRaw(raw: String?): TransitionStyle =
            entries.firstOrNull { it.name == raw } ?: ZOOM_PUNCH
    }
}

enum class SocialPlatform(val displayName: String, val formatName: String) {
    YOUTUBE("YouTube", "Shorts"),
    INSTAGRAM("Instagram", "Reels");

    companion object {
        fun fromRaw(raw: String?): SocialPlatform =
            entries.firstOrNull { it.name == raw } ?: YOUTUBE
    }
}

enum class PrivacyLevel(val label: String) {
    PUBLIC("Public"),
    UNLISTED("Unlisted"),
    PRIVATE("Private");

    companion object {
        fun fromRaw(raw: String?): PrivacyLevel =
            entries.firstOrNull { it.name == raw } ?: PUBLIC
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val planTier: String = PlanTier.FREE.name,
    val dailyClipsUsed: Int = 0,
    val dailyClipLimit: Int = 3,
    val autoPublishEnabled: Boolean = false,
    val preferredSubtitleLang: String = SubtitleLanguage.ENGLISH.name,
    val preferredCaptionStyle: String = CaptionStyle.NEON_POP.name,
    val totalClipsGenerated: Int = 0,
    val totalPublished: Int = 0,
    val storageUsedMb: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "displayName" to displayName.take(100).ifBlank { "Creator" },
            "email" to email.take(200),
            "photoUrl" to photoUrl?.take(500),
            "planTier" to PlanTier.fromRaw(planTier).name,
            "dailyClipsUsed" to dailyClipsUsed.coerceIn(0, 1000),
            "dailyClipLimit" to dailyClipLimit.coerceIn(1, 1000),
            "autoPublishEnabled" to autoPublishEnabled,
            "preferredSubtitleLang" to SubtitleLanguage.fromRaw(preferredSubtitleLang).name,
            "preferredCaptionStyle" to CaptionStyle.fromRaw(preferredCaptionStyle).name,
            "totalClipsGenerated" to totalClipsGenerated.coerceIn(0, 100000),
            "totalPublished" to totalPublished.coerceIn(0, 100000),
            "storageUsedMb" to storageUsedMb.coerceIn(0.0, 100000.0),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class VideoProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val sourceType: String = "YOUTUBE_URL",
    val sourceUrl: String = "",
    val channelName: String = "",
    val thumbnailUrl: String = "",
    val durationSeconds: Int = 0,
    val resolutionLabel: String = "1920x1080",
    val status: String = "QUEUED",
    val progressPercent: Int = 0,
    val currentStage: String = "",
    val transcript: String = "",
    val summary: String = "",
    val clipCount: Int = 0,
    val errorMessage: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "title" to title.take(200).ifBlank { "Untitled Video" },
            "sourceType" to if (sourceType == "LOCAL_VIDEO") "LOCAL_VIDEO" else "YOUTUBE_URL",
            "sourceUrl" to sourceUrl.take(1000),
            "channelName" to channelName.take(150),
            "thumbnailUrl" to thumbnailUrl.take(1000),
            "durationSeconds" to durationSeconds.coerceIn(0, 86400),
            "resolutionLabel" to resolutionLabel.take(40),
            "status" to status,
            "progressPercent" to progressPercent.coerceIn(0, 100),
            "currentStage" to currentStage.take(160),
            "transcript" to transcript.take(40000),
            "summary" to summary.take(2000),
            "clipCount" to clipCount.coerceIn(0, 50),
            "errorMessage" to errorMessage?.take(1000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class GeneratedClip(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val projectTitle: String = "",
    val sourceUri: String = "",
    val exportedFilePath: String = "",
    val startMs: Long = 0L,
    val endMs: Long = 30000L,
    val viralScore: Int = 85,
    val hookReason: String = "",
    val emotionTag: String = "ENGAGING",
    val viralTitle: String = "",
    val seoDescription: String = "",
    val youtubeCaption: String = "",
    val instagramCaption: String = "",
    val thumbnailConcept: String = "",
    val hashtags: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val subtitleLanguage: String = SubtitleLanguage.ENGLISH.name,
    val captionStyle: String = CaptionStyle.NEON_POP.name,
    val subtitleEn: String = "",
    val subtitleHi: String = "",
    val subtitleHinglish: String = "",
    val aspectRatio: String = "9:16",
    val exportResolution: String = "1080p",
    val exportFps: Int = 30,
    val faceTrackingEnabled: Boolean = true,
    val speakerTrackingEnabled: Boolean = true,
    val dynamicZoomEnabled: Boolean = true,
    val noiseReductionEnabled: Boolean = true,
    val audioNormalized: Boolean = true,
    val transitionStyle: String = TransitionStyle.ZOOM_PUNCH.name,
    val renderStatus: String = "PENDING",
    val publishStatus: String = "DRAFT",
    val scheduledTimeIso: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun activeSubtitleText(): String = when (SubtitleLanguage.fromRaw(subtitleLanguage)) {
        SubtitleLanguage.ENGLISH -> subtitleEn.ifBlank { subtitleHinglish }
        SubtitleLanguage.HINDI -> subtitleHi.ifBlank { subtitleEn }
        SubtitleLanguage.HINGLISH -> subtitleHinglish.ifBlank { subtitleEn }
    }

    fun durationSeconds(): Int = ((endMs - startMs).coerceAtLeast(1000L) / 1000L).toInt()

    fun toCreateMap(): Map<String, Any> {
        val safeStart = startMs.coerceIn(0L, 86400000L)
        val safeEnd = endMs.coerceIn(safeStart, 86400000L)
        return mapOf(
            "userId" to userId,
            "projectId" to projectId,
            "projectTitle" to projectTitle.take(200),
            "sourceUri" to sourceUri.take(1000),
            "exportedFilePath" to exportedFilePath.take(1000),
            "startMs" to safeStart,
            "endMs" to safeEnd,
            "viralScore" to viralScore.coerceIn(0, 100),
            "hookReason" to hookReason.take(500),
            "emotionTag" to emotionTag.take(60),
            "viralTitle" to viralTitle.take(200).ifBlank { "Viral Highlight Clip" },
            "seoDescription" to seoDescription.take(2500),
            "youtubeCaption" to youtubeCaption.take(2000),
            "instagramCaption" to instagramCaption.take(2200),
            "thumbnailConcept" to thumbnailConcept.take(500),
            "hashtags" to hashtags.take(20).map { it.take(50) },
            "keywords" to keywords.take(20).map { it.take(50) },
            "subtitleLanguage" to SubtitleLanguage.fromRaw(subtitleLanguage).name,
            "captionStyle" to CaptionStyle.fromRaw(captionStyle).name,
            "subtitleEn" to subtitleEn.take(4000),
            "subtitleHi" to subtitleHi.take(4000),
            "subtitleHinglish" to subtitleHinglish.take(4000),
            "aspectRatio" to "9:16",
            "exportResolution" to if (exportResolution == "720p") "720p" else "1080p",
            "exportFps" to exportFps.coerceIn(24, 60),
            "faceTrackingEnabled" to faceTrackingEnabled,
            "speakerTrackingEnabled" to speakerTrackingEnabled,
            "dynamicZoomEnabled" to dynamicZoomEnabled,
            "noiseReductionEnabled" to noiseReductionEnabled,
            "audioNormalized" to audioNormalized,
            "transitionStyle" to TransitionStyle.fromRaw(transitionStyle).name,
            "renderStatus" to renderStatus,
            "publishStatus" to publishStatus,
            "scheduledTimeIso" to scheduledTimeIso.take(60),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class ConnectedAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val platform: String = SocialPlatform.YOUTUBE.name,
    val handle: String = "",
    val displayName: String = "",
    val channelOrAccountId: String = "",
    val privacyDefault: String = PrivacyLevel.PUBLIC.name,
    val autoPublish: Boolean = false,
    val connectedStatus: String = "CONNECTED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "platform" to SocialPlatform.fromRaw(platform).name,
        "handle" to handle.take(120).ifBlank { "@creator" },
        "displayName" to displayName.take(120).ifBlank { "Creator Channel" },
        "channelOrAccountId" to channelOrAccountId.take(200),
        "privacyDefault" to PrivacyLevel.fromRaw(privacyDefault).name,
        "autoPublish" to autoPublish,
        "connectedStatus" to connectedStatus,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class PublishJob(
    @DocumentId val id: String = "",
    val userId: String = "",
    val clipId: String = "",
    val clipTitle: String = "",
    val platform: String = SocialPlatform.YOUTUBE.name,
    val accountHandle: String = "",
    val privacy: String = PrivacyLevel.PUBLIC.name,
    val status: String = "QUEUED",
    val scheduledFor: String = "",
    val platformPostUrl: String = "",
    val errorMessage: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "clipId" to clipId,
        "clipTitle" to clipTitle.take(200).ifBlank { "Untitled Clip" },
        "platform" to SocialPlatform.fromRaw(platform).name,
        "accountHandle" to accountHandle.take(120).ifBlank { "@creator" },
        "privacy" to PrivacyLevel.fromRaw(privacy).name,
        "status" to status,
        "scheduledFor" to scheduledFor.take(60),
        "platformPostUrl" to platformPostUrl.take(500),
        "errorMessage" to errorMessage.take(1000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class SystemConfig(
    @DocumentId val id: String = "global",
    val freeDailyLimit: Int = 3,
    val proDailyLimit: Int = 20,
    val premiumDailyLimit: Int = 100,
    val maxVideoDurationMinutes: Int = 60,
    val aiModelName: String = "gemini-3.8-flash",
    val autoPublishGlobalEnabled: Boolean = true,
    val storageQuotaGb: Int = 500,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> = mapOf(
        "freeDailyLimit" to freeDailyLimit.coerceIn(1, 50),
        "proDailyLimit" to proDailyLimit.coerceIn(1, 200),
        "premiumDailyLimit" to premiumDailyLimit.coerceIn(1, 1000),
        "maxVideoDurationMinutes" to maxVideoDurationMinutes.coerceIn(1, 240),
        "aiModelName" to aiModelName.take(100).ifBlank { "gemini-3.8-flash" },
        "autoPublishGlobalEnabled" to autoPublishGlobalEnabled,
        "storageQuotaGb" to storageQuotaGb.coerceIn(1, 10000),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ProcessingLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val eventType: String = "AI_ANALYSIS",
    val status: String = "COMPLETED",
    val message: String = "",
    val clipsCount: Int = 0,
    val durationMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "userEmail" to userEmail.take(200),
        "eventType" to eventType,
        "status" to status,
        "message" to message.take(500).ifBlank { "Event logged" },
        "clipsCount" to clipsCount.coerceIn(0, 100),
        "durationMs" to durationMs.coerceIn(0L, 3600000L),
        "createdAt" to FieldValue.serverTimestamp()
    )
}
