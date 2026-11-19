package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CaptionStyle
import com.example.data.model.PlanTier
import com.example.data.model.SubtitleLanguage
import com.example.data.model.SystemConfig
import com.example.data.model.UserProfile
import com.example.data.model.VideoProject
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.PipelineProgressCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatDuration
import com.example.ui.theme.AmberViral
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PipelineUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    userProfile: UserProfile?,
    systemConfig: SystemConfig,
    pipelineState: PipelineUiState,
    projects: List<VideoProject>,
    onInspectYouTubeUrl: (String) -> Unit,
    onInspectLocalVideo: (Uri) -> Unit,
    onClearInspected: () -> Unit,
    onRunPipeline: (Int, String, SubtitleLanguage, CaptionStyle, Boolean) -> Unit,
    onOpenClipsTab: () -> Unit,
    onOpenPlansTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    var youtubeUrlInput by rememberSaveable { mutableStateOf("") }
    var creatorNotes by rememberSaveable { mutableStateOf("") }
    var selectedLanguage by rememberSaveable { mutableStateOf(SubtitleLanguage.HINGLISH.name) }
    var selectedCaptionStyle by rememberSaveable { mutableStateOf(CaptionStyle.NEON_POP.name) }
    var autoPublishOptIn by rememberSaveable { mutableStateOf(false) }

    val plan = PlanTier.fromRaw(userProfile?.planTier)
    val dailyLimit = when (plan) {
        PlanTier.FREE -> systemConfig.freeDailyLimit
        PlanTier.PRO -> systemConfig.proDailyLimit
        PlanTier.PREMIUM -> systemConfig.premiumDailyLimit
    }
    val usedToday = userProfile?.dailyClipsUsed ?: 0
    val maxSlider = (dailyLimit - usedToday).coerceIn(1, 20)
    var clipCountSlider by rememberSaveable(maxSlider) {
        mutableFloatStateOf(minOf(3, maxSlider).toFloat())
    }

    // Zero-permission Android Photo Picker for authorized local video files
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onInspectLocalVideo(uri)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Studio Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(ObsidianSurfaceElevated)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_hero_1795069349084),
                        contentDescription = "ClipGenius AI Studio header background",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.50f)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        ObsidianBlack.copy(alpha = 0.88f),
                                        ObsidianBlack.copy(alpha = 0.55f)
                                    )
                                )
                            )
                    )
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_clipgenius_logo_1795069348797),
                                    contentDescription = "ClipGenius AI logo",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "ClipGenius AI",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Welcome, ${userProfile?.displayName?.ifBlank { "Creator" } ?: "Creator"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SoftLavender
                                    )
                                }
                            }
                            Surface(
                                onClick = onOpenPlansTab,
                                shape = RoundedCornerShape(50),
                                color = ElectricViolet.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, ElectricViolet),
                                modifier = Modifier.testTag("studio_plan_badge")
                            ) {
                                Text(
                                    text = "${plan.displayName.uppercase()} • ${plan.maxResolution}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Daily AI Clip Quota",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "$usedToday / $dailyLimit clips used",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (usedToday >= dailyLimit) CrimsonError else CyanSpark
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (usedToday.toFloat() / dailyLimit.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = if (usedToday >= dailyLimit) CrimsonError else ElectricViolet,
                            trackColor = ObsidianOutline
                        )
                    }
                }
            }

            // 2. Video Input Card (YouTube URL + Direct Authorized Video Upload)
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), accentBorder = true) {
                    Text(
                        text = "1. Paste Video Link or Pick Authorized Video",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Paste a public YouTube URL for instant Gemini analysis, or select an authorized device video for full on-device 9:16 Media3 HD rendering.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = youtubeUrlInput,
                        onValueChange = { youtubeUrlInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("youtube_url_input"),
                        label = { Text("YouTube Video or Shorts URL") },
                        placeholder = { Text("https://www.youtube.com/watch?v=...") },
                        leadingIcon = {
                            Icon(Icons.Filled.Link, contentDescription = null, tint = ElectricViolet)
                        },
                        trailingIcon = {
                            if (youtubeUrlInput.isNotBlank()) {
                                IconButton(onClick = { youtubeUrlInput = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear URL", tint = TextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = ObsidianOutline,
                            focusedContainerColor = ObsidianSurface,
                            unfocusedContainerColor = ObsidianSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onInspectYouTubeUrl(youtubeUrlInput) },
                            enabled = !pipelineState.isInspecting && !pipelineState.isProcessing,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ElectricViolet),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftLavender),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("validate_url_button")
                        ) {
                            if (pipelineState.isInspecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = SoftLavender
                                )
                            } else {
                                Icon(Icons.Filled.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Validate URL", maxLines = 1)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            enabled = !pipelineState.isInspecting && !pipelineState.isProcessing,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CyberMagenta.copy(alpha = 0.8f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF9A8D4)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pick_video_button")
                        ) {
                            Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Upload Video", maxLines = 1)
                        }
                    }

                    // Error banner if URL validation or pipeline fails
                    AnimatedVisibility(visible = !pipelineState.errorMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CrimsonError.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = CrimsonError)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = pipelineState.errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CrimsonError
                                )
                            }
                        }
                    }

                    // Inspected Video Preview Card
                    val inspected = pipelineState.inspectedVideo
                    AnimatedVisibility(visible = inspected != null) {
                        if (inspected != null) {
                            Spacer(Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ObsidianSurface,
                                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 88.dp, height = 56.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(ObsidianSurfaceElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val firstFrame = inspected.previewFrames.firstOrNull()
                                        when {
                                            firstFrame != null -> Image(
                                                bitmap = firstFrame.asImageBitmap(),
                                                contentDescription = "Detected video frame preview",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            inspected.thumbnailUrl.isNotBlank() -> AsyncImage(
                                                model = inspected.thumbnailUrl,
                                                contentDescription = "YouTube thumbnail preview",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            else -> Icon(
                                                Icons.Filled.VideoLibrary,
                                                contentDescription = null,
                                                tint = ElectricViolet
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = inspected.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = "${inspected.channelName} • ${formatDuration(inspected.durationSeconds)} • ${inspected.resolutionLabel}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldSuccess
                                        )
                                    }
                                    IconButton(onClick = onClearInspected) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove video", tint = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. AI Clipping, Subtitles & Auto-Publish Config Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "2. AI Clipping & Multilingual Subtitle Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto-Generate Viral Clips",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        StatusPill(
                            text = "${clipCountSlider.roundToInt()} Clips (9:16 ${plan.maxResolution})",
                            color = ElectricViolet
                        )
                    }
                    Slider(
                        value = clipCountSlider.coerceIn(1f, maxSlider.toFloat()),
                        onValueChange = { clipCountSlider = it },
                        valueRange = 1f..maxSlider.coerceAtLeast(1).toFloat(),
                        steps = (maxSlider - 2).coerceAtLeast(0),
                        colors = SliderDefaults.colors(
                            thumbColor = CyberMagenta,
                            activeTrackColor = ElectricViolet,
                            inactiveTrackColor = ObsidianOutline
                        ),
                        modifier = Modifier.testTag("clip_count_slider")
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Primary Subtitle Language (All 3 generated automatically)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SubtitleLanguage.entries.forEach { lang ->
                            val selected = selectedLanguage == lang.name
                            FilterChip(
                                selected = selected,
                                onClick = { selectedLanguage = lang.name },
                                label = { Text("${lang.badge} • ${lang.label}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricViolet.copy(alpha = 0.28f),
                                    selectedLabelColor = TextPrimary
                                ),
                                modifier = Modifier.testTag("lang_chip_${lang.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Animated Caption Style",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CaptionStyle.entries.forEach { style ->
                            val selected = selectedCaptionStyle == style.name
                            FilterChip(
                                selected = selected,
                                onClick = { selectedCaptionStyle = style.name },
                                label = { Text(style.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberMagenta.copy(alpha = 0.25f),
                                    selectedLabelColor = TextPrimary
                                ),
                                modifier = Modifier.testTag("caption_style_${style.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = creatorNotes,
                        onValueChange = { creatorNotes = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("creator_notes_input"),
                        label = { Text("Optional Focus / Context (Topic, Hooks, Target Audience)") },
                        placeholder = { Text("e.g., Focus on funny reactions, business tips, or Hindi podcast quotes") },
                        maxLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = ObsidianOutline,
                            focusedContainerColor = ObsidianSurface,
                            unfocusedContainerColor = ObsidianSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Queue Approved Clips to Connected Accounts",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = if (plan.supportsAutoPublish) {
                                    "Explicitly enabled for your connected YouTube/Instagram accounts."
                                } else {
                                    "Requires Pro or Premium plan for automatic publishing queue."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = autoPublishOptIn && plan.supportsAutoPublish,
                            onCheckedChange = { if (plan.supportsAutoPublish) autoPublishOptIn = it },
                            enabled = plan.supportsAutoPublish,
                            colors = SwitchDefaults.colors(checkedTrackColor = ElectricViolet),
                            modifier = Modifier.testTag("auto_publish_switch")
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    NeonGradientButton(
                        text = if (pipelineState.isProcessing) "AI Pipeline Running..." else "Analyze & Auto-Generate Clips",
                        icon = Icons.Filled.AutoAwesome,
                        isLoading = pipelineState.isProcessing,
                        enabled = pipelineState.inspectedVideo != null && !pipelineState.isProcessing,
                        tag = "start_ai_pipeline_button",
                        onClick = {
                            onRunPipeline(
                                clipCountSlider.roundToInt(),
                                creatorNotes,
                                SubtitleLanguage.fromRaw(selectedLanguage),
                                CaptionStyle.fromRaw(selectedCaptionStyle),
                                autoPublishOptIn
                            )
                        }
                    )
                }
            }

            // 4. Live Processing Pipeline Card
            if (pipelineState.isProcessing || pipelineState.lastGeneratedClipCount > 0) {
                item {
                    if (pipelineState.isProcessing) {
                        PipelineProgressCard(
                            stepLabel = pipelineState.currentStepLabel,
                            progressPercent = pipelineState.progressPercent
                        )
                    } else if (pipelineState.lastGeneratedClipCount > 0) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            accentBorder = true,
                            onClick = onOpenClipsTab
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "${pipelineState.lastGeneratedClipCount} Viral Clips Generated!",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Tap to open the 9:16 Clip Studio, customize subtitles & publish.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SoftLavender
                                        )
                                    }
                                }
                                StatusPill(text = "OPEN", color = EmeraldSuccess)
                            }
                        }
                    }
                }
            }

            // 5. Recent Video Projects
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Video Projects (${projects.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    if (projects.isNotEmpty()) {
                        StatusPill(text = "CLOUD SYNCED", color = CyanSpark)
                    }
                }
            }

            if (projects.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.ContentCut,
                                contentDescription = null,
                                tint = ElectricViolet,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Ready for Your First Viral Video",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Paste a YouTube link or upload a video above. Gemini AI will transcribe, detect emotional peaks, and cut 9:16 shorts automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            } else {
                items(projects, key = { it.id }) { project ->
                    ProjectHistoryCard(project = project, onOpenClips = onOpenClipsTab)
                }
            }
        }
    }
}

@Composable
private fun ProjectHistoryCard(
    project: VideoProject,
    onOpenClips: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val statusColor = when (project.status) {
        "READY" -> EmeraldSuccess
        "FAILED" -> CrimsonError
        else -> AmberViral
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_card_${project.id}"),
        onClick = { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (project.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = project.thumbnailUrl,
                    contentDescription = project.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 74.dp, height = 52.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${project.channelName} • ${formatDuration(project.durationSeconds)} • ${project.clipCount} clips",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusPill(text = project.status, color = statusColor)
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                if (project.summary.isNotBlank()) {
                    Text(
                        text = "AI Summary",
                        style = MaterialTheme.typography.labelMedium,
                        color = SoftLavender
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = project.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
                if (project.transcript.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Transcript Preview",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanSpark
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = project.transcript.take(320) + if (project.transcript.length > 320) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                if (project.clipCount > 0) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenClips,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ElectricViolet),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Inspect ${project.clipCount} Generated Clips", color = SoftLavender)
                    }
                }
            }
        }
    }
}
