package com.example.roulette.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import com.example.roulette.data.EuropeanWheel
import com.example.roulette.data.PocketColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BallSpinEasing = CubicBezierEasing(0.1f, 0.0f, 0.1f, 1.0f)

sealed class SpinState {
    object Idle : SpinState()
    object Spinning : SpinState()
    data class Result(val number: Int, val color: PocketColor, val index: Int) : SpinState()
}

@Composable
fun RouletteApp() {
    val coroutineScope    = rememberCoroutineScope()
    val wheelRotation     = remember { Animatable(0f) }
    val ballAngleDeg      = remember { Animatable(-90f) }      // starts at 12 o'clock
    val ballRadiusFraction = remember { Animatable(0.965f) }   // outer rim
    var spinState by remember { mutableStateOf<SpinState>(SpinState.Idle) }

    // Continuous slow clockwise wheel rotation — always running
    LaunchedEffect(Unit) {
        while (true) {
            wheelRotation.animateTo(
                targetValue = wheelRotation.value + 360f,
                animationSpec = tween(durationMillis = 8000, easing = LinearEasing)
            )
        }
    }

    val onSpin: () -> Unit = {
        if (spinState !is SpinState.Spinning) {
            coroutineScope.launch {
                val sweepAngle           = 360f / 37f
                val wheelDegPerSec       = 360f / 8f    // 45°/s clockwise
                val spinDurationMs       = 4500
                val spinDurationSec      = spinDurationMs / 1000f

                // Snap ball back to outer rim for the new spin
                ballRadiusFraction.snapTo(0.965f)  // outer bezel rim

                // Pick the winning pocket, then compute where it will be
                // on-screen by the time the ball finishes its arc.
                val targetIndex          = (0..36).random()
                val wheelMoveDuringSpin  = wheelDegPerSec * spinDurationSec
                val targetScreenAngle    = (
                    -90f + targetIndex * sweepAngle +
                    wheelRotation.value + wheelMoveDuringSpin
                ).mod(360f)

                // Ball travels counterclockwise (decreasing angle) to land there.
                val currentMod  = ballAngleDeg.value.mod(360f)
                val ccwDelta    = (currentMod - targetScreenAngle).mod(360f)
                val fullLaps    = (5..7).random()
                val newBallAngle = ballAngleDeg.value - (fullLaps * 360f + ccwDelta)

                spinState = SpinState.Spinning

                // Ball orbit — counterclockwise, decelerating
                val ballJob = launch {
                    ballAngleDeg.animateTo(
                        targetValue = newBallAngle,
                        animationSpec = tween(durationMillis = spinDurationMs, easing = BallSpinEasing)
                    )
                }

                // Drop ball inward during the final ~1.3 s of the spin
                delay(3200)
                ballRadiusFraction.animateTo(
                    targetValue = 0.70f,  // centre of inner color band
                    animationSpec = tween(durationMillis = 1300,
                                         easing = CubicBezierEasing(0.4f, 0f, 0.8f, 1f))
                )

                ballJob.join()

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
    Text(text = "TAP TO SPIN", style = MaterialTheme.typography.labelSmall,
         color = Color.White.copy(alpha = 0.6f), modifier = modifier)
}
