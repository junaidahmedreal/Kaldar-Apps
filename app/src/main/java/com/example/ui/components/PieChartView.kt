package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

data class CategorySlice(
    val category: String,
    val amount: Double,
    val color: Color,
    val percentage: Float
)

val CATEGORY_COLORS = listOf(
    Color(0xFFEF4444), // Food - Red
    Color(0xFF10B981), // Groceries - Emerald
    Color(0xFFF59E0B), // Fuel - Amber
    Color(0xFF06B6D4), // Medical - Cyan
    Color(0xFF3B82F6), // Travel - Blue
    Color(0xFF8B5CF6), // Shopping - Purple
    Color(0xFFEC4899), // Utilities - Pink
    Color(0xFFF97316), // Entertainment - Orange
    Color(0xFF64748B), // Office - Slate
    Color(0xFF94A3B8)  // Other - Gray
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PieChartView(
    slices: List<CategorySlice>,
    totalAmount: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(slices) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(700))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pie_chart_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (slices.isEmpty() || totalAmount == 0.0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No expenses recorded this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Donut Chart Canvas with Total in Center
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                val surfaceColor = MaterialTheme.colorScheme.surface
                Canvas(
                    modifier = Modifier
                        .size(190.dp)
                        .testTag("pie_chart_canvas")
                ) {
                    val strokeWidth = 32.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val arcSize = Size(diameter, diameter)
                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                    var startAngle = -90f

                    for (slice in slices) {
                        val sweepAngle = (slice.percentage / 100f) * 360f * progress.value
                        if (sweepAngle > 0.5f) {
                            drawArc(
                                color = slice.color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            startAngle += sweepAngle
                        }
                    }
                }

                // Center Label
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Total",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currencySymbol ${String.format(Locale.US, "%.0f", totalAmount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Legend Grid
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                slices.forEach { slice ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(slice.color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${slice.category} (${String.format(Locale.US, "%.1f", slice.percentage)}%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
