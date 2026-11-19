package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material.icons.outlined.Publish
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.PlanTier
import com.example.data.repository.ClipGeniusRepository
import com.example.data.service.GeminiVideoAiService
import com.example.data.service.SocialPublisherService
import com.example.data.service.VideoProcessorService
import com.example.data.service.YouTubeMetadataService
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.firebase.signOutUser
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ClipEditorScreen
import com.example.ui.screens.ClipsScreen
import com.example.ui.screens.PlansAndAdminScreen
import com.example.ui.screens.PublishHubScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.ClipGeniusTheme
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ClipGeniusViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // CRITICAL: Configure Firebase App Check with launch intent BEFORE UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            ClipGeniusTheme {
                ClipGeniusRoot()
            }
        }
    }
}

private enum class MainTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    STUDIO("studio", "Studio", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    CLIPS("clips", "Clips", Icons.Filled.MovieFilter, Icons.Outlined.MovieFilter),
    PUBLISH("publish", "Publish", Icons.Filled.Publish, Icons.Outlined.Publish),
    PLANS("plans", "Plans", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings)
}

@Composable
fun ClipGeniusRoot() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedClipGeniusShell(user = user)
        }
    }
}

@Composable
private fun AuthenticatedClipGeniusShell(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: ClipGeniusViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ClipGeniusViewModel(
                    repository = ClipGeniusRepository(db),
                    metadataService = YouTubeMetadataService(app),
                    aiService = GeminiVideoAiService(),
                    videoProcessor = VideoProcessorService(app),
                    socialPublisher = SocialPublisherService(app),
                    currentUserId = user.uid,
                    userDisplayName = user.displayName ?: "Creator",
                    userEmail = user.email.orEmpty(),
                    userPhotoUrl = user.photoUrl?.toString()
                )
            }
        }
    )

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
    val regeneratingClipId by viewModel.regeneratingClipId.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    var selectedTab by rememberSaveable { mutableStateOf(MainTab.STUDIO) }
    var activeEditorClipId by rememberSaveable { mutableStateOf<String?>(null) }

    val editorClip = remember(activeEditorClipId, clips) {
        activeEditorClipId?.let { id -> clips.firstOrNull { it.id == id } }
    }

    if (editorClip != null) {
        ClipEditorScreen(
            clip = editorClip,
            planTier = PlanTier.fromRaw(userProfile?.planTier),
            connectedAccounts = socialAccounts,
            renderProgress = renderProgressMap[editorClip.id],
            isRegenerating = regeneratingClipId == editorClip.id,
            onBack = { activeEditorClipId = null },
            onSaveUpdates = { map, notice -> viewModel.updateClip(editorClip.id, map, notice) },
            onRenderClip = { viewModel.renderClipWithMedia3(editorClip) },
            onAttachLocalVideoAndRender = { uri -> viewModel.attachLocalSourceAndRender(editorClip, uri) },
            onRegenerateMetadata = { tone -> viewModel.regenerateClipMetadata(editorClip, tone) },
            onCopyMetadata = { platform -> viewModel.copyClipMetadata(editorClip, platform) },
            onPublishNow = { platform, handle, privacy ->
                viewModel.publishClipNow(editorClip, platform, handle, privacy)
            },
            onSchedulePublish = { platform, handle, privacy, scheduleIso ->
                viewModel.scheduleClipPublish(editorClip, platform, handle, privacy, scheduleIso)
            },
            onDeleteClip = { viewModel.deleteClip(editorClip.id) }
        )
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        val useRail = maxWidth >= 720.dp

        Scaffold(
            containerColor = ObsidianBlack,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = ObsidianSurfaceElevated,
                        contentColor = TextPrimary
                    )
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        MainTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            val labelText = if (tab == MainTab.PLANS && viewModel.isAdmin) "Admin" else tab.label
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = labelText
                                    )
                                },
                                label = { Text(labelText, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TextPrimary,
                                    selectedTextColor = SoftLavender,
                                    indicatorColor = ElectricViolet.copy(alpha = 0.28f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(containerColor = ObsidianSurface) {
                        MainTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            val labelText = if (tab == MainTab.PLANS && viewModel.isAdmin) "Admin" else tab.label
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = labelText
                                    )
                                },
                                label = { Text(labelText, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = TextPrimary,
                                    selectedTextColor = SoftLavender,
                                    indicatorColor = ElectricViolet.copy(alpha = 0.28f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                when (selectedTab) {
                    MainTab.STUDIO -> StudioScreen(
                        userProfile = userProfile,
                        systemConfig = systemConfig,
                        pipelineState = pipelineState,
                        projects = projects,
                        onInspectYouTubeUrl = viewModel::inspectYouTubeUrl,
                        onInspectLocalVideo = viewModel::inspectLocalVideo,
                        onClearInspected = viewModel::clearInspectedVideo,
                        onRunPipeline = viewModel::runAutomaticClippingWorkflow,
                        onOpenClipsTab = { selectedTab = MainTab.CLIPS },
                        onOpenPlansTab = { selectedTab = MainTab.PLANS },
                        modifier = Modifier.weight(1f)
                    )

                    MainTab.CLIPS -> ClipsScreen(
                        clips = clips,
                        renderProgressMap = renderProgressMap,
                        onOpenClipEditor = { clipId -> activeEditorClipId = clipId },
                        onGoToStudio = { selectedTab = MainTab.STUDIO },
                        modifier = Modifier.weight(1f)
                    )

                    MainTab.PUBLISH -> PublishHubScreen(
                        planTier = PlanTier.fromRaw(userProfile?.planTier),
                        accounts = socialAccounts,
                        publishJobs = publishJobs,
                        onConnectAccount = viewModel::connectSocialAccount,
                        onUpdateAccount = viewModel::updateSocialAccount,
                        onDisconnectAccount = viewModel::disconnectSocialAccount,
                        modifier = Modifier.weight(1f)
                    )

                    MainTab.PLANS -> PlansAndAdminScreen(
                        userProfile = userProfile,
                        isAdmin = viewModel.isAdmin,
                        systemConfig = systemConfig,
                        adminUsers = adminUsers,
                        telemetryLogs = telemetryLogs,
                        onSwitchPlan = viewModel::switchSubscriptionPlan,
                        onAdminUpdateUserPlan = viewModel::adminUpdateUserPlan,
                        onAdminSaveConfig = viewModel::adminSaveSystemConfig,
                        onSignOut = {
                            signOutUser(credentialManager = credentialManager, scope = scope)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
