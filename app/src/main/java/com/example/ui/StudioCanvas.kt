package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.engine.WallpaperParams

/**
 * High-performance Studio Canvas wrapper providing touch gestures,
 * selection bounding boxes, and decoupled top-anchored tuning toolbars
 * for both M3 shapes and Nagasaki Depth Text layers.
 */
@Composable
fun StudioCanvas(
    params: WallpaperParams,
    selectedShapeId: String?,
    selectedTextId: String? = null,
    onSelectShape: (String?) -> Unit,
    onSelectTextLayer: (String?) -> Unit = {},
    onCommitShapePosition: (String, Float, Float) -> Unit,
    onCommitShapeScale: (String, Float) -> Unit,
    onCommitShapeRotation: (String, Float) -> Unit,
    onSetShapeColorIndex: (String, Int) -> Unit,
    onBringShapeToFront: (String) -> Unit,
    onSendShapeToBack: (String) -> Unit,
    onDuplicateShape: (String) -> Unit,
    onDeleteShape: (String) -> Unit,
    onToggleShapeWireframe: (String) -> Unit,
    onToggleShapeLiquidGlass: (String) -> Unit = {},
    onUpdateOpacity: (String, Float) -> Unit = { _, _ -> },
    onUpdateShadowRadius: (String, Float) -> Unit = { _, _ -> },
    onUpdateBlurRadius: (String, Float) -> Unit = { _, _ -> },
    // Depth Text Callbacks
    onUpdateTextContent: (String, String) -> Unit = { _, _ -> },
    onCommitTextPosition: (String, Float, Float) -> Unit = { _, _, _ -> },
    onCommitTextScale: (String, Float) -> Unit = { _, _ -> },
    onCommitTextRotation: (String, Float) -> Unit = { _, _ -> },
    onSetTextColorIndex: (String, Int) -> Unit = { _, _ -> },
    onBringTextToFront: (String) -> Unit = {},
    onSendTextToBack: (String) -> Unit = {},
    onDeleteText: (String) -> Unit = {},
    onUpdateTextOpacity: (String, Float) -> Unit = { _, _ -> },
    onUpdateTextShadowRadius: (String, Float) -> Unit = { _, _ -> },
    isFullscreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        StudioTouchCanvas(
            params = params,
            selectedShapeId = selectedShapeId,
            selectedTextId = selectedTextId,
            onSelectShape = onSelectShape,
            onSelectTextLayer = onSelectTextLayer,
            onCommitShapePosition = onCommitShapePosition,
            onCommitShapeScale = onCommitShapeScale,
            onCommitShapeRotation = onCommitShapeRotation,
            onSetShapeColorIndex = onSetShapeColorIndex,
            onBringShapeToFront = onBringShapeToFront,
            onSendShapeToBack = onSendShapeToBack,
            onDuplicateShape = onDuplicateShape,
            onDeleteShape = onDeleteShape,
            onToggleShapeWireframe = onToggleShapeWireframe,
            onToggleShapeLiquidGlass = onToggleShapeLiquidGlass,
            onUpdateOpacity = onUpdateOpacity,
            onUpdateShadowRadius = onUpdateShadowRadius,
            onUpdateBlurRadius = onUpdateBlurRadius,
            onUpdateTextContent = onUpdateTextContent,
            onCommitTextPosition = onCommitTextPosition,
            onCommitTextScale = onCommitTextScale,
            onCommitTextRotation = onCommitTextRotation,
            onSetTextColorIndex = onSetTextColorIndex,
            onBringTextToFront = onBringTextToFront,
            onSendTextToBack = onSendTextToBack,
            onDeleteText = onDeleteText,
            onUpdateTextOpacity = onUpdateTextOpacity,
            onUpdateTextShadowRadius = onUpdateTextShadowRadius,
            isFullscreen = isFullscreen,
            modifier = Modifier.fillMaxSize()
        )
    }
}
