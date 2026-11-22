package com.aistudio.tdbassistant.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.tdbassistant.data.preferences.MotionQuality
import com.aistudio.tdbassistant.domain.OrbState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AiOrbVisualizer(
    orbState: OrbState,
    motionQuality: MotionQuality,
    wakeWordActive: Boolean,
    onOrbClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "tdb_ai_orb")
    val pulseSpeed = when (orbState) {
        OrbState.LISTENING -> 900
        OrbState.PROCESSING -> 1300
        OrbState.SPEAKING -> 1000
        OrbState.EXECUTING -> 800
        OrbState.ERROR -> 1600
        OrbState.IDLE -> if (motionQuality == MotionQuality.LOW) 5000 else 3200
    }

    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseSpeed, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (orbState == OrbState.PROCESSING || orbState == OrbState.EXECUTING) 2800 else 9000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )

    val coreScale by animateFloatAsState(
        targetValue = when (orbState) {
            OrbState.LISTENING -> 1.08f
            OrbState.SPEAKING -> 1.06f
            OrbState.EXECUTING -> 1.11f
            OrbState.PROCESSING -> 0.96f
            OrbState.ERROR -> 0.94f
            OrbState.IDLE -> 1.0f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "core_scale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val goldColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error

    val activeAccent by animateColorAsState(
        targetValue = when (orbState) {
            OrbState.IDLE -> primaryColor
            OrbState.LISTENING -> Color(0xFF19A974)
            OrbState.PROCESSING -> goldColor
            OrbState.SPEAKING -> primaryColor
            OrbState.EXECUTING -> Color(0xFF0E9F6E)
            OrbState.ERROR -> errorColor
        },
        label = "orb_accent"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_orb_stage"),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            activeAccent.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            // Top Left Status Pill
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, activeAccent.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(activeAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${orbState.labelBn} · ${orbState.labelEn}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (wakeWordActive) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "HEY TDB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Center Interactive Animated Orb
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(210.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOrbClick)
                    .testTag("ai_orb_button"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseR = size.minDimension * 0.28f * coreScale
                    val rippleExpand = if (motionQuality == MotionQuality.LOW) 0f else pulse * 26.dp.toPx()

                    // Outer soft aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                activeAccent.copy(alpha = 0.34f),
                                goldColor.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = baseR * 1.9f + rippleExpand
                        ),
                        radius = baseR * 1.9f + rippleExpand,
                        center = center
                    )

                    // Expanding state wave ring
                    if (motionQuality != MotionQuality.LOW) {
                        drawCircle(
                            color = activeAccent.copy(alpha = (1f - pulse) * 0.55f),
                            radius = baseR * 1.15f + rippleExpand * 0.85f,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Rotating orbital arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                activeAccent,
                                goldColor,
                                Color.Transparent,
                                activeAccent.copy(alpha = 0.7f)
                            ),
                            center = center
                        ),
                        startAngle = rotation,
                        sweepAngle = if (orbState == OrbState.PROCESSING || orbState == OrbState.EXECUTING) 240f else 150f,
                        useCenter = false,
                        topLeft = Offset(center.x - baseR * 1.25f, center.y - baseR * 1.25f),
                        size = androidx.compose.ui.geometry.Size(baseR * 2.5f, baseR * 2.5f),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Inner luminous sphere
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.88f),
                                activeAccent.copy(alpha = 0.88f),
                                goldColor.copy(alpha = 0.65f)
                            ),
                            center = Offset(center.x - baseR * 0.25f, center.y - baseR * 0.25f),
                            radius = baseR * 1.15f
                        ),
                        radius = baseR,
                        center = center
                    )

                    // Orbiting champagne-gold satellite dot
                    if (motionQuality != MotionQuality.LOW) {
                        val rad = Math.toRadians(rotation.toDouble())
                        val sat = Offset(
                            x = center.x + (baseR * 1.25f * cos(rad)).toFloat(),
                            y = center.y + (baseR * 1.25f * sin(rad)).toFloat()
                        )
                        drawCircle(color = goldColor, radius = 4.dp.toPx(), center = sat)
                    }
                }

                // State icon inside Orb
                val icon = when (orbState) {
                    OrbState.IDLE -> Icons.Rounded.Mic
                    OrbState.LISTENING -> Icons.Rounded.GraphicEq
                    OrbState.PROCESSING -> Icons.Rounded.AutoAwesome
                    OrbState.SPEAKING -> Icons.Rounded.RecordVoiceOver
                    OrbState.EXECUTING -> Icons.Rounded.Bolt
                    OrbState.ERROR -> Icons.Rounded.ErrorOutline
                }
                Icon(
                    imageVector = icon,
                    contentDescription = orbState.labelEn,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Bottom hint
            Text(
                text = when (orbState) {
                    OrbState.LISTENING -> "কথা বলুন… থামতে Orb-এ ট্যাপ করুন"
                    OrbState.PROCESSING -> "উত্তর তৈরি হচ্ছে…"
                    OrbState.SPEAKING -> "কথা বলছি…"
                    OrbState.EXECUTING -> "কমান্ড কার্যকর হচ্ছে…"
                    OrbState.ERROR -> "আবার চেষ্টা করতে ট্যাপ করুন"
                    OrbState.IDLE -> "কথা বলতে Orb-এ ট্যাপ করুন বা নিচে লিখুন"
                },
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )
        }
    }
}
