package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CaptionStyle
import com.example.ui.theme.AmberViral
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.DeepRoyalPurple
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCardGlass
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianOutlineBright
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accentBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val borderBrush = if (accentBorder) {
        Brush.linearGradient(listOf(ElectricViolet.copy(alpha = 0.75f), CyberMagenta.copy(alpha = 0.55f)))
    } else {
        Brush.linearGradient(listOf(ObsidianOutlineBright.copy(alpha = 0.45f), ObsidianOutline.copy(alpha = 0.35f)))
    }
    val bgBrush = Brush.verticalGradient(
        listOf(
            ObsidianSurfaceElevated.copy(alpha = 0.90f),
            ObsidianCardGlass.copy(alpha = 0.85f)
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .border(BorderStroke(1.dp, borderBrush), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding)
    ) {
        Column(content = content)
    }
}

@Composable
fun NeonGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    tag: String = "neon_button"
) {
    val shape = RoundedCornerShape(16.dp)
    val gradient = if (enabled) {
        Brush.horizontalGradient(listOf(ElectricViolet, NeonPurple, CyberMagenta))
    } else {
        Brush.horizontalGradient(listOf(ObsidianOutline, ObsidianOutline))
    }

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContentColor = TextSecondary
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .height(52.dp)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(gradient, shape),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ViralScoreRing(
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp
) {
    val color = when {
        score >= 90 -> EmeraldSuccess
        score >= 80 -> ElectricViolet
        else -> AmberViral
    }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 5.dp.toPx()
            drawArc(
                color = ObsidianOutline,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(color, CyberMagenta, color)),
                startAngle = -90f,
                sweepAngle = 360f * (score.coerceIn(0, 100) / 100f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "VIRAL",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = color
            )
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

@Composable
fun MetricGlassTile(
    label: String,
    value: String,
    accent: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PipelineProgressCard(
    stepLabel: String,
    progressPercent: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    GlassCard(modifier = modifier.fillMaxWidth(), accentBorder = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(CyanSpark.copy(alpha = glowAlpha))
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stepLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$progressPercent%",
                style = MaterialTheme.typography.labelLarge,
                color = CyanSpark
            )
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { (progressPercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = ElectricViolet,
            trackColor = ObsidianOutline
        )
    }
}

/**
 * Live word-by-word animated subtitle preview supporting English, Hindi, and Hinglish
 * across all 4 viral caption styles.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimatedSubtitleOverlay(
    subtitleText: String,
    captionStyle: CaptionStyle,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val words = remember(subtitleText) {
        subtitleText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    }
    var activeWordIndex by remember(subtitleText) { mutableIntStateOf(0) }

    LaunchedEffect(subtitleText, isPlaying) {
        if (words.isEmpty()) return@LaunchedEffect
        while (isPlaying) {
            delay(360L)
            activeWordIndex = (activeWordIndex + 1) % words.size
        }
    }

    if (words.isEmpty()) return

    // Show a 5-word rolling window so captions stay punchy like real Shorts/Reels
    val windowStart = (activeWordIndex / 5) * 5
    val windowWords = words.subList(windowStart, (windowStart + 5).coerceAtMost(words.size))
    val activeLocalIndex = activeWordIndex - windowStart

    val containerBg = when (captionStyle) {
        CaptionStyle.NEON_POP -> Color(0xB3090514)
        CaptionStyle.BOLD_BOX -> Color(0xE60B0617)
        CaptionStyle.MINIMAL_GLASS -> Color(0x661C1438)
        CaptionStyle.KARAOKE_GLOW -> Color(0xAA080412)
    }
    val borderColor = when (captionStyle) {
        CaptionStyle.NEON_POP -> ElectricViolet.copy(alpha = 0.8f)
        CaptionStyle.BOLD_BOX -> AmberViral.copy(alpha = 0.85f)
        CaptionStyle.MINIMAL_GLASS -> Color.White.copy(alpha = 0.25f)
        CaptionStyle.KARAOKE_GLOW -> CyanSpark.copy(alpha = 0.75f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(containerBg)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            windowWords.forEachIndexed { idx, word ->
                val isCurrent = idx == activeLocalIndex
                val highlightBg by animateColorAsState(
                    targetValue = when {
                        !isCurrent -> Color.Transparent
                        captionStyle == CaptionStyle.NEON_POP -> ElectricViolet
                        captionStyle == CaptionStyle.BOLD_BOX -> AmberViral
                        captionStyle == CaptionStyle.MINIMAL_GLASS -> Color.White.copy(alpha = 0.22f)
                        else -> CyberMagenta
                    },
                    label = "wordBg"
                )
                val textColor = when {
                    isCurrent && captionStyle == CaptionStyle.BOLD_BOX -> ObsidianBlack
                    isCurrent -> Color.White
                    captionStyle == CaptionStyle.KARAOKE_GLOW && idx < activeLocalIndex -> CyanSpark
                    else -> Color.White.copy(alpha = 0.86f)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(highlightBg)
                        .padding(horizontal = if (isCurrent) 6.dp else 2.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (captionStyle == CaptionStyle.BOLD_BOX) word.uppercase() else word,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = if (isCurrent) 17.sp else 15.sp,
                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold
                        ),
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return if (mins >= 60) {
        val hrs = mins / 60
        val remMins = mins % 60
        String.format("%d:%02d:%02d", hrs, remMins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}
