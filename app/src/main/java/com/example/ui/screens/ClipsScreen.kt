package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.GeneratedClip
import com.example.data.model.SubtitleLanguage
import com.example.ui.components.GlassCard
import com.example.ui.components.MetricGlassTile
import com.example.ui.components.StatusPill
import com.example.ui.components.ViralScoreRing
import com.example.ui.components.formatDuration
import com.example.ui.theme.AmberViral
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ClipsScreen(
    clips: List<GeneratedClip>,
    renderProgressMap: Map<String, Int>,
    onOpenClipEditor: (String) -> Unit,
    onGoToStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("ALL") }

    val filteredClips = remember(clips, searchQuery, selectedFilter) {
        clips.filter { clip ->
            val matchesQuery = searchQuery.isBlank() ||
                clip.viralTitle.contains(searchQuery, ignoreCase = true) ||
                clip.projectTitle.contains(searchQuery, ignoreCase = true) ||
                clip.emotionTag.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "RENDERED" -> clip.renderStatus == "RENDERED"
                "PUBLISHED" -> clip.publishStatus == "PUBLISHED"
                "TOP_VIRAL" -> clip.viralScore >= 90
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    val renderedCount = clips.count { it.renderStatus == "RENDERED" }
    val publishedCount = clips.count { it.publishStatus == "PUBLISHED" }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Viral Clips Library",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tap any AI-generated short to preview 9:16 vertical playback, edit EN/HI/Hinglish subtitles, render Full HD MP4, or publish.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricGlassTile(
                        label = "Total Clips",
                        value = "${clips.size}",
                        accent = ElectricViolet,
                        icon = Icons.Filled.AutoAwesome,
                        modifier = Modifier.weight(1f)
                    )
                    MetricGlassTile(
                        label = "9:16 Rendered",
                        value = "$renderedCount",
                        accent = CyanSpark,
                        icon = Icons.Filled.MovieCreation,
                        modifier = Modifier.weight(1f)
                    )
                    MetricGlassTile(
                        label = "Published",
                        value = "$publishedCount",
                        accent = EmeraldSuccess,
                        icon = Icons.Filled.Publish,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clips_search_input"),
                    placeholder = { Text("Search viral title, project, or emotion tag...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ElectricViolet) },
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
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filters = listOf(
                        "ALL" to "All (${clips.size})",
                        "TOP_VIRAL" to "90+ Viral",
                        "RENDERED" to "9:16 Rendered",
                        "PUBLISHED" to "Published"
                    )
                    items(filters) { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricViolet.copy(alpha = 0.28f),
                                selectedLabelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("clips_filter_${key.lowercase()}")
                        )
                    }
                }
            }

            if (filteredClips.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.MovieCreation,
                                contentDescription = null,
                                tint = ElectricViolet,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = if (clips.isEmpty()) "No Clips Generated Yet" else "No Matching Clips",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Paste a YouTube link or upload an authorized video in Studio to generate viral vertical shorts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = onGoToStudio,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ElectricViolet)
                            ) {
                                Text("Go to Studio", color = SoftLavender)
                            }
                        }
                    }
                }
            } else {
                items(filteredClips, key = { it.id }) { clip ->
                    ClipListCard(
                        clip = clip,
                        renderProgress = renderProgressMap[clip.id],
                        onClick = { onOpenClipEditor(clip.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClipListCard(
    clip: GeneratedClip,
    renderProgress: Int?,
    onClick: () -> Unit
) {
    val lang = SubtitleLanguage.fromRaw(clip.subtitleLanguage)
    val renderColor = when (clip.renderStatus) {
        "RENDERED" -> EmeraldSuccess
        "RENDERING" -> CyanSpark
        "FAILED" -> CrimsonError
        else -> AmberViral
    }
    val publishColor = when (clip.publishStatus) {
        "PUBLISHED" -> EmeraldSuccess
        "SCHEDULED" -> ElectricViolet
        "FAILED" -> CrimsonError
        else -> TextSecondary
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("clip_card_${clip.id}"),
        accentBorder = clip.viralScore >= 90,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ViralScoreRing(score = clip.viralScore)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clip.viralTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${formatDuration((clip.startMs / 1000L).toInt())} → ${formatDuration((clip.endMs / 1000L).toInt())} (${clip.durationSeconds()}s) • ${clip.exportResolution} ${clip.exportFps}fps",
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftLavender
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = clip.hookReason,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Subtitles,
                contentDescription = null,
                tint = CyberMagenta,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = clip.activeSubtitleText().ifBlank { "Multilingual captions ready" },
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (renderProgress != null) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (renderProgress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = CyanSpark,
                trackColor = ObsidianOutline
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(text = lang.badge, color = CyberMagenta)
                StatusPill(text = clip.emotionTag.take(12), color = ElectricViolet)
                StatusPill(text = clip.renderStatus, color = renderColor)
                if (clip.publishStatus != "DRAFT") {
                    StatusPill(text = clip.publishStatus, color = publishColor)
                }
            }
            OutlinedButton(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftLavender),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("edit_clip_btn_${clip.id}")
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Studio", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
