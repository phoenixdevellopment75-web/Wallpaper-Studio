package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.engine.WallpaperParams

/**
 * High-performance Studio Canvas wrapper providing touch gestures,
 * selection bounding boxes, and decoupled top-anchored shape tuning toolbar.
 */
@Composable
fun StudioCanvas(
    params: WallpaperParams,
    selectedShapeId: String?,
    onSelectShape: (String?) -> Unit,
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
    isFullscreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        StudioTouchCanvas(
            params = params,
            selectedShapeId = selectedShapeId,
            onSelectShape = onSelectShape,
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
            isFullscreen = isFullscreen,
            modifier = Modifier.fillMaxSize()
        )
    }
}
