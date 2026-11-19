package com.example.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CaptionStyle
import com.example.data.model.ConnectedAccount
import com.example.data.model.GeneratedClip
import com.example.data.model.PlanTier
import com.example.data.model.PrivacyLevel
import com.example.data.model.ProcessingLog
import com.example.data.model.PublishJob
import com.example.data.model.SocialPlatform
import com.example.data.model.SubtitleLanguage
import com.example.data.model.SystemConfig
import com.example.data.model.UserProfile
import com.example.data.model.VideoProject
import com.example.data.repository.ClipGeniusRepository
import com.example.data.service.GeminiVideoAiService
import com.example.data.service.InspectedVideoInfo
import com.example.data.service.SocialPublisherService
import com.example.data.service.VideoProcessorService
import com.example.data.service.YouTubeMetadataService
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PipelineUiState(
    val isInspecting: Boolean = false,
    val inspectedVideo: InspectedVideoInfo? = null,
    val isProcessing: Boolean = false,
    val currentStepIndex: Int = 0,
    val currentStepLabel: String = "",
    val progressPercent: Int = 0,
    val lastCompletedProjectId: String? = null,
    val lastGeneratedClipCount: Int = 0,
    val errorMessage: String? = null
)

class ClipGeniusViewModel(
    private val repository: ClipGeniusRepository,
    private val metadataService: YouTubeMetadataService,
    private val aiService: GeminiVideoAiService,
    private val videoProcessor: VideoProcessorService,
    private val socialPublisher: SocialPublisherService,
    val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String,
    private val userPhotoUrl: String?
) : ViewModel() {

    val isAdmin: Boolean = repository.isCurrentUserAdmin()

    private val _pipelineState = MutableStateFlow(PipelineUiState())
    val pipelineState: StateFlow<PipelineUiState> = _pipelineState.asStateFlow()

    private val _renderProgressMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val renderProgressMap: StateFlow<Map<String, Int>> = _renderProgressMap.asStateFlow()

    private val _regeneratingClipId = MutableStateFlow<String?>(null)
    val regeneratingClipId: StateFlow<String?> = _regeneratingClipId.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _firestoreError = MutableStateFlow<String?>(null)
    val firestoreError: StateFlow<String?> = _firestoreError.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            handleStreamError("profile", error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val projects: StateFlow<List<VideoProject>> = repository.observeProjects(currentUserId)
        .catch { error ->
            handleStreamError("projects", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val clips: StateFlow<List<GeneratedClip>> = repository.observeClips(currentUserId)
        .catch { error ->
            handleStreamError("clips", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val socialAccounts: StateFlow<List<ConnectedAccount>> = repository.observeSocialAccounts(currentUserId)
        .catch { error ->
            handleStreamError("social_accounts", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val publishJobs: StateFlow<List<PublishJob>> = repository.observePublishJobs(currentUserId)
        .catch { error ->
            handleStreamError("publish_jobs", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val systemConfig: StateFlow<SystemConfig> = repository.observeSystemConfig()
        .catch { error ->
            handleStreamError("system_config", error)
            emit(SystemConfig())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SystemConfig())

    val adminUsers: StateFlow<List<UserProfile>> = if (isAdmin) {
        repository.observeAllUsersForAdmin(currentUserId)
            .catch { error ->
                handleStreamError("admin_users", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
    } else {
        flowOf(emptyList<UserProfile>())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
    }

    val telemetryLogs: StateFlow<List<ProcessingLog>> = (if (isAdmin) {
        repository.observeAllLogsForAdmin(currentUserId)
    } else {
        repository.observeUserLogs(currentUserId)
    }).catch { error ->
        handleStreamError("admin_logs", error)
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureUserProfileExists(
                userId = currentUserId,
                displayName = userDisplayName,
                email = userEmail,
                photoUrl = userPhotoUrl
            )
        }
    }

    private fun handleStreamError(streamName: String, error: Throwable) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Stream error in $streamName", root)
        _firestoreError.value = root.localizedMessage ?: "Failed to sync $streamName"
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun clearPipelineError() {
        _pipelineState.update { it.copy(errorMessage = null) }
    }

    fun clearInspectedVideo() {
        _pipelineState.update { it.copy(inspectedVideo = null, errorMessage = null) }
    }

    fun inspectYouTubeUrl(url: String) {
        if (url.isBlank()) {
            _pipelineState.update { it.copy(errorMessage = "Paste a YouTube video or Shorts link first.") }
            return
        }
        viewModelScope.launch {
            _pipelineState.update { it.copy(isInspecting = true, errorMessage = null) }
            val result = metadataService.inspectYouTubeUrl(url)
            result.fold(
                onSuccess = { info ->
                    _pipelineState.update {
                        it.copy(isInspecting = false, inspectedVideo = info, errorMessage = null)
                    }
                    _snackbarMessage.value = "Validated: ${info.title}"
                },
                onFailure = { err ->
                    _pipelineState.update {
                        it.copy(
                            isInspecting = false,
                            errorMessage = err.localizedMessage ?: "Failed to inspect YouTube link."
                        )
                    }
                }
            )
        }
    }

    fun inspectLocalVideo(uri: Uri) {
        viewModelScope.launch {
            _pipelineState.update { it.copy(isInspecting = true, errorMessage = null) }
            val result = metadataService.inspectLocalVideo(uri)
            result.fold(
                onSuccess = { info ->
                    _pipelineState.update {
                        it.copy(isInspecting = false, inspectedVideo = info, errorMessage = null)
                    }
                    _snackbarMessage.value = "Loaded ${info.title} (${info.durationSeconds}s • ${info.resolutionLabel})"
                },
                onFailure = { err ->
                    _pipelineState.update {
                        it.copy(
                            isInspecting = false,
                            errorMessage = err.localizedMessage ?: "Could not inspect selected video file."
                        )
                    }
                }
            )
        }
    }

    fun runAutomaticClippingWorkflow(
        requestedClips: Int,
        creatorNotes: String,
        subtitleLanguage: SubtitleLanguage,
        captionStyle: CaptionStyle,
        autoPublishRequested: Boolean
    ) {
        val inspected = _pipelineState.value.inspectedVideo
        if (inspected == null) {
            _pipelineState.update { it.copy(errorMessage = "Validate a YouTube URL or pick an authorized video first.") }
            return
        }

        val profile = userProfile.value
        val plan = PlanTier.fromRaw(profile?.planTier)
        val cfg = systemConfig.value
        val dailyLimit = when (plan) {
            PlanTier.FREE -> cfg.freeDailyLimit
            PlanTier.PRO -> cfg.proDailyLimit
            PlanTier.PREMIUM -> cfg.premiumDailyLimit
        }
        val used = profile?.dailyClipsUsed ?: 0
        val remaining = (dailyLimit - used).coerceAtLeast(0)
        if (remaining <= 0) {
            _pipelineState.update {
                it.copy(
                    errorMessage = "Daily clip limit reached ($used/$dailyLimit on ${plan.displayName} plan). Upgrade in Plans & Admin to unlock more clips."
                )
            }
            return
        }

        val effectiveClipCount = requestedClips.coerceIn(1, remaining.coerceAtMost(20))
        val startedAtMs = System.currentTimeMillis()

        viewModelScope.launch {
            _pipelineState.update {
                it.copy(
                    isProcessing = true,
                    currentStepIndex = 1,
                    currentStepLabel = "Step 1/8: Validating video permissions & creating workspace...",
                    progressPercent = 10,
                    errorMessage = null
                )
            }

            val projectDraft = VideoProject(
                userId = currentUserId,
                title = inspected.title,
                sourceType = inspected.sourceType,
                sourceUrl = inspected.sourceUrl,
                channelName = inspected.channelName,
                thumbnailUrl = inspected.thumbnailUrl,
                durationSeconds = inspected.durationSeconds,
                resolutionLabel = inspected.resolutionLabel,
                status = "ANALYZING",
                progressPercent = 15,
                currentStage = "Extracting video metadata & keyframes"
            )

            val projectId = repository.createProject(projectDraft).getOrElse { err ->
                _pipelineState.update {
                    it.copy(isProcessing = false, errorMessage = "Failed to create project: ${err.localizedMessage}")
                }
                return@launch
            }

            _pipelineState.update {
                it.copy(
                    currentStepIndex = 3,
                    currentStepLabel = "Step 3/8: Transcribing & scanning viral peaks with Gemini AI...",
                    progressPercent = 35
                )
            }
            repository.updateProjectProgress(
                projectId = projectId,
                status = "ANALYZING",
                progressPercent = 40,
                currentStage = "Gemini AI analyzing emotional hooks & topic shifts"
            )

            val analysisResult = aiService.analyzeVideoAndGenerateClips(
                videoInfo = inspected,
                requestedClipCount = effectiveClipCount,
                creatorNotes = creatorNotes,
                preferredLanguage = subtitleLanguage
            )

            val analysis = analysisResult.getOrElse { err ->
                val msg = err.localizedMessage ?: "AI video analysis failed."
                repository.updateProjectProgress(
                    projectId = projectId,
                    status = "FAILED",
                    progressPercent = 100,
                    currentStage = "Failed",
                    errorMessage = msg
                )
                repository.logProcessingEvent(
                    eventType = "AI_ANALYSIS",
                    status = "FAILED",
                    message = msg,
                    clipsCount = 0,
                    durationMs = System.currentTimeMillis() - startedAtMs
                )
                _pipelineState.update { it.copy(isProcessing = false, errorMessage = msg) }
                return@launch
            }

            _pipelineState.update {
                it.copy(
                    currentStepIndex = 5,
                    currentStepLabel = "Step 5/8: Generating ${analysis.clips.size} vertical clips, subtitles & viral SEO...",
                    progressPercent = 68
                )
            }

            val localSourceUri = if (inspected.sourceType == "LOCAL_VIDEO") inspected.sourceUrl else ""
            val generatedClips = analysis.clips.map { draft ->
                GeneratedClip(
                    userId = currentUserId,
                    projectId = projectId,
                    projectTitle = inspected.title,
                    sourceUri = localSourceUri,
                    exportedFilePath = "",
                    startMs = draft.startSec * 1000L,
                    endMs = draft.endSec * 1000L,
                    viralScore = draft.viralScore,
                    hookReason = draft.hookReason,
                    emotionTag = draft.emotionTag,
                    viralTitle = draft.viralTitle,
                    seoDescription = draft.seoDescription,
                    youtubeCaption = draft.youtubeCaption,
                    instagramCaption = draft.instagramCaption,
                    thumbnailConcept = draft.thumbnailConcept,
                    hashtags = draft.hashtags,
                    keywords = draft.keywords,
                    subtitleLanguage = subtitleLanguage.name,
                    captionStyle = captionStyle.name,
                    subtitleEn = draft.subtitleEn,
                    subtitleHi = draft.subtitleHi,
                    subtitleHinglish = draft.subtitleHinglish,
                    aspectRatio = "9:16",
                    exportResolution = plan.maxResolution,
                    exportFps = plan.maxFps,
                    faceTrackingEnabled = true,
                    speakerTrackingEnabled = true,
                    dynamicZoomEnabled = true,
                    noiseReductionEnabled = true,
                    audioNormalized = true,
                    transitionStyle = draft.transitionStyle,
                    renderStatus = "PENDING",
                    publishStatus = "DRAFT"
                )
            }

            val savedClipIds = repository.createClips(generatedClips).getOrElse { err ->
                val msg = err.localizedMessage ?: "Failed to save clips to Firestore."
                _pipelineState.update { it.copy(isProcessing = false, errorMessage = msg) }
                return@launch
            }

            // Optional auto-render the top viral clip via Media3 Transformer if authorized local video was provided
            if (localSourceUri.isNotBlank() && savedClipIds.isNotEmpty()) {
                _pipelineState.update {
                    it.copy(
                        currentStepIndex = 7,
                        currentStepLabel = "Step 7/8: Rendering top viral clip in 9:16 ${plan.maxResolution} via Media3...",
                        progressPercent = 85
                    )
                }
                val firstClip = generatedClips.first().copy(id = savedClipIds.first())
                renderClipInternal(firstClip, Uri.parse(localSourceUri))
            }

            // Optional explicit auto-publish queue for Pro/Premium users
            if (autoPublishRequested && plan.supportsAutoPublish && cfg.autoPublishGlobalEnabled) {
                val autoAccounts = socialAccounts.value.filter { it.autoPublish }
                if (autoAccounts.isNotEmpty() && savedClipIds.isNotEmpty()) {
                    val topClipId = savedClipIds.first()
                    val topClipTitle = generatedClips.first().viralTitle
                    autoAccounts.forEach { acc ->
                        repository.createPublishJob(
                            PublishJob(
                                userId = currentUserId,
                                clipId = topClipId,
                                clipTitle = topClipTitle,
                                platform = acc.platform,
                                accountHandle = acc.handle,
                                privacy = acc.privacyDefault,
                                status = "SCHEDULED",
                                scheduledFor = "Auto-Publish Ready",
                                platformPostUrl = "",
                                errorMessage = ""
                            )
                        )
                    }
                    repository.updateClip(topClipId, mapOf("publishStatus" to "SCHEDULED"))
                }
            }

            repository.updateProjectProgress(
                projectId = projectId,
                status = "READY",
                progressPercent = 100,
                currentStage = "Completed — ${savedClipIds.size} viral clips ready",
                durationSeconds = analysis.detectedDurationSeconds,
                transcript = analysis.transcript,
                summary = analysis.summary,
                clipCount = savedClipIds.size
            )

            repository.incrementUsageStats(
                userId = currentUserId,
                clipsAdded = savedClipIds.size,
                storageMbAdded = savedClipIds.size * 4.5
            )

            repository.logProcessingEvent(
                eventType = "AI_ANALYSIS",
                status = "COMPLETED",
                message = "Generated ${savedClipIds.size} viral clips for '${inspected.title}'",
                clipsCount = savedClipIds.size,
                durationMs = System.currentTimeMillis() - startedAtMs
            )

            _pipelineState.update {
                it.copy(
                    isProcessing = false,
                    currentStepIndex = 8,
                    currentStepLabel = "Done! ${savedClipIds.size} vertical clips ready to preview & publish.",
                    progressPercent = 100,
                    lastCompletedProjectId = projectId,
                    lastGeneratedClipCount = savedClipIds.size,
                    errorMessage = null
                )
            }
            _snackbarMessage.value = "Generated ${savedClipIds.size} viral clips! Open Clips tab to edit & publish."
        }
    }

    fun attachLocalSourceAndRender(clip: GeneratedClip, sourceUri: Uri) {
        viewModelScope.launch {
            repository.updateClip(clip.id, mapOf("sourceUri" to sourceUri.toString()))
            renderClipInternal(clip.copy(sourceUri = sourceUri.toString()), sourceUri)
        }
    }

    fun renderClipWithMedia3(clip: GeneratedClip) {
        if (clip.sourceUri.isBlank()) {
            _snackbarMessage.value = "Attach an authorized local video file to render a 9:16 MP4 on device."
            return
        }
        viewModelScope.launch {
            renderClipInternal(clip, Uri.parse(clip.sourceUri))
        }
    }

    private suspend fun renderClipInternal(clip: GeneratedClip, sourceUri: Uri) {
        val startedAt = System.currentTimeMillis()
        _renderProgressMap.update { it + (clip.id to 5) }
        repository.updateClip(clip.id, mapOf("renderStatus" to "RENDERING"))

        val result = videoProcessor.renderVerticalClip(
            clip = clip,
            sourceUri = sourceUri,
            onProgress = { pct ->
                _renderProgressMap.update { it + (clip.id to pct) }
            }
        )

        result.fold(
            onSuccess = { rendered ->
                val sizeMb = rendered.fileSizeBytes / (1024.0 * 1024.0)
                repository.updateClip(
                    clip.id,
                    mapOf(
                        "exportedFilePath" to rendered.localFilePath,
                        "renderStatus" to "RENDERED"
                    )
                )
                repository.incrementUsageStats(userId = currentUserId, storageMbAdded = sizeMb)
                repository.logProcessingEvent(
                    eventType = "CLIP_RENDER",
                    status = "COMPLETED",
                    message = "Rendered 9:16 ${clip.exportResolution} clip '${clip.viralTitle}'",
                    clipsCount = 1,
                    durationMs = System.currentTimeMillis() - startedAt
                )
                _renderProgressMap.update { it - clip.id }
                _snackbarMessage.value = if (rendered.galleryUri != null) {
                    "Rendered 9:16 ${clip.exportResolution} MP4 & saved to Movies/ClipGenius!"
                } else {
                    "Rendered 9:16 ${clip.exportResolution} MP4 ready to publish!"
                }
            },
            onFailure = { err ->
                repository.updateClip(clip.id, mapOf("renderStatus" to "FAILED"))
                repository.logProcessingEvent(
                    eventType = "CLIP_RENDER",
                    status = "FAILED",
                    message = "Render failed: ${err.localizedMessage}",
                    clipsCount = 0,
                    durationMs = System.currentTimeMillis() - startedAt
                )
                _renderProgressMap.update { it - clip.id }
                _snackbarMessage.value = "Render error: ${err.localizedMessage ?: "Unsupported codec or URI permission expired"}"
            }
        )
    }

    fun updateClip(clipId: String, updates: Map<String, Any>, successNotice: String? = null) {
        viewModelScope.launch {
            repository.updateClip(clipId, updates).fold(
                onSuccess = {
                    if (successNotice != null) _snackbarMessage.value = successNotice
                },
                onFailure = { err ->
                    _snackbarMessage.value = "Update failed: ${err.localizedMessage}"
                }
            )
        }
    }

    fun deleteClip(clipId: String) {
        viewModelScope.launch {
            repository.deleteClip(clipId).onSuccess {
                _snackbarMessage.value = "Clip deleted."
            }
        }
    }

    fun regenerateClipMetadata(clip: GeneratedClip, toneGoal: String) {
        viewModelScope.launch {
            _regeneratingClipId.value = clip.id
            aiService.regenerateClipCopy(clip, toneGoal).fold(
                onSuccess = { fresh ->
                    repository.updateClip(
                        clip.id,
                        mapOf(
                            "viralTitle" to fresh.viralTitle.take(200),
                            "seoDescription" to fresh.seoDescription.take(2500),
                            "youtubeCaption" to fresh.youtubeCaption.take(2000),
                            "instagramCaption" to fresh.instagramCaption.take(2200),
                            "thumbnailConcept" to fresh.thumbnailConcept.take(500),
                            "hashtags" to fresh.hashtags.take(20),
                            "keywords" to fresh.keywords.take(20),
                            "subtitleEn" to fresh.subtitleEn.take(4000),
                            "subtitleHi" to fresh.subtitleHi.take(4000),
                            "subtitleHinglish" to fresh.subtitleHinglish.take(4000)
                        )
                    )
                    _snackbarMessage.value = "Gemini AI refreshed viral titles, captions & EN/HI/Hinglish subtitles!"
                },
                onFailure = { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Gemini regeneration failed."
                }
            )
            _regeneratingClipId.value = null
        }
    }

    // --- SOCIAL ACCOUNTS & PUBLISHING ---
    fun connectSocialAccount(
        platform: SocialPlatform,
        handle: String,
        displayName: String,
        channelOrAccountId: String,
        privacyDefault: PrivacyLevel,
        autoPublish: Boolean
    ) {
        val cleanHandle = handle.trim().let { if (it.startsWith("@")) it else "@$it" }
        val cleanName = displayName.trim().ifBlank { cleanHandle.removePrefix("@") }
        viewModelScope.launch {
            val account = ConnectedAccount(
                userId = currentUserId,
                platform = platform.name,
                handle = cleanHandle,
                displayName = cleanName,
                channelOrAccountId = channelOrAccountId.trim(),
                privacyDefault = privacyDefault.name,
                autoPublish = autoPublish,
                connectedStatus = "CONNECTED"
            )
            repository.addSocialAccount(account).fold(
                onSuccess = {
                    _snackbarMessage.value = "Connected ${platform.displayName} account $cleanHandle"
                },
                onFailure = { err ->
                    _snackbarMessage.value = "Failed to connect account: ${err.localizedMessage}"
                }
            )
        }
    }

    fun updateSocialAccount(accountId: String, privacy: PrivacyLevel, autoPublish: Boolean) {
        viewModelScope.launch {
            repository.updateSocialAccount(accountId, privacy.name, autoPublish)
        }
    }

    fun disconnectSocialAccount(accountId: String) {
        viewModelScope.launch {
            repository.removeSocialAccount(accountId).onSuccess {
                _snackbarMessage.value = "Social account disconnected."
            }
        }
    }

    fun publishClipNow(
        clip: GeneratedClip,
        platform: SocialPlatform,
        accountHandle: String,
        privacy: PrivacyLevel
    ) {
        viewModelScope.launch {
            val outcome = socialPublisher.launchPlatformPublishShare(clip, platform)
            val job = PublishJob(
                userId = currentUserId,
                clipId = clip.id,
                clipTitle = clip.viralTitle,
                platform = platform.name,
                accountHandle = accountHandle.ifBlank { "@creator" },
                privacy = privacy.name,
                status = "PUBLISHED",
                scheduledFor = "",
                platformPostUrl = if (platform == SocialPlatform.YOUTUBE) "youtube-shorts-share" else "instagram-reels-share",
                errorMessage = ""
            )
            repository.createPublishJob(job)
            repository.updateClip(clip.id, mapOf("publishStatus" to "PUBLISHED"))
            repository.incrementUsageStats(userId = currentUserId, publishedAdded = 1)
            repository.logProcessingEvent(
                eventType = "SOCIAL_PUBLISH",
                status = "COMPLETED",
                message = "Published '${clip.viralTitle}' to ${platform.displayName} ($accountHandle)",
                clipsCount = 1,
                durationMs = 0L
            )
            _snackbarMessage.value = outcome.message
        }
    }

    fun scheduleClipPublish(
        clip: GeneratedClip,
        platform: SocialPlatform,
        accountHandle: String,
        privacy: PrivacyLevel,
        scheduledFor: String
    ) {
        viewModelScope.launch {
            val job = PublishJob(
                userId = currentUserId,
                clipId = clip.id,
                clipTitle = clip.viralTitle,
                platform = platform.name,
                accountHandle = accountHandle.ifBlank { "@creator" },
                privacy = privacy.name,
                status = "SCHEDULED",
                scheduledFor = scheduledFor.take(60),
                platformPostUrl = "",
                errorMessage = ""
            )
            repository.createPublishJob(job).fold(
                onSuccess = {
                    repository.updateClip(
                        clip.id,
                        mapOf(
                            "publishStatus" to "SCHEDULED",
                            "scheduledTimeIso" to scheduledFor.take(60)
                        )
                    )
                    _snackbarMessage.value = "Scheduled for ${platform.displayName} ($scheduledFor)"
                },
                onFailure = { err ->
                    _snackbarMessage.value = "Scheduling failed: ${err.localizedMessage}"
                }
            )
        }
    }

    fun copyClipMetadata(clip: GeneratedClip, platform: SocialPlatform) {
        socialPublisher.copyMetadataToClipboard(clip, platform)
        _snackbarMessage.value = "Copied ${platform.displayName} title, caption & hashtags!"
    }

    // --- PLANS & ADMIN ACTIONS ---
    fun switchSubscriptionPlan(tier: PlanTier) {
        val cfg = systemConfig.value
        val limit = when (tier) {
            PlanTier.FREE -> cfg.freeDailyLimit
            PlanTier.PRO -> cfg.proDailyLimit
            PlanTier.PREMIUM -> cfg.premiumDailyLimit
        }
        viewModelScope.launch {
            repository.updateUserPlan(currentUserId, tier, limit).fold(
                onSuccess = {
                    repository.logProcessingEvent(
                        eventType = "PLAN_CHANGE",
                        status = "COMPLETED",
                        message = "Switched plan to ${tier.displayName} ($limit clips/day)",
                        clipsCount = 0,
                        durationMs = 0L
                    )
                    _snackbarMessage.value = "Activated ${tier.displayName} plan ($limit clips/day • ${tier.maxResolution})"
                },
                onFailure = { err ->
                    _snackbarMessage.value = "Plan update failed: ${err.localizedMessage}"
                }
            )
        }
    }

    fun updateAutoPublishPreference(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateUserPreferences(currentUserId, autoPublishEnabled = enabled)
        }
    }

    fun adminUpdateUserPlan(targetUserId: String, tier: PlanTier) {
        if (!isAdmin) return
        val cfg = systemConfig.value
        val limit = when (tier) {
            PlanTier.FREE -> cfg.freeDailyLimit
            PlanTier.PRO -> cfg.proDailyLimit
            PlanTier.PREMIUM -> cfg.premiumDailyLimit
        }
        viewModelScope.launch {
            repository.updateUserPlan(targetUserId, tier, limit).fold(
                onSuccess = {
                    _snackbarMessage.value = "Updated user subscription to ${tier.displayName}."
                },
                onFailure = { err ->
                    _snackbarMessage.value = "Admin update failed: ${err.localizedMessage}"
                }
            )
        }
    }

    fun adminSaveSystemConfig(config: SystemConfig) {
        if (!isAdmin) return
        viewModelScope.launch {
            repository.saveSystemConfig(config).fold(
                onSuccess = { _snackbarMessage.value = "Global system configuration saved." },
                onFailure = { err -> _snackbarMessage.value = "Config save failed: ${err.localizedMessage}" }
            )
        }
    }

    private companion object {
        const val TAG = "ClipGeniusVM"
    }
}
