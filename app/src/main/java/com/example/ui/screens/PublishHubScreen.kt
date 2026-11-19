package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ConnectedAccount
import com.example.data.model.PlanTier
import com.example.data.model.PrivacyLevel
import com.example.data.model.PublishJob
import com.example.data.model.SocialPlatform
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberViral
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.InstagramPink
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.YouTubeRed

@Composable
fun PublishHubScreen(
    planTier: PlanTier,
    accounts: List<ConnectedAccount>,
    publishJobs: List<PublishJob>,
    onConnectAccount: (SocialPlatform, String, String, String, PrivacyLevel, Boolean) -> Unit,
    onUpdateAccount: (String, PrivacyLevel, Boolean) -> Unit,
    onDisconnectAccount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showConnectDialog by remember { mutableStateOf(false) }
    var preselectedPlatform by remember { mutableStateOf(SocialPlatform.YOUTUBE) }
    var jobFilter by rememberSaveable { mutableStateOf("ALL") }

    val filteredJobs = remember(publishJobs, jobFilter) {
        when (jobFilter) {
            "SCHEDULED" -> publishJobs.filter { it.status == "SCHEDULED" || it.status == "QUEUED" }
            "PUBLISHED" -> publishJobs.filter { it.status == "PUBLISHED" }
            "FAILED" -> publishJobs.filter { it.status == "FAILED" }
            else -> publishJobs
        }
    }

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
                    text = "Social Publishing Hub",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Connect multiple YouTube & Instagram accounts securely via OAuth metadata (zero passwords stored) and manage Shorts & Reels uploads.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Security Guarantee Notice
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EmeraldSuccess.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = EmeraldSuccess)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Official OAuth & Android Native Upload Handoff: Passwords are never requested or stored. Copyright and platform policies are strictly enforced.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Connect Buttons Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            preselectedPlatform = SocialPlatform.YOUTUBE
                            showConnectDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, YouTubeRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("connect_youtube_button")
                    ) {
                        Icon(Icons.Filled.AddLink, contentDescription = null, tint = YouTubeRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Link YouTube", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            preselectedPlatform = SocialPlatform.INSTAGRAM
                            showConnectDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, InstagramPink),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("connect_instagram_button")
                    ) {
                        Icon(Icons.Filled.AddLink, contentDescription = null, tint = InstagramPink, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Link Instagram", maxLines = 1)
                    }
                }
            }

            // Connected Accounts Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Connected Accounts (${accounts.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    StatusPill(
                        text = if (planTier == PlanTier.PREMIUM) "MULTI-ACCOUNT" else "${planTier.displayName.uppercase()}",
                        color = ElectricViolet
                    )
                }
            }

            if (accounts.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No Social Channels Connected",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Link a YouTube channel or Instagram Creator/Business handle above to configure default privacy and automatic publishing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                items(accounts, key = { it.id }) { account ->
                    val platform = SocialPlatform.fromRaw(account.platform)
                    val accent = if (platform == SocialPlatform.YOUTUBE) YouTubeRed else InstagramPink
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("social_account_card_${account.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusPill(text = "${platform.displayName} ${platform.formatName}", color = accent)
                                    Spacer(Modifier.width(8.dp))
                                    StatusPill(text = account.privacyDefault, color = CyanSpark)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = account.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = account.handle + if (account.channelOrAccountId.isNotBlank()) " • ID: ${account.channelOrAccountId}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            IconButton(onClick = { onDisconnectAccount(account.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Disconnect account", tint = CrimsonError)
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Auto-Publish Approved Clips",
                                style = MaterialTheme.typography.bodySmall,
                                color = SoftLavender
                            )
                            Switch(
                                checked = account.autoPublish,
                                onCheckedChange = { enabled ->
                                    onUpdateAccount(account.id, PrivacyLevel.fromRaw(account.privacyDefault), enabled)
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = ElectricViolet)
                            )
                        }
                    }
                }
            }

            // Publishing Queue & Upload History
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Publishing Queue & History (${publishJobs.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL" to "All", "SCHEDULED" to "Scheduled", "PUBLISHED" to "Published", "FAILED" to "Failed")
                        .forEach { (key, label) ->
                            FilterChip(
                                selected = jobFilter == key,
                                onClick = { jobFilter = key },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricViolet.copy(alpha = 0.28f),
                                    selectedLabelColor = TextPrimary
                                )
                            )
                        }
                }
            }

            if (filteredJobs.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No Publishing Jobs Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Open any clip in the Clips tab and tap Publish Clip to share directly to YouTube Shorts or Instagram Reels, or schedule a release slot.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                items(filteredJobs, key = { it.id }) { job ->
                    val platform = SocialPlatform.fromRaw(job.platform)
                    val accent = if (platform == SocialPlatform.YOUTUBE) YouTubeRed else InstagramPink
                    val statusColor = when (job.status) {
                        "PUBLISHED" -> EmeraldSuccess
                        "SCHEDULED" -> ElectricViolet
                        "FAILED" -> CrimsonError
                        else -> AmberViral
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (job.status == "PUBLISHED") Icons.Filled.CheckCircle else if (job.status == "SCHEDULED") Icons.Filled.Schedule else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                StatusPill(text = platform.displayName, color = accent)
                            }
                            StatusPill(text = job.status, color = statusColor)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = job.clipTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${job.accountHandle} • Privacy: ${job.privacy}" +
                                if (job.scheduledFor.isNotBlank()) " • Scheduled: ${job.scheduledFor}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        if (job.errorMessage.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = job.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = CrimsonError
                            )
                        }
                    }
                }
            }
        }
    }

    if (showConnectDialog) {
        ConnectAccountDialog(
            initialPlatform = preselectedPlatform,
            onDismiss = { showConnectDialog = false },
            onConfirm = { platform, handle, displayName, channelId, privacy, autoPublish ->
                showConnectDialog = false
                onConnectAccount(platform, handle, displayName, channelId, privacy, autoPublish)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConnectAccountDialog(
    initialPlatform: SocialPlatform,
    onDismiss: () -> Unit,
    onConfirm: (SocialPlatform, String, String, String, PrivacyLevel, Boolean) -> Unit
) {
    var platform by remember { mutableStateOf(initialPlatform) }
    var handle by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var channelId by remember { mutableStateOf("") }
    var privacy by remember { mutableStateOf(PrivacyLevel.PUBLIC) }
    var autoPublish by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = { Text("Link ${platform.displayName} Account", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SocialPlatform.entries.forEach { p ->
                        FilterChip(
                            selected = platform == p,
                            onClick = { platform = p },
                            label = { Text(p.displayName) }
                        )
                    }
                }
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text(if (platform == SocialPlatform.YOUTUBE) "Channel Handle (@yourchannel)" else "Instagram Handle (@yourhandle)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = channelId,
                    onValueChange = { channelId = it },
                    label = { Text(if (platform == SocialPlatform.YOUTUBE) "YouTube Channel ID (Optional)" else "IG Business Account ID (Optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrivacyLevel.entries.forEach { p ->
                        FilterChip(
                            selected = privacy == p,
                            onClick = { privacy = p },
                            label = { Text(p.label) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Auto-Publish Opt-In", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    Switch(checked = autoPublish, onCheckedChange = { autoPublish = it })
                }
            }
        },
        confirmButton = {
            NeonGradientButton(
                text = "Link Account",
                enabled = handle.isNotBlank(),
                tag = "confirm_connect_account_button",
                onClick = { onConfirm(platform, handle, displayName, channelId, privacy, autoPublish) },
                modifier = Modifier.width(150.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
