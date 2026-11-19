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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.PlanTier
import com.example.data.model.ProcessingLog
import com.example.data.model.SystemConfig
import com.example.data.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.components.MetricGlassTile
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberViral
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun PlansAndAdminScreen(
    userProfile: UserProfile?,
    isAdmin: Boolean,
    systemConfig: SystemConfig,
    adminUsers: List<UserProfile>,
    telemetryLogs: List<ProcessingLog>,
    onSwitchPlan: (PlanTier) -> Unit,
    onAdminUpdateUserPlan: (String, PlanTier) -> Unit,
    onAdminSaveConfig: (SystemConfig) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSection by rememberSaveable { mutableStateOf("PLANS") }
    var logFilter by rememberSaveable { mutableStateOf("ALL") }

    val activePlan = PlanTier.fromRaw(userProfile?.planTier)

    // Editable Admin Config fields synced from live SystemConfig
    var freeLimitInput by remember(systemConfig.freeDailyLimit) { mutableStateOf(systemConfig.freeDailyLimit.toString()) }
    var proLimitInput by remember(systemConfig.proDailyLimit) { mutableStateOf(systemConfig.proDailyLimit.toString()) }
    var premiumLimitInput by remember(systemConfig.premiumDailyLimit) { mutableStateOf(systemConfig.premiumDailyLimit.toString()) }
    var maxDurationInput by remember(systemConfig.maxVideoDurationMinutes) { mutableStateOf(systemConfig.maxVideoDurationMinutes.toString()) }
    var storageQuotaInput by remember(systemConfig.storageQuotaGb) { mutableStateOf(systemConfig.storageQuotaGb.toString()) }
    var aiModelInput by remember(systemConfig.aiModelName) { mutableStateOf(systemConfig.aiModelName) }
    var globalAutoPublish by remember(systemConfig.autoPublishGlobalEnabled) { mutableStateOf(systemConfig.autoPublishGlobalEnabled) }

    val filteredLogs = remember(telemetryLogs, logFilter) {
        when (logFilter) {
            "FAILED" -> telemetryLogs.filter { it.status == "FAILED" }
            "AI_ANALYSIS" -> telemetryLogs.filter { it.eventType == "AI_ANALYSIS" }
            "SOCIAL_PUBLISH" -> telemetryLogs.filter { it.eventType == "SOCIAL_PUBLISH" }
            else -> telemetryLogs
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (activeSection == "ADMIN") "Admin Control Panel" else "Subscription & Studio",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = userProfile?.email.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftLavender
                        )
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError),
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sign Out")
                    }
                }
            }

            // Switcher between Plans & Admin / Telemetry
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = activeSection == "PLANS",
                        onClick = { activeSection = "PLANS" },
                        leadingIcon = { Icon(Icons.Filled.WorkspacePremium, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Plans & Usage") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricViolet.copy(alpha = 0.28f),
                            selectedLabelColor = TextPrimary
                        ),
                        modifier = Modifier.testTag("section_plans_chip")
                    )
                    FilterChip(
                        selected = activeSection == "ADMIN",
                        onClick = { activeSection = "ADMIN" },
                        leadingIcon = { Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(if (isAdmin) "Admin Dashboard" else "Activity & Telemetry") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberMagenta.copy(alpha = 0.28f),
                            selectedLabelColor = TextPrimary
                        ),
                        modifier = Modifier.testTag("section_admin_chip")
                    )
                }
            }

            if (activeSection == "PLANS") {
                // Creator Metrics
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricGlassTile(
                            label = "Clips Created",
                            value = "${userProfile?.totalClipsGenerated ?: 0}",
                            accent = ElectricViolet,
                            icon = Icons.Filled.Analytics,
                            modifier = Modifier.weight(1f)
                        )
                        MetricGlassTile(
                            label = "Published",
                            value = "${userProfile?.totalPublished ?: 0}",
                            accent = EmeraldSuccess,
                            icon = Icons.Filled.CheckCircle,
                            modifier = Modifier.weight(1f)
                        )
                        MetricGlassTile(
                            label = "Storage MB",
                            value = String.format(Locale.US, "%.1f", userProfile?.storageUsedMb ?: 0.0),
                            accent = CyanSpark,
                            icon = Icons.Filled.CloudQueue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 3 Subscription Tier Cards
                items(PlanTier.entries) { tier ->
                    val isCurrent = tier == activePlan
                    val limitForTier = when (tier) {
                        PlanTier.FREE -> systemConfig.freeDailyLimit
                        PlanTier.PRO -> systemConfig.proDailyLimit
                        PlanTier.PREMIUM -> systemConfig.premiumDailyLimit
                    }
                    val features = when (tier) {
                        PlanTier.FREE -> listOf(
                            "$limitForTier AI clips per day",
                            "720p HD vertical 9:16 export (30 FPS)",
                            "English, Hindi & Hinglish AI subtitles",
                            "Viral titles, SEO descriptions & hashtags"
                        )
                        PlanTier.PRO -> listOf(
                            "$limitForTier AI clips per day",
                            "1080x1920 Full HD 60 FPS export",
                            "Automatic social publishing queue",
                            "Smart face tracking, zoom & audio mastering"
                        )
                        PlanTier.PREMIUM -> listOf(
                            "$limitForTier AI clips per day (Bulk Clipping)",
                            "Unlimited connected YouTube & Instagram accounts",
                            "Priority Gemini AI processing & advanced editing",
                            "1080x1920 Full HD 60 FPS + scheduled releases"
                        )
                    }

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plan_card_${tier.name.lowercase()}"),
                        accentBorder = isCurrent
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${tier.displayName} Plan",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    text = tier.priceMonthlyLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (tier == PlanTier.PREMIUM) CyberMagenta else CyanSpark
                                )
                            }
                            if (isCurrent) {
                                StatusPill(text = "ACTIVE", color = EmeraldSuccess)
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        features.forEach { feat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 3.dp)
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = ElectricViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(feat, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        if (isCurrent) {
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Current Active Subscription")
                            }
                        } else {
                            NeonGradientButton(
                                text = "Switch to ${tier.displayName}",
                                tag = "activate_plan_${tier.name.lowercase()}",
                                onClick = { onSwitchPlan(tier) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                // ADMIN DASHBOARD & TELEMETRY
                val failedCount = telemetryLogs.count { it.status == "FAILED" }
                val totalPlatformClips = if (isAdmin && adminUsers.isNotEmpty()) {
                    adminUsers.sumOf { it.totalClipsGenerated }
                } else {
                    userProfile?.totalClipsGenerated ?: 0
                }
                val totalStorageMb = if (isAdmin && adminUsers.isNotEmpty()) {
                    adminUsers.sumOf { it.storageUsedMb }
                } else {
                    userProfile?.storageUsedMb ?: 0.0
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricGlassTile(
                            label = if (isAdmin) "Users" else "AI Runs",
                            value = if (isAdmin) "${adminUsers.size}" else "${telemetryLogs.size}",
                            accent = ElectricViolet,
                            icon = Icons.Filled.People,
                            modifier = Modifier.weight(1f)
                        )
                        MetricGlassTile(
                            label = "Total Clips",
                            value = "$totalPlatformClips",
                            accent = CyanSpark,
                            icon = Icons.Filled.Analytics,
                            modifier = Modifier.weight(1f)
                        )
                        MetricGlassTile(
                            label = "Failed Jobs",
                            value = "$failedCount",
                            accent = if (failedCount > 0) CrimsonError else EmeraldSuccess,
                            icon = Icons.Filled.ErrorOutline,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (isAdmin) {
                    // Global System Config Editor (Admin Only)
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), accentBorder = true) {
                            Text(
                                text = "Global API, Limits & Storage Configuration",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Platform Storage Used: ${String.format(Locale.US, "%.1f", totalStorageMb)} MB / ${systemConfig.storageQuotaGb} GB",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanSpark
                            )
                            Spacer(Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = freeLimitInput,
                                    onValueChange = { freeLimitInput = it.filter(Char::isDigit) },
                                    label = { Text("Free/day") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = proLimitInput,
                                    onValueChange = { proLimitInput = it.filter(Char::isDigit) },
                                    label = { Text("Pro/day") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = premiumLimitInput,
                                    onValueChange = { premiumLimitInput = it.filter(Char::isDigit) },
                                    label = { Text("Prem/day") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = maxDurationInput,
                                    onValueChange = { maxDurationInput = it.filter(Char::isDigit) },
                                    label = { Text("Max Video (min)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = storageQuotaInput,
                                    onValueChange = { storageQuotaInput = it.filter(Char::isDigit) },
                                    label = { Text("Storage (GB)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = aiModelInput,
                                onValueChange = { aiModelInput = it },
                                label = { Text("Active Gemini AI Model") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Global Auto-Publish Enabled", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                Switch(
                                    checked = globalAutoPublish,
                                    onCheckedChange = { globalAutoPublish = it },
                                    colors = SwitchDefaults.colors(checkedTrackColor = ElectricViolet)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            NeonGradientButton(
                                text = "Save System Configuration",
                                icon = Icons.Filled.Save,
                                tag = "admin_save_config_button",
                                onClick = {
                                    onAdminSaveConfig(
                                        SystemConfig(
                                            freeDailyLimit = freeLimitInput.toIntOrNull()?.coerceIn(1, 50) ?: 3,
                                            proDailyLimit = proLimitInput.toIntOrNull()?.coerceIn(1, 200) ?: 20,
                                            premiumDailyLimit = premiumLimitInput.toIntOrNull()?.coerceIn(1, 1000) ?: 100,
                                            maxVideoDurationMinutes = maxDurationInput.toIntOrNull()?.coerceIn(1, 240) ?: 60,
                                            aiModelName = aiModelInput.ifBlank { "gemini-3.8-flash" },
                                            autoPublishGlobalEnabled = globalAutoPublish,
                                            storageQuotaGb = storageQuotaInput.toIntOrNull()?.coerceIn(1, 10000) ?: 500
                                        )
                                    )
                                }
                            )
                        }
                    }

                    // Manage Users & Subscriptions (Admin Only)
                    item {
                        Text(
                            text = "Manage Users & Subscriptions (${adminUsers.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                    items(adminUsers, key = { it.id.ifBlank { it.userId } }) { targetUser ->
                        val targetTier = PlanTier.fromRaw(targetUser.planTier)
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(targetUser.displayName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text(
                                        text = "${targetUser.email} • ${targetUser.dailyClipsUsed}/${targetUser.dailyClipLimit} today",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                StatusPill(text = targetTier.name, color = ElectricViolet)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PlanTier.entries.forEach { option ->
                                    FilterChip(
                                        selected = option == targetTier,
                                        onClick = { onAdminUpdateUserPlan(targetUser.userId, option) },
                                        label = { Text(option.displayName) }
                                    )
                                }
                            }
                        }
                    }
                }

                // AI Processing & Upload Telemetry Logs
                item {
                    Text(
                        text = if (isAdmin) "System AI & Upload Logs (${filteredLogs.size})" else "My AI & Publish Logs (${filteredLogs.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ALL" to "All", "AI_ANALYSIS" to "AI", "SOCIAL_PUBLISH" to "Publish", "FAILED" to "Failed")
                            .forEach { (key, label) ->
                                FilterChip(
                                    selected = logFilter == key,
                                    onClick = { logFilter = key },
                                    label = { Text(label) }
                                )
                            }
                    }
                }

                if (filteredLogs.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("No telemetry logs recorded yet.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                } else {
                    items(filteredLogs.take(30), key = { it.id }) { log ->
                        val color = when (log.status) {
                            "COMPLETED" -> EmeraldSuccess
                            "FAILED" -> CrimsonError
                            else -> AmberViral
                        }
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPill(text = log.eventType, color = ElectricViolet)
                                StatusPill(text = log.status, color = color)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(log.message, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${log.userEmail} • ${log.clipsCount} clips • ${log.durationMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
