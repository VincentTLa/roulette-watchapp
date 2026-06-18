package com.vincentla.roulette.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import com.vincentla.roulette.data.EuropeanWheel
import com.vincentla.roulette.data.PocketColor
import kotlin.math.cos
import kotlin.math.sin

private val SectorRed   = Color(0xFFB71C1C)
private val SectorBlack = Color(0xFF1A1A1A)
private val SectorGreen = Color(0xFF2E7D32)
private val RoseGold    = Color(0xFFB08D57)

@Composable
fun RouletteWheel(
    wheelRotationDeg: Float,
    ballAngleDeg: Float,
    ballRadiusFraction: Float,
    ballInWheel: Boolean,
    ballWheelAngleDeg: Float,
    highlightIndex: Int?,
    isSpinning: Boolean,
    onSpin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val highlightAlpha = remember { Animatable(0f) }
    LaunchedEffect(highlightIndex) {
        if (highlightIndex != null) {
            repeat(3) {
                highlightAlpha.animateTo(0.38f, tween(300))
                highlightAlpha.animateTo(0f, tween(300))
            }
        } else {
            highlightAlpha.snapTo(0f)
        }
    }
    val currentHighlight = highlightAlpha.value

    Box(
        modifier = modifier.clickable(
            enabled = !isSpinning,
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onSpin() }
    ) {
        // ── Rotating wheel disc ──────────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = wheelRotationDeg }
        ) {
            val R            = size.minDimension / 2f
            val center       = Offset(size.width / 2f, size.height / 2f)
            val sweepAngle   = 360f / 37f
            val outerSectorR = R * 0.93f
            val splitR       = R * 0.775f
            val innerSectorR = R * 0.62f
            val labelR       = R * 0.853f

            // 1. Outer bezel — radial gradient for depth
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF252525), Color(0xFF080808)),
                    center = center, radius = R
                ),
                radius = R, center = center
            )

            // 2. Coloured sectors + highlight
            val arcTL   = Offset(center.x - outerSectorR, center.y - outerSectorR)
            val arcSize = Size(outerSectorR * 2f, outerSectorR * 2f)
            EuropeanWheel.pocketOrder.forEachIndexed { index, number ->
                val startAngle = -90f - sweepAngle / 2f + index * sweepAngle
                val fillColor = when (EuropeanWheel.colorFor(number)) {
                    PocketColor.RED   -> SectorRed
                    PocketColor.BLACK -> SectorBlack
                    PocketColor.GREEN -> SectorGreen
                }
                drawArc(color = fillColor, startAngle = startAngle, sweepAngle = sweepAngle,
                        useCenter = true, topLeft = arcTL, size = arcSize)
                if (index == highlightIndex && currentHighlight > 0f) {
                    drawArc(color = Color.White.copy(alpha = currentHighlight),
                            startAngle = startAngle, sweepAngle = sweepAngle,
                            useCenter = true, topLeft = arcTL, size = arcSize)
                }
            }

            // 3. Gold outline accent on green pocket (index 0)
            drawArc(
                color = RoseGold,
                startAngle = -90f - sweepAngle / 2f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = arcTL, size = arcSize,
                style = Stroke(width = 4f)
            )

            // 4. Sector depth overlay — pseudo-3D dome effect
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f    to Color.White.copy(alpha = 0.10f),
                        0.65f to Color.Transparent,
                        1f    to Color.Black.copy(alpha = 0.18f)
                    ),
                    center = center, radius = outerSectorR
                ),
                radius = outerSectorR, center = center
            )

            // 5. Separator ring
            drawCircle(color = RoseGold, radius = splitR, center = center, style = Stroke(1.5f))

            // 6. Outer ring border
            drawCircle(color = RoseGold, radius = outerSectorR, center = center, style = Stroke(2.5f))

            // 7. Diamond markers — one per sector
            val markerR    = R * 0.965f
            val markerSize = R * 0.028f
            val markerPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(255, 176, 141, 87)
                style = android.graphics.Paint.Style.FILL
                isAntiAlias = true
            }
            for (i in 0 until 37) {
                val midAngle = -90f + (i + 0.5f) * sweepAngle
                val midRad   = Math.toRadians(midAngle.toDouble())
                val mx = (center.x + markerR * cos(midRad)).toFloat()
                val my = (center.y + markerR * sin(midRad)).toFloat()
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.translate(mx, my)
                    canvas.nativeCanvas.rotate(midAngle + 90f)
                    val dp = android.graphics.Path().apply {
                        moveTo(0f, -markerSize)
                        lineTo(markerSize * 0.55f, 0f)
                        lineTo(0f, markerSize)
                        lineTo(-markerSize * 0.55f, 0f)
                        close()
                    }
                    canvas.nativeCanvas.drawPath(dp, markerPaint)
                    canvas.nativeCanvas.restore()
                }
            }

            // 8. Number labels — condensed bold font
            val textSize   = R * 0.115f
            val textOffset = textSize * 0.36f
            val labelPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textAlign = Paint.Align.CENTER
                this.textSize = textSize
                typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
                isAntiAlias = true
            }
            val outlinePaint = Paint(labelPaint).apply {
                color = android.graphics.Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = textSize * 0.12f
                strokeJoin = android.graphics.Paint.Join.ROUND
            }
            EuropeanWheel.pocketOrder.forEachIndexed { index, number ->
                val midAngle = -90f + index * sweepAngle
                val midRad   = Math.toRadians(midAngle.toDouble())
                val lx = (center.x + labelR * cos(midRad)).toFloat()
                val ly = (center.y + labelR * sin(midRad)).toFloat()
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.translate(lx, ly)
                    canvas.nativeCanvas.rotate(midAngle + 90f)
                    canvas.nativeCanvas.drawText(number.toString(), 0f, textOffset, outlinePaint)
                    canvas.nativeCanvas.drawText(number.toString(), 0f, textOffset, labelPaint)
                    canvas.nativeCanvas.restore()
                }
            }

            // 9. Inner ring borders (double ring)
            drawCircle(color = RoseGold, radius = innerSectorR + 4f, center = center, style = Stroke(1.5f))
            drawCircle(color = RoseGold, radius = innerSectorR,      center = center, style = Stroke(3f))

            // 10. Inner disc — vignette gradient (lighter near ring, very dark center)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF1A1A1A), Color(0xFF050505)),
                    center = center, radius = innerSectorR - 2f
                ),
                radius = innerSectorR - 2f, center = center
            )

            // 11. Centre hub (three concentric circles)
            drawCircle(color = RoseGold,          radius = R * 0.055f, center = center)
            drawCircle(color = Color(0xFF1C1C1C), radius = R * 0.038f, center = center)
            drawCircle(color = RoseGold,          radius = R * 0.016f, center = center)

            // 12. Ball on rotating canvas — drawn here when locked to a pocket
            if (ballInWheel) {
                val angleRad = Math.toRadians(ballWheelAngleDeg.toDouble())
                val bx    = (center.x + R * ballRadiusFraction * cos(angleRad)).toFloat()
                val by    = (center.y + R * ballRadiusFraction * sin(angleRad)).toFloat()
                val ballR = R * 0.038f
                // Soft glow behind ball when settled
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(bx, by), radius = ballR * 3f
                    ),
                    radius = ballR * 3f, center = Offset(bx, by)
                )
                drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = ballR * 1.2f,
                           center = Offset(bx + ballR * 0.35f, by + ballR * 0.35f))
                drawCircle(color = Color.White, radius = ballR, center = Offset(bx, by))
                drawCircle(color = Color.White.copy(alpha = 0.65f), radius = ballR * 0.32f,
                           center = Offset(bx - ballR * 0.28f, by - ballR * 0.28f))
            }
        }

        // ── Static layer: ball orbiting (only while not locked to wheel) ─────
        if (!ballInWheel) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val R       = size.minDimension / 2f
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val angleRad = Math.toRadians(ballAngleDeg.toDouble())
                val bx = (centerX + R * ballRadiusFraction * cos(angleRad)).toFloat()
                val by = (centerY + R * ballRadiusFraction * sin(angleRad)).toFloat()
                val ballR = R * 0.038f
                drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = ballR * 1.2f,
                           center = Offset(bx + ballR * 0.35f, by + ballR * 0.35f))
                drawCircle(color = Color.White, radius = ballR, center = Offset(bx, by))
                drawCircle(color = Color.White.copy(alpha = 0.65f), radius = ballR * 0.32f,
                           center = Offset(bx - ballR * 0.28f, by - ballR * 0.28f))
            }
        }
    }
}
