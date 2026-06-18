package com.example.roulette.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.Canvas
import com.example.roulette.data.EuropeanWheel
import com.example.roulette.data.PocketColor
import kotlin.math.cos
import kotlin.math.sin

private val SectorRed = Color(0xFFB71C1C)
private val SectorBlack = Color(0xFF1A1A1A)
private val SectorGreen = Color(0xFF1B5E20)
private val SectorBorder = Color(0xFFD4AF37)
private val HubDark = Color(0xFF2C2C2C)
private val PointerGold = Color(0xFFFFD700)

@Composable
fun RouletteWheel(
    rotationDegrees: Float,
    highlightIndex: Int?,
    isSpinning: Boolean,
    onSpin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val highlightAlpha = remember { Animatable(0f) }

    LaunchedEffect(highlightIndex) {
        if (highlightIndex != null) {
            repeat(3) {
                highlightAlpha.animateTo(0.35f, tween(300))
                highlightAlpha.animateTo(0f, tween(300))
            }
        } else {
            highlightAlpha.snapTo(0f)
        }
    }

    // Read in composition scope so recompose drives redraw each animation frame.
    val currentHighlight = highlightAlpha.value

    Box(
        modifier = modifier.clickable(
            enabled = !isSpinning,
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onSpin() }
    ) {
        // Rotating wheel
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotationDegrees }
        ) {
            val sweepAngle = 360f / 37f
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val arcTopLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)

            // Draw filled sectors
            EuropeanWheel.pocketOrder.forEachIndexed { index, number ->
                val startAngle = -90f - sweepAngle / 2f + index * sweepAngle
                val fillColor = when (EuropeanWheel.colorFor(number)) {
                    PocketColor.RED -> SectorRed
                    PocketColor.BLACK -> SectorBlack
                    PocketColor.GREEN -> SectorGreen
                }
                drawArc(
                    color = fillColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = arcTopLeft,
                    size = arcSize
                )

                // Winning pocket pulse overlay
                if (index == highlightIndex && currentHighlight > 0f) {
                    drawArc(
                        color = Color.White.copy(alpha = currentHighlight),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = arcTopLeft,
                        size = arcSize
                    )
                }
            }

            // Number labels — drawn radially, rotated to face outward
            val labelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = radius * 0.10f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val shadowPaint = android.graphics.Paint(labelPaint).apply {
                color = android.graphics.Color.BLACK
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = radius * 0.025f
            }
            val labelRadius = radius * 0.72f
            val textOffset = labelPaint.textSize * 0.35f

            EuropeanWheel.pocketOrder.forEachIndexed { index, number ->
                val midAngle = -90f + index * sweepAngle
                val midAngleRad = Math.toRadians(midAngle.toDouble())
                val lx = (center.x + labelRadius * cos(midAngleRad)).toFloat()
                val ly = (center.y + labelRadius * sin(midAngleRad)).toFloat()
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.translate(lx, ly)
                    canvas.nativeCanvas.rotate(midAngle + 90f)
                    canvas.nativeCanvas.drawText(number.toString(), 0f, textOffset, shadowPaint)
                    canvas.nativeCanvas.drawText(number.toString(), 0f, textOffset, labelPaint)
                    canvas.nativeCanvas.restore()
                }
            }

            // Outer rim circle
            drawCircle(
                color = SectorBorder,
                radius = radius,
                center = center,
                style = Stroke(width = 2f)
            )

            // Center hub
            drawCircle(color = HubDark, radius = radius * 0.12f, center = center)
            drawCircle(
                color = SectorBorder,
                radius = radius * 0.12f,
                center = center,
                style = Stroke(width = 1.5f)
            )
        }

        // Static layer: pointer only (not rotated)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val centerX = size.width / 2f
            val rimTop = size.height / 2f - radius
            val pointerH = radius * 0.1f
            val pointerW = radius * 0.07f

            val path = Path().apply {
                moveTo(centerX, rimTop)
                lineTo(centerX - pointerW / 2f, rimTop + pointerH)
                lineTo(centerX + pointerW / 2f, rimTop + pointerH)
                close()
            }
            drawPath(path, PointerGold)
            drawPath(path, Color(0xFF8B6914), style = Stroke(width = 1.5f, cap = StrokeCap.Round))
        }
    }
}
