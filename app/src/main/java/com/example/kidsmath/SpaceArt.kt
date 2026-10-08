package com.example.kidsmath

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val Ink = Color(0xFF092354)
private val Sunshine = Color(0xFFFFD34B)

internal fun DrawScope.star(center: Offset, radius: Float, color: Color, glow: Boolean = true) {
    if (radius <= .1f) return
    if (glow) drawCircle(Brush.radialGradient(listOf(color.copy(alpha = .32f), Color.Transparent),
        center, radius * 2.8f), radius * 2.8f, center)
    val path = Path()
    repeat(10) { index ->
        val angle = -Math.PI / 2 + index * Math.PI / 5
        val r = if (index % 2 == 0) radius else radius * .48f
        val point = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * r
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    drawPath(path, Color(0xFFAA5D13), style = Stroke(radius * .12f))
    drawPath(path, Brush.linearGradient(listOf(Color(0xFFFFF2A1), color, Color(0xFFFFAC35)),
        center - Offset(radius, radius), center + Offset(radius, radius)))
    drawCircle(Color.White.copy(alpha = .7f), radius * .12f, center + Offset(-radius * .16f, -radius * .28f))
}

internal fun DrawScope.planet(center: Offset, radius: Float, color: Color, vivid: Boolean = false) {
    if (radius <= .1f) return
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = .28f), Color.Transparent),
        center, radius * 1.35f), radius * 1.35f, center)
    val disk = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center - Offset(radius, radius),
        center + Offset(radius, radius))) }
    clipPath(disk) {
        drawCircle(Brush.radialGradient(listOf(if (vivid) androidx.compose.ui.graphics.lerp(color, Color.White, .45f) else Color(0xFFFFD893), color,
            if (vivid) androidx.compose.ui.graphics.lerp(color, Ink, .45f) else Color(0xFF39266F)),
            center + Offset(-radius * .48f, -radius * .62f), radius * 2f), radius, center)
        // Curved cloud bands and shaded craters give the same small illustration several personalities.
        repeat(3) { index ->
            val band = Path().apply {
                val y = center.y - radius * .75f + index * radius * .62f
                moveTo(center.x - radius * 1.2f, y)
                cubicTo(center.x - radius * .3f, y + radius * .55f, center.x + radius * .3f,
                    y - radius * .45f, center.x + radius * 1.2f, y + radius * .2f)
            }
            drawPath(band, color.copy(alpha = .45f), style = Stroke(radius * .22f))
        }
        val random = Random(48)
        repeat(9) { index ->
            val crater = center + Offset(random.nextFloat() * 1.7f - .85f, random.nextFloat() * 1.7f - .85f) * radius
            val r = radius * (.06f + random.nextFloat() * .12f)
            drawOval(Color(0xFF342767).copy(alpha = .34f), crater - Offset(r, r * .6f), Size(r * 2f, r * 1.25f))
            drawArc(Color(0xFFFFD1AB).copy(alpha = .48f), 20f, 140f, false,
                crater - Offset(r, r * .6f), Size(r * 2f, r * 1.25f), style = Stroke(radius * .025f))
            if (index % 3 == 0) drawCircle(color.copy(alpha = .6f), r * .35f, crater)
        }
        drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF191950).copy(alpha = if (vivid) .18f else .55f)),
            center + Offset(-radius * .6f, -radius * .6f), radius * 2f), radius, center)
    }
    drawCircle(Color(0xFFC1EDFF).copy(alpha = .45f), radius, center, style = Stroke(radius * .025f))
    drawArc(Color.White.copy(alpha = .6f), 205f, 65f, false, center - Offset(radius * .89f, radius * .89f),
        Size(radius * 1.78f, radius * 1.78f), style = Stroke(radius * .035f))
}

internal fun DrawScope.rocket(center: Offset, width: Float, flame: Float = 1f, enginesOn: Boolean = true) {
    if (width <= .1f) return
    val x = center.x; val y = center.y
    drawOval(Color(0xFF020828).copy(alpha = .25f), Offset(x - width * .48f, y + width * .24f),
        Size(width * .95f, width * .16f))
    if (enginesOn) {
        val exhaust = Path().apply {
            moveTo(x - width * .28f, y - width * .12f)
            quadraticTo(x - width * .55f, y - width * .23f, x - width * (.82f + .14f * flame), y + width * .09f)
            lineTo(x - width * .63f, y + width * .08f)
            lineTo(x - width * .72f, y + width * .20f)
            quadraticTo(x - width * .4f, y + width * .18f, x - width * .28f, y + width * .12f)
            close()
        }
        drawPath(exhaust, Brush.linearGradient(listOf(Color(0xFFFFB62F), Color(0xFFFF6026)),
            Offset(x - width, y), Offset(x, y)))
        drawOval(Sunshine, Offset(x - width * (.67f + .06f * flame), y - width * .055f),
            Size(width * .42f, width * .12f))
        drawOval(Color(0xFFFFF5C4), Offset(x - width * .48f, y - width * .035f), Size(width * .2f, width * .07f))
    }
    val fins = Path().apply {
        moveTo(x - width * .27f, y - width * .09f)
        quadraticTo(x - width * .44f, y - width * .36f, x + width * .04f, y - width * .28f)
        lineTo(x + width * .16f, y - width * .1f)
        lineTo(x + width * .16f, y + width * .1f)
        quadraticTo(x - width * .02f, y + width * .38f, x - width * .39f, y + width * .3f)
        lineTo(x - width * .27f, y + width * .08f); close()
    }
    drawPath(fins, Ink, style = Stroke(width * .025f))
    drawPath(fins, Brush.verticalGradient(listOf(Color(0xFFFF8B72), Color(0xFFE82A43)), y - width * .3f, y + width * .3f))
    val body = Path().apply {
        moveTo(x - width * .39f, y - width * .11f)
        cubicTo(x - width * .08f, y - width * .28f, x + width * .37f, y - width * .27f, x + width * .48f, y)
        cubicTo(x + width * .28f, y + width * .25f, x - width * .06f, y + width * .22f, x - width * .39f, y + width * .10f)
        close()
    }
    drawPath(body, Ink, style = Stroke(width * .025f))
    drawPath(body, Brush.verticalGradient(listOf(Color.White, Color(0xFFE4F6FF), Color(0xFF8DBADA)),
        y - width * .2f, y + width * .2f))
    val nose = Path().apply {
        moveTo(x + width * .24f, y - width * .19f)
        quadraticTo(x + width * .6f, y - width * .17f, x + width * .48f, y)
        quadraticTo(x + width * .39f, y + width * .14f, x + width * .24f, y + width * .18f)
        quadraticTo(x + width * .18f, y, x + width * .24f, y - width * .19f); close()
    }
    drawPath(nose, Brush.verticalGradient(listOf(Color(0xFFFF987F), Color(0xFFFF414C), Color(0xFFBC1634)),
        y - width * .2f, y + width * .2f))
    drawLine(Color.White.copy(alpha = .8f), Offset(x + width * .28f, y - width * .14f),
        Offset(x + width * .41f, y - width * .10f), width * .025f)
    drawOval(Color(0xFF5597CA), Offset(x - width * .37f, y - width * .12f), Size(width * .09f, width * .24f))
    drawCircle(Ink, width * .14f, Offset(x + width * .02f, y))
    drawCircle(Color(0xFF6CE1FF), width * .116f, Offset(x + width * .02f, y))
    drawCircle(Brush.radialGradient(listOf(Color(0xFF43DAFF), Color(0xFF0673CA)),
        Offset(x, y - width * .025f), width * .15f), width * .09f, Offset(x + width * .02f, y))
    drawCircle(Color.White.copy(alpha = .9f), width * .035f, Offset(x - width * .008f, y - width * .045f))
    val wing = Path().apply {
        moveTo(x - width * .09f, y + width * .07f)
        quadraticTo(x - width * .28f, y + width * .08f, x - width * .31f, y + width * .28f)
        quadraticTo(x - width * .06f, y + width * .21f, x + width * .06f, y + width * .10f); close()
    }
    drawPath(wing, Color(0xFFEE3B52)); drawPath(wing, Ink.copy(alpha = .5f), style = Stroke(width * .012f))
}

internal fun DrawScope.astronaut(center: Offset, height: Float, celebrating: Boolean = false) {
    val r = height * .19f
    val head = center + Offset(0f, -height * .24f)
    val suit = Color(0xFFEDF9FF)
    val blue = Color(0xFF279EDD)
    // Rounded suit, boots and waving arms, with a warm child face inside a glossy helmet.
    drawRoundRect(Color(0xFF80B9DE), center + Offset(-r * .95f, -height * .1f),
        Size(r * 1.9f, height * .37f), CornerRadius(r * .4f))
    drawRoundRect(suit, center + Offset(-r * .75f, -height * .12f),
        Size(r * 1.5f, height * .36f), CornerRadius(r * .4f))
    val leftFoot = center + Offset(-r * .47f, height * .30f)
    val rightFoot = center + Offset(r * .5f, height * .30f)
    listOf(leftFoot, rightFoot).forEach {
        drawLine(Ink, it - Offset(0f, r * .65f), it, r * .67f)
        drawLine(suit, it - Offset(0f, r * .65f), it, r * .53f)
        drawOval(blue, it - Offset(r * .4f, r * .16f), Size(r * .9f, r * .38f))
    }
    val leftHand = center + Offset(-r * 1.6f, if (celebrating) -height * .21f else height * .04f)
    val rightHand = center + Offset(r * 1.55f, -height * .25f)
    listOf(leftHand to -1f, rightHand to 1f).forEach { (hand, side) ->
        val shoulder = center + Offset(side * r * .65f, -height * .04f)
        drawLine(Ink, shoulder, hand, r * .55f)
        drawLine(suit, shoulder, hand, r * .43f)
        drawCircle(blue, r * .27f, hand)
        drawCircle(suit, r * .22f, hand + Offset(0f, -r * .17f))
        repeat(3) { finger ->
            drawLine(suit, hand + Offset((finger - 1) * r * .12f, -r * .17f),
                hand + Offset((finger - 1) * r * .16f, -r * .43f), r * .10f)
        }
    }
    drawRoundRect(Ink, center + Offset(-r * .39f, -r * .12f), Size(r * .78f, r * .65f), CornerRadius(r * .1f))
    drawRoundRect(blue, center + Offset(-r * .32f, -r * .06f), Size(r * .64f, r * .51f), CornerRadius(r * .1f))
    drawCircle(Color(0xFFFFD34B), r * .07f, center + Offset(-r * .12f, r * .12f))
    drawCircle(Color(0xFFFF678C), r * .07f, center + Offset(r * .12f, r * .12f))
    drawCircle(Ink, r * 1.17f, head)
    drawCircle(Brush.linearGradient(listOf(Color.White, Color(0xFF7EC8F3))), r * 1.11f, head)
    drawCircle(Color(0xFF226AA5), r * .95f, head)
    drawCircle(Color(0xFFFFD3A4), r * .78f, head + Offset(0f, r * .06f))
    val hair = Path().apply {
        moveTo(head.x - r * .78f, head.y)
        cubicTo(head.x - r * .9f, head.y - r * .85f, head.x + r * .6f, head.y - r, head.x + r * .75f, head.y)
        lineTo(head.x + r * .35f, head.y - r * .36f)
        lineTo(head.x + r * .04f, head.y - r * .13f)
        lineTo(head.x - r * .1f, head.y - r * .37f)
        lineTo(head.x - r * .48f, head.y - r * .07f); close()
    }
    drawPath(hair, Color(0xFF64371E))
    drawArc(Color(0xFFAA6737), 215f, 70f, false, head - Offset(r * .6f, r * .65f),
        Size(r * 1.15f, r), style = Stroke(r * .14f))
    listOf(-.3f, .3f).forEach { eye ->
        val point = head + Offset(r * eye, r * .06f)
        if (celebrating) drawArc(Ink, 195f, 150f, false, point - Offset(r * .14f, r * .10f),
            Size(r * .28f, r * .20f), style = Stroke(r * .055f))
        else {
            drawOval(Ink, point - Offset(r * .09f, r * .15f), Size(r * .18f, r * .28f))
            drawCircle(Color.White, r * .035f, point + Offset(-r * .025f, -r * .075f))
        }
        drawOval(Color(0xFFFFA190).copy(alpha = .7f), head + Offset(r * eye - r * .17f, r * .24f), Size(r * .34f, r * .14f))
    }
    drawArc(Color(0xFF963B35), 0f, 180f, true, head + Offset(-r * .27f, r * .19f), Size(r * .54f, r * .40f))
    drawArc(Color.White.copy(alpha = .85f), 205f, 70f, false, head - Offset(r, r), Size(r * 2f, r * 2f), style = Stroke(r * .09f))
    drawCircle(blue, r * .19f, head + Offset(-r * 1.02f, r * .15f))
    drawCircle(blue, r * .19f, head + Offset(r * 1.02f, r * .15f))
}

internal fun DrawScope.confetti(progress: Float, count: Int, seed: Int = 23) {
    val random = Random(seed)
    val colors = listOf(Sunshine, Color(0xFF46D6FF), Color(0xFF70EF8A), Color(0xFFFF88CF), Color(0xFFAE91FF))
    repeat(count) { index ->
        val startX = random.nextFloat() * size.width
        val startY = -random.nextFloat() * size.height
        val point = Offset(startX + sin(progress * 9f + index) * 18f, startY + progress * size.height * 2f)
        rotate(progress * 390f + index * 17f, point) {
            if (index % 5 == 0) star(point, 6f + size.minDimension * .008f, colors[index % colors.size], glow = false)
            else drawRoundRect(colors[index % colors.size], point, Size(5f + size.width * .002f, 10f), CornerRadius(2f))
        }
    }
}

internal fun DrawScope.trophy(center: Offset, height: Float) {
    val cup = Path().apply {
        moveTo(center.x - height * .33f, center.y - height * .45f)
        lineTo(center.x + height * .33f, center.y - height * .45f)
        quadraticTo(center.x + height * .32f, center.y + height * .07f, center.x, center.y + height * .12f)
        quadraticTo(center.x - height * .32f, center.y + height * .07f, center.x - height * .33f, center.y - height * .45f)
        close()
    }
    drawLine(Color(0xFFFFAD21), center, center + Offset(0f, height * .35f), height * .13f)
    drawRoundRect(Sunshine, center + Offset(-height * .25f, height * .31f),
        Size(height * .5f, height * .12f), CornerRadius(height * .04f))
    listOf(-1f, 1f).forEach { side ->
        drawOval(Color(0xFFFFBB33), center + Offset(side * height * .31f - height * .15f, -height * .34f),
            Size(height * .3f, height * .3f), style = Stroke(height * .06f))
    }
    drawPath(cup, Color(0xFFC87815), style = Stroke(height * .03f))
    drawPath(cup, Brush.linearGradient(listOf(Color(0xFFFFF5AA), Sunshine, Color(0xFFFFA721)),
        center - Offset(height * .3f, 0f), center + Offset(height * .3f, 0f)))
    star(center + Offset(0f, -height * .2f), height * .12f, Color(0xFFFFF5AA), glow = false)
}

internal fun DrawScope.energyCrystal(center: Offset, radius: Float) {
    if (radius <= 0f) return
    drawCircle(Brush.radialGradient(listOf(Color(0xFF28E9FF).copy(alpha = .40f), Color.Transparent), center, radius * 2.5f), radius * 2.5f, center)
    val shape = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * .65f, center.y - radius * .15f)
        lineTo(center.x + radius * .45f, center.y + radius * .75f)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius * .55f, center.y + radius * .5f)
        lineTo(center.x - radius * .65f, center.y - radius * .1f)
        close()
    }
    drawPath(shape, Brush.linearGradient(listOf(Color(0xFFDCFFFF), Color(0xFF35DDFF), Color(0xFF3975EE)), center - Offset(radius, radius), center + Offset(radius, radius)))
    drawPath(shape, Color.White, style = Stroke(radius * .08f))
    drawLine(Color.White.copy(alpha = .8f), center - Offset(0f, radius), center + Offset(radius * .10f, radius * .8f), radius * .07f)
    drawLine(Color.White.copy(alpha = .7f), center - Offset(radius * .6f, radius * .1f), center + Offset(radius * .6f, -radius * .15f), radius * .07f)
}
