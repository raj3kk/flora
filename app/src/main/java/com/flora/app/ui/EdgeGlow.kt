package com.flora.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

/**
 * P1 appearance bundle — Edge Glow (SPEC-FREEZE: chat-area
 * ke kinaron par animated border; system-wide / lock-screen
 * edge lighting is scope me NAHI hai — uske liye alag spec
 * + notification/overlay raasta chahiye hoga).
 *
 * Caller is composable ko sirf tab dikhata hai jab edge-glow
 * toggle ON ho AUR Flora bol/sun rahi ho (SPEAKING /
 * LISTENING). Baaki haalat me ye composable call hi nahi
 * hota, isliye animation loop bhi nahi chalta.
 *
 * Ye Box clickable nahi hai — touch neeche ke messages
 * list tak jaata hai, glow kabhi scroll/input ko rokta nahi.
 * Rang wahi avatar rang hai (orbOuterColor — N3 colors).
 */
@Composable
fun EdgeGlowBorder(avatar: String = "gulabi") {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        )
    )
    val color = orbOuterColor(avatar)
    Box(
        modifier = Modifier.fillMaxSize()
            .border(6.dp, color.copy(alpha = alpha), RectangleShape)
            .border(2.dp, color.copy(alpha = 0.9f), RectangleShape)
    )
}
