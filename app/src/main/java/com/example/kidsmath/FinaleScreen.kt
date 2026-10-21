package com.example.kidsmath

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.*

private val FinaleGold = Color(0xFFFFDC72)
private val FinaleNavy = Color(0xFF081638)

@Composable
internal fun FinaleScreen(state: AppState) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle, state) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (!resumed) state.persistFinale()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); state.persistFinale() }
    }
    LaunchedEffect(resumed, state) {
        if (!resumed) return@LaunchedEffect
        var previous = withFrameMillis { it }
        while ((state.finaleElapsedMs ?: FinaleTimeline.durationMs) < FinaleTimeline.durationMs) {
            val now = withFrameMillis { it }
            // Background time and a stalled renderer must never skip a story beat.
            state.advanceFinale((now - previous).coerceIn(0, 100))
            previous = now
        }
    }
    val elapsed = state.finaleElapsedMs ?: FinaleTimeline.durationMs
    val art = finaleArt()
    Box(Modifier.fillMaxSize().background(FinaleNavy).testTag("campaign-finale")) {
        if (elapsed >= FinaleTimeline.durationMs) {
            VictoryScreen(art, state::openMap)
        } else {
            val frame = FinaleTimeline.frame(elapsed)
            Crossfade(frame.scene, animationSpec = tween(650), label = "Story dissolve") { scene ->
                val progress = if (scene == frame.scene) frame.progress else 1f
                Canvas(Modifier.fillMaxSize().semantics { contentDescription = scene.description }) {
                    storyScene(scene, progress, art)
                }
            }
            TextButton(state::skipFinale, Modifier.align(Alignment.TopEnd).padding(8.dp)
                .background(FinaleNavy.copy(alpha = .68f), RoundedCornerShape(24.dp)).testTag("skip-finale")) {
                Text("Skip", color = Color.White, fontSize = 16.sp)
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FinaleScene.entries.forEach { scene ->
                    Canvas(Modifier.size(if (scene == frame.scene) 9.dp else 6.dp)) {
                        drawCircle(if (scene.ordinal <= frame.scene.ordinal) FinaleGold else Color.White.copy(alpha = .3f))
                    }
                }
            }
        }
    }
}

private data class FinaleArt(val space: ImageBitmap, val planet: ImageBitmap, val earth: ImageBitmap,
    val princessSad: ImageBitmap, val princessWave: ImageBitmap, val princessHappy: ImageBitmap,
    val astronaut: ImageBitmap, val astronautWalk: ImageBitmap, val astronautPoint: ImageBitmap,
    val dragonGrumpy: ImageBitmap, val dragonHappy: ImageBitmap, val key: ImageBitmap, val gift: ImageBitmap,
    val castle: ImageBitmap)

@Composable
private fun finaleArt() = FinaleArt(
    ImageBitmap.imageResource(R.drawable.finale_space), ImageBitmap.imageResource(R.drawable.finale_planet),
    ImageBitmap.imageResource(R.drawable.finale_earth), finaleCharacter(R.drawable.finale_princess_sad),
    finaleCharacter(R.drawable.finale_princess_wave, caption = true), finaleCharacter(R.drawable.finale_princess_happy, caption = true),
    finaleCharacter(R.drawable.finale_astronaut), finaleCharacter(R.drawable.finale_astronaut_walk, caption = true),
    finaleCharacter(R.drawable.finale_astronaut_point, caption = true), finaleCharacter(R.drawable.finale_dragon_grumpy),
    finaleCharacter(R.drawable.finale_dragon_happy, caption = true), ImageBitmap.imageResource(R.drawable.finale_key),
    ImageBitmap.imageResource(R.drawable.finale_star), ImageBitmap.imageResource(R.drawable.finale_castle))

/** Strip transparent margins once; pose sheets have captions below row 182.
 * Drawing then uses visible height and bottom-center anchoring for every pose. */
@Composable
private fun finaleCharacter(@DrawableRes resource: Int, caption: Boolean = false): ImageBitmap {
    val resources = LocalContext.current.resources
    return remember(resources, resource, caption) {
        val bitmap = BitmapFactory.decodeResource(resources, resource)
        val bottom = if (caption) min(182, bitmap.height) else bitmap.height
        var left = bitmap.width; var top = bottom; var right = -1; var feet = -1
        for (y in 0 until bottom) for (x in 0 until bitmap.width) {
            // Ignore near-invisible export specks when finding the visible bounds.
            if ((bitmap.getPixel(x, y) ushr 24) > 20) {
                left = min(left, x); right = max(right, x)
                top = min(top, y); feet = max(feet, y)
            }
        }
        check(right >= left && feet >= top) { "Empty finale character: $resource" }
        Bitmap.createBitmap(bitmap, left, top, right - left + 1, feet - top + 1).asImageBitmap()
    }
}
private fun DrawScope.backdrop(image: ImageBitmap, zoom: Float = 1f, alpha: Float = 1f) {
    val scale = max(size.width / image.width, size.height / image.height) * zoom
    val w = image.width * scale; val h = image.height * scale
    drawImage(image, dstOffset = IntOffset(((size.width - w) / 2).roundToInt(), ((size.height - h) / 2).roundToInt()),
        dstSize = IntSize(w.roundToInt(), h.roundToInt()), alpha = alpha, filterQuality = FilterQuality.High)
}

/** Position is the bottom center of a sprite; character assets are normalized when loaded. */
private fun DrawScope.character(image: ImageBitmap, x: Float, feet: Float, height: Float, alpha: Float = 1f, anchorX: Float = .5f) {
    val sourceHeight = image.height
    val h = size.height * height; val w = h * image.width / sourceHeight
    drawImage(image, srcSize = IntSize(image.width, sourceHeight),
        dstOffset = IntOffset((size.width * x - w * anchorX).roundToInt(), (size.height * feet - h).roundToInt()),
        dstSize = IntSize(w.roundToInt(), h.roundToInt()), alpha = alpha.coerceIn(0f, 1f), filterQuality = FilterQuality.High)
}

private fun smooth(p: Float) = p.coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }
private fun part(p: Float, start: Float, end: Float) = smooth((p - start) / (end - start))
private fun mix(a: Float, b: Float, p: Float) = a + (b - a) * p

private fun DrawScope.ship(x: Float, y: Float, width: Float, heading: Float = -90f,
    engines: Boolean = false, door: Float = 0f, passengers: Boolean = false, time: Float = 0f) {
    val c = Offset(size.width * x, size.height * y)
    val w = min(size.width * width, size.height * .55f)
    rotate(heading, c) { rocket(c, w, 1f + .12f * sin(time * 50), enginesOn = engines, shadow = false) }
    if (door > 0f) {
        val d = w * .2f * door
        drawRoundRect(FinaleNavy, c + Offset(-d / 2, w * .09f), Size(d, w * .24f), CornerRadius(d / 2))
        drawLine(Color(0xFFCBDFF9), c + Offset(0f, w * .28f), c + Offset(w * .05f, w * .64f), w * .13f)
        repeat(4) { rung ->
            val y0 = w * (.33f + rung * .075f)
            drawLine(Color(0xFF6086B8), c + Offset(-w * .05f, y0), c + Offset(w * .08f, y0), w * .018f)
        }
    }
    if (passengers) {
        drawCircle(Color(0xFFFFD0AF), w * .035f, c + Offset(-w * .03f, -w * .025f))
        drawCircle(Color(0xFFFFDF83), w * .035f, c + Offset(w * .045f, -w * .025f))
    }
}

private fun DrawScope.fantasy(art: FinaleArt, p: Float, unlocked: Boolean = false) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF151740), Color(0xFF804483), Color(0xFFE49CBC))))
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE3BC), Color(0xFFD977C9)),
        Offset(size.width * .2f, size.height * .27f), size.height * .21f), size.height * .21f,
        Offset(size.width * .2f, size.height * .27f))
    repeat(35) { i ->
        star(Offset(size.width * ((i * .618034f) % 1), size.height * (.06f + ((i * .173f) % .45f))),
            size.height * (.002f + .001f * (i % 3)), Color.White.copy(alpha = .5f))
    }
    drawOval(Color(0xFF62426D), Offset(-size.width * .1f, size.height * .68f), Size(size.width * 1.3f, size.height * .65f))
    drawOval(Brush.verticalGradient(listOf(Color(0xFFAC6D9B), Color(0xFF503759))),
        Offset(-size.width * .2f, size.height * .79f), Size(size.width * 1.5f, size.height * .5f))
    character(art.castle, .85f, .83f, .58f)
    // One foreground tower with a readable locked door; the princess appears at its balcony.
    val tower = Rect(size.width * .71f, size.height * .24f, size.width * .80f, size.height * .86f)
    drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF755D9D), Color(0xFFB49BCB), Color(0xFF665080)), tower.left, tower.right),
        tower.topLeft, tower.size, CornerRadius(size.height * .025f))
    repeat(12) { row ->
        val y = tower.top + row * tower.height / 12
        drawLine(Color(0xFF64517C).copy(alpha = .28f), Offset(tower.left, y), Offset(tower.right, y), size.height * .002f)
        repeat(3) { col ->
            val x = tower.left + tower.width * ((col + .5f * (row % 2)) / 3)
            drawLine(Color(0xFF64517C).copy(alpha = .22f), Offset(x, y), Offset(x, y + tower.height / 12), size.height * .002f)
        }
    }
    val roof = Path().apply {
        moveTo(tower.left - size.width * .015f, tower.top + size.height * .02f)
        lineTo(tower.center.x, tower.top - size.height * .15f)
        lineTo(tower.right + size.width * .015f, tower.top + size.height * .02f); close()
    }
    drawPath(roof, Brush.verticalGradient(listOf(Color(0xFFFFA7D3), Color(0xFFBD478C))))
    val window = Rect(size.width * .728f, size.height * .33f, size.width * .782f, size.height * .53f)
    drawRoundRect(FinaleNavy, window.topLeft, window.size, CornerRadius(window.width / 2))
    val door = Rect(size.width * .735f, size.height * .65f, size.width * .78f, size.height * .86f)
    drawRoundRect(if (unlocked) FinaleNavy else Color(0xFF724052), door.topLeft, door.size, CornerRadius(door.width / 2))
    drawRoundRect(FinaleGold, door.topLeft, door.size, CornerRadius(door.width / 2), style = Stroke(size.height * .004f))
    if (!unlocked) {
        val lock = Offset(door.center.x, door.center.y)
        drawArc(FinaleGold, 180f, 180f, false, lock - Offset(door.width * .2f, door.width * .3f),
            Size(door.width * .4f, door.width * .5f), style = Stroke(size.height * .006f))
        drawRoundRect(FinaleGold, lock - Offset(door.width * .27f, 0f), Size(door.width * .54f, door.width * .42f), CornerRadius(5f))
        drawCircle(FinaleNavy, size.height * .006f, lock + Offset(0f, door.width * .19f))
    }
    repeat(14) { i ->
        val x = size.width * (.03f + i * .071f)
        val y = size.height * (.9f + .06f * sin(i.toFloat()))
        star(Offset(x, y), size.height * .009f, Color(0xFFFFCBDF).copy(alpha = .65f))
    }
}

private fun DrawScope.princessInTower(art: FinaleArt, p: Float, released: Boolean = false) {
    clipRect(size.width * .71f, size.height * .30f, size.width * .80f, size.height * .53f) {
        character(if (released) art.princessHappy else art.princessSad, .755f, .64f + .002f * sin(p * 7f), .31f)
    }
    drawRoundRect(Color(0xFFD6B6E3), Offset(size.width * .713f, size.height * .52f),
        Size(size.width * .084f, size.height * .025f), CornerRadius(5f))
}

private fun DrawScope.key(art: FinaleArt, x: Float, y: Float, alpha: Float = 1f) {
    // Crop the empty headroom in this prop, preserving its golden heart silhouette.
    val h = size.height * .14f
    val w = h * art.key.width / (art.key.height - 75)
    drawImage(art.key, srcOffset = IntOffset(0, 75), srcSize = IntSize(art.key.width, art.key.height - 75),
        dstOffset = IntOffset((x * size.width - w / 2).roundToInt(), (y * size.height - h / 2).roundToInt()),
        dstSize = IntSize(w.roundToInt(), h.roundToInt()), alpha = alpha, filterQuality = FilterQuality.High)
}

private fun DrawScope.gift(art: FinaleArt, x: Float, y: Float, p: Float) {
    val c = Offset(size.width * x, size.height * y)
    drawCircle(Brush.radialGradient(listOf(FinaleGold.copy(alpha = .6f), Color.Transparent), c, size.height * .13f), size.height * .13f, c)
    val h = size.height * .15f
    drawImage(art.gift, srcOffset = IntOffset(0, 100), srcSize = IntSize(art.gift.width, art.gift.height - 100),
        dstOffset = IntOffset((c.x - h * .42f).roundToInt(), (c.y - h / 2).roundToInt()),
        dstSize = IntSize((h * .84f).roundToInt(), h.roundToInt()), filterQuality = FilterQuality.High)
    sparkles(x, y, p, false)
}

private fun DrawScope.sparkles(x: Float, y: Float, p: Float, hearts: Boolean) {
    repeat(9) { i ->
        val a = i * 2.39996f
        val radius = size.height * (.07f + .04f * ((p * 2 + i * .13f) % 1f))
        val c = Offset(size.width * x + cos(a) * radius, size.height * y + sin(a) * radius)
        star(c, size.height * (.006f + .003f * sin(p * 9 + i)), if (hearts && i % 2 == 0) Color(0xFFFF92C8) else FinaleGold)
        if (hearts && i % 3 == 0) {
            val r = size.height * .008f
            val heart = Path().apply {
                moveTo(c.x, c.y + r)
                cubicTo(c.x - r * 3, c.y - r, c.x - r, c.y - r * 2, c.x, c.y - r * .6f)
                cubicTo(c.x + r, c.y - r * 2, c.x + r * 3, c.y - r, c.x, c.y + r); close()
            }
            drawPath(heart, Color(0xFFFF92C8))
        }
    }
}

private fun DrawScope.homeLandscape() {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF42C1F0), Color(0xFFC3F3FF), Color(0xFFFDF4BA))))
    drawCircle(Color(0xFFFFF1A6), size.height * .1f, Offset(size.width * .17f, size.height * .19f))
    repeat(5) { i ->
        val c = Offset(size.width * (.12f + i * .21f), size.height * (.23f + .06f * (i % 2)))
        drawOval(Color.White.copy(alpha = .8f), c - Offset(size.width * .07f, size.height * .03f), Size(size.width * .14f, size.height * .07f))
        drawCircle(Color.White.copy(alpha = .85f), size.height * .055f, c)
    }
    drawOval(Color(0xFF4AAE75), Offset(-size.width * .1f, size.height * .62f), Size(size.width * 1.2f, size.height * .6f))
    drawRect(Brush.verticalGradient(listOf(Color(0xFF9ED865), Color(0xFF4B9E54)), size.height * .77f, size.height),
        Offset(0f, size.height * .78f), Size(size.width, size.height * .22f))
    repeat(34) { i ->
        val c = Offset(size.width * ((i * .618034f) % 1), size.height * (.84f + .14f * ((i * .37f) % 1)))
        val color = listOf(Color(0xFFFFBBDD), Color(0xFFFFDB66), Color.White)[i % 3]
        repeat(5) { petal ->
            val a = petal * 1.2566f
            drawCircle(color, size.height * .005f, c + Offset(cos(a), sin(a)) * size.height * .006f)
        }
        drawCircle(Color(0xFFFFA42B), size.height * .003f, c)
    }
}

private fun DrawScope.storyScene(scene: FinaleScene, p: Float, art: FinaleArt) {
    when (scene) {
        FinaleScene.JOURNEY -> {
            backdrop(art.space, 1f + p * .04f)
            planet(Offset(size.width * .77f, size.height * .43f), size.height * (.14f + p * .07f), Color(0xFFDA8CCB))
            ship(mix(-.15f, .75f, smooth(p)), mix(.73f, .43f, smooth(p)), mix(.23f, .055f, p), -18f, true, time = p)
            drawRect(FinaleNavy.copy(alpha = 1f - part(p, 0f, .12f)))
        }
        FinaleScene.CASTLE -> {
            // Baked-in castle is used only for the establishing shot, never behind a second castle.
            run {
                fantasy(art, p); princessInTower(art, p)
                character(art.dragonGrumpy, .66f, .86f, .31f, anchorX = .42f)
                key(art, .705f, .70f)
                sparkles(.76f, .38f, p, false)
                if (p < .40f) backdrop(art.planet, 1f + p * .15f, 1f - part(p, .10f, .40f))
            }
        }
        FinaleScene.MEETING -> {
            fantasy(art, p); princessInTower(art, p)
            val landing = part(p, 0f, .38f)
            ship(.23f, mix(-.30f, .64f, landing), .19f, engines = p < .35f, door = part(p, .32f, .48f), time = p)
            if (p > .40f) character(art.astronautWalk, mix(.25f, .53f, part(p, .4f, 1f)), .87f - .004f * abs(sin(p * 40)), .27f)
            character(art.dragonGrumpy, .66f - .015f * part(p, .7f, 1f), .86f, .31f, anchorX = .42f)
            key(art, .70f, .70f)
        }
        FinaleScene.GIFT -> {
            fantasy(art, p); princessInTower(art, p)
            ship(.23f, .64f, .19f, door = 1f)
            character(art.astronautPoint, .53f, .87f, .27f)
            val delight = FinaleEmotions.dragonDelight(FinaleFrame(scene, p))
            val dragonFeet = .86f - if (p > .44f) .012f * abs(sin(p * 19)) else 0f
            // The grumpy silhouette includes a flame to the right; anchor its feet, not the flame.
            character(art.dragonGrumpy, .66f, dragonFeet, .31f, alpha = 1f - delight, anchorX = .42f)
            character(art.dragonHappy, .66f, dragonFeet, .31f, alpha = delight)
            val offer = part(p, .08f, .48f)
            gift(art, mix(.56f, .64f, offer), .68f - .025f * sin(offer * PI.toFloat()), p)
            val exchange = part(p, .54f, .91f)
            key(art, mix(.70f, .565f, exchange), mix(.70f, .76f, exchange))
            if (p > .55f) sparkles(.65f, .57f, p, true)
        }
        FinaleScene.RESCUE -> {
            val unlocked = FinaleEmotions.princessReleased(FinaleFrame(scene, p))
            fantasy(art, p, unlocked)
            ship(.23f, .64f, .19f, door = 1f)
            if (p < .36f) princessInTower(art, p, released = unlocked)
            val astronautX = mix(.53f, .70f, part(p, 0f, .3f))
            character(if (p < .3f) art.astronautWalk else art.astronaut, astronautX, .87f, .27f)
            if (p < .39f) key(art, mix(.565f, .752f, part(p, 0f, .32f)), .755f, 1f - part(p, .32f, .39f))
            if (unlocked) {
                character(art.princessHappy, mix(.755f, .62f, part(p, .36f, .73f)), .87f, .26f,
                    part(p, .36f, .46f))
                sparkles(.66f, .62f, p, true)
            }
            character(art.dragonHappy, .86f, .87f, .28f)
        }
        FinaleScene.BOARDING -> {
            fantasy(art, p, true)
            val walk = part(p, 0f, .52f)
            val entered = part(p, .46f, .60f)
            val takeoff = part(p, .65f, 1f)
            ship(mix(.23f, .13f, takeoff), mix(.64f, -.4f, takeoff), .19f,
                engines = p > .63f, door = 1f - entered, passengers = entered > .5f, time = p)
            if (entered < 1f) {
                character(art.astronautWalk, mix(.70f, .24f, walk), .87f, .27f, 1f - entered)
                character(art.princessWave, mix(.62f, .25f, walk), .87f, .26f, 1f - entered)
            }
            character(art.dragonHappy, .86f, .87f - .006f * sin(p * 15), .28f)
            sparkles(.86f, .58f, p, true)
        }
        FinaleScene.EARTH -> {
            backdrop(art.earth, 1f + p * .05f)
            ship(mix(-.15f, .61f, smooth(p)), mix(.25f, .6f, smooth(p)), mix(.22f, .055f, p),
                24f, true, passengers = true, time = p)
        }
        FinaleScene.HOME -> {
            homeLandscape()
            ship(.32f, mix(-.25f, .63f, part(p, 0f, .4f)), .20f,
                engines = p < .37f, door = part(p, .4f, .53f), time = p)
            if (p > .5f) {
                character(art.astronaut, mix(.32f, .48f, part(p, .5f, .73f)), .86f, .30f,
                    alpha = part(p, .5f, .60f))
                character(art.princessHappy, mix(.33f, .64f, part(p, .54f, .8f)), .86f, .29f,
                    part(p, .54f, .64f))
                sparkles(.57f, .56f, p, true)
                confetti((p * 1.4f) % 1f, 48, seed = 92)
            }
        }
    }
}

@Composable
private fun VictoryScreen(art: FinaleArt, map: () -> Unit) {
    Box(Modifier.fillMaxSize().testTag("finale-victory")) {
        Canvas(Modifier.fillMaxSize()) {
            homeLandscape()
            character(art.astronaut, .15f, .88f, .37f)
            character(art.princessHappy, .85f, .88f, .36f)
            sparkles(.15f, .45f, .7f, true); sparkles(.85f, .45f, .7f, true)
        }
        Column(Modifier.align(Alignment.Center).fillMaxWidth(.62f)
            .background(FinaleNavy.copy(alpha = .94f), RoundedCornerShape(28.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("MISSION COMPLETE!", color = FinaleGold, fontFamily = RoundedBold,
                fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("You Saved the Princess!", color = Color.White, fontSize = 21.sp)
            Text("All ${CampaignConfig.levels.size} Levels Completed!", color = Color(0xFFBEEBFF), fontSize = 18.sp)
            Button(map, Modifier.heightIn(min = 52.dp).testTag("finale-map"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF228B66))) {
                Text("Back to Space Map", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}
