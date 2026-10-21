package dev.forcetower.unes.ui.feature.commute

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import dev.forcetower.melon.feature.transit.domain.model.GeoPoint
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import kotlin.math.cos
import kotlin.math.hypot

// The design's "mapa esquemático": a quiet street grid with the real
// coordinates projected into the box, so the route, the stops and the buses
// keep their true shape without a map SDK or a key. Equirectangular
// projection is plenty at city scale; latitude is scaled by cos(lat) so a
// kilometre reads the same on both axes.
internal data class SchematicStop(
    val id: String,
    val location: GeoPoint,
    val kind: SchematicStopKind,
)

internal enum class SchematicStopKind { Regular, Mine, Campus, Selected }

internal data class SchematicBus(
    val id: String,
    val location: GeoPoint,
    val heading: Double?,
    val emphasized: Boolean,
)

@Composable
internal fun CommuteSchematicMap(
    stops: List<SchematicStop>,
    modifier: Modifier = Modifier,
    height: Dp = 188.dp,
    path: List<GeoPoint> = emptyList(),
    buses: List<SchematicBus> = emptyList(),
    userLocation: GeoPoint? = null,
    lineColor: Color = MaterialTheme.colorScheme.outline,
    onStopTapped: ((String) -> Unit)? = null,
) {
    val plate = MaterialTheme.colorScheme.surfaceContainerHigh
    val grid = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
    val block = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.045f)
    val ink = MaterialTheme.colorScheme.onBackground
    val card = MaterialTheme.melon.surface.card
    val accent = MaterialTheme.colorScheme.primary
    val you = MaterialTheme.melon.palette.sky
    val description = stringResource(R.string.commute_map_schematic)
    val points = remember(stops, path, buses, userLocation) {
        stops.map { it.location } + path + buses.map { it.location } + listOfNotNull(userLocation)
    }
    val frame = remember(points) { SchematicFrame.fit(points) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(20.dp))
            .background(plate)
            .semantics { contentDescription = description },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onStopTapped == null) {
                        Modifier
                    } else {
                        Modifier.pointerInput(stops, frame) {
                            detectTapGestures { tap ->
                                val hit = stops.minByOrNull { stop ->
                                    val point = frame.project(stop.location, size.toSize())
                                    hypot(point.x - tap.x, point.y - tap.y)
                                } ?: return@detectTapGestures
                                val point = frame.project(hit.location, size.toSize())
                                if (hypot(point.x - tap.x, point.y - tap.y) <= 28.dp.toPx()) onStopTapped(hit.id)
                            }
                        }
                    },
                ),
        ) {
            drawStreets(grid, block)
            if (path.size > 1) {
                val route = Path().apply {
                    path.forEachIndexed { index, point ->
                        val projected = frame.project(point, size)
                        if (index == 0) moveTo(projected.x, projected.y) else lineTo(projected.x, projected.y)
                    }
                }
                drawPath(route, plate, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(
                    route,
                    lineColor,
                    style = Stroke(4.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            stops.forEach { stop ->
                val center = frame.project(stop.location, size)
                when (stop.kind) {
                    SchematicStopKind.Regular -> {
                        drawCircle(card, 3.5.dp.toPx(), center)
                        drawCircle(lineColor, 3.5.dp.toPx(), center, style = Stroke(2.dp.toPx()))
                    }
                    SchematicStopKind.Campus -> {
                        drawRoundRect(
                            color = ink.copy(alpha = 0.12f),
                            topLeft = Offset(center.x - 9.dp.toPx(), center.y - 9.dp.toPx()),
                            size = Size(18.dp.toPx(), 18.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
                        )
                        drawCircle(ink, 3.dp.toPx(), center)
                    }
                    SchematicStopKind.Mine, SchematicStopKind.Selected -> {
                        val halo = if (stop.kind == SchematicStopKind.Selected) accent else lineColor
                        drawCircle(halo.copy(alpha = 0.2f), 12.dp.toPx(), center)
                        drawCircle(card, 7.dp.toPx(), center)
                        drawCircle(ink, 7.dp.toPx(), center, style = Stroke(2.5.dp.toPx()))
                        drawCircle(ink, 2.4.dp.toPx(), center)
                    }
                }
            }
            buses.forEach { bus ->
                val center = frame.project(bus.location, size)
                val alpha = if (bus.emphasized) 1f else 0.5f
                drawCircle(card.copy(alpha = 0.9f * alpha), 12.dp.toPx(), center)
                if (bus.heading == null) {
                    drawCircle(lineColor.copy(alpha = alpha), 6.dp.toPx(), center)
                } else {
                    rotate(bus.heading.toFloat(), center) {
                        val arrow = Path().apply {
                            moveTo(center.x, center.y - 9.dp.toPx())
                            lineTo(center.x + 6.dp.toPx(), center.y + 6.dp.toPx())
                            lineTo(center.x, center.y + 3.dp.toPx())
                            lineTo(center.x - 6.dp.toPx(), center.y + 6.dp.toPx())
                            close()
                        }
                        drawPath(arrow, lineColor.copy(alpha = alpha))
                        drawPath(
                            arrow,
                            card.copy(alpha = alpha),
                            style = Stroke(1.5.dp.toPx(), join = StrokeJoin.Round),
                        )
                    }
                }
            }
            if (userLocation != null) {
                val center = frame.project(userLocation, size)
                drawCircle(you.copy(alpha = 0.18f), 12.dp.toPx(), center)
                drawCircle(you, 6.dp.toPx(), center)
                drawCircle(card, 6.dp.toPx(), center, style = Stroke(2.5.dp.toPx()))
            }
        }
    }
}

// The quiet grid behind every schematic: gently bowed avenues and a few blocks.
private fun DrawScope.drawStreets(
    grid: Color,
    block: Color,
) {
    val stroke = Stroke(1.2.dp.toPx())
    val w = size.width
    val h = size.height
    (0 until 5).forEach { index ->
        val y = h * (0.09f + 0.2f * index)
        val path = Path().apply {
            moveTo(0f, y)
            quadraticTo(w / 2f, y - h * 0.06f, w, y + h * 0.04f)
        }
        drawPath(path, grid, style = stroke)
    }
    (0 until 7).forEach { index ->
        val x = w * (0.1f + 0.14f * index)
        val path = Path().apply {
            moveTo(x, 0f)
            quadraticTo(x + w * 0.025f, h / 2f, x - w * 0.015f, h)
        }
        drawPath(path, grid, style = stroke)
    }
    listOf(
        Offset(0.13f, 0.14f) to Size(0.09f, 0.11f),
        Offset(0.29f, 0.69f) to Size(0.08f, 0.13f),
        Offset(0.54f, 0.5f) to Size(0.10f, 0.10f),
        Offset(0.69f, 0.68f) to Size(0.09f, 0.14f),
        Offset(0.82f, 0.32f) to Size(0.08f, 0.09f),
        Offset(0.41f, 0.18f) to Size(0.07f, 0.09f),
    ).forEach { (origin, extent) ->
        drawRoundRect(
            color = block,
            topLeft = Offset(origin.x * w, origin.y * h),
            size = Size(extent.width * w, extent.height * h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
        )
    }
}

// The bounding box of everything drawn, padded, mapped onto the canvas while
// keeping one scale for both axes.
internal class SchematicFrame private constructor(
    private val minLat: Double,
    private val maxLat: Double,
    private val minLon: Double,
    private val maxLon: Double,
) {
    private val cosLat = cos(Math.toRadians((minLat + maxLat) / 2))

    fun project(
        point: GeoPoint,
        canvas: Size,
    ): Offset {
        val inset = 0.12f
        val usableWidth = canvas.width * (1 - inset * 2)
        val usableHeight = canvas.height * (1 - inset * 2)
        val spanX = ((maxLon - minLon) * cosLat).coerceAtLeast(MIN_SPAN)
        val spanY = (maxLat - minLat).coerceAtLeast(MIN_SPAN)
        val scale = minOf(usableWidth / spanX, usableHeight / spanY).toFloat()
        val contentWidth = (spanX * scale).toFloat()
        val contentHeight = (spanY * scale).toFloat()
        val originX = (canvas.width - contentWidth) / 2f
        val originY = (canvas.height - contentHeight) / 2f
        val centerLon = (minLon + maxLon) / 2
        val centerLat = (minLat + maxLat) / 2
        val x = originX + contentWidth / 2f + ((point.longitude - centerLon) * cosLat * scale).toFloat()
        val y = originY + contentHeight / 2f - ((point.latitude - centerLat) * scale).toFloat()
        return Offset(x, y)
    }

    companion object {
        // Degrees: keeps a single stop from blowing up to an infinite scale.
        private const val MIN_SPAN = 0.004

        fun fit(points: List<GeoPoint>): SchematicFrame {
            if (points.isEmpty()) return SchematicFrame(-12.24, -12.20, -38.98, -38.95)
            return SchematicFrame(
                minLat = points.minOf { it.latitude },
                maxLat = points.maxOf { it.latitude },
                minLon = points.minOf { it.longitude },
                maxLon = points.maxOf { it.longitude },
            )
        }
    }
}
