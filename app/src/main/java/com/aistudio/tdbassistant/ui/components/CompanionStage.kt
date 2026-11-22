package com.aistudio.tdbassistant.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.tdbassistant.R
import com.aistudio.tdbassistant.data.preferences.MotionQuality
import com.aistudio.tdbassistant.domain.CompanionPose
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompanionStage(
    pose: CompanionPose,
    isFullBody: Boolean,
    motionQuality: MotionQuality,
    onPoseSelected: (CompanionPose, Boolean) -> Unit,
    onToggleFullBody: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "companion_aura")
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (motionQuality == MotionQuality.LOW) 6000 else 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )

    var dragTiltX by remember { mutableFloatStateOf(0f) }
    var dragTiltY by remember { mutableFloatStateOf(0f) }

    val targetScale = when {
        isFullBody -> 0.94f
        pose == CompanionPose.SITTING -> 1.02f
        pose == CompanionPose.HAPPY -> 1.12f
        else -> 1.08f
    }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = when (pose) {
            CompanionPose.SITTING -> 22f
            CompanionPose.HAPPY -> -10f
            else -> 0f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "offset_y"
    )
    val animatedRotationZ by animateFloatAsState(
        targetValue = when (pose) {
            CompanionPose.WAVING -> if (breathe > 0.5f) 4.5f else -4.5f
            CompanionPose.HAPPY -> 2.5f
            else -> dragTiltX * 0.15f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "rot_z"
    )

    val statusDotColor by animateColorAsState(
        targetValue = when (pose) {
            CompanionPose.THINKING -> MaterialTheme.colorScheme.secondary
            CompanionPose.LISTENING -> Color(0xFF28A86B)
            CompanionPose.SPEAKING -> MaterialTheme.colorScheme.primary
            CompanionPose.HAPPY -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.primary
        },
        label = "dot_color"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val goldColor = MaterialTheme.colorScheme.secondary
    val surfaceTop = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    val surfaceBottom = MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("companion_stage"),
        shape = RoundedCornerShape(28.dp),
        color = surfaceBottom,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(surfaceTop, surfaceBottom)
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            dragTiltX = 0f
                            dragTiltY = 0f
                        },
                        onDragCancel = {
                            dragTiltX = 0f
                            dragTiltY = 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        dragTiltX = (dragTiltX + dragAmount.x * 0.25f).coerceIn(-18f, 18f)
                        dragTiltY = (dragTiltY + dragAmount.y * 0.25f).coerceIn(-12f, 12f)
                    }
                }
        ) {
            // Breathing emerald & champagne-gold halo rings
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height * 0.48f)
                val baseRadius = minOf(size.width, size.height) * 0.34f
                val pulseBoost = if (motionQuality == MotionQuality.LOW) 0f else breathe * 14.dp.toPx()

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.20f),
                            goldColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * 1.45f + pulseBoost
                    ),
                    radius = baseRadius * 1.45f + pulseBoost,
                    center = center
                )

                if (motionQuality != MotionQuality.LOW) {
                    drawCircle(
                        color = primaryColor.copy(alpha = if (pose == CompanionPose.LISTENING) 0.45f else 0.18f),
                        radius = baseRadius + pulseBoost * 0.6f,
                        center = center,
                        style = Stroke(width = 1.6.dp.toPx())
                    )

                    if (pose == CompanionPose.THINKING || pose == CompanionPose.SPEAKING) {
                        val rad = Math.toRadians(orbitAngle.toDouble())
                        val orbPos = Offset(
                            x = center.x + (baseRadius * 1.06f * cos(rad)).toFloat(),
                            y = center.y + (baseRadius * 1.06f * sin(rad)).toFloat()
                        )
                        drawCircle(
                            color = goldColor,
                            radius = 4.5.dp.toPx(),
                            center = orbPos
                        )
                    }
                }
            }

            // Companion Portrait Image with responsive spring framing
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp, bottom = 52.dp),
                contentAlignment = Alignment.Center
            ) {
                val bobOffset = if (motionQuality == MotionQuality.LOW) 0f else (breathe - 0.5f) * 6f
                Image(
                    painter = painterResource(id = R.drawable.img_tdb_companion),
                    contentDescription = stringResource(id = R.string.cd_companion_portrait),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(if (isFullBody) 178.dp else 196.dp)
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                            translationY = animatedOffsetY + bobOffset + dragTiltY * 0.35f
                            translationX = dragTiltX * 0.45f
                            rotationZ = animatedRotationZ
                        }
                        .clip(RoundedCornerShape(if (isFullBody) 26.dp else 999.dp))
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    primaryColor.copy(alpha = 0.55f),
                                    goldColor.copy(alpha = 0.65f)
                                )
                            ),
                            shape = RoundedCornerShape(if (isFullBody) 26.dp else 999.dp)
                        )
                )
            }

            // Top-left Presence Status Pill
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = pose.labelBn,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Bottom Gesture Strip
            LazyRow(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    GestureChip(
                        label = "হাত নাড়ো",
                        icon = Icons.Rounded.PanTool,
                        selected = pose == CompanionPose.WAVING,
                        tag = "gesture_wave",
                        onClick = { onPoseSelected(CompanionPose.WAVING, true) }
                    )
                }
                item {
                    GestureChip(
                        label = "বসো",
                        icon = Icons.Rounded.Weekend,
                        selected = pose == CompanionPose.SITTING,
                        tag = "gesture_sit",
                        onClick = { onPoseSelected(CompanionPose.SITTING, false) }
                    )
                }
                item {
                    GestureChip(
                        label = "দাঁড়াও",
                        icon = Icons.Rounded.SelfImprovement,
                        selected = pose == CompanionPose.IDLE,
                        tag = "gesture_stand",
                        onClick = { onPoseSelected(CompanionPose.IDLE, false) }
                    )
                }
                item {
                    GestureChip(
                        label = "হাসি",
                        icon = Icons.Rounded.SentimentVerySatisfied,
                        selected = pose == CompanionPose.HAPPY,
                        tag = "gesture_happy",
                        onClick = { onPoseSelected(CompanionPose.HAPPY, true) }
                    )
                }
                item {
                    GestureChip(
                        label = if (isFullBody) "পোর্ট্রেট" else "পূর্ণাঙ্গ",
                        icon = if (isFullBody) Icons.Rounded.CenterFocusStrong else Icons.Rounded.AutoAwesome,
                        selected = isFullBody,
                        tag = "gesture_view_toggle",
                        onClick = onToggleFullBody
                    )
                }
            }
        }
    }
}

@Composable
private fun GestureChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .height(34.dp)
            .testTag(tag),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
