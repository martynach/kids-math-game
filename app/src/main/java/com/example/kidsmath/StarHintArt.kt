package com.example.kidsmath

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min

internal val HintGold = Color(0xFFFFD13B)
internal val HintPink = Color(0xFFFF63CB)
internal val HintInk = Color(0xFF091D57)
internal fun HintColor.tint() = if (this == HintColor.YELLOW) HintGold else HintPink

/** Screen geometry is derived solely from available size; model positions remain row/column indexes. */
internal class HintGeometry(width: Float, height: Float) {
    private val surface = Rect(0f, 0f, width, height)
    val cell = min(width * .58f / 5, height * .82f / 4)
    val grid = Rect((width - 5 * cell) / 2, height * .12f,
        (width + 5 * cell) / 2, height * .12f + 4 * cell)
    val bagWidth = min(width * .16f, height * .48f)
    val yellowBag = Offset(width * .12f, height * .68f)
    val pinkBag = Offset(width * .88f, height * .68f)
    fun nearestSlots(point: Offset) = (0 until StarHintRules.CAPACITY).sortedBy {
        (center(it) - point).getDistanceSquared()
    }
    fun center(index: Int): Offset {
        val (column, row) = StarHintRules.cell(index)
        return Offset(grid.left + (column + .5f) * cell, grid.top + (row + .5f) * cell)
    }
    // Generous invisible targets stay inside the hint surface, clear of the keypad.
    val dropBounds get() = grid.inflate(cell * .65f).intersect(surface)
    fun bagTouchBounds(color: HintColor) = bagBounds(color).inflate(cell * .65f).intersect(surface)
    fun bag(color: HintColor) = if (color == HintColor.YELLOW) yellowBag else pinkBag
    fun mouth(color: HintColor) = bag(color) - Offset(0f, bagWidth * .42f)
    fun bagBounds(color: HintColor): Rect {
        val center = bag(color)
        return Rect(center - Offset(bagWidth * .56f, bagWidth * .65f),
            center + Offset(bagWidth * .56f, bagWidth * .55f))
    }
}

internal fun hintStarPath(center: Offset, radius: Float): Path = Path().apply {
    repeat(10) { i ->
        val angle = -Math.PI / 2 + i * Math.PI / 5
        val r = if (i % 2 == 0) radius else radius * .48f
        val x = center.x + cos(angle).toFloat() * r
        val y = center.y + sin(angle).toFloat() * r
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

internal fun DrawScope.countingStar(center: Offset, radius: Float, color: Color, outline: Boolean = false, dimFraction: Float = 0f) {
    val path = hintStarPath(center, radius)
    if (outline) {
        drawPath(path, Color(0xFF69A7E5).copy(alpha = .24f), style = Stroke(radius * .045f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(radius * .15f, radius * .12f))))
        return
    }
    val dim = dimFraction.coerceIn(0f, 1f)
    val tint = lerp(color, Color(0xFF748097), dim)
    val highlight = lerp(lerp(color, Color.White, .60f), Color(0xFFA2AFC2), dim)
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = .25f * (1f - dim)), Color.Transparent),
        center, radius * 1.55f), radius * 1.55f, center)
    val dark = lerp(if (color == HintPink) Color(0xFFC51D8C) else Color(0xFFE19209), Color(0xFF3E495E), dim)
    drawPath(path, dark, style = Stroke(radius * .12f, join = StrokeJoin.Round))
    drawPath(path, Brush.linearGradient(listOf(highlight, tint, dark),
        center - Offset(radius, radius), center + Offset(radius, radius)))
    drawPath(path, highlight, style = Stroke(radius * .045f, join = StrokeJoin.Round))
    drawLine(Color.White.copy(alpha = .7f * (1f - .8f * dim)), center + Offset(-radius * .29f, -radius * .20f),
        center + Offset(-radius * .14f, -radius * .47f), radius * .07f, StrokeCap.Round)
}

internal fun DrawScope.starBag(geometry: HintGeometry, color: HintColor) {
    val center = geometry.bag(color)
    val w = geometry.bagWidth
    val tint = color.tint()
    val dark = if (color == HintColor.YELLOW) Color(0xFFC87803) else Color(0xFFA61986)
    drawOval(Color(0xFF020C30).copy(alpha = .65f), center + Offset(-w * .64f, w * .39f), Size(w * 1.28f, w * .18f))
    val body = Path().apply {
        moveTo(center.x - w * .36f, center.y - w * .40f)
        cubicTo(center.x - w * .41f, center.y - w * .07f, center.x - w * .68f, center.y + w * .34f,
            center.x - w * .40f, center.y + w * .44f)
        cubicTo(center.x - w * .17f, center.y + w * .54f, center.x + w * .33f, center.y + w * .52f,
            center.x + w * .46f, center.y + w * .36f)
        cubicTo(center.x + w * .57f, center.y + w * .15f, center.x + w * .34f, center.y - w * .13f,
            center.x + w * .36f, center.y - w * .40f)
        close()
    }
    drawPath(body, Brush.radialGradient(listOf(lerp(tint, Color.White, .30f), tint, dark),
        center - Offset(w * .2f, 0f), w * .85f))
    drawPath(body, dark, style = Stroke(w * .035f))
    val mouth = geometry.mouth(color)
    drawOval(dark, mouth - Offset(w * .45f, w * .12f), Size(w * .90f, w * .26f))
    repeat(5) { i ->
        countingStar(mouth + Offset((i - 2) * w * .135f, -w * (.06f + (i % 2) * .13f)), w * .14f, tint)
    }
    drawArc(lerp(tint, Color.White, .25f), 0f, 180f, false,
        mouth - Offset(w * .46f, w * .12f), Size(w * .92f, w * .28f), style = Stroke(w * .075f))
    countingStar(center + Offset(0f, w * .11f), w * .22f, tint)
    drawLine(lerp(tint, Color.White, .30f), center + Offset(-w * .35f, -w * .12f),
        center + Offset(-w * .40f, w * .13f), w * .035f, StrokeCap.Round)
}

/** A union of occupied cells. Disconnected components connect through empty row gutters
 * and an outer spine, so one continuous outline never encloses another color's star. */
internal fun hintGroupPath(indices: List<Int>, geometry: HintGeometry, color: HintColor): Path {
    if (indices.isEmpty()) return Path()
    val occupied = indices.toSet()
    var region = Path()
    fun union(rect: Rect) { region = Path.combine(PathOperation.Union, region, Path().apply { addRect(rect) }) }
    val c = geometry.cell
    if (indices.last() - indices.first() + 1 == indices.size) {
        // Consecutive slots form row strips joined across their shared columns:
        // a single clean stepped outline without internal seams.
        val rows = indices.groupBy { it / 5 }.toSortedMap()
        rows.forEach { (row, slots) ->
            val first = geometry.center(slots.first()); val last = geometry.center(slots.last())
            union(Rect(first.x - c * .45f, first.y - c * .43f, last.x + c * .45f, last.y + c * .43f))
            val below = rows[row + 1]
            if (below != null) {
                val left = maxOf(slots.first() % 5, below.first() % 5)
                val right = minOf(slots.last() % 5, below.last() % 5)
                if (left <= right) {
                    val from = geometry.center(row * 5 + left)
                    val to = geometry.center((row + 1) * 5 + right)
                    union(Rect(from.x - c * .45f, from.y, to.x + c * .45f, to.y))
                }
            }
        }
        return region
    }
    indices.forEach { i ->
        val p = geometry.center(i)
        union(Rect(p - Offset(c * .45f, c * .43f), p + Offset(c * .45f, c * .43f)))
    }
    val remaining = occupied.toMutableSet()
    val anchors = mutableListOf<Int>()
    while (remaining.isNotEmpty()) {
        val anchor = remaining.first(); anchors.add(anchor)
        val queue = ArrayDeque<Int>(); queue.add(anchor); remaining.remove(anchor)
        while (queue.isNotEmpty()) {
            val i = queue.removeFirst()
            val neighbors = listOfNotNull(if (i % 5 > 0) i - 1 else null,
                if (i % 5 < 4) i + 1 else null, i - 5, i + 5)
            // Join a spanning tree, not every adjacent edge. This avoids closed
            // rings/holes around stars belonging to the other color.
            neighbors.forEach { neighbor ->
                if (remaining.remove(neighbor)) {
                    queue.add(neighbor)
                    val from = geometry.center(i); val to = geometry.center(neighbor)
                    if (i / 5 == neighbor / 5)
                        union(Rect(minOf(from.x, to.x), from.y - c * .43f, maxOf(from.x, to.x), from.y + c * .43f))
                    else union(Rect(from.x - c * .45f, minOf(from.y, to.y), from.x + c * .45f, maxOf(from.y, to.y)))
                }
            }
        }
    }
    if (anchors.size > 1) {
        val spine = if (color == HintColor.YELLOW) geometry.grid.left - c * .05f else geometry.grid.right + c * .05f
        val gutterYs = anchors.map { geometry.grid.top + (it / 5) * c + c * .025f }
        union(Rect(spine - c * .018f, gutterYs.min() - c * .018f, spine + c * .018f, gutterYs.max() + c * .018f))
        anchors.forEachIndexed { index, i ->
            val p = geometry.center(i); val y = gutterYs[index]
            union(Rect(minOf(spine, p.x) - c * .018f, y - c * .018f,
                maxOf(spine, p.x) + c * .018f, y + c * .018f))
            union(Rect(p.x - c * .018f, y, p.x + c * .018f, p.y))
        }
    }
    return region
}

internal fun DrawScope.hintGroup(indices: List<Int>, geometry: HintGeometry, color: HintColor) {
    val path = hintGroupPath(indices, geometry, color)
    val tint = color.tint()
    drawPath(path, tint.copy(alpha = .14f), style = Stroke(geometry.cell * .11f,
        pathEffect = PathEffect.cornerPathEffect(geometry.cell * .12f)))
    drawPath(path, tint, style = Stroke(geometry.cell * .035f, join = StrokeJoin.Round,
        pathEffect = PathEffect.chainPathEffect(
            PathEffect.dashPathEffect(floatArrayOf(geometry.cell * .15f, geometry.cell * .09f)),
            PathEffect.cornerPathEffect(geometry.cell * .12f))))
}

