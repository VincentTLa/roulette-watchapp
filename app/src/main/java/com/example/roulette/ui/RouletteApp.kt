package com.example.roulette.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.launch

private val SpinEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

sealed class SpinState {
    object Idle : SpinState()
    object Spinning : SpinState()
    data class Result(val number: Int, val color: PocketColor, val index: Int) : SpinState()
}

@Composable
fun RouletteApp() {
    val coroutineScope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }
    var spinState by remember { mutableStateOf<SpinState>(SpinState.Idle) }

    val onSpin: () -> Unit = {
        if (spinState !is SpinState.Spinning) {
            coroutineScope.launch {
                val sweepAngle = 360f / 37f
                val targetIndex = (0..36).random()
                val targetOffset = (-(targetIndex * sweepAngle)).mod(360f)
                val currentMod = rotation.value.mod(360f)
                val delta = (targetOffset - currentMod).mod(360f)
                val newRotation = rotation.value + (5..8).random() * 360f + delta

                spinState = SpinState.Spinning
                rotation.animateTo(
                    targetValue = newRotation,
                    animationSpec = tween(durationMillis = 3500, easing = SpinEasing)
                )

                val number = EuropeanWheel.pocketOrder[targetIndex]
                spinState = SpinState.Result(number, EuropeanWheel.colorFor(number), targetIndex)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RouletteWheel(
            rotationDegrees = rotation.value,
            highlightIndex = (spinState as? SpinState.Result)?.index,
            isSpinning = spinState is SpinState.Spinning,
            onSpin = onSpin,
            modifier = Modifier.fillMaxSize()
        )

        when (val state = spinState) {
            is SpinState.Idle -> HintText(modifier = Modifier.align(Alignment.Center))
            is SpinState.Result -> ResultOverlay(
                number = state.number,
                color = state.color,
                modifier = Modifier.align(Alignment.Center)
            )
            is SpinState.Spinning -> Unit
        }
    }
}

@Composable
private fun ResultOverlay(number: Int, color: PocketColor, modifier: Modifier = Modifier) {
    val textColor = when (color) {
        PocketColor.RED -> Color(0xFFFF6B6B)
        PocketColor.BLACK -> Color(0xFFE0E0E0)
        PocketColor.GREEN -> Color(0xFF69F0AE)
    }
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f), shape = CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Text(
            text = color.name,
            style = MaterialTheme.typography.labelMedium,
            color = textColor.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun HintText(modifier: Modifier = Modifier) {
    Text(
        text = "TAP TO SPIN",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.6f),
        modifier = modifier
    )
}
