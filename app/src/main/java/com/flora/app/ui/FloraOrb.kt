package com.flora.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Flora orb (v1.9): Flora ka apna gol nishan, sirf Compose Canvas
 * se bana hua — koi creator asset nahi.
 * busy (bol/soch rahi hai) ho to halka pulse karta hai, warna
 * static rehta hai taaki battery par bojh na pade.
 */
@Composable
fun FloraOrb(busy: Boolean) {
    if (busy) {
        val transition = rememberInfiniteTransition()
        val pulse by transition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            )
        )
        Canvas(modifier = Modifier.size(64.dp)) {
            val r = (size.minDimension / 2.5f) * pulse
            drawCircle(
                color = Color(0xFF7C4A03),
                radius = r,
                center = Offset(size.width / 2f, size.height / 2f)
            )
            drawCircle(
                color = Color(0xFFFFC978),
                radius = r * 0.6f,
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }
    } else {
        Canvas(modifier = Modifier.size(64.dp)) {
            val r = size.minDimension / 2.5f
            drawCircle(
                color = Color(0xFF7C4A03),
                radius = r,
                center = Offset(size.width / 2f, size.height / 2f)
            )
            drawCircle(
                color = Color(0xFFFFC978),
                radius = r * 0.6f,
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }
    }
}
