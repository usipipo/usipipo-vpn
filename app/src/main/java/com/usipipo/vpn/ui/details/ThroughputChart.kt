package com.usipipo.vpn.ui.details

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.usipipo.vpn.R
import com.usipipo.vpn.ui.theme.UsipipoTheme

/**
 * Gráfica de líneas de descarga y subida del último minuto, dibujada con Canvas.
 * Las muestras nuevas entran por la derecha; las dos series comparten escala.
 */
@Composable
fun ThroughputChart(
    downHistory: List<Long>,
    upHistory: List<Long>,
    modifier: Modifier = Modifier,
) {
    val downColor = MaterialTheme.colorScheme.secondary
    val upColor = MaterialTheme.colorScheme.primary
    val baseline = MaterialTheme.colorScheme.outline
    val description = stringResource(R.string.cd_throughput_chart)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .semantics { contentDescription = description },
    ) {
        val peak = maxOf(downHistory.maxOrNull() ?: 0L, upHistory.maxOrNull() ?: 0L, 1L).toFloat()
        drawLine(
            color = baseline,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx(),
        )
        drawSeries(downHistory, peak, downColor)
        drawSeries(upHistory, peak, upColor)
    }
}

private fun DrawScope.drawSeries(values: List<Long>, peak: Float, color: Color) {
    if (values.size < 2) return
    val step = size.width / (StatsViewModel.HISTORY_SIZE - 1)
    val padding = 4.dp.toPx()
    val usableHeight = size.height - padding * 2
    val path = Path()
    values.forEachIndexed { index, value ->
        val x = size.width - (values.size - 1 - index) * step
        val y = size.height - padding - (value / peak) * usableHeight
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx()))
}

@Preview(name = "ThroughputChart")
@Preview(name = "ThroughputChart oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewThroughputChart() {
    UsipipoTheme {
        Surface {
            ThroughputChart(
                downHistory = List(60) { (it % 20) * 500_000L },
                upHistory = List(60) { (it % 20) * 500_000L },
            )
        }
    }
}

@Preview(name = "ThroughputChart vacío")
@Preview(name = "ThroughputChart vacío oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewThroughputChartVacio() {
    UsipipoTheme {
        Surface {
            ThroughputChart(downHistory = emptyList(), upHistory = emptyList())
        }
    }
}
