package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.ui.theme.ObsidianBlack
import com.example.ui.viewmodel.ClipGeniusViewModel
import com.example.ui.viewmodel.PipelineUiState
import org.json.JSONArray
import org.json.JSONObject

private const val LOCAL_WEB_PREFIX = "file:///android_asset/web/"
private const val LOCAL_WEB_ENTRY = "file:///android_asset/web/index.html"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebStudioScreen(
    viewModel: ClipGeniusViewModel,
    onOpenNativeClipEditor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val clips by viewModel.clips.collectAsStateWithLifecycle()
    val socialAccounts by viewModel.socialAccounts.collectAsStateWithLifecycle()
    val publishJobs by viewModel.publishJobs.collectAsStateWithLifecycle()
    val systemConfig by viewModel.systemConfig.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val telemetryLogs by viewModel.telemetryLogs.collectAsStateWithLifecycle()
    val pipelineState by viewModel.pipelineState.collectAsStateWithLifecycle()
    val renderProgressMap by viewModel.renderProgressMap.collectAsStateWithLifecycle()

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Temporary URI read grant still works for current session
            }
            viewModel.inspectLocalVideo(uri)
        }
    }

    val stateJson = remember(
        viewModel.isAdmin,
        userProfile,
        projects,
        clips,
        socialAccounts,
        publishJobs,
        systemConfig,
        adminUsers,
        telemetryLogs,
        pipelineState,
        renderProgressMap
    ) {
        buildWebStateJson(
            isAdmin = viewModel.isAdmin,
            userProfile = userProfile,
            projects = projects,
            clips = clips,
            socialAccounts = socialAccounts,
            publishJobs = publishJobs,
            systemConfig = systemConfig,
            adminUsers = adminUsers,
            telemetryLogs = telemetryLogs,
            pipelineState = pipelineState,
            renderProgressMap = renderProgressMap
        )
    }

    val latestStateJson by rememberUpdatedState(stateJson)
    val latestOnOpenEditor by rememberUpdatedState(onOpenNativeClipEditor)

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isPageLoaded by remember { mutableStateOf(false) }

    val pushStateToWeb: () -> Unit = remember {
        {
            webViewRef?.evaluateJavascript(
                "window.onNativeStateUpdate(${JSONObject.quote(latestStateJson)})",
                null
            )
        }
    }

    LaunchedEffect(stateJson, isPageLoaded) {
        if (isPageLoaded) {
            webViewRef?.evaluateJavascript(
                "window.onNativeStateUpdate(${JSONObject.quote(stateJson)})",
                null
            )
        }
    }

    val jsBridge = remember(viewModel) {
        ClipGeniusJsBridge(
            viewModel = viewModel,
            onRequestSync = { pushStateToWeb() },
            onPickLocalVideo = {
                videoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
            },
            onOpenNativeEditor = { clipId -> latestOnOpenEditor(clipId) }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                removeJavascriptInterface("ClipGeniusBridge")
                stopLoading()
                destroy()
            }
            webViewRef = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("web_studio_container")
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("web_studio_webview"),
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(AndroidColor.parseColor("#06040C"))
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false

                    addJavascriptInterface(jsBridge, "ClipGeniusBridge")

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val target = request?.url?.toString().orEmpty()
                            // Security invariant: only allow bundled local assets inside WebView
                            // so ClipGeniusBridge is never exposed to untrusted remote origins.
                            return !target.startsWith(LOCAL_WEB_PREFIX)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isPageLoaded = true
                            view?.evaluateJavascript(
                                "window.onNativeStateUpdate(${JSONObject.quote(latestStateJson)})",
                                null
                            )
                        }
                    }

                    loadUrl(LOCAL_WEB_ENTRY)
                    webViewRef = this
                }
            }
        )
    }
}

private class ClipGeniusJsBridge(
    private val viewModel: ClipGeniusViewModel,
    private val onRequestSync: () -> Unit,
    private val onPickLocalVideo: () -> Unit,
    private val onOpenNativeEditor: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun requestInitialSync() {
        mainHandler.post { onRequestSync() }
    }

    @JavascriptInterface
    fun validateYouTubeUrl(url: String) {
        mainHandler.post { viewModel.inspectYouTubeUrl(url) }
    }

    @JavascriptInterface
    fun pickLocalVideo() {
        mainHandler.post { onPickLocalVideo() }
    }

    @JavascriptInterface
    fun clearInspectedVideo() {
        mainHandler.post { viewModel.clearInspectedVideo() }
    }

    @JavascriptInterface
    fun runPipeline(
        count: Int,
        notes: String,
        lang: String,
        style: String,
        autoPublish: Boolean
    ) {
        mainHandler.post {
            viewModel.runAutomaticClippingWorkflow(
                requestedClips = count,
                creatorNotes = notes,
                subtitleLanguage = SubtitleLanguage.fromRaw(lang),
                captionStyle = CaptionStyle.fromRaw(style),
                autoPublishRequested = autoPublish
            )
        }
    }

    @JavascriptInterface
    fun openNativeClipEditor(clipId: String) {
        mainHandler.post { onOpenNativeEditor(clipId) }
    }

    @JavascriptInterface
    fun saveClipEdits(clipId: String, jsonPayload: String) {
        mainHandler.post {
            runCatching {
                val obj = JSONObject(jsonPayload)
                val lang = SubtitleLanguage.fromRaw(obj.optString("subtitleLanguage"))
                val style = CaptionStyle.fromRaw(obj.optString("captionStyle"))
                val subtitleText = obj.optString("subtitleText")
                val subtitleField = when (lang) {
                    SubtitleLanguage.ENGLISH -> "subtitleEn"
                    SubtitleLanguage.HINDI -> "subtitleHi"
                    SubtitleLanguage.HINGLISH -> "subtitleHinglish"
                }
                val updates = mutableMapOf<String, Any>(
                    "viralTitle" to obj.optString("viralTitle").take(200),
                    "seoDescription" to obj.optString("seoDescription").take(2500),
                    "youtubeCaption" to obj.optString("youtubeCaption").take(2000),
                    "instagramCaption" to obj.optString("instagramCaption").take(2200),
                    "subtitleLanguage" to lang.name,
                    "captionStyle" to style.name,
                    subtitleField to subtitleText.take(4000)
                )
                viewModel.updateClip(clipId, updates, "Saved clip edits to cloud!")
            }
        }
    }

    @JavascriptInterface
    fun regenerateClip(clipId: String) {
        mainHandler.post {
            val clip = viewModel.clips.value.firstOrNull { it.id == clipId } ?: return@post
            viewModel.regenerateClipMetadata(clip, "High retention viral hook")
        }
    }

    @JavascriptInterface
    fun renderClip(clipId: String) {
        mainHandler.post {
            val clip = viewModel.clips.value.firstOrNull { it.id == clipId } ?: return@post
            viewModel.renderClipWithMedia3(clip)
        }
    }

    @JavascriptInterface
    fun publishClipNow(clipId: String, platform: String, privacy: String) {
        mainHandler.post {
            val clip = viewModel.clips.value.firstOrNull { it.id == clipId } ?: return@post
            val resolvedPlatform = SocialPlatform.fromRaw(platform)
            val resolvedPrivacy = PrivacyLevel.fromRaw(privacy)
            val accountHandle = viewModel.socialAccounts.value
                .firstOrNull { it.platform == resolvedPlatform.name }
                ?.handle
                ?: "@creator"
            viewModel.publishClipNow(clip, resolvedPlatform, accountHandle, resolvedPrivacy)
        }
    }

    @JavascriptInterface
    fun copyClipMetadata(clipId: String, platform: String) {
        mainHandler.post {
            val clip = viewModel.clips.value.firstOrNull { it.id == clipId } ?: return@post
            viewModel.copyClipMetadata(clip, SocialPlatform.fromRaw(platform))
        }
    }

    @JavascriptInterface
    fun connectAccount(
        platform: String,
        handle: String,
        displayName: String,
        privacy: String
    ) {
        mainHandler.post {
            viewModel.connectSocialAccount(
                platform = SocialPlatform.fromRaw(platform),
                handle = handle,
                displayName = displayName,
                channelOrAccountId = "",
                privacyDefault = PrivacyLevel.fromRaw(privacy),
                autoPublish = false
            )
        }
    }

    @JavascriptInterface
    fun disconnectAccount(accountId: String) {
        mainHandler.post { viewModel.disconnectSocialAccount(accountId) }
    }

    @JavascriptInterface
    fun switchPlan(tier: String) {
        mainHandler.post { viewModel.switchSubscriptionPlan(PlanTier.fromRaw(tier)) }
    }

    @JavascriptInterface
    fun adminUpdateUserPlan(uid: String, tier: String) {
        mainHandler.post { viewModel.adminUpdateUserPlan(uid, PlanTier.fromRaw(tier)) }
    }

    @JavascriptInterface
    fun adminSaveSystemConfig(jsonPayload: String) {
        mainHandler.post {
            runCatching {
                val obj = JSONObject(jsonPayload)
                val cfg = SystemConfig(
                    freeDailyLimit = obj.optInt("freeDailyLimit", 3),
                    proDailyLimit = obj.optInt("proDailyLimit", 20),
                    premiumDailyLimit = obj.optInt("premiumDailyLimit", 100),
                    maxVideoDurationMinutes = obj.optInt("maxVideoDurationMinutes", 60),
                    aiModelName = obj.optString("aiModelName", "gemini-3.8-flash"),
                    autoPublishGlobalEnabled = obj.optBoolean("autoPublishGlobalEnabled", true),
                    storageQuotaGb = obj.optInt("storageQuotaGb", 500)
                )
                viewModel.adminSaveSystemConfig(cfg)
            }
        }
    }
}

private fun buildWebStateJson(
    isAdmin: Boolean,
    userProfile: UserProfile?,
    projects: List<VideoProject>,
    clips: List<GeneratedClip>,
    socialAccounts: List<ConnectedAccount>,
    publishJobs: List<PublishJob>,
    systemConfig: SystemConfig,
    adminUsers: List<UserProfile>,
    telemetryLogs: List<ProcessingLog>,
    pipelineState: PipelineUiState,
    renderProgressMap: Map<String, Int>
): String {
    val root = JSONObject()
    root.put("isAdmin", isAdmin)

    val profileObj = JSONObject().apply {
        put("displayName", userProfile?.displayName ?: "Creator Studio")
        put("email", userProfile?.email.orEmpty())
        put("planTier", userProfile?.planTier ?: PlanTier.FREE.name)
        put("dailyClipsUsed", userProfile?.dailyClipsUsed ?: 0)
        put("dailyClipLimit", userProfile?.dailyClipLimit ?: systemConfig.freeDailyLimit)
        put("totalClipsGenerated", userProfile?.totalClipsGenerated ?: 0)
        put("totalPublished", userProfile?.totalPublished ?: 0)
        put("storageUsedMb", userProfile?.storageUsedMb ?: 0.0)
    }
    root.put("userProfile", profileObj)

    val configObj = JSONObject().apply {
        put("freeDailyLimit", systemConfig.freeDailyLimit)
        put("proDailyLimit", systemConfig.proDailyLimit)
        put("premiumDailyLimit", systemConfig.premiumDailyLimit)
        put("maxVideoDurationMinutes", systemConfig.maxVideoDurationMinutes)
        put("aiModelName", systemConfig.aiModelName)
        put("autoPublishGlobalEnabled", systemConfig.autoPublishGlobalEnabled)
        put("storageQuotaGb", systemConfig.storageQuotaGb)
    }
    root.put("systemConfig", configObj)

    val inspected = pipelineState.inspectedVideo
    val inspectedObj = if (inspected != null) {
        JSONObject().apply {
            put("sourceType", inspected.sourceType)
            put("sourceUrl", inspected.sourceUrl)
            put("title", inspected.title)
            put("channelName", inspected.channelName)
            put("thumbnailUrl", inspected.thumbnailUrl)
            put("durationSeconds", inspected.durationSeconds)
            put("resolutionLabel", inspected.resolutionLabel)
        }
    } else {
        JSONObject.NULL
    }

    val pipelineObj = JSONObject().apply {
        put("isInspecting", pipelineState.isInspecting)
        put("isProcessing", pipelineState.isProcessing)
        put("currentStepLabel", pipelineState.currentStepLabel)
        put("progressPercent", pipelineState.progressPercent)
        put("errorMessage", pipelineState.errorMessage ?: JSONObject.NULL)
        put("inspectedVideo", inspectedObj)
    }
    root.put("pipeline", pipelineObj)

    val projectsArr = JSONArray()
    projects.forEach { proj ->
        projectsArr.put(
            JSONObject().apply {
                put("id", proj.id)
                put("title", proj.title)
                put("channelName", proj.channelName)
                put("durationSeconds", proj.durationSeconds)
                put("status", proj.status)
                put("clipCount", proj.clipCount)
                put("summary", proj.summary)
            }
        )
    }
    root.put("projects", projectsArr)

    val clipsArr = JSONArray()
    clips.forEach { clip ->
        clipsArr.put(
            JSONObject().apply {
                put("id", clip.id)
                put("projectTitle", clip.projectTitle)
                put("startMs", clip.startMs)
                put("endMs", clip.endMs)
                put("viralScore", clip.viralScore)
                put("hookReason", clip.hookReason)
                put("emotionTag", clip.emotionTag)
                put("viralTitle", clip.viralTitle)
                put("seoDescription", clip.seoDescription)
                put("youtubeCaption", clip.youtubeCaption)
                put("instagramCaption", clip.instagramCaption)
                put("subtitleLanguage", clip.subtitleLanguage)
                put("captionStyle", clip.captionStyle)
                put("subtitleEn", clip.subtitleEn)
                put("subtitleHi", clip.subtitleHi)
                put("subtitleHinglish", clip.subtitleHinglish)
                put("exportResolution", clip.exportResolution)
                put("renderStatus", clip.renderStatus)
                put("publishStatus", clip.publishStatus)
            }
        )
    }
    root.put("clips", clipsArr)

    val accountsArr = JSONArray()
    socialAccounts.forEach { acc ->
        accountsArr.put(
            JSONObject().apply {
                put("id", acc.id)
                put("platform", acc.platform)
                put("handle", acc.handle)
                put("displayName", acc.displayName)
                put("privacyDefault", acc.privacyDefault)
                put("autoPublish", acc.autoPublish)
            }
        )
    }
    root.put("socialAccounts", accountsArr)

    val jobsArr = JSONArray()
    publishJobs.forEach { job ->
        jobsArr.put(
            JSONObject().apply {
                put("id", job.id)
                put("clipTitle", job.clipTitle)
                put("platform", job.platform)
                put("accountHandle", job.accountHandle)
                put("privacy", job.privacy)
                put("status", job.status)
                put("scheduledFor", job.scheduledFor)
            }
        )
    }
    root.put("publishJobs", jobsArr)

    val usersArr = JSONArray()
    adminUsers.forEach { u ->
        usersArr.put(
            JSONObject().apply {
                put("userId", u.userId)
                put("displayName", u.displayName)
                put("email", u.email)
                put("planTier", u.planTier)
                put("dailyClipsUsed", u.dailyClipsUsed)
                put("dailyClipLimit", u.dailyClipLimit)
            }
        )
    }
    root.put("adminUsers", usersArr)

    val logsArr = JSONArray()
    telemetryLogs.forEach { log ->
        logsArr.put(
            JSONObject().apply {
                put("id", log.id)
                put("eventType", log.eventType)
                put("status", log.status)
                put("message", log.message)
                put("userEmail", log.userEmail)
                put("clipsCount", log.clipsCount)
                put("durationMs", log.durationMs)
            }
        )
    }
    root.put("telemetryLogs", logsArr)

    val renderMapObj = JSONObject()
    renderProgressMap.forEach { (clipId, pct) ->
        renderMapObj.put(clipId, pct)
    }
    root.put("renderProgressMap", renderMapObj)

    return root.toString()
}
