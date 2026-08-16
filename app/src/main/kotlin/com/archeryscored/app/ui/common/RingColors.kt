package com.archeryscored.app.ui.common

import androidx.compose.ui.graphics.Color
import com.archeryscored.model.RingColor

private val ringColorMap = mapOf(
    RingColor.GOLD to Color(0xFFF4C430),
    RingColor.RED to Color(0xFFD32F2F),
    RingColor.BLUE to Color(0xFF1976D2),
    RingColor.BLACK to Color(0xFF212121),
    RingColor.WHITE to Color(0xFFF5F5F5)
)

fun colorFor(ringColor: RingColor): Color = ringColorMap[ringColor] ?: Color.Gray
