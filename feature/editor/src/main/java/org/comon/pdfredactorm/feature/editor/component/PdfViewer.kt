package org.comon.pdfredactorm.feature.editor.component

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.get
import org.comon.pdfredactorm.core.model.DetectedPii
import org.comon.pdfredactorm.core.model.RedactionMask
import kotlin.math.abs
import org.comon.pdfredactorm.feature.editor.R

@Composable
fun PdfViewer(
    bitmap: Bitmap,
    redactions: List<RedactionMask>,
    detectedPii: List<DetectedPii>,
    isMaskingMode: Boolean,
    isColorPickingMode: Boolean,
    pdfPageWidth: Int,
    pdfPageHeight: Int,
    onAddRedaction: (Float, Float, Float, Float) -> Unit,
    onConvertPiiToMask: (DetectedPii) -> Unit,
    onRemoveDetectedPii: (DetectedPii) -> Unit,
    onColorPicked: (Int) -> Unit,
    onColorPickDrag: (Offset?, Int?) -> Unit,
    onShowDeleteMaskDialog: (String) -> Unit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Calculate initial scale to fit entire PDF within canvas
    // Use the minimum of width-based and height-based scales to ensure nothing is cut off
    val fitScale = remember(bitmap, canvasSize) {
        if (canvasSize.height > 0 && canvasSize.width > 0 && bitmap.height > 0 && bitmap.width > 0) {
            val widthScale = canvasSize.width.toFloat() / bitmap.width.toFloat()
            val heightScale = canvasSize.height.toFloat() / bitmap.height.toFloat()
            // Use minimum to ensure entire document fits
            minOf(widthScale, heightScale)
        } else {
            1f
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Reset scale and offset when bitmap or canvas size changes
    LaunchedEffect(bitmap, canvasSize) {
        scale = 1f
        offset = Offset.Zero
    }

    // Temporary drag state for drawing new mask
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }

    // Context menu state for detected PII
    var selectedPii by remember { mutableStateOf<DetectedPii?>(null) }
    var showPiiContextMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(fitScale, bitmap) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (!isMaskingMode && !isColorPickingMode) {
                        // Update scale
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        scale = newScale

                        // Only allow panning when zoomed in (scale > 1)
                        if (scale > 1f) {
                            // Calculate bounds to keep PDF within view
                            val scaledWidth = bitmap.width * fitScale * scale
                            val scaledHeight = bitmap.height * fitScale * scale

                            val maxOffsetX = 0f
                            val minOffsetX = -(scaledWidth - canvasSize.width).coerceAtLeast(0f)
                            val maxOffsetY = 0f
                            val minOffsetY = -(scaledHeight - canvasSize.height).coerceAtLeast(0f)

                            // Apply pan with bounds
                            val newOffset = offset + pan
                            offset = Offset(
                                x = newOffset.x.coerceIn(minOffsetX, maxOffsetX),
                                y = newOffset.y.coerceIn(minOffsetY, maxOffsetY)
                            )
                        } else {
                            // Reset offset when scale returns to 1
                            offset = Offset.Zero
                        }
                    }
                }

            }
            .pointerInput(isColorPickingMode, fitScale, bitmap) {
                if (isColorPickingMode) {
                    var lastColor: Int? = null
                    detectDragGestures(
                        onDragStart = { dragStartOffset ->
                            // Initial pick
                            val totalScale = fitScale * scale
                            // offset is the pan offset (state), dragStartOffset is the touch position
                             val startX = (dragStartOffset.x - offset.x) / totalScale
                             val startY = (dragStartOffset.y - offset.y) / totalScale
                             
                             if (startX >= 0 && startX < bitmap.width && startY >= 0 && startY < bitmap.height) {
                                 val pixelColor = bitmap[startX.toInt(), startY.toInt()]
                                 lastColor = pixelColor
                                 onColorPickDrag(dragStartOffset, pixelColor)
                             }
                        },
                        onDragEnd = {
                            lastColor?.let { color ->
                                onColorPicked(color)
                            }
                            onColorPickDrag(null, null)
                            lastColor = null
                        },
                        onDragCancel = {
                            onColorPickDrag(null, null)
                            lastColor = null
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val position = change.position
                            
                            // Calculate bitmap coordinates
                            val totalScale = fitScale * scale
                            // position is relative to the Box (Canvas)
                            // The content is translated by 'offset' and scaled by 'totalScale'
                            
                            val bitmapX = (position.x - offset.x) / totalScale
                            val bitmapY = (position.y - offset.y) / totalScale
                            
                            if (bitmapX >= 0 && bitmapX < bitmap.width && bitmapY >= 0 && bitmapY < bitmap.height) {
                                val pixelColor = bitmap[bitmapX.toInt(), bitmapY.toInt()]
                                lastColor = pixelColor
                                onColorPickDrag(position, pixelColor)
                            }
                        }
                    )
                }
            }
            .pointerInput(isColorPickingMode, fitScale, bitmap) {
                if (isColorPickingMode) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            val totalScale = fitScale * scale
                            val bitmapX = (tapOffset.x - offset.x) / totalScale
                            val bitmapY = (tapOffset.y - offset.y) / totalScale
                            
                            if (bitmapX >= 0 && bitmapX < bitmap.width && bitmapY >= 0 && bitmapY < bitmap.height) {
                                val pixelColor = bitmap[bitmapX.toInt(), bitmapY.toInt()]
                                onColorPicked(pixelColor)
                            }
                        },
                        onPress = { pressOffset ->
                            // Handle initial press for drag preview
                            val totalScale = fitScale * scale
                            val bitmapX = (pressOffset.x - offset.x) / totalScale
                            val bitmapY = (pressOffset.y - offset.y) / totalScale
                            
                            if (bitmapX >= 0 && bitmapX < bitmap.width && bitmapY >= 0 && bitmapY < bitmap.height) {
                                val pixelColor = bitmap[bitmapX.toInt(), bitmapY.toInt()]
                                onColorPickDrag(pressOffset, pixelColor)
                            }
                            tryAwaitRelease()
                            // On release (if not dragged enough to trigger drag gesture), confirm selection
                            // But detectTapGestures handles onTap separately.
                            // We need to clear preview on release if it was a tap.
                            onColorPickDrag(null, null)
                            
                            // If it was a tap, onTap will fire.
                            // If it was a drag, detectDragGestures will take over? 
                            // Compose gesture handling can be tricky. 
                            // Let's use a simpler approach: update preview in onPress, clear in onRelease.
                            // Actual selection happens in onTap or onDragEnd.
                        }
                    )
                }
            }
            .pointerInput(isMaskingMode, fitScale) {
                if (isMaskingMode) {
                    detectDragGestures(
                        onDragStart = { dragStart = it },
                        onDragEnd = {
                            if (dragStart != null && dragEnd != null) {
                                // Convert screen coordinates to bitmap coordinates
                                val totalScale = fitScale * scale
                                val start = (dragStart!! - offset) / totalScale
                                val end = (dragEnd!! - offset) / totalScale

                                // Map from Bitmap size to PDF size
                                val bitmapWidth = bitmap.width.toFloat()
                                val bitmapHeight = bitmap.height.toFloat()

                                val scaleX = pdfPageWidth / bitmapWidth
                                val scaleY = pdfPageHeight / bitmapHeight

                                val x = minOf(start.x, end.x) * scaleX
                                val y = minOf(start.y, end.y) * scaleY
                                val w = abs(start.x - end.x) * scaleX
                                val h = abs(start.y - end.y) * scaleY

                                onAddRedaction(x, y, w, h)
                            }
                            dragStart = null
                            dragEnd = null
                        },
                        onDrag = { change, _ ->
                            dragEnd = change.position
                        }
                    )
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = fitScale * scale,
                    scaleY = fitScale * scale,
                    translationX = offset.x,
                    translationY = offset.y,
                    transformOrigin = TransformOrigin(0f, 0f)
                )
                .pointerInput(isMaskingMode, isColorPickingMode, fitScale) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            if (!isMaskingMode && !isColorPickingMode) {
                                val bitmapWidth = bitmap.width.toFloat()
                                val bitmapHeight = bitmap.height.toFloat()
                                val scaleX = pdfPageWidth / bitmapWidth
                                val scaleY = pdfPageHeight / bitmapHeight

                                val tapX = tapOffset.x * scaleX
                                val tapY = tapOffset.y * scaleY

                                // Check if tap is inside any redaction
                                val hitMask = redactions.find { mask ->
                                    tapX >= mask.x && tapX <= mask.x + mask.width &&
                                    tapY >= mask.y && tapY <= mask.y + mask.height
                                }

                                if (hitMask != null) {
                                    onShowDeleteMaskDialog(hitMask.id)
                                    return@detectTapGestures
                                }

                                // Check if tap is inside any detected PII
                                val hitPii = detectedPii.find { pii ->
                                    tapX >= pii.x && tapX <= pii.x + pii.width &&
                                    tapY >= pii.y && tapY <= pii.y + pii.height
                                }

                                if (hitPii != null) {
                                    selectedPii = hitPii
                                    showPiiContextMenu = true
                                }
                            }
                        }
                    )
                }
        ) {
            // Draw PDF Page
            drawImage(bitmap.asImageBitmap())
            
            // Draw Existing Redactions and Detected PII
            val bitmapWidth = bitmap.width.toFloat()
            val bitmapHeight = bitmap.height.toFloat()
            val scaleX = bitmapWidth / pdfPageWidth
            val scaleY = bitmapHeight / pdfPageHeight

            // Draw detected PII (in yellow/blue with border)
            detectedPii.forEach { pii ->
                drawRect(
                    color = Color.Yellow.copy(alpha = 0.3f),
                    topLeft = Offset(pii.x * scaleX, pii.y * scaleY),
                    size = Size(pii.width * scaleX, pii.height * scaleY)
                )
                drawRect(
                    color = Color.Blue,
                    topLeft = Offset(pii.x * scaleX, pii.y * scaleY),
                    size = Size(pii.width * scaleX, pii.height * scaleY),
                    style = Stroke(width = 2f)
                )
            }

            // Draw redactions (with mask color)
            redactions.forEach { mask ->
                // Fill with mask color
                drawRect(
                    color = Color(mask.color).copy(alpha = 0.5f),
                    topLeft = Offset(mask.x * scaleX, mask.y * scaleY),
                    size = Size(mask.width * scaleX, mask.height * scaleY)
                )
                // Draw black border for visibility
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(mask.x * scaleX, mask.y * scaleY),
                    size = Size(mask.width * scaleX, mask.height * scaleY),
                    style = Stroke(width = 2f)
                )
            }
            
            // Draw Current Drag Selection
            if (isMaskingMode && dragStart != null && dragEnd != null) {
                // Calculate the rectangle in bitmap space considering both fitScale and user zoom
                val totalScale = fitScale * scale
                val start = (dragStart!! - offset) / totalScale
                val end = (dragEnd!! - offset) / totalScale

                drawRect(
                    color = Color.Red.copy(alpha = 0.3f),
                    topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y)),
                    size = Size(abs(start.x - end.x), abs(start.y - end.y))
                )
                drawRect(
                    color = Color.Red,
                    topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y)),
                    size = Size(abs(start.x - end.x), abs(start.y - end.y)),
                    style = Stroke(width = 2f)
                )
            }
        }

        // Context Menu for Detected PII
        if (showPiiContextMenu && selectedPii != null) {
            AlertDialog(
                onDismissRequest = {
                    showPiiContextMenu = false
                    selectedPii = null
                },
                title = { Text(stringResource(R.string.detected_pii_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.pii_type_label, selectedPii?.type ?: ""))
                        Text(stringResource(R.string.pii_text_label, selectedPii?.text ?: ""))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        selectedPii?.let { onConvertPiiToMask(it) }
                        showPiiContextMenu = false
                        selectedPii = null
                    }) {
                        Text(stringResource(R.string.masking_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        selectedPii?.let { onRemoveDetectedPii(it) }
                        showPiiContextMenu = false
                        selectedPii = null
                    }) {
                        Text(stringResource(R.string.cancel_button))
                    }
                }
            )
        }
    }
}
