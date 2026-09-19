package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

@Composable
fun RedactionCanvas(
    bitmap: Bitmap,
    redactionBoxes: List<Rect>,
    onBoxesChanged: (List<Rect>) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var currentDrag by remember { mutableStateOf<Offset?>(null) }

    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .testTag("redaction_canvas_container")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("redaction_canvas")
                .pointerInput(bitmap) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragStart = offset
                            currentDrag = offset
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentDrag = change.position
                        },
                        onDragEnd = {
                            val start = dragStart
                            val end = currentDrag
                            if (start != null && end != null) {
                                val left = min(start.x, end.x)
                                val top = min(start.y, end.y)
                                val right = max(start.x, end.x)
                                val bottom = max(start.y, end.y)

                                if ((right - left) > 10 && (bottom - top) > 10) {
                                    val newRect = Rect(left, top, right, bottom)
                                    onBoxesChanged(redactionBoxes + newRect)
                                }
                            }
                            dragStart = null
                            currentDrag = null
                        },
                        onDragCancel = {
                            dragStart = null
                            currentDrag = null
                        }
                    )
                }
        ) {
            // Draw image fitted inside canvas
            val canvasWidth = size.width
            val canvasHeight = size.height

            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(canvasWidth.toInt(), canvasHeight.toInt())
            )

            // Draw confirmed redaction black boxes
            for (box in redactionBoxes) {
                drawRect(
                    color = Color.Black,
                    topLeft = box.topLeft,
                    size = box.size,
                    style = Fill
                )
            }

            // Draw currently active dragging box with semi-transparent black and white border
            val start = dragStart
            val curr = currentDrag
            if (start != null && curr != null) {
                val left = min(start.x, curr.x)
                val top = min(start.y, curr.y)
                val right = max(start.x, curr.x)
                val bottom = max(start.y, curr.y)
                val activeRect = Rect(left, top, right, bottom)

                drawRect(
                    color = Color(0xCC000000),
                    topLeft = activeRect.topLeft,
                    size = activeRect.size,
                    style = Fill
                )
            }
        }
    }
}

fun applyRedactionsToBitmap(
    sourceBitmap: Bitmap,
    redactionBoxes: List<Rect>,
    displayedWidth: Float,
    displayedHeight: Float
): Bitmap {
    if (redactionBoxes.isEmpty() || displayedWidth <= 0 || displayedHeight <= 0) {
        return sourceBitmap
    }

    val resultBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(resultBitmap)
    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        style = Paint.Style.FILL
    }

    val scaleX = resultBitmap.width.toFloat() / displayedWidth
    val scaleY = resultBitmap.height.toFloat() / displayedHeight

    for (box in redactionBoxes) {
        val left = box.left * scaleX
        val top = box.top * scaleY
        val right = box.right * scaleX
        val bottom = box.bottom * scaleY
        canvas.drawRect(left, top, right, bottom, paint)
    }

    return resultBitmap
}
