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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.domain.CgVertex
import com.example.domain.WeightBalanceResult
import com.example.domain.WeightUnit
import com.example.ui.theme.LocalCockpitColors

@Composable
fun CgEnvelopeCanvas(
    envelope: List<CgVertex>,
    result: WeightBalanceResult,
    weightUnit: WeightUnit = WeightUnit.LBS,
    modifier: Modifier = Modifier
) {
    val cockpit = LocalCockpitColors.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.45f)
    ) {
        if (envelope.size < 3) return@Canvas

        val padLeft = 78f
        val padRight = 24f
        val padTop = 24f
        val padBottom = 46f
        val plotW = (size.width - padLeft - padRight).coerceAtLeast(10f)
        val plotH = (size.height - padTop - padBottom).coerceAtLeast(10f)

        val minArm = minOf(
            envelope.minOf { it.armIn },
            result.takeoffCgIn,
            result.zeroFuelCgIn
        ) - 1.5
        val maxArm = maxOf(
            envelope.maxOf { it.armIn },
            result.takeoffCgIn,
            result.zeroFuelCgIn
        ) + 1.5
        val minWt = (minOf(envelope.minOf { it.weightLbs }, result.zeroFuelWeightLbs) - 150.0).coerceAtLeast(0.0)
        val maxWt = maxOf(envelope.maxOf { it.weightLbs }, result.takeoffWeightLbs) + 150.0

        fun toX(arm: Double): Float {
            val frac = ((arm - minArm) / (maxArm - minArm).coerceAtLeast(0.1)).toFloat()
            return padLeft + frac * plotW
        }

        fun toY(wt: Double): Float {
            val frac = ((wt - minWt) / (maxWt - minWt).coerceAtLeast(1.0)).toFloat()
            return padTop + plotH - frac * plotH
        }

        val axisPaint = Paint().apply {
            isAntiAlias = true
            color = onSurfaceVariant.toArgb()
            textSize = 23f
            typeface = Typeface.MONOSPACE
        }

        // Grid lines
        for (i in 0..4) {
            val y = padTop + (plotH / 4f) * i
            drawLine(
                color = cockpit.gridLine.copy(alpha = 0.5f),
                start = Offset(padLeft, y),
                end = Offset(padLeft + plotW, y),
                strokeWidth = 1.2f
            )
            val wtValLbs = maxWt - ((maxWt - minWt) / 4.0) * i
            val dispWt = weightUnit.fromLbs(wtValLbs).toInt()
            axisPaint.textAlign = Paint.Align.RIGHT
            drawContext.canvas.nativeCanvas.drawText("$dispWt", padLeft - 8f, y + 8f, axisPaint)
        }
        for (i in 0..4) {
            val x = padLeft + (plotW / 4f) * i
            drawLine(
                color = cockpit.gridLine.copy(alpha = 0.5f),
                start = Offset(x, padTop),
                end = Offset(x, padTop + plotH),
                strokeWidth = 1.2f
            )
            val armValIn = minArm + ((maxArm - minArm) / 4.0) * i
            val dispArm = weightUnit.armFromIn(armValIn)
            axisPaint.textAlign = Paint.Align.CENTER
            drawContext.canvas.nativeCanvas.drawText("%.1f%s".format(dispArm, weightUnit.armSymbol), x, size.height - 10f, axisPaint)
        }

        // Envelope Polygon
        val polyPath = Path().apply {
            envelope.forEachIndexed { idx, v ->
                val px = toX(v.armIn)
                val py = toY(v.weightLbs)
                if (idx == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        drawPath(polyPath, color = primary.copy(alpha = 0.16f), style = Fill)
        drawPath(polyPath, color = primary, style = Stroke(width = 3.2f))

        // Fuel burn trajectory line: Takeoff -> Landing -> ZFW
        val toPoint = Offset(toX(result.takeoffCgIn), toY(result.takeoffWeightLbs))
        val ldPoint = Offset(toX(result.landingCgIn), toY(result.landingWeightLbs))
        val zfPoint = Offset(toX(result.zeroFuelCgIn), toY(result.zeroFuelWeightLbs))

        drawLine(color = secondary.copy(alpha = 0.8f), start = zfPoint, end = toPoint, strokeWidth = 3f)

        // Dots
        drawCircle(
            color = if (result.isZeroFuelWithinEnvelope) cockpit.safeGreen else cockpit.dangerRed,
            radius = 8f,
            center = zfPoint
        )
        drawCircle(
            color = secondary,
            radius = 9f,
            center = ldPoint
        )
        drawCircle(
            color = if (result.isTakeoffWithinEnvelope) cockpit.safeGreen else cockpit.dangerRed,
            radius = 12f,
            center = toPoint
        )
    }
}
