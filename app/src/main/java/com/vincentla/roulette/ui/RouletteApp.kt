package com.vincentla.roulette.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.vincentla.roulette.data.EuropeanWheel
import com.vincentla.roulette.data.PocketColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BallSpinEasing  = CubicBezierEasing(0.1f, 0.0f, 0.1f, 1.0f)
private val WheelSpinEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)  // ease-out: decelerate to rest
private val RoseGold = Color(0xFFB08D57)

sealed class SpinState {
    object Idle : SpinState()
    object Spinning : SpinState()
    data class Result(val number: Int, val color: PocketColor, val index: Int) : SpinState()
}

@Composable
fun RouletteApp() {
    val coroutineScope     = rememberCoroutineScope()
    val wheelRotation      = remember { Animatable(0f) }
    val ballAngleDeg       = remember { Animatable(-90f) }
    val ballRadiusFraction = remember { Animatable(0.965f) }
    var spinState          by remember { mutableStateOf<SpinState>(SpinState.Idle) }
    var ballInWheel        by remember { mutableStateOf(false) }
    var ballWheelAngleDeg  by remember { mutableStateOf(0f) }

    // ponytail: wheel rests at idle (battery + OLED burn-in); it only turns during a spin.
    val onSpin: () -> Unit = {
        if (spinState !is SpinState.Spinning) {
            coroutineScope.launch {
                val sweepAngle           = 360f / 37f
                val wheelDegPerSec       = 360f / 8f
                val spinDurationMs       = 4500
                val spinDurationSec      = spinDurationMs / 1000f

                if (ballInWheel) {
                    ballAngleDeg.snapTo(ballWheelAngleDeg + wheelRotation.value)
                    ballInWheel = false
                }
                ballRadiusFraction.snapTo(0.965f)

                val targetIndex          = (0..36).random()
                val wheelMoveDuringSpin  = wheelDegPerSec * spinDurationSec
                val targetScreenAngle    = (
                    -90f + targetIndex * sweepAngle +
                    wheelRotation.value + wheelMoveDuringSpin
                ).mod(360f)

                val currentMod  = ballAngleDeg.value.mod(360f)
                val ccwDelta    = (currentMod - targetScreenAngle).mod(360f)
                val fullLaps    = (5..7).random()
                val newBallAngle = ballAngleDeg.value - (fullLaps * 360f + ccwDelta)

                spinState = SpinState.Spinning

                // Wheel turns clockwise during the spin only, advancing exactly the
                // amount the targeting math above accounts for, then rests again.
                val wheelJob = launch {
                    wheelRotation.animateTo(
                        targetValue = wheelRotation.value + wheelMoveDuringSpin,
                        animationSpec = tween(durationMillis = spinDurationMs, easing = WheelSpinEasing)
                    )
                }

                val ballJob = launch {
                    ballAngleDeg.animateTo(
                        targetValue = newBallAngle,
                        animationSpec = tween(durationMillis = spinDurationMs, easing = BallSpinEasing)
                    )
                }

                delay(3200)
                ballRadiusFraction.animateTo(
                    targetValue = 0.70f,
                    animationSpec = tween(durationMillis = 1300,
                                         easing = CubicBezierEasing(0.4f, 0f, 0.8f, 1f))
                )

                ballJob.join()
                wheelJob.join()

                ballWheelAngleDeg = ballAngleDeg.value - wheelRotation.value
                ballInWheel = true

                val number = EuropeanWheel.pocketOrder[targetIndex]
                spinState  = SpinState.Result(number, EuropeanWheel.colorFor(number), targetIndex)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RouletteWheel(
            wheelRotationDeg   = wheelRotation.value,
            ballAngleDeg       = ballAngleDeg.value,
            ballRadiusFraction = ballRadiusFraction.value,
            ballInWheel        = ballInWheel,
            ballWheelAngleDeg  = ballWheelAngleDeg,
            highlightIndex     = (spinState as? SpinState.Result)?.index,
            isSpinning         = spinState is SpinState.Spinning,
            onSpin             = onSpin,
            modifier           = Modifier.fillMaxSize()
        )

        when (val state = spinState) {
            is SpinState.Idle    -> HintText(modifier = Modifier.align(Alignment.Center))
            is SpinState.Result  -> ResultOverlay(
                number = state.number, color = state.color,
                modifier = Modifier.align(Alignment.Center)
            )
            is SpinState.Spinning -> Unit
        }
    }
}

@Composable
private fun ResultOverlay(number: Int, color: PocketColor, modifier: Modifier = Modifier) {
    val textColor = when (color) {
        PocketColor.RED   -> Color(0xFFFF6B6B)
        PocketColor.BLACK -> Color(0xFFE0E0E0)
        PocketColor.GREEN -> Color(0xFF69F0AE)
    }
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f), shape = CircleShape)
            .border(2.dp, RoseGold, CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = number.toString(), style = MaterialTheme.typography.titleLarge,
             fontWeight = FontWeight.Bold, color = textColor)
        Text(text = color.name, style = MaterialTheme.typography.labelMedium,
             color = textColor.copy(alpha = 0.85f))
    }
}

@Composable
private fun HintText(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "hint")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintAlpha"
    )
    Text(text = "TAP TO SPIN", style = MaterialTheme.typography.labelSmall,
         color = Color.White.copy(alpha = alpha), modifier = modifier)
}
