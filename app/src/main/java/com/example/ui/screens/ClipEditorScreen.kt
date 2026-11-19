package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.data.model.CaptionStyle
import com.example.data.model.ConnectedAccount
import com.example.data.model.GeneratedClip
import com.example.data.model.PlanTier
import com.example.data.model.PrivacyLevel
import com.example.data.model.SocialPlatform
import com.example.data.model.SubtitleLanguage
import com.example.data.model.TransitionStyle
import com.example.ui.components.AnimatedSubtitleOverlay
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.StatusPill
import com.example.ui.components.ViralScoreRing
import com.example.ui.components.formatDuration
import com.example.ui.theme.AmberViral
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.InstagramPink
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.YouTubeRed
import java.io.File

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClipEditorScreen(
    clip: GeneratedClip,
    planTier: PlanTier,
    connectedAccounts: List<ConnectedAccount>,
    renderProgress: Int?,
    isRegenerating: Boolean,
    onBack: () -> Unit,
    onSaveUpdates: (Map<String, Any>, String?) -> Unit,
    onRenderClip: () -> Unit,
    onAttachLocalVideoAndRender: (Uri) -> Unit,
    onRegenerateMetadata: (String) -> Unit,
    onCopyMetadata: (SocialPlatform) -> Unit,
    onPublishNow: (SocialPlatform, String, PrivacyLevel) -> Unit,
    onSchedulePublish: (SocialPlatform, String, PrivacyLevel, String) -> Unit,
    onDeleteClip: () -> Unit
) {
    BackHandler(onBack = onBack)

    var viralTitle by remember(clip.id, clip.viralTitle) { mutableStateOf(clip.viralTitle) }
    var seoDescription by remember(clip.id, clip.seoDescription) { mutableStateOf(clip.seoDescription) }
    var youtubeCaption by remember(clip.id, clip.youtubeCaption) { mutableStateOf(clip.youtubeCaption) }
    var instagramCaption by remember(clip.id, clip.instagramCaption) { mutableStateOf(clip.instagramCaption) }
    var activeLang by remember(clip.id, clip.subtitleLanguage) {
        mutableStateOf(SubtitleLanguage.fromRaw(clip.subtitleLanguage))
    }
    var activeCaptionStyle by remember(clip.id, clip.captionStyle) {
        mutableStateOf(CaptionStyle.fromRaw(clip.captionStyle))
    }
    var subtitleEn by remember(clip.id, clip.subtitleEn) { mutableStateOf(clip.subtitleEn) }
    var subtitleHi by remember(clip.id, clip.subtitleHi) { mutableStateOf(clip.subtitleHi) }
    var subtitleHinglish by remember(clip.id, clip.subtitleHinglish) { mutableStateOf(clip.subtitleHinglish) }

    var faceTracking by remember(clip.id, clip.faceTrackingEnabled) { mutableStateOf(clip.faceTrackingEnabled) }
    var speakerTracking by remember(clip.id, clip.speakerTrackingEnabled) { mutableStateOf(clip.speakerTrackingEnabled) }
    var dynamicZoom by remember(clip.id, clip.dynamicZoomEnabled) { mutableStateOf(clip.dynamicZoomEnabled) }
    var noiseReduction by remember(clip.id, clip.noiseReductionEnabled) { mutableStateOf(clip.noiseReductionEnabled) }
    var audioNormalized by remember(clip.id, clip.audioNormalized) { mutableStateOf(clip.audioNormalized) }
    var transitionStyle by remember(clip.id, clip.transitionStyle) {
        mutableStateOf(TransitionStyle.fromRaw(clip.transitionStyle))
    }
    var exportResolution by remember(clip.id, clip.exportResolution) { mutableStateOf(clip.exportResolution) }
    var exportFps by remember(clip.id, clip.exportFps) { mutableStateOf(clip.exportFps) }

    var isPreviewPlaying by remember { mutableStateOf(true) }
    var showPublishDialog by remember { mutableStateOf(false) }

    val activeSubtitleText = when (activeLang) {
        SubtitleLanguage.ENGLISH -> subtitleEn
        SubtitleLanguage.HINDI -> subtitleHi
        SubtitleLanguage.HINGLISH -> subtitleHinglish
    }

    val videoAttachPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onAttachLocalVideoAndRender(uri)
        }
    }

    // Determine playable local video source (rendered 9:16 MP4 takes precedence over raw sourceUri)
    val playableUri: Uri? = remember(clip.exportedFilePath, clip.sourceUri) {
        val rendered = clip.exportedFilePath.takeIf { it.isNotBlank() }?.let { File(it) }
        when {
            rendered != null && rendered.exists() -> Uri.fromFile(rendered)
            clip.sourceUri.isNotBlank() -> Uri.parse(clip.sourceUri)
            else -> null
        }
    }
    val isPlayingRenderedFile = remember(clip.exportedFilePath) {
        clip.exportedFilePath.isNotBlank() && File(clip.exportedFilePath).exists()
    }

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "9:16 Clip Editor",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = "${formatDuration((clip.startMs / 1000L).toInt())} → ${formatDuration((clip.endMs / 1000L).toInt())} • ${clip.exportResolution} ${clip.exportFps}fps",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftLavender
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onSaveUpdates(
                                mapOf(
                                    "viralTitle" to viralTitle.take(200).ifBlank { clip.viralTitle },
                                    "seoDescription" to seoDescription.take(2500),
                                    "youtubeCaption" to youtubeCaption.take(2000),
                                    "instagramCaption" to instagramCaption.take(2200),
                                    "subtitleLanguage" to activeLang.name,
                                    "captionStyle" to activeCaptionStyle.name,
                                    "subtitleEn" to subtitleEn.take(4000),
                                    "subtitleHi" to subtitleHi.take(4000),
                                    "subtitleHinglish" to subtitleHinglish.take(4000),
                                    "faceTrackingEnabled" to faceTracking,
                                    "speakerTrackingEnabled" to speakerTracking,
                                    "dynamicZoomEnabled" to dynamicZoom,
                                    "noiseReductionEnabled" to noiseReduction,
                                    "audioNormalized" to audioNormalized,
                                    "transitionStyle" to transitionStyle.name,
                                    "exportResolution" to exportResolution,
                                    "exportFps" to exportFps
                                ),
                                "Saved clip edits to cloud!"
                            )
                        },
                        modifier = Modifier.testTag("editor_save_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Clip Edits", tint = CyanSpark)
                    }
                    IconButton(
                        onClick = {
                            onDeleteClip()
                            onBack()
                        },
                        modifier = Modifier.testTag("editor_delete_button")
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete Clip", tint = CrimsonError)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Real 9:16 Vertical Video Viewport + Live Word-by-Word Animated Captions
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 270.dp)
                                .fillMaxWidth(0.76f)
                                .aspectRatio(9f / 16f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(ObsidianSurface)
                                .border(
                                    1.5.dp,
                                    Brush.verticalGradient(listOf(ElectricViolet, CyberMagenta)),
                                    RoundedCornerShape(24.dp)
                                )
                        ) {
                            if (playableUri != null) {
                                VerticalMedia3Player(
                                    uri = playableUri,
                                    startMs = if (isPlayingRenderedFile) 0L else clip.startMs,
                                    endMs = if (isPlayingRenderedFile) Long.MAX_VALUE else clip.endMs,
                                    isPlaying = isPreviewPlaying,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.img_studio_hero_1795069349084),
                                    contentDescription = "9:16 Studio Preview Canvas",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(0.7f)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    ObsidianBlack.copy(alpha = 0.25f),
                                                    ObsidianBlack.copy(alpha = 0.78f)
                                                )
                                            )
                                        )
                                )
                            }

                            // Top overlay badges
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                                    .align(Alignment.TopCenter),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPill(
                                    text = "9:16 • $exportResolution",
                                    color = if (isPlayingRenderedFile) EmeraldSuccess else CyanSpark
                                )
                                ViralScoreRing(score = clip.viralScore, size = 44.dp)
                            }

                            // Play/Pause floating control
                            IconButton(
                                onClick = { isPreviewPlaying = !isPreviewPlaying },
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(ObsidianBlack.copy(alpha = 0.55f))
                                    .testTag("preview_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isPreviewPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPreviewPlaying) "Pause preview" else "Play preview",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            // Live Word-by-Word Animated Subtitle Overlay
                            AnimatedSubtitleOverlay(
                                subtitleText = activeSubtitleText,
                                captionStyle = activeCaptionStyle,
                                isPlaying = isPreviewPlaying,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 12.dp, vertical = 22.dp)
                            )
                        }
                    }
                }

                // 2. Render 9:16 MP4 & Publish CTA Row
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), accentBorder = true) {
                        if (renderProgress != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Media3 Rendering 9:16 $exportResolution MP4...",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CyanSpark
                                )
                                Text(
                                    text = "$renderProgress%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CyanSpark
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (renderProgress / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                color = CyanSpark,
                                trackColor = ObsidianOutline
                            )
                            Spacer(Modifier.height(12.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (clip.sourceUri.isBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        videoAttachPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                        )
                                    },
                                    enabled = renderProgress == null,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, CyanSpark),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanSpark),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("attach_video_render_button")
                                ) {
                                    Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Attach & Render", maxLines = 1)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onRenderClip,
                                    enabled = renderProgress == null,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, CyanSpark),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanSpark),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("render_916_button")
                                ) {
                                    Icon(Icons.Filled.MovieFilter, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (isPlayingRenderedFile) "Re-Render 9:16" else "Render 9:16 MP4",
                                        maxLines = 1
                                    )
                                }
                            }

                            NeonGradientButton(
                                text = "Publish Clip",
                                icon = Icons.Filled.Publish,
                                tag = "open_publish_dialog_button",
                                onClick = { showPublishDialog = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. AI Subtitles (English / Hindi / Hinglish + Caption Styles)
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "AI Multilingual Subtitles & Animated Captions",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(10.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SubtitleLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = activeLang == lang,
                                    onClick = { activeLang = lang },
                                    label = { Text("${lang.badge} • ${lang.label}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricViolet.copy(alpha = 0.28f),
                                        selectedLabelColor = TextPrimary
                                    ),
                                    modifier = Modifier.testTag("editor_lang_${lang.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CaptionStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = activeCaptionStyle == style,
                                    onClick = { activeCaptionStyle = style },
                                    label = { Text(style.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberMagenta.copy(alpha = 0.25f),
                                        selectedLabelColor = TextPrimary
                                    ),
                                    modifier = Modifier.testTag("editor_style_${style.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = activeSubtitleText,
                            onValueChange = { updated ->
                                when (activeLang) {
                                    SubtitleLanguage.ENGLISH -> subtitleEn = updated
                                    SubtitleLanguage.HINDI -> subtitleHi = updated
                                    SubtitleLanguage.HINGLISH -> subtitleHinglish = updated
                                }
                            },
                            label = { Text("${activeLang.label} Synchronized Subtitle Script") },
                            minLines = 3,
                            maxLines = 6,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricViolet,
                                unfocusedBorderColor = ObsidianOutline,
                                focusedContainerColor = ObsidianSurface,
                                unfocusedContainerColor = ObsidianSurface,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_subtitle_input")
                        )
                    }
                }

                // 4. Automatic Video Editor Settings (9:16, 1080p/720p, 30/60 FPS, Face/Speaker Tracking, Zoom, Audio)
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Automatic Video Editing & Audio Mastering",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("720p", "1080p").forEach { res ->
                                val allowed = res == "720p" || planTier != PlanTier.FREE
                                FilterChip(
                                    selected = exportResolution == res,
                                    onClick = { if (allowed) exportResolution = res },
                                    enabled = allowed,
                                    label = { Text(if (res == "1080p") "1080x1920 FHD" else "720x1280 HD") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanSpark.copy(alpha = 0.24f),
                                        selectedLabelColor = TextPrimary
                                    )
                                )
                            }
                            listOf(30, 60).forEach { fps ->
                                val allowed = fps == 30 || planTier != PlanTier.FREE
                                FilterChip(
                                    selected = exportFps == fps,
                                    onClick = { if (allowed) exportFps = fps },
                                    enabled = allowed,
                                    label = { Text("$fps FPS") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricViolet.copy(alpha = 0.28f),
                                        selectedLabelColor = TextPrimary
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        EditorToggleRow("Smart Face Tracking (9:16 Center Lock)", faceTracking) { faceTracking = it }
                        EditorToggleRow("Automatic Speaker Tracking", speakerTracking) { speakerTracking = it }
                        EditorToggleRow("Dynamic Zoom Punch", dynamicZoom) { dynamicZoom = it }
                        EditorToggleRow("AI Noise Reduction", noiseReduction) { noiseReduction = it }
                        EditorToggleRow("Broadcast Audio Normalization (-14 LUFS)", audioNormalized) { audioNormalized = it }

                        Spacer(Modifier.height(8.dp))
                        Text("Transition Style", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TransitionStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = transitionStyle == style,
                                    onClick = { transitionStyle = style },
                                    label = { Text(style.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricViolet.copy(alpha = 0.26f),
                                        selectedLabelColor = TextPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // 5. AI Viral Title, SEO Description, Platform Captions & Thumbnail Concept
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AI Titles, SEO & Captions",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            OutlinedButton(
                                onClick = { onRegenerateMetadata("High-Retention Viral Curiosity Hook") },
                                enabled = !isRegenerating,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ElectricViolet),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("regenerate_ai_copy_button")
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = SoftLavender)
                                Spacer(Modifier.width(6.dp))
                                Text(if (isRegenerating) "Refreshing..." else "AI Remix", color = SoftLavender)
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        EditorTextField("Viral Short Title", viralTitle, { viralTitle = it }, singleLine = true)
                        Spacer(Modifier.height(10.dp))
                        EditorTextField("SEO Description", seoDescription, { seoDescription = it }, minLines = 2)
                        Spacer(Modifier.height(10.dp))
                        EditorTextField("YouTube Shorts Caption", youtubeCaption, { youtubeCaption = it }, minLines = 2)
                        Spacer(Modifier.height(10.dp))
                        EditorTextField("Instagram Reels Caption", instagramCaption, { instagramCaption = it }, minLines = 2)

                        if (clip.thumbnailConcept.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ObsidianSurface,
                                border = BorderStroke(1.dp, AmberViral.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Suggested Thumbnail Concept", style = MaterialTheme.typography.labelMedium, color = AmberViral)
                                    Spacer(Modifier.height(4.dp))
                                    Text(clip.thumbnailConcept, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                }
                            }
                        }

                        if (clip.hashtags.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text("Hashtags & Keywords", style = MaterialTheme.typography.labelMedium, color = SoftLavender)
                            Spacer(Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                clip.hashtags.forEach { tag ->
                                    StatusPill(text = tag, color = ElectricViolet)
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onCopyMetadata(SocialPlatform.YOUTUBE) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, YouTubeRed.copy(alpha = 0.7f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp), tint = YouTubeRed)
                                Spacer(Modifier.width(6.dp))
                                Text("Copy YT", color = TextPrimary)
                            }
                            OutlinedButton(
                                onClick = { onCopyMetadata(SocialPlatform.INSTAGRAM) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, InstagramPink.copy(alpha = 0.7f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp), tint = InstagramPink)
                                Spacer(Modifier.width(6.dp))
                                Text("Copy IG", color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPublishDialog) {
        PublishClipDialog(
            clipTitle = viralTitle,
            connectedAccounts = connectedAccounts,
            onDismiss = { showPublishDialog = false },
            onPublishNow = { platform, handle, privacy ->
                showPublishDialog = false
                onPublishNow(platform, handle, privacy)
            },
            onSchedule = { platform, handle, privacy, scheduleIso ->
                showPublishDialog = false
                onSchedulePublish(platform, handle, privacy, scheduleIso)
            }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VerticalMedia3Player(
    uri: Uri,
    startMs: Long,
    endMs: Long,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(uri, startMs, endMs) {
        ExoPlayer.Builder(context).build().apply {
            val builder = MediaItem.Builder().setUri(uri)
            if (endMs != Long.MAX_VALUE && endMs > startMs) {
                builder.setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(endMs)
                        .build()
                )
            }
            setMediaItem(builder.build())
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    DisposableEffect(exoPlayer, isPlaying) {
        exoPlayer.playWhenReady = isPlaying
        onDispose {}
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        update = { view ->
            if (view.player != exoPlayer) view.player = exoPlayer
        },
        modifier = modifier
    )
}

@Composable
private fun EditorToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = ElectricViolet)
        )
    }
}

@Composable
private fun EditorTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = false,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = if (singleLine) 1 else 5,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ElectricViolet,
            unfocusedBorderColor = ObsidianOutline,
            focusedContainerColor = ObsidianSurface,
            unfocusedContainerColor = ObsidianSurface,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PublishClipDialog(
    clipTitle: String,
    connectedAccounts: List<ConnectedAccount>,
    onDismiss: () -> Unit,
    onPublishNow: (SocialPlatform, String, PrivacyLevel) -> Unit,
    onSchedule: (SocialPlatform, String, PrivacyLevel, String) -> Unit
) {
    var selectedPlatform by remember { mutableStateOf(SocialPlatform.YOUTUBE) }
    var selectedPrivacy by remember { mutableStateOf(PrivacyLevel.PUBLIC) }
    var scheduleTime by remember { mutableStateOf("2026-11-20 18:00") }

    val matchingAccounts = connectedAccounts.filter { it.platform == selectedPlatform.name }
    val activeHandle = matchingAccounts.firstOrNull()?.handle ?: "@creator_${selectedPlatform.name.lowercase()}"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = "Publish Vertical Short",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = clipTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftLavender,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text("Destination Platform", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SocialPlatform.entries.forEach { plat ->
                        FilterChip(
                            selected = selectedPlatform == plat,
                            onClick = { selectedPlatform = plat },
                            label = { Text("${plat.displayName} ${plat.formatName}") }
                        )
                    }
                }

                Text(
                    text = "Target Account: $activeHandle",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldSuccess
                )

                Text("Privacy", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrivacyLevel.entries.forEach { level ->
                        FilterChip(
                            selected = selectedPrivacy == level,
                            onClick = { selectedPrivacy = level },
                            label = { Text(level.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = scheduleTime,
                    onValueChange = { scheduleTime = it },
                    label = { Text("Schedule Slot (Optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onSchedule(selectedPlatform, activeHandle, selectedPrivacy, scheduleTime) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Schedule")
                }
                OutlinedButton(
                    onClick = { onPublishNow(selectedPlatform, activeHandle, selectedPrivacy) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricViolet)
                ) {
                    Icon(Icons.Filled.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Publish Now")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
