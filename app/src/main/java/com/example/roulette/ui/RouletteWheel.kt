package com.example.roulette.ui

import android.graphics.Paint
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import com.example.roulette.data.EuropeanWheel
import com.example.roulette.data.PocketColor
import kotlin.math.cos
import kotlin.math.sin

private val SectorRed   = Color(0xFFB71C1C)
private val SectorBlack = Color(0xFF1A1A1A)
private val SectorGreen = Color(0xFF1B5E20)
private val RoseGold    = Color(0xFFB08D57)
private val OuterDark   = Color(0xFF0D0D0D)
private val InnerDark   = Color(0xFF080808)

@Composable
fun RouletteWheel(
    wheelRotationDeg: Float,
    ballAngleDeg: Float,
    ballRadiusFraction: Float,
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
            val outerSectorR = R * 0.93f   // outer edge of label ring
            val splitR       = R * 0.775f  // boundary between label ring and color band
            val innerSectorR = R * 0.62f   // inner edge of color band
            val labelR       = R * 0.853f  // centre of label ring: (0.93 + 0.775) / 2

            // 1. Outer dark background
            drawCircle(color = OuterDark, radius = R, center = center)

            // 2. Coloured sectors (full pie to outerSectorR; inner disc masks the centre)
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
                // Highlight overlay on winning sector
                if (index == highlightIndex && currentHighlight > 0f) {
                    drawArc(color = Color.White.copy(alpha = currentHighlight),
                            startAngle = startAngle, sweepAngle = sweepAngle,
                            useCenter = true, topLeft = arcTL, size = arcSize)
                }
            }

            // 3. Separator ring between label ring and color band
            drawCircle(color = RoseGold, radius = splitR, center = center,
                       style = Stroke(width = 1.5f))

            // 4. Outer ring border
            drawCircle(color = RoseGold, radius = outerSectorR, center = center,
                       style = Stroke(width = 2.5f))

            // 4. Diamond markers — one per sector, at sector midpoint angle
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

            // 5. Number labels — white bold, radial, dark outline
            val textSize   = R * 0.115f
            val textOffset = textSize * 0.36f
            val labelPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textAlign = Paint.Align.CENTER
                this.textSize = textSize
                isFakeBoldText = true; isAntiAlias = true
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

            // 6. Inner ring borders (double ring — matches reference)
            drawCircle(color = RoseGold, radius = innerSectorR + 4f, center = center,
                       style = Stroke(width = 1.5f))
            drawCircle(color = RoseGold, radius = innerSectorR,      center = center,
                       style = Stroke(width = 3f))

            // 7. Inner dark disc — masks sector centres, creates large dark area
            drawCircle(color = InnerDark, radius = innerSectorR - 2f, center = center)

            // 8. Centre hub (three concentric circles)
            drawCircle(color = RoseGold,          radius = R * 0.055f, center = center)
            drawCircle(color = Color(0xFF1C1C1C), radius = R * 0.038f, center = center)
            drawCircle(color = RoseGold,          radius = R * 0.016f, center = center)
        }

        // ── Static layer: ball orbits independently of wheel rotation ────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val R       = size.minDimension / 2f
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val angleRad = Math.toRadians(ballAngleDeg.toDouble())
            val bx = (centerX + R * ballRadiusFraction * cos(angleRad)).toFloat()
            val by = (centerY + R * ballRadiusFraction * sin(angleRad)).toFloat()
            val ballR = R * 0.038f

            // Drop shadow
            drawCircle(color = Color.Black.copy(alpha = 0.45f),
                       radius = ballR * 1.2f,
                       center = Offset(bx + ballR * 0.35f, by + ballR * 0.35f))
            // Ball body
            drawCircle(color = Color.White, radius = ballR, center = Offset(bx, by))
            // Specular glint
            drawCircle(color = Color.White.copy(alpha = 0.65f),
                       radius = ballR * 0.32f,
                       center = Offset(bx - ballR * 0.28f, by - ballR * 0.28f))
        }
    }
}
