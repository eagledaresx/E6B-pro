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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.domain.HoldingEntryType
import com.example.domain.HoldingResult
import com.example.ui.theme.LocalCockpitColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun HoldingPatternCanvas(
    holding: HoldingResult,
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
            .aspectRatio(1.25f)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(size.width, size.height) * 0.42f

        // Compass ring
        drawCircle(
            color = cockpit.gridLine.copy(alpha = 0.6f),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )

        // Holding entry sectors centered on fix
        // In screen coordinates, 0 deg = top (-90 deg in math)
        val outboundScreenAngle = (holding.outboundCourseDeg - 90.0).toFloat()
        val teardropStart: Float
        val teardropSweep = 70f
        val parallelStart: Float
        val parallelSweep = 110f
        val directStart: Float
        val directSweep = 180f

        if (holding.isRightTurns) {
            teardropStart = outboundScreenAngle - 70f
            parallelStart = outboundScreenAngle
            directStart = outboundScreenAngle + 110f
        } else {
            teardropStart = outboundScreenAngle
            parallelStart = outboundScreenAngle - 110f
            directStart = outboundScreenAngle + 70f
        }

        val arcSize = Size(radius * 1.9f, radius * 1.9f)
        val arcTopLeft = Offset(cx - radius * 0.95f, cy - radius * 0.95f)

        drawArc(
            color = secondary.copy(alpha = if (holding.entryType == HoldingEntryType.TEARDROP) 0.28f else 0.08f),
            startAngle = teardropStart,
            sweepAngle = teardropSweep,
            useCenter = true,
            topLeft = arcTopLeft,
            size = arcSize
        )
        drawArc(
            color = cockpit.dangerRed.copy(alpha = if (holding.entryType == HoldingEntryType.PARALLEL) 0.25f else 0.08f),
            startAngle = parallelStart,
            sweepAngle = parallelSweep,
            useCenter = true,
            topLeft = arcTopLeft,
            size = arcSize
        )
        drawArc(
            color = cockpit.safeGreen.copy(alpha = if (holding.entryType == HoldingEntryType.DIRECT) 0.22f else 0.08f),
            startAngle = directStart,
            sweepAngle = directSweep,
            useCenter = true,
            topLeft = arcTopLeft,
            size = arcSize
        )

        // Draw racetrack rotated by inbound course
        rotate(degrees = holding.inboundCourseDeg.toFloat(), pivot = Offset(cx, cy)) {
            val legLen = radius * 0.72f
            val turnR = radius * 0.24f
            val dirSign = if (holding.isRightTurns) 1f else -1f

            // Inbound leg (coming from bottom to fix at center)
            drawLine(
                color = primary,
                start = Offset(cx, cy + legLen),
                end = Offset(cx, cy),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
            // Outbound leg
            drawLine(
                color = primary.copy(alpha = 0.65f),
                start = Offset(cx + dirSign * 2f * turnR, cy),
                end = Offset(cx + dirSign * 2f * turnR, cy + legLen),
                strokeWidth = 3.5f,
                cap = StrokeCap.Round
            )
            // Fix turn arc (top)
            drawArc(
                color = primary,
                startAngle = if (holding.isRightTurns) 180f else 0f,
                sweepAngle = if (holding.isRightTurns) 180f else -180f,
                useCenter = false,
                topLeft = Offset(if (holding.isRightTurns) cx else cx - 2f * turnR, cy - turnR),
                size = Size(2f * turnR, 2f * turnR),
                style = Stroke(width = 4f)
            )
            // Inbound turn arc (bottom)
            drawArc(
                color = primary,
                startAngle = if (holding.isRightTurns) 0f else 180f,
                sweepAngle = if (holding.isRightTurns) 180f else -180f,
                useCenter = false,
                topLeft = Offset(if (holding.isRightTurns) cx else cx - 2f * turnR, cy + legLen - turnR),
                size = Size(2f * turnR, 2f * turnR),
                style = Stroke(width = 4f)
            )
        }

        // Fix triangle at center
        val fixPath = Path().apply {
            moveTo(cx, cy - 9f)
            lineTo(cx + 8f, cy + 7f)
            lineTo(cx - 8f, cy + 7f)
            close()
        }
        drawPath(fixPath, color = onSurface)

        // Aircraft approaching arrow (from opposite of aircraftHeadingDeg toward center)
        val approachFromRad = Math.toRadians((holding.aircraftHeadingDeg + 180.0) - 90.0)
        val acStart = Offset(
            cx + radius * 0.9f * cos(approachFromRad).toFloat(),
            cy + radius * 0.9f * sin(approachFromRad).toFloat()
        )
        val acEnd = Offset(
            cx + radius * 0.2f * cos(approachFromRad).toFloat(),
            cy + radius * 0.2f * sin(approachFromRad).toFloat()
        )
        drawLine(
            color = secondary,
            start = acStart,
            end = acEnd,
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )

        val paint = Paint().apply {
            isAntiAlias = true
            color = onSurfaceVariant.toArgb()
            textSize = radius * 0.11f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
        }
        drawContext.canvas.nativeCanvas.drawText("N", cx, cy - radius - 8f, paint)
    }
}
