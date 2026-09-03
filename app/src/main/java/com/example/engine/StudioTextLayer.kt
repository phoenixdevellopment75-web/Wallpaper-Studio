package com.example.engine

import java.util.UUID

/**
 * Text alignment options for Studio Text Layers.
 */
enum class StudioTextAlign {
    LEFT,
    CENTER,
    RIGHT
}

/**
 * Depth Text Layer entity in Studio Mode.
 * Implements Nagasaki-style architectural display typography:
 * ultra-bold, condensed, zero inter-letter gap, exaggerated vertical ascenders.
 * Supports zIndex (bring to front/send to back), opacity, rotation,
 * and elevation drop shadow.
 */
data class StudioTextLayer(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "STUDIO",
    val normalizedX: Float = 0.5f,
    val normalizedY: Float = 0.5f,
    val normalizedSize: Float = 0.16f, // Relative to base dimension (minOf width, height)
    val rotationDeg: Float = 0.0f,
    val colorIndex: Int = 1,
    val customColorHex: Long? = null,
    val opacity: Float = 1.0f,
    val shadowRadius: Float = 16.0f, // Drop shadow elevation radius
    val zIndex: Int = 0,
    val isCondensed: Boolean = true,
    val letterSpacing: Float = -0.04f, // Zero to negative inter-letter gap for compact architectural look
    val textAlign: StudioTextAlign = StudioTextAlign.CENTER
)
