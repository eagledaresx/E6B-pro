package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.example.domain.E6BMath
import com.example.ui.theme.LocalCockpitColors
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.sin

/**
 * Interactive Rotatable E6B Circular Slide Rule ("Whiz Wheel").
 * Users drag the inner disc to rotate the logarithmic scales in real time!
 */
@Composable
fun WhizWheelCanvas(
    innerRotationDeg: Double,
    onRotationDelta: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val cockpit = LocalCockpitColors.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                var prevAngle = 0.0
                detectDragGestures(
                    onDragStart = { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        prevAngle = Math.toDegrees(atan2((offset.y - cy).toDouble(), (offset.x - cx).toDouble()))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val currAngle = Math.toDegrees(
                            atan2((change.position.y - cy).toDouble(), (change.position.x - cx).toDouble())
                        )
                        var delta = currAngle - prevAngle
                        if (delta > 180.0) delta -= 360.0
                        if (delta < -180.0) delta += 360.0
                        prevAngle = currAngle
                        onRotationDelta(delta)
                    }
                )
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = min(size.width, size.height) * 0.46f
        val splitR = outerR * 0.77f
        val innerR = outerR * 0.56f

        // Outer stationary ring
        drawCircle(
            color = surfaceVariant.copy(alpha = 0.5f),
            radius = outerR,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = primary.copy(alpha = 0.6f),
            radius = outerR,
            center = Offset(cx, cy),
            style = Stroke(width = 2.5f)
        )

        // Inner rotatable disc
        drawCircle(
            color = surface,
            radius = splitR,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = secondary,
            radius = splitR,
            center = Offset(cx, cy),
            style = Stroke(width = 2.5f)
        )
        drawCircle(
            color = cockpit.gridLine,
            radius = innerR,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f)
        )

        val outerLabelPaint = Paint().apply {
            isAntiAlias = true
            color = primary.toArgb()
            textSize = outerR * 0.095f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
        }
        val innerLabelPaint = Paint().apply {
            isAntiAlias = true
            color = onSurface.toArgb()
            textSize = outerR * 0.088f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
        }

        val majorMarks = listOf(10, 12, 15, 18, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, 90)

        // Draw outer logarithmic ticks (10..99)
        for (v in 10..99) {
            val deg = 360.0 * log10(v / 10.0) - 90.0
            val rad = Math.toRadians(deg)
            val isMajor = v in majorMarks
            val tickIn = if (isMajor) splitR + 2f else splitR + (outerR - splitR) * 0.18f
            val tickOut = if (isMajor) splitR + (outerR - splitR) * 0.42f else splitR + (outerR - splitR) * 0.34f
            drawLine(
                color = if (isMajor) primary else onSurfaceVariant.copy(alpha = 0.55f),
                start = Offset(cx + tickIn * cos(rad).toFloat(), cy + tickIn * sin(rad).toFloat()),
                end = Offset(cx + tickOut * cos(rad).toFloat(), cy + tickOut * sin(rad).toFloat()),
                strokeWidth = if (isMajor) 2.2f else 1.1f
            )
            if (isMajor) {
                val tx = cx + (outerR * 0.89f) * cos(rad).toFloat()
                val ty = cy + (outerR * 0.89f) * sin(rad).toFloat() + outerLabelPaint.textSize * 0.35f
                drawContext.canvas.nativeCanvas.drawText(v.toString(), tx, ty, outerLabelPaint)
            }
        }

        // Draw inner rotating logarithmic ticks (10..99) + 60 Rate Triangle Index
        for (v in 10..99) {
            val deg = 360.0 * log10(v / 10.0) - 90.0 + innerRotationDeg
            val rad = Math.toRadians(deg)
            val isMajor = v in majorMarks
            val tickOut = splitR - 2f
            val tickIn = if (isMajor) splitR - (splitR - innerR) * 0.45f else splitR - (splitR - innerR) * 0.25f
            val tickColor = when {
                v == 60 -> cockpit.dangerRed
                v == 36 -> secondary
                isMajor -> onSurface
                else -> onSurfaceVariant.copy(alpha = 0.45f)
            }
            drawLine(
                color = tickColor,
                start = Offset(cx + tickIn * cos(rad).toFloat(), cy + tickIn * sin(rad).toFloat()),
                end = Offset(cx + tickOut * cos(rad).toFloat(), cy + tickOut * sin(rad).toFloat()),
                strokeWidth = if (v == 60 || v == 36) 4f else if (isMajor) 2.2f else 1f
            )
            if (isMajor) {
                val tx = cx + (splitR * 0.81f) * cos(rad).toFloat()
                val ty = cy + (splitR * 0.81f) * sin(rad).toFloat() + innerLabelPaint.textSize * 0.35f
                innerLabelPaint.color = if (v == 60) cockpit.dangerRed.toArgb() else if (v == 36) secondary.toArgb() else onSurface.toArgb()
                drawContext.canvas.nativeCanvas.drawText(v.toString(), tx, ty, innerLabelPaint)
            }
        }

        // Draw prominent Rate Index Pointer at 60 on inner wheel
        val idxRad = Math.toRadians(360.0 * log10(60.0 / 10.0) - 90.0 + innerRotationDeg)
        val tip = Offset(cx + (splitR - 4f) * cos(idxRad).toFloat(), cy + (splitR - 4f) * sin(idxRad).toFloat())
        val base1 = Offset(cx + (splitR - 20f) * cos(idxRad - 0.08).toFloat(), cy + (splitR - 20f) * sin(idxRad - 0.08).toFloat())
        val base2 = Offset(cx + (splitR - 20f) * cos(idxRad + 0.08).toFloat(), cy + (splitR - 20f) * sin(idxRad + 0.08).toFloat())
        val pointerPath = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(base1.x, base1.y)
            lineTo(base2.x, base2.y)
            close()
        }
        drawPath(pointerPath, color = cockpit.dangerRed)

        // Center hub readout: Rate at 60 Index
        val rate = E6BMath.computeWhizWheelRateAt60(innerRotationDeg)
        val hubPaint = Paint().apply {
            isAntiAlias = true
            color = primary.toArgb()
            textSize = innerR * 0.30f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            color = onSurfaceVariant.toArgb()
            textSize = innerR * 0.14f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        drawContext.canvas.nativeCanvas.drawText("INDEX 60", cx, cy - innerR * 0.18f, subPaint)
        drawContext.canvas.nativeCanvas.drawText("%.1f".format(rate), cx, cy + innerR * 0.16f, hubPaint)
        drawContext.canvas.nativeCanvas.drawText("DRAG DIAL", cx, cy + innerR * 0.42f, subPaint)
    }
}
