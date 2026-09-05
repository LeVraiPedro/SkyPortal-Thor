// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.ui.portal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skyportalthor.app.data.GameFeature
import com.skyportalthor.app.portal.PortalState
import com.skyportalthor.app.portal.led.PortalRgb
import com.skyportalthor.app.ui.PortalPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
internal fun AnimatedPortalPanel(
    portalState: PortalState,
    playerTwoEnabled: Boolean,
    teamCount: Int,
    modifier: Modifier = Modifier,
    onTeams: () -> Unit,
    onDiagnostics: () -> Unit
) {
    val visual = remember(portalState) { AnimatedPortalStateMapper.from(portalState) }
    val toneColor = visual.tone.toColor()
    val showTrapZone = portalState.skylandersGame?.features?.contains(GameFeature.TRAPS) == true

    val leftColor by animateColorAsState(
        targetValue = visual.left.toComposeColor(),
        animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing),
        label = "portal-left-color"
    )
    val rightColor by animateColorAsState(
        targetValue = visual.right.toComposeColor(),
        animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing),
        label = "portal-right-color"
    )
    val trapColor by animateColorAsState(
        targetValue = (visual.trap ?: PortalRgb.Black).toComposeColor(),
        animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing),
        label = "portal-trap-color"
    )
    val activation by animateFloatAsState(
        targetValue = if (visual.active) 1f else 0.18f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "portal-activation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "portal-ambience")
    val ambience by infiniteTransition.animateFloat(
        initialValue = 0.86f,
        targetValue = if (visual.pulse) 1.12f else 0.94f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "portal-breath"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_500, easing = LinearEasing)
        ),
        label = "portal-ring-rotation"
    )

    val occupiedSignature = remember(portalState.slots) {
        portalState.slots.joinToString(separator = "|") { slot ->
            "${slot.logicalSlot}:${slot.actualPortalSlot}:${slot.figure?.figureId}:${slot.figure?.variantId}:${slot.sourceUri}"
        }
    }
    val interactionBurst = remember { Animatable(0f) }
    LaunchedEffect(occupiedSignature) {
        interactionBurst.snapTo(1f)
        interactionBurst.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier.heightIn(min = 132.dp),
        colors = CardDefaults.cardColors(containerColor = PortalPalette.Panel),
        border = BorderStroke(1.dp, toneColor.copy(alpha = 0.72f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            // Measure the text and RGB strips first; the canvas owns the remaining space.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        PortalPalette.Panel.copy(alpha = 0.88f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "PORTAL EXPERIENCE • V6",
                        color = PortalPalette.Accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(toneColor, CircleShape)
                        )
                        Text(
                            "${visual.title} • ${visual.detail}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onTeams) {
                        Text(if (teamCount > 0) "Équipes ($teamCount)" else "Équipes")
                    }
                    OutlinedButton(onClick = onDiagnostics) {
                        Text("Diagnostic")
                    }
                }
            }

            PortalCanvas(
                visual = visual,
                leftColor = leftColor,
                rightColor = rightColor,
                trapColor = trapColor,
                showTrapZone = showTrapZone,
                activation = activation,
                ambience = ambience,
                rotation = rotation,
                interactionBurst = interactionBurst.value,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 6.dp)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                color = PortalPalette.Background.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, toneColor.copy(alpha = 0.38f)),
                shape = RoundedCornerShape(13.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PortalColorChip("G", visual.left, leftColor)
                    PortalColorChip("D", visual.right, rightColor)
                    if (showTrapZone) {
                        visual.trap?.let { PortalColorChip("Trap", it, trapColor) }
                    }
                    visual.warning?.let { warning ->
                        Text(
                            "Canal LED : $warning",
                            color = PortalPalette.Warning,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortalColorChip(
    label: String,
    rgb: PortalRgb,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.88f)),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(7.dp).background(color, CircleShape))
            Text(
                label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black
            )
            Text(
                rgb.toHex(),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PortalCanvas(
    visual: AnimatedPortalState,
    leftColor: Color,
    rightColor: Color,
    trapColor: Color,
    showTrapZone: Boolean,
    activation: Float,
    ambience: Float,
    rotation: Float,
    interactionBurst: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.clipToBounds().semantics {
            contentDescription = visual.accessibilityDescription
            stateDescription = visual.title
        }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        if (canvasWidth <= 0f || canvasHeight <= 0f) return@Canvas

        val portalWidth = min(canvasWidth * 0.68f, canvasHeight * 6.20f)
        val portalHeight = portalWidth * 0.16f
        val center = Offset(canvasWidth * 0.50f, canvasHeight * 0.40f)
        val portalTopLeft = Offset(
            x = center.x - portalWidth / 2f,
            y = center.y - portalHeight / 2f
        )
        val portalSize = Size(portalWidth, portalHeight)
        val glowStrength = (
            0.16f +
                activation * 0.34f * ambience +
                interactionBurst * 0.26f
            ).coerceIn(0.10f, 0.80f)

        repeat(4) { layer ->
            val expansion = portalHeight * (0.16f + layer * 0.12f)
            val layerAlpha = glowStrength / (layer + 1f)
            drawOval(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        leftColor.copy(alpha = layerAlpha),
                        rightColor.copy(alpha = layerAlpha)
                    ),
                    startX = portalTopLeft.x - expansion,
                    endX = portalTopLeft.x + portalWidth + expansion
                ),
                topLeft = Offset(
                    x = portalTopLeft.x - expansion,
                    y = portalTopLeft.y - expansion * 0.42f
                ),
                size = Size(
                    width = portalWidth + expansion * 2f,
                    height = portalHeight + expansion * 0.84f
                )
            )
        }

        val baseWidth = portalWidth * 0.74f
        val baseHeight = (portalHeight * 0.32f).coerceAtLeast(10f)
        val baseTop = center.y + portalHeight * 0.26f
        drawRoundRect(
            color = Color(0xFF050B14).copy(alpha = 0.98f),
            topLeft = Offset(center.x - baseWidth / 2f, baseTop),
            size = Size(baseWidth, baseHeight),
            cornerRadius = CornerRadius(baseHeight / 2f)
        )
        drawOval(
            color = Color(0xFF1A2A43),
            topLeft = Offset(
                center.x - baseWidth * 0.48f,
                baseTop - baseHeight * 0.34f
            ),
            size = Size(baseWidth * 0.96f, baseHeight * 0.72f)
        )
        drawOval(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    leftColor.copy(alpha = 0.30f * activation),
                    rightColor.copy(alpha = 0.30f * activation)
                ),
                startX = center.x - baseWidth / 2f,
                endX = center.x + baseWidth / 2f
            ),
            topLeft = Offset(
                center.x - baseWidth * 0.48f,
                baseTop - baseHeight * 0.34f
            ),
            size = Size(baseWidth * 0.96f, baseHeight * 0.72f)
        )

        drawOval(
            color = Color(0xFF030811).copy(alpha = 0.9f),
            topLeft = Offset(portalTopLeft.x, portalTopLeft.y + portalHeight * 0.10f),
            size = portalSize
        )
        drawOval(
            color = Color(0xFF182A43),
            topLeft = portalTopLeft,
            size = portalSize
        )
        drawOval(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    leftColor.copy(alpha = 0.76f * activation),
                    rightColor.copy(alpha = 0.76f * activation)
                ),
                startX = portalTopLeft.x,
                endX = portalTopLeft.x + portalWidth
            ),
            topLeft = portalTopLeft,
            size = portalSize,
            style = Stroke(width = (portalHeight * 0.17f).coerceIn(5f, 15f))
        )

        val innerInsetX = portalWidth * 0.10f
        val innerInsetY = portalHeight * 0.18f
        val innerTopLeft = Offset(
            portalTopLeft.x + innerInsetX,
            portalTopLeft.y + innerInsetY
        )
        val innerSize = Size(
            portalWidth - innerInsetX * 2f,
            portalHeight - innerInsetY * 2f
        )
        drawOval(
            color = Color(0xFF091321),
            topLeft = innerTopLeft,
            size = innerSize
        )
        drawOval(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    leftColor.copy(alpha = 0.40f * activation),
                    rightColor.copy(alpha = 0.40f * activation)
                ),
                startX = innerTopLeft.x,
                endX = innerTopLeft.x + innerSize.width
            ),
            topLeft = innerTopLeft,
            size = innerSize
        )

        // Move highlights along the horizontal ellipse; rotating the whole ellipse would
        // send its wide axis through the status/RGB strips at quarter turns.
        drawArc(
            color = Color.White.copy(alpha = 0.22f * activation),
            startAngle = rotation + 8f,
            sweepAngle = 82f,
            useCenter = false,
            topLeft = portalTopLeft,
            size = portalSize,
            style = Stroke(
                width = (portalHeight * 0.055f).coerceAtLeast(2f),
                cap = StrokeCap.Round
            )
        )
        drawArc(
            color = Color.White.copy(alpha = 0.14f * activation),
            startAngle = rotation + 188f,
            sweepAngle = 58f,
            useCenter = false,
            topLeft = portalTopLeft,
            size = portalSize,
            style = Stroke(
                width = (portalHeight * 0.04f).coerceAtLeast(1.5f),
                cap = StrokeCap.Round
            )
        )

        val radiusX = portalWidth * 0.43f
        val radiusY = portalHeight * 0.40f
        repeat(16) { index ->
            val angle = (2.0 * PI * index / 16.0)
            val cosValue = cos(angle).toFloat()
            val sinValue = sin(angle).toFloat()
            val start = Offset(
                x = center.x + cosValue * radiusX * 0.90f,
                y = center.y + sinValue * radiusY * 0.90f
            )
            val end = Offset(
                x = center.x + cosValue * radiusX,
                y = center.y + sinValue * radiusY
            )
            drawLine(
                color = (if (cosValue < 0f) leftColor else rightColor)
                    .copy(alpha = 0.52f * activation),
                start = start,
                end = end,
                strokeWidth = (portalHeight * 0.025f).coerceAtLeast(1.3f),
                cap = StrokeCap.Round
            )
        }

        val sideLightRadius = (portalHeight * 0.12f).coerceAtLeast(4f)
        drawCircle(
            color = leftColor.copy(alpha = (0.62f + interactionBurst * 0.28f) * activation),
            radius = sideLightRadius,
            center = Offset(
                center.x - portalWidth * 0.40f,
                center.y + portalHeight * 0.02f
            )
        )
        drawCircle(
            color = rightColor.copy(alpha = (0.62f + interactionBurst * 0.28f) * activation),
            radius = sideLightRadius,
            center = Offset(
                center.x + portalWidth * 0.40f,
                center.y + portalHeight * 0.02f
            )
        )

        if (showTrapZone) {
            visual.trap?.let {
                val trapCenter = Offset(
                    center.x,
                    baseTop + baseHeight * 0.18f
                )
                val trapSize = (portalHeight * 0.27f).coerceAtLeast(9f)
                drawCircle(
                    color = trapColor.copy(alpha = 0.28f * ambience * activation),
                    radius = trapSize * 1.45f,
                    center = trapCenter
                )
                val trapPath = Path().apply {
                    moveTo(trapCenter.x, trapCenter.y - trapSize)
                    lineTo(trapCenter.x + trapSize * 0.72f, trapCenter.y)
                    lineTo(trapCenter.x, trapCenter.y + trapSize)
                    lineTo(trapCenter.x - trapSize * 0.72f, trapCenter.y)
                    close()
                }
                drawPath(
                    path = trapPath,
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.78f), trapColor),
                        center = trapCenter,
                        radius = trapSize * 1.25f
                    ),
                    alpha = activation
                )
                drawPath(
                    path = trapPath,
                    color = Color.White.copy(alpha = 0.64f * activation),
                    style = Stroke(width = (trapSize * 0.10f).coerceAtLeast(1f))
                )
            }
        }
    }
}

private fun PortalVisualTone.toColor(): Color = when (this) {
    PortalVisualTone.NEUTRAL -> PortalPalette.Muted
    PortalVisualTone.INFO -> PortalPalette.Accent
    PortalVisualTone.SUCCESS -> PortalPalette.Success
    PortalVisualTone.WARNING -> PortalPalette.Warning
    PortalVisualTone.ERROR -> PortalPalette.Error
}

private fun PortalRgb.toComposeColor(): Color = Color(
    red = red,
    green = green,
    blue = blue
)
