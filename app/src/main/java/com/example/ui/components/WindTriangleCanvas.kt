package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.domain.RunwayWindResult
import com.example.domain.WindTriangleResult
import com.example.ui.theme.LocalCockpitColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun WindTriangleCanvas(
    result: WindTriangleResult,
    modifier: Modifier = Modifier
) {
    val cockpit = LocalCockpitColors.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.08f)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(size.width, size.height) * 0.42f

        // Background concentric radar rings
        drawCircle(
            color = surfaceVariant.copy(alpha = 0.35f),
            radius = radius,
            center = Offset(cx, cy)
        )
        listOf(0.35f, 0.7f, 1.0f).forEach { factor ->
            drawCircle(
                color = cockpit.gridLine.copy(alpha = 0.65f),
                radius = radius * factor,
                center = Offset(cx, cy),
                style = Stroke(width = if (factor == 1f) 2.5f else 1.2f)
            )
        }

        // Compass ticks
        val labelPaint = Paint().apply {
            isAntiAlias = true
            color = onSurfaceVariant.toArgb()
            textSize = radius * 0.11f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
        }

        for (deg in 0 until 360 step 10) {
            val rad = Math.toRadians((deg - 90).toDouble())
            val isCardinal = deg % 90 == 0
            val isMajor = deg % 30 == 0
            val innerFactor = when {
                isCardinal -> 0.86f
                isMajor -> 0.90f
                else -> 0.94f
            }
            val p1 = Offset(cx + radius * innerFactor * cos(rad).toFloat(), cy + radius * innerFactor * sin(rad).toFloat())
            val p2 = Offset(cx + radius * cos(rad).toFloat(), cy + radius * sin(rad).toFloat())
            drawLine(
                color = if (isMajor) primary.copy(alpha = 0.8f) else cockpit.dialTick.copy(alpha = 0.45f),
                start = p1,
                end = p2,
                strokeWidth = if (isMajor) 2.2f else 1.1f
            )
            if (isMajor) {
                val tx = cx + radius * 1.11f * cos(rad).toFloat()
                val ty = cy + radius * 1.11f * sin(rad).toFloat() + labelPaint.textSize * 0.35f
                val text = when (deg) {
                    0 -> "N"
                    90 -> "E"
                    180 -> "S"
                    270 -> "W"
                    else -> "%02d".format(deg / 10)
                }
                labelPaint.color = if (isCardinal) primary.toArgb() else onSurfaceVariant.toArgb()
                drawContext.canvas.nativeCanvas.drawText(text, tx, ty, labelPaint)
            }
        }

        // Normalize vector magnitudes for clear visualization
        val maxSpeed = maxOf(result.trueAirspeedKt, result.groundSpeedKt, result.windSpeedKt, 50.0)
        val scale = (radius * 0.82f) / maxSpeed.toFloat()

        // True Course vector (from center)
        val tcRad = Math.toRadians(result.trueCourseDeg - 90.0)
        val tcEnd = Offset(
            cx + (result.groundSpeedKt.toFloat().coerceAtLeast(20f) * scale) * cos(tcRad).toFloat(),
            cy + (result.groundSpeedKt.toFloat().coerceAtLeast(20f) * scale) * sin(tcRad).toFloat()
        )
        drawVectorArrow(
            start = Offset(cx, cy),
            end = tcEnd,
            color = cockpit.safeGreen,
            strokeWidth = 5f
        )

        // True Heading vector (Air vector from center)
        val thRad = Math.toRadians(result.trueHeadingDeg - 90.0)
        val thEnd = Offset(
            cx + (result.trueAirspeedKt.toFloat() * scale) * cos(thRad).toFloat(),
            cy + (result.trueAirspeedKt.toFloat() * scale) * sin(thRad).toFloat()
        )
        drawVectorArrow(
            start = Offset(cx, cy),
            end = thEnd,
            color = primary,
            strokeWidth = 5f
        )

        // Wind Vector connecting Air tip (thEnd) -> Ground tip (tcEnd)
        if (result.isPossible && result.windSpeedKt > 0.5) {
            drawVectorArrow(
                start = thEnd,
                end = tcEnd,
                color = secondary,
                strokeWidth = 4.2f
            )
        }

        // Draw center aircraft symbol rotated along True Heading
        val headingRad = Math.toRadians(result.trueHeadingDeg - 90.0).toFloat()
        val nose = Offset(cx + 18f * cos(headingRad), cy + 18f * sin(headingRad))
        val leftWing = Offset(cx + 14f * cos(headingRad + 2.3f), cy + 14f * sin(headingRad + 2.3f))
        val rightWing = Offset(cx + 14f * cos(headingRad - 2.3f), cy + 14f * sin(headingRad - 2.3f))
        val tail = Offset(cx - 8f * cos(headingRad), cy - 8f * sin(headingRad))
        val planePath = Path().apply {
            moveTo(nose.x, nose.y)
            lineTo(rightWing.x, rightWing.y)
            lineTo(tail.x, tail.y)
            lineTo(leftWing.x, leftWing.y)
            close()
        }
        drawPath(planePath, color = onSurface)

        // Legend pills inside bottom corners
        val legendPaint = Paint().apply {
            isAntiAlias = true
            textSize = radius * 0.095f
            typeface = Typeface.MONOSPACE
        }
        legendPaint.color = primary.toArgb()
        drawContext.canvas.nativeCanvas.drawText("TH/TAS", 18f, size.height - 16f, legendPaint)
        legendPaint.color = cockpit.safeGreen.toArgb()
        drawContext.canvas.nativeCanvas.drawText("TC/GS", size.width * 0.38f, size.height - 16f, legendPaint)
        legendPaint.color = secondary.toArgb()
        drawContext.canvas.nativeCanvas.drawText("WIND", size.width * 0.74f, size.height - 16f, legendPaint)
    }
}

@Composable
fun RunwayWindCanvas(
    runwayWind: RunwayWindResult,
    modifier: Modifier = Modifier
) {
    val cockpit = LocalCockpitColors.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.35f)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(size.width, size.height) * 0.42f

        // Compass ring
        drawCircle(
            color = cockpit.gridLine.copy(alpha = 0.65f),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )

        // Draw Runway aligned to runwayHeadingDeg
        val rwyRad = Math.toRadians(runwayWind.runwayHeadingDeg - 90.0).toFloat()
        val rwyHalfLen = radius * 0.75f
        val dx = rwyHalfLen * cos(rwyRad)
        val dy = rwyHalfLen * sin(rwyRad)
        drawLine(
            color = onSurfaceVariant.copy(alpha = 0.35f),
            start = Offset(cx - dx, cy - dy),
            end = Offset(cx + dx, cy + dy),
            strokeWidth = 34f,
            cap = StrokeCap.Butt
        )
        drawLine(
            color = onSurface.copy(alpha = 0.75f),
            start = Offset(cx - dx * 0.85f, cy - dy * 0.85f),
            end = Offset(cx + dx * 0.85f, cy + dy * 0.85f),
            strokeWidth = 2.5f
        )

        // Wind arrow coming FROM windDirectionDeg toward center
        val windFromRad = Math.toRadians(runwayWind.windDirectionDeg - 90.0).toFloat()
        val start = Offset(cx + radius * 0.96f * cos(windFromRad), cy + radius * 0.96f * sin(windFromRad))
        val end = Offset(cx + radius * 0.25f * cos(windFromRad), cy + radius * 0.25f * sin(windFromRad))
        val windColor = if (abs(runwayWind.crosswindKt) >= 15.0 || runwayWind.headwindKt < 0.0) {
            cockpit.dangerRed
        } else {
            secondary
        }
        drawVectorArrow(start = start, end = end, color = windColor, strokeWidth = 5.5f)

        val rwyLabel = "%02d".format((runwayWind.runwayHeadingDeg / 10).toInt().coerceIn(1, 36))
        val paint = Paint().apply {
            isAntiAlias = true
            color = primary.toArgb()
            textSize = radius * 0.16f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        drawContext.canvas.nativeCanvas.drawText("RWY $rwyLabel", cx, cy + radius + paint.textSize, paint)
    }
}

private fun DrawScope.drawVectorArrow(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    val dx = end.x - start.x
    val dy = end.y - start.y
    val len = kotlin.math.hypot(dx, dy)
    if (len < 6f) return
    val angle = kotlin.math.atan2(dy, dx)
    val headLen = 14f
    val spread = 0.45f
    val p1 = Offset(end.x - headLen * cos(angle - spread), end.y - headLen * sin(angle - spread))
    val p2 = Offset(end.x - headLen * cos(angle + spread), end.y - headLen * sin(angle + spread))
    val path = Path().apply {
        moveTo(end.x, end.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }
    drawPath(path, color = color)
}
