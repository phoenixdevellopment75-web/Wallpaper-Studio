package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.engine.ProceduralM3Assembler
import com.example.engine.StudioTextLayer
import com.example.engine.WallpaperParams
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Reusable zero-allocation paints for selection frame and handles
private val selectionBoxPaint = android.graphics.Paint().apply {
    style = android.graphics.Paint.Style.STROKE
    color = android.graphics.Color.WHITE
    pathEffect = android.graphics.DashPathEffect(floatArrayOf(16f, 12f), 0f)
    isAntiAlias = true
}
private val selectionStemPaint = android.graphics.Paint().apply {
    style = android.graphics.Paint.Style.STROKE
    color = android.graphics.Color.WHITE
    isAntiAlias = true
}
private val selectionOuterHandlePaint = android.graphics.Paint().apply {
    style = android.graphics.Paint.Style.FILL
    color = android.graphics.Color.WHITE
    isAntiAlias = true
    setShadowLayer(8f, 0f, 4f, 0x66000000)
}
private val selectionInnerHandlePaint = android.graphics.Paint().apply {
    style = android.graphics.Paint.Style.FILL
    color = 0xFF6750A4.toInt()
    isAntiAlias = true
}
private val selectionRotHandlePaint = android.graphics.Paint().apply {
    style = android.graphics.Paint.Style.FILL
    color = 0xFF386A20.toInt()
    isAntiAlias = true
}

/**
 * High-performance, zero-friction interactive touch canvas for Studio mode.
 * Supports both M3 vector shapes and Nagasaki Depth Text layers in unified zIndex hierarchy.
 */
@Composable
fun StudioTouchCanvas(
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
    onDeleteShape: (String) -> Unit,
    onDuplicateShape: (String) -> Unit,
    onToggleShapeWireframe: (String) -> Unit,
    onToggleShapeLiquidGlass: (String) -> Unit = { _ -> },
    onUpdateOpacity: (String, Float) -> Unit = { _, _ -> },
    onUpdateShadowRadius: (String, Float) -> Unit = { _, _ -> },
    onUpdateBlurRadius: (String, Float) -> Unit = { _, _ -> },
    // Text Layer Callbacks
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
    isFullscreen: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val minTouchTargetPx = with(density) { 48.dp.toPx() }
    val handleTouchRadiusPx = with(density) { 28.dp.toPx() }

    val palette = params.palette
    val bgColors = if (palette.colors.size >= 2) {
        listOf(palette.colors[0], palette.colors[1])
    } else {
        listOf(Color(0xFF1C1B1F), Color(0xFF2B2930))
    }

    val selectedShape = params.customShapes.find { it.id == selectedShapeId }
    val selectedText = params.customTexts.find { it.id == selectedTextId }

    // Fast local drag offsets to avoid recomposition friction
    var localDragOffset by remember(selectedShapeId, selectedTextId) { mutableStateOf(Offset.Zero) }
    var activeCornerHandleIndex by remember { mutableIntStateOf(-1) }
    var localScaleMultiplier by remember(selectedShapeId, selectedTextId) { mutableFloatStateOf(1f) }
    var localRotationDeg by remember(selectedShapeId, selectedTextId) { mutableStateOf<Float?>(null) }

    Box(
        modifier = modifier
            .testTag("studio_touch_canvas_root")
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .then(
                    if (isFullscreen) Modifier.fillMaxSize()
                    else Modifier
                        .fillMaxSize(0.94f)
                        .aspectRatio(params.aspectRatio.ratio)
                        .clip(RoundedCornerShape(28.dp))
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(28.dp)
                        )
                        .shadow(16.dp, RoundedCornerShape(28.dp))
                )
                .background(Brush.verticalGradient(bgColors))
                // Two-finger multi-touch transform (Pinch-to-scale & rotate directly on canvas)
                .pointerInput(selectedShapeId, selectedTextId, params.scale) {
                    detectTransformGestures { _, _, zoom, rotation ->
                        if (selectedShapeId != null) {
                            val curr = params.customShapes.find { it.id == selectedShapeId }
                            if (curr != null) {
                                if (zoom != 1f) {
                                    val newW = (curr.normalizedWidth * zoom).coerceIn(0.12f, 1.4f)
                                    onCommitShapeScale(selectedShapeId, newW)
                                }
                                if (rotation != 0f) {
                                    val newRot = (curr.rotationDeg + rotation) % 360f
                                    onCommitShapeRotation(selectedShapeId, newRot)
                                }
                            }
                        } else if (selectedTextId != null) {
                            val currText = params.customTexts.find { it.id == selectedTextId }
                            if (currText != null) {
                                if (zoom != 1f) {
                                    val newSize = (currText.normalizedSize * zoom).coerceIn(0.06f, 0.45f)
                                    onCommitTextScale(selectedTextId, newSize)
                                }
                                if (rotation != 0f) {
                                    val newRot = (currText.rotationDeg + rotation) % 360f
                                    onCommitTextRotation(selectedTextId, newRot)
                                }
                            }
                        }
                    }
                }
                // Tap hit testing & selection for both shapes and text layers
                .pointerInput(params.customShapes, params.customTexts) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            val canvasW = size.width.toFloat()
                            val canvasH = size.height.toFloat()
                            val baseDim = minOf(canvasW, canvasH)
                            val normTapX = tapOffset.x / canvasW
                            val normTapY = tapOffset.y / canvasH

                            // Check text layers first (if higher zIndex)
                            sealed class TargetHit(val z: Int, val isShape: Boolean, val id: String)
                            val candidates = mutableListOf<TargetHit>()

                            for (s in params.customShapes) {
                                val scaleFactor = params.scale * baseDim
                                val shapeW = s.normalizedWidth * scaleFactor
                                val shapeH = if (s.type.isProportional1to1) shapeW else s.normalizedHeight * scaleFactor
                                val halfNormW = (shapeW / canvasW) / 1.7f
                                val halfNormH = (shapeH / canvasH) / 1.7f
                                val dx = kotlin.math.abs(normTapX - s.normalizedX)
                                val dy = kotlin.math.abs(normTapY - s.normalizedY)
                                if (dx <= halfNormW && dy <= halfNormH) {
                                    candidates.add(TargetHit(s.zIndex, true, s.id))
                                }
                            }

                            for (t in params.customTexts) {
                                val bounds = ProceduralM3Assembler.measureTextBounds(t, baseDim, params.scale)
                                val halfNormW = ((bounds.width() + 32f) / canvasW) / 2f
                                val halfNormH = ((bounds.height() + 32f) / canvasH) / 2f
                                val dx = kotlin.math.abs(normTapX - t.normalizedX)
                                val dy = kotlin.math.abs(normTapY - t.normalizedY)
                                if (dx <= halfNormW && dy <= halfNormH) {
                                    candidates.add(TargetHit(t.zIndex, false, t.id))
                                }
                            }

                            val topHit = candidates.maxByOrNull { it.z }
                            if (topHit == null) {
                                onSelectShape(null)
                                onSelectTextLayer(null)
                            } else if (topHit.isShape) {
                                onSelectShape(topHit.id)
                                onSelectTextLayer(null)
                            } else {
                                onSelectTextLayer(topHit.id)
                                onSelectShape(null)
                            }
                        }
                    )
                }
                // Zero-friction drag & direct corner resize handles
                .pointerInput(selectedShapeId, selectedTextId, params.customShapes, params.customTexts, params.scale) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            localDragOffset = Offset.Zero
                            activeCornerHandleIndex = -1
                            localScaleMultiplier = 1f
                            localRotationDeg = null

                            val canvasW = size.width.toFloat()
                            val canvasH = size.height.toFloat()
                            val baseDim = minOf(canvasW, canvasH)
                            val scaleFactor = params.scale * baseDim

                            if (selectedShape != null) {
                                val cx = selectedShape.normalizedX * canvasW
                                val cy = selectedShape.normalizedY * canvasH
                                val w = selectedShape.normalizedWidth * scaleFactor
                                val h = if (selectedShape.type.isProportional1to1) w else selectedShape.normalizedHeight * scaleFactor
                                val halfW = (w / 2f) + 16.dp.toPx()
                                val halfH = (h / 2f) + 16.dp.toPx()
                                val stemLength = 26.dp.toPx()
                                val rotRad = ((selectedShape.rotationDeg + params.rotationDegrees) * (PI / 180f)).toFloat()

                                val cornerOffsets = listOf(
                                    Offset(-halfW, -halfH),
                                    Offset(halfW, -halfH),
                                    Offset(halfW, halfH),
                                    Offset(-halfW, halfH)
                                )

                                cornerOffsets.forEachIndexed { index, corner ->
                                    val rotatedX = cx + corner.x * cos(rotRad) - corner.y * sin(rotRad)
                                    val rotatedY = cy + corner.x * sin(rotRad) + corner.y * cos(rotRad)
                                    val dist = sqrt((startOffset.x - rotatedX) * (startOffset.x - rotatedX) + (startOffset.y - rotatedY) * (startOffset.y - rotatedY))
                                    if (dist <= handleTouchRadiusPx) {
                                        activeCornerHandleIndex = index
                                    }
                                }

                                val stemRotX = cx + 0f * cos(rotRad) - (-halfH - stemLength) * sin(rotRad)
                                val stemRotY = cy + 0f * sin(rotRad) + (-halfH - stemLength) * cos(rotRad)
                                val stemDist = sqrt((startOffset.x - stemRotX) * (startOffset.x - stemRotX) + (startOffset.y - stemRotY) * (startOffset.y - stemRotY))
                                if (stemDist <= handleTouchRadiusPx) {
                                    activeCornerHandleIndex = 4
                                }
                            } else if (selectedText != null) {
                                val cx = selectedText.normalizedX * canvasW
                                val cy = selectedText.normalizedY * canvasH
                                val bounds = ProceduralM3Assembler.measureTextBounds(selectedText, baseDim, params.scale)
                                val halfW = (bounds.width() / 2f) + 16.dp.toPx()
                                val halfH = (bounds.height() / 2f) + 16.dp.toPx()
                                val stemLength = 26.dp.toPx()
                                val rotRad = ((selectedText.rotationDeg + params.rotationDegrees) * (PI / 180f)).toFloat()

                                val cornerOffsets = listOf(
                                    Offset(-halfW, -halfH),
                                    Offset(halfW, -halfH),
                                    Offset(halfW, halfH),
                                    Offset(-halfW, halfH)
                                )

                                cornerOffsets.forEachIndexed { index, corner ->
                                    val rotatedX = cx + corner.x * cos(rotRad) - corner.y * sin(rotRad)
                                    val rotatedY = cy + corner.x * sin(rotRad) + corner.y * cos(rotRad)
                                    val dist = sqrt((startOffset.x - rotatedX) * (startOffset.x - rotatedX) + (startOffset.y - rotatedY) * (startOffset.y - rotatedY))
                                    if (dist <= handleTouchRadiusPx) {
                                        activeCornerHandleIndex = index
                                    }
                                }

                                val stemRotX = cx + 0f * cos(rotRad) - (-halfH - stemLength) * sin(rotRad)
                                val stemRotY = cy + 0f * sin(rotRad) + (-halfH - stemLength) * cos(rotRad)
                                val stemDist = sqrt((startOffset.x - stemRotX) * (startOffset.x - stemRotX) + (startOffset.y - stemRotY) * (startOffset.y - stemRotY))
                                if (stemDist <= handleTouchRadiusPx) {
                                    activeCornerHandleIndex = 4
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val canvasW = size.width.toFloat()
                            val canvasH = size.height.toFloat()
                            val baseDim = minOf(canvasW, canvasH)
                            val scaleFactor = params.scale * baseDim

                            if (selectedShape != null) {
                                val cx = selectedShape.normalizedX * canvasW
                                val cy = selectedShape.normalizedY * canvasH

                                if (activeCornerHandleIndex == 4) {
                                    val currentTouch = change.position
                                    val angleRad = kotlin.math.atan2(currentTouch.y - cy, currentTouch.x - cx)
                                    var deg = Math.toDegrees(angleRad.toDouble()).toFloat() + 90f
                                    if (deg < 0) deg += 360f
                                    localRotationDeg = deg % 360f
                                } else if (activeCornerHandleIndex in 0..3) {
                                    val currentDist = sqrt((change.position.x - cx) * (change.position.x - cx) + (change.position.y - cy) * (change.position.y - cy))
                                    val w = selectedShape.normalizedWidth * scaleFactor
                                    val h = if (selectedShape.type.isProportional1to1) w else selectedShape.normalizedHeight * scaleFactor
                                    val initialDist = sqrt((w / 2f) * (w / 2f) + (h / 2f) * (h / 2f)).coerceAtLeast(1f)
                                    val ratio = (currentDist / initialDist).coerceIn(0.25f, 3.5f)
                                    localScaleMultiplier = ratio
                                } else {
                                    localDragOffset = Offset(
                                        localDragOffset.x + dragAmount.x,
                                        localDragOffset.y + dragAmount.y
                                    )
                                }
                            } else if (selectedText != null) {
                                val cx = selectedText.normalizedX * canvasW
                                val cy = selectedText.normalizedY * canvasH

                                if (activeCornerHandleIndex == 4) {
                                    val currentTouch = change.position
                                    val angleRad = kotlin.math.atan2(currentTouch.y - cy, currentTouch.x - cx)
                                    var deg = Math.toDegrees(angleRad.toDouble()).toFloat() + 90f
                                    if (deg < 0) deg += 360f
                                    localRotationDeg = deg % 360f
                                } else if (activeCornerHandleIndex in 0..3) {
                                    val currentDist = sqrt((change.position.x - cx) * (change.position.x - cx) + (change.position.y - cy) * (change.position.y - cy))
                                    val bounds = ProceduralM3Assembler.measureTextBounds(selectedText, baseDim, params.scale)
                                    val initialDist = sqrt((bounds.width() / 2f) * (bounds.width() / 2f) + (bounds.height() / 2f) * (bounds.height() / 2f)).coerceAtLeast(1f)
                                    val ratio = (currentDist / initialDist).coerceIn(0.25f, 3.5f)
                                    localScaleMultiplier = ratio
                                } else {
                                    localDragOffset = Offset(
                                        localDragOffset.x + dragAmount.x,
                                        localDragOffset.y + dragAmount.y
                                    )
                                }
                            }
                        },
                        onDragEnd = {
                            val canvasW = size.width.toFloat()
                            val canvasH = size.height.toFloat()
                            val baseDim = minOf(canvasW, canvasH)
                            val scaleFactor = params.scale * baseDim

                            if (selectedShape != null) {
                                if (activeCornerHandleIndex == 4 && localRotationDeg != null) {
                                    onCommitShapeRotation(selectedShape.id, localRotationDeg!!)
                                } else if (activeCornerHandleIndex in 0..3) {
                                    val newNormW = (selectedShape.normalizedWidth * localScaleMultiplier)
                                        .coerceIn(minTouchTargetPx / scaleFactor, 1.4f)
                                    onCommitShapeScale(selectedShape.id, newNormW)
                                } else if (activeCornerHandleIndex == -1 && localDragOffset != Offset.Zero) {
                                    val newX = (selectedShape.normalizedX + (localDragOffset.x / canvasW)).coerceIn(0.05f, 0.95f)
                                    val newY = (selectedShape.normalizedY + (localDragOffset.y / canvasH)).coerceIn(0.05f, 0.95f)
                                    onCommitShapePosition(selectedShape.id, newX, newY)
                                }
                            } else if (selectedText != null) {
                                if (activeCornerHandleIndex == 4 && localRotationDeg != null) {
                                    onCommitTextRotation(selectedText.id, localRotationDeg!!)
                                } else if (activeCornerHandleIndex in 0..3) {
                                    val newSize = (selectedText.normalizedSize * localScaleMultiplier).coerceIn(0.06f, 0.45f)
                                    onCommitTextScale(selectedText.id, newSize)
                                } else if (activeCornerHandleIndex == -1 && localDragOffset != Offset.Zero) {
                                    val newX = (selectedText.normalizedX + (localDragOffset.x / canvasW)).coerceIn(0.05f, 0.95f)
                                    val newY = (selectedText.normalizedY + (localDragOffset.y / canvasH)).coerceIn(0.05f, 0.95f)
                                    onCommitTextPosition(selectedText.id, newX, newY)
                                }
                            }

                            localDragOffset = Offset.Zero
                            activeCornerHandleIndex = -1
                            localScaleMultiplier = 1f
                            localRotationDeg = null
                        },
                        onDragCancel = {
                            localDragOffset = Offset.Zero
                            activeCornerHandleIndex = -1
                            localScaleMultiplier = 1f
                            localRotationDeg = null
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val baseDim = minOf(size.width, size.height)
                val colors = palette.colors

                // Unified Render Items sorted by zIndex
                sealed class CanvasItem(val z: Int) {
                    class Shape(val shape: com.example.engine.CustomCanvasShape) : CanvasItem(shape.zIndex)
                    class Text(val text: StudioTextLayer) : CanvasItem(text.zIndex)
                }

                val allItems = (params.customShapes.map { CanvasItem.Shape(it) } +
                        params.customTexts.map { CanvasItem.Text(it) }).sortedBy { it.z }

                for (item in allItems) {
                    when (item) {
                        is CanvasItem.Shape -> {
                            val shape = item.shape
                            val isSelected = shape.id == selectedShapeId
                            val dragX = if (isSelected) localDragOffset.x else 0f
                            val dragY = if (isSelected) localDragOffset.y else 0f

                            val cx = (shape.normalizedX * size.width) + dragX
                            val cy = (shape.normalizedY * size.height) + dragY
                            val scaleFactor = params.scale * baseDim
                            val effectiveScale = if (isSelected && activeCornerHandleIndex in 0..3) localScaleMultiplier else 1f
                            val effectiveRot = if (isSelected && activeCornerHandleIndex == 4 && localRotationDeg != null) {
                                localRotationDeg!!
                            } else {
                                shape.rotationDeg + params.rotationDegrees
                            }

                            val w = shape.normalizedWidth * scaleFactor * effectiveScale
                            val h = if (shape.type.isProportional1to1) {
                                w
                            } else {
                                shape.normalizedHeight * scaleFactor * effectiveScale
                            }

                            val shapeColor = if (shape.customColorHex != null) {
                                Color(shape.customColorHex.toULong())
                            } else {
                                colors[shape.colorIndex % colors.size]
                            }

                            drawIntoCanvas { composeCanvas ->
                                val nativeCanvas = composeCanvas.nativeCanvas
                                ProceduralM3Assembler.drawSingleShape(
                                    canvas = nativeCanvas,
                                    type = shape.type,
                                    cx = cx,
                                    cy = cy,
                                    w = w,
                                    h = h,
                                    rotationDeg = effectiveRot,
                                    color = shapeColor.hashCode(),
                                    opacity = shape.opacity,
                                    isWireframe = shape.isWireframe || params.isWireframe,
                                    strokeWidth = shape.strokeWidth * (size.width / 500f),
                                    scallopLobes = shape.scallopLobes,
                                    castShadow = true,
                                    shadowRadius = shape.shadowRadius,
                                    blurRadius = shape.blurRadius,
                                    isLiquidGlass = shape.isLiquidGlass
                                )
                            }

                            if (isSelected) {
                                drawSelectionBoundingBoxWithCorners(
                                    cx = cx,
                                    cy = cy,
                                    w = w,
                                    h = h,
                                    rotationDeg = effectiveRot
                                )
                            }
                        }
                        is CanvasItem.Text -> {
                            val textLayer = item.text
                            val isSelected = textLayer.id == selectedTextId
                            val dragX = if (isSelected) localDragOffset.x else 0f
                            val dragY = if (isSelected) localDragOffset.y else 0f

                            val cx = (textLayer.normalizedX * size.width) + dragX
                            val cy = (textLayer.normalizedY * size.height) + dragY
                            val effectiveScale = if (isSelected && activeCornerHandleIndex in 0..3) localScaleMultiplier else 1f
                            val effectiveRot = if (isSelected && activeCornerHandleIndex == 4 && localRotationDeg != null) {
                                localRotationDeg!!
                            } else {
                                textLayer.rotationDeg + params.rotationDegrees
                            }
                            val fontSizePx = textLayer.normalizedSize * baseDim * params.scale * effectiveScale

                            val textColor = if (textLayer.customColorHex != null) {
                                textLayer.customColorHex.toInt()
                            } else {
                                colors[textLayer.colorIndex % colors.size].hashCode()
                            }

                            drawIntoCanvas { composeCanvas ->
                                ProceduralM3Assembler.drawSingleText(
                                    canvas = composeCanvas.nativeCanvas,
                                    textLayer = textLayer,
                                    cx = cx,
                                    cy = cy,
                                    fontSizePx = fontSizePx,
                                    color = textColor,
                                    rotationDeg = effectiveRot
                                )
                            }

                            if (isSelected) {
                                val bounds = ProceduralM3Assembler.measureTextBounds(textLayer, baseDim, params.scale * effectiveScale)
                                drawSelectionBoundingBoxWithCorners(
                                    cx = cx,
                                    cy = cy,
                                    w = bounds.width(),
                                    h = bounds.height(),
                                    rotationDeg = effectiveRot
                                )
                            }
                        }
                    }
                }
            }

            // Viewport-clamped floating toolbar for Shape
            if (selectedShape != null && !isFullscreen) {
                val canvasWidthPx = constraints.maxWidth.toFloat()
                val canvasHeightPx = constraints.maxHeight.toFloat()
                val baseDim = minOf(canvasWidthPx, canvasHeightPx)
                val scaleFactor = params.scale * baseDim
                val shapeW = if (selectedShape.type.isProportional1to1) {
                    selectedShape.normalizedWidth * scaleFactor * localScaleMultiplier
                } else {
                    selectedShape.normalizedWidth * scaleFactor * localScaleMultiplier
                }
                val shapeH = if (selectedShape.type.isProportional1to1) {
                    shapeW
                } else {
                    selectedShape.normalizedHeight * scaleFactor * localScaleMultiplier
                }
                val cx = (selectedShape.normalizedX * canvasWidthPx) + localDragOffset.x
                val cy = (selectedShape.normalizedY * canvasHeightPx) + localDragOffset.y

                val shapeBounds = Rect(
                    left = cx - (shapeW / 2f),
                    top = cy - (shapeH / 2f),
                    right = cx + (shapeW / 2f),
                    bottom = cy + (shapeH / 2f)
                )

                StudioShapeToolbar(
                    shape = selectedShape,
                    palette = palette,
                    shapeBounds = shapeBounds,
                    canvasTopOffsetDp = 0.dp,
                    containerHeightDp = with(density) { constraints.maxHeight.toDp() },
                    onColorSelected = { colorIdx -> onSetShapeColorIndex(selectedShape.id, colorIdx) },
                    onRotate = { deg -> onCommitShapeRotation(selectedShape.id, deg) },
                    onBringToFront = { onBringShapeToFront(selectedShape.id) },
                    onSendToBack = { onSendShapeToBack(selectedShape.id) },
                    onDuplicate = { onDuplicateShape(selectedShape.id) },
                    onDelete = { onDeleteShape(selectedShape.id) },
                    onToggleWireframe = { onToggleShapeWireframe(selectedShape.id) },
                    onToggleLiquidGlass = { onToggleShapeLiquidGlass(selectedShape.id) },
                    onUpdateOpacity = { op -> onUpdateOpacity(selectedShape.id, op) },
                    onUpdateShadowRadius = { sr -> onUpdateShadowRadius(selectedShape.id, sr) },
                    onUpdateBlurRadius = { br -> onUpdateBlurRadius(selectedShape.id, br) },
                    onDeselect = { onSelectShape(null) }
                )
            }

            // Viewport-clamped floating toolbar for Depth Text
            if (selectedText != null && !isFullscreen) {
                val canvasWidthPx = constraints.maxWidth.toFloat()
                val canvasHeightPx = constraints.maxHeight.toFloat()
                val baseDim = minOf(canvasWidthPx, canvasHeightPx)
                val bounds = ProceduralM3Assembler.measureTextBounds(selectedText, baseDim, params.scale * localScaleMultiplier)
                val cx = (selectedText.normalizedX * canvasWidthPx) + localDragOffset.x
                val cy = (selectedText.normalizedY * canvasHeightPx) + localDragOffset.y

                val textRect = Rect(
                    left = cx - (bounds.width() / 2f),
                    top = cy - (bounds.height() / 2f),
                    right = cx + (bounds.width() / 2f),
                    bottom = cy + (bounds.height() / 2f)
                )

                StudioTextToolbar(
                    textLayer = selectedText,
                    palette = palette,
                    textBounds = textRect,
                    canvasTopOffsetDp = 0.dp,
                    containerHeightDp = with(density) { constraints.maxHeight.toDp() },
                    onUpdateText = { str -> onUpdateTextContent(selectedText.id, str) },
                    onColorSelected = { cIdx -> onSetTextColorIndex(selectedText.id, cIdx) },
                    onRotate = { deg -> onCommitTextRotation(selectedText.id, deg) },
                    onBringToFront = { onBringTextToFront(selectedText.id) },
                    onSendToBack = { onSendTextToBack(selectedText.id) },
                    onDelete = { onDeleteText(selectedText.id) },
                    onUpdateOpacity = { op -> onUpdateTextOpacity(selectedText.id, op) },
                    onUpdateShadowRadius = { sr -> onUpdateTextShadowRadius(selectedText.id, sr) },
                    onDeselect = { onSelectTextLayer(null) }
                )
            }
        }
    }
}

/**
 * Draws the bounding box and 4 prominent corner anchor nodes plus rotation stem.
 */
private fun DrawScope.drawSelectionBoundingBoxWithCorners(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    rotationDeg: Float
) {
    val halfW = (w / 2f) + 16.dp.toPx()
    val halfH = (h / 2f) + 16.dp.toPx()

    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        native.save()
        if (rotationDeg != 0f) {
            native.rotate(rotationDeg, cx, cy)
        }

        // 1. Dashed Bounding Outline
        selectionBoxPaint.strokeWidth = 2.dp.toPx()
        native.drawRoundRect(
            cx - halfW, cy - halfH, cx + halfW, cy + halfH,
            16.dp.toPx(), 16.dp.toPx(), selectionBoxPaint
        )

        // Rotation stem line extending upward
        val stemLength = 26.dp.toPx()
        selectionStemPaint.strokeWidth = 2.dp.toPx()
        native.drawLine(cx, cy - halfH, cx, cy - halfH - stemLength, selectionStemPaint)

        // 2. 4 Distinct Corner Anchor Nodes + Rotation Handle
        val cornerRadiusPx = 10.dp.toPx()
        val cornerDotRadiusPx = 4.dp.toPx()

        val corners = arrayOf(
            Pair(cx - halfW, cy - halfH),
            Pair(cx + halfW, cy - halfH),
            Pair(cx + halfW, cy + halfH),
            Pair(cx - halfW, cy + halfH)
        )

        for ((x, y) in corners) {
            native.drawCircle(x, y, cornerRadiusPx, selectionOuterHandlePaint)
            native.drawCircle(x, y, cornerDotRadiusPx, selectionInnerHandlePaint)
        }

        // Rotation Handle Node
        val rotY = cy - halfH - stemLength
        native.drawCircle(cx, rotY, cornerRadiusPx, selectionOuterHandlePaint)
        native.drawCircle(cx, rotY, cornerDotRadiusPx, selectionRotHandlePaint)

        native.restore()
    }
}
