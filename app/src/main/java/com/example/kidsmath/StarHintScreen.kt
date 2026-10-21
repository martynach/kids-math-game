package com.example.kidsmath

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun StarHintButton(enabled: Boolean, onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "Hint star pulse")
    val scale by pulse.animateFloat(1f, 1.12f,
        infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "Gentle scale")
    Box(Modifier.size(76.dp).semantics { contentDescription = "Star counting hint" }
        .clickable(enabled = enabled, onClickLabel = "Open hint", onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(60.dp).graphicsLayer { scaleX = scale; scaleY = scale }) {
            countingStar(center, size.minDimension * .43f, HintGold)
            val shimmer = (scale - 1f) / .12f
            drawCircle(Color.White.copy(alpha = .35f + shimmer * .5f), size.minDimension * .035f,
                center + Offset(size.width * .24f, -size.height * .26f))
        }
        Text("?", color = HintInk, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

/** Disposal of this composable cancels the timeline, travel, return, cross and drag jobs. */
@Composable
internal fun StarHintScreen(question: Question, input: String, level: Int,
                            close: () -> Unit, totalLevels: Int = CampaignConfig.levels.size,
                            keypad: @Composable () -> Unit) {
    val session = remember { StarHintState(question, level, totalLevels) }
    var revision by remember { mutableIntStateOf(0) }
    val changed = { revision++ }
    @Suppress("UNUSED_VARIABLE") val observe = revision
    val stars = session.stars
    val tutorial = rememberHintTutorial(session.tutorialStage)
    LaunchedEffect(input) { tutorial.interact() }
    val arrived = remember { mutableStateMapOf<Long, Boolean>() }
    val origins = remember { mutableStateMapOf<Long, Offset>() }
    var demonstrated by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(session) {
        if (!session.automatic) return@LaunchedEffect
        try {
            val cues = hintTimeline(question)
            var index = 0
            val start = withFrameMillis { it }
            while (index < cues.size) {
                val elapsed = withFrameMillis { it } - start
                while (index < cues.size && cues[index].atMs <= elapsed) {
                    when (val action = cues[index++].action) {
                        is HintAction.Add -> session.add(action.color)
                        is HintAction.Cross -> session.stars.getOrNull(action.index)?.let {
                            session.toggle(it.id); demonstrated = it.id
                        }
                    }
                    changed()
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (invalid: IllegalArgumentException) {
            Log.e("StarHint", "Invalid visualization data", invalid); close()
        } catch (invalid: IllegalStateException) {
            Log.e("StarHint", "Visualization timeline failed", invalid); close()
        }
    }
    Column(Modifier.fillMaxSize().hintActivity(tutorial).testTag("star-hint").semantics {
        contentDescription = "Star counting screen"
        stateDescription = if (session.automatic) "Automatic counting" else "Interactive counting"
    }, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("STAR EXPLORER", color = HintGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(if (session.automatic) "Watch & count" else "Drag stars to count", color = Color(0xFFB8D9FF), fontSize = 15.sp)
            }
            Box(Modifier.weight(1.5f).shadow(8.dp, RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFD5EEFF))), RoundedCornerShape(20.dp))
                .border(2.dp, Color.White, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center) {
                Text("${question.expression} = ${input.ifEmpty { "?" }}", color = HintInk,
                    fontSize = 32.sp, fontFamily = RoundedBold, fontWeight = FontWeight.Black)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Box(Modifier.padding(end = 6.dp).size(50.dp).shadow(5.dp, CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF4676E9), Color(0xFF203D9E))), CircleShape)
                    .border(1.dp, Color(0xFF8AB5FF), CircleShape)
                    .semantics { contentDescription = "Close hint" }
                    .clickable(onClick = close), contentAlignment = Alignment.Center) {
                    Text("×", fontSize = 36.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val geometry = remember(constraints.maxWidth, constraints.maxHeight) {
                HintGeometry(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
            }
            val minimumDragDistance = with(density) { 24.dp.toPx() }
            val showGroups = question.addition && session.automatic
            val groups = HintColor.entries.associateWith { color ->
                val indices = stars.indices.filter { stars[it].color == color && !stars[it].returning && arrived[stars[it].id] == true }
                val expected = if (color == HintColor.YELLOW) question.left else question.right
                if (!showGroups || indices.size != expected) emptyList() else indices
            }
            Canvas(Modifier.fillMaxSize()) {
                // A quiet landing surface complements the existing space backdrop.
                drawOval(Brush.verticalGradient(listOf(Color(0xFF3B5AA8).copy(alpha = .12f), Color(0xFF10255F))),
                    Offset(-size.width * .1f, size.height * .91f), androidx.compose.ui.geometry.Size(size.width * 1.2f, size.height * .5f))
                // Keep decorative background stars from being mistaken for counters.
                val panel = geometry.grid.inflate(geometry.cell * .20f)
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF0C245B), Color(0xFF0B1C4B))),
                    panel.topLeft, panel.size, androidx.compose.ui.geometry.CornerRadius(geometry.cell * .30f))
                repeat(StarHintRules.CAPACITY) { index ->
                    countingStar(geometry.center(index), geometry.cell * .32f, HintGold, outline = true)
                }
                groups.forEach { (color, indices) -> if (indices.isNotEmpty()) hintGroup(indices, geometry, color) }
                starBag(geometry, HintColor.YELLOW)
                if (question.addition) starBag(geometry, HintColor.PINK)
            }
            Box(Modifier.offset { IntOffset(geometry.grid.left.roundToInt(), geometry.grid.top.roundToInt()) }
                .size(with(density) { (geometry.cell * 5).toDp() }, with(density) { (geometry.cell * 4).toDp() })
                .testTag("hint-grid").semantics { stateDescription = "5 columns, 4 rows, ${stars.size} stars" })
            HintColor.entries.filter { question.addition || it == HintColor.YELLOW }.forEach { color ->
                val bounds = geometry.bagTouchBounds(color)
                var dragStart by remember(color) { mutableStateOf(Offset.Zero) }
                var drag by remember(color) { mutableStateOf(Offset.Zero) }
                Box(Modifier.offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
                    .size(with(density) { bounds.width.toDp() }, with(density) { bounds.height.toDp() })
                    .testTag("hint-bag-${color.name.lowercase()}").semantics {
                        contentDescription = "${color.name.lowercase()} star bag"
                        if (showGroups) stateDescription = "${session.count(color)} stars"
                    }.pointerInput(session, geometry, color) {
                        if (!session.automatic) detectDragGestures(
                            onDragStart = { dragStart = it + bounds.topLeft; drag = dragStart },
                            onDrag = { change, _ -> change.consume(); drag = change.position + bounds.topLeft },
                            onDragCancel = { drag = Offset.Zero },
                            onDragEnd = {
                                if ((drag - dragStart).getDistance() >= minimumDragDistance && geometry.dropBounds.contains(drag)) {
                                    session.add(color, geometry.nearestSlots(drag))?.let { origins[it] = drag }
                                    changed()
                                }
                                drag = Offset.Zero
                            })
                    })
                if (!session.automatic && drag != Offset.Zero) {
                    val tokenSize = geometry.cell * .94f
                    Canvas(Modifier.offset { IntOffset((drag.x - tokenSize / 2).roundToInt(), (drag.y - tokenSize / 2).roundToInt()) }
                        .size(with(density) { tokenSize.toDp() }).testTag("hint-drag-${color.name.lowercase()}")) {
                        countingStar(center, size.minDimension * .34f, color.tint())
                    }
                }
                // Counters sit above the sacks, away from paths and touch targets.
                if (showGroups && groups[color].isNullOrEmpty()) {
                    val point = geometry.mouth(color)
                    Box(Modifier.offset { IntOffset((point.x - geometry.bagWidth * .20f).roundToInt(),
                        (point.y - geometry.bagWidth * .58f).roundToInt()) }
                        .width(with(density) { (geometry.bagWidth * .40f).toDp() })
                        .background(color.tint(), RoundedCornerShape(10.dp)).padding(vertical = 1.dp)
                        .testTag("hint-count-${color.name.lowercase()}"), contentAlignment = Alignment.Center) {
                        Text(session.count(color).toString(), color = HintInk, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            groups.forEach { (color, indices) ->
                if (indices.isNotEmpty()) {
                    val first = geometry.center(indices.first())
                    // Put the count beside the outline, outside all star hit targets.
                    val x = if (color == HintColor.YELLOW) geometry.grid.left - geometry.cell * .73f
                        else geometry.grid.right + geometry.cell * .10f
                    Box(Modifier.offset { IntOffset(x.roundToInt(), (first.y - geometry.cell * .42f).roundToInt()) }
                        .width(with(density) { (geometry.cell * .63f).toDp() })
                        .background(color.tint(), RoundedCornerShape(8.dp))
                        .testTag("hint-group-${color.name.lowercase()}")
                        .semantics { contentDescription = "${color.name.lowercase()} group of ${indices.size}" },
                        contentAlignment = Alignment.Center) {
                        Text(indices.size.toString(), color = HintInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            stars.forEach { item ->
                key(item.id) {
                    MovingHintStar(item, geometry, !session.automatic, origins[item.id] ?: geometry.mouth(item.color),
                        moveTo = { point -> session.move(item.id, geometry.nearestSlots(point)); changed() },
                        toggle = { tutorial.interact(); if (session.toggle(item.id)) changed() },
                        returnTo = { color -> session.beginReturn(item.id, color).also { if (it) changed() } },
                        returned = { session.finishReturn(item.id); arrived.remove(item.id); origins.remove(item.id); changed() },
                        landed = { arrived[item.id] = true },
                        demonstrate = demonstrated == item.id && item.crossed)
                }
            }
            TutorialFinger(if (session.tutorialStage != null) tutorial.progress else null, Modifier.fillMaxSize()) { _, wave ->
                if (session.tutorialStage == HintTutorialStage.EXTRACT) {
                    val start = geometry.mouth(HintColor.YELLOW)
                    start + (geometry.center(0) - start) * wave * .8f
                } else {
                    val lit = stars.firstOrNull { !it.crossed && !it.returning }
                    (lit?.let { geometry.center(it.slot) } ?: Offset.Zero) + Offset(0f, (1f - wave) * geometry.cell * .2f)
                }
            }
        }
        keypad()
    }
}

@Composable
private fun MovingHintStar(item: HintStar, geometry: HintGeometry, interactive: Boolean, origin: Offset,
                           moveTo: (Offset) -> Unit,
                           toggle: () -> Unit, returnTo: (HintColor) -> Boolean,
                           returned: () -> Unit, landed: () -> Unit, demonstrate: Boolean) {
    val target = geometry.center(item.slot)
    val motion = remember { Animatable(origin, Offset.VectorConverter) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentReturn by rememberUpdatedState(returnTo)
    val currentToggle by rememberUpdatedState(toggle)
    val currentMove by rememberUpdatedState(moveTo)
    var settleRevision by remember { mutableIntStateOf(0) }
    val crossed by animateFloatAsState(if (item.crossed) 1f else 0f, tween(StarHintRules.CROSS_MS), label = "Dim star")
    val touchPulse = remember { Animatable(0f) }
    LaunchedEffect(demonstrate) {
        if (demonstrate) { touchPulse.snapTo(1f); touchPulse.animateTo(0f, tween(450)) }
    }
    LaunchedEffect(target, item.returning, settleRevision) {
        motion.animateTo(if (item.returning) geometry.mouth(item.color) else target,
            tween(StarHintRules.TRAVEL_MS, easing = FastOutSlowInEasing))
        if (item.returning) returned() else landed()
    }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val minimumDragDistance = with(density) { 24.dp.toPx() }
    val tokenSize = geometry.cell * .94f
    val enabled = interactive && !item.returning
    Box(Modifier.offset {
        val center = motion.value + drag
        IntOffset((center.x - tokenSize / 2).roundToInt(), (center.y - tokenSize / 2).roundToInt())
    }.size(with(density) { tokenSize.toDp() })
        .testTag("hint-star-${item.id}").semantics {
            contentDescription = "${item.color.name.lowercase()} counting star"
            stateDescription = if (item.crossed) "Dimmed" else "Lit"
        }.pointerInput(item.id, enabled, geometry) {
            if (enabled) detectDragGestures(
                onDragStart = { dragging = true; drag = Offset.Zero },
                onDrag = { change, amount -> change.consume(); drag += amount },
                onDragCancel = {
                    val release = motion.value + drag
                    scope.launch {
                        motion.snapTo(release)
                        drag = Offset.Zero; dragging = false; settleRevision++
                    }
                },
                onDragEnd = {
                    val release = motion.value + drag
                    val deliberate = drag.getDistance() >= minimumDragDistance
                    val correctBag = deliberate && geometry.bagTouchBounds(item.color).contains(release)
                    scope.launch {
                        // Keep the dragged token visible until its animation starts at this exact position.
                        motion.snapTo(release)
                        drag = Offset.Zero; dragging = false
                        if (correctBag) currentReturn(item.color)
                        else if (deliberate && geometry.dropBounds.contains(release)) currentMove(release)
                        settleRevision++
                    }
                })
        }.clickable(enabled = enabled, onClickLabel = "Toggle star light") { if (!dragging) currentToggle() }) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * .34f
            countingStar(center, radius, item.color.tint(), dimFraction = crossed)

            if (touchPulse.value > 0) {
                drawCircle(HintGold.copy(alpha = touchPulse.value * .75f), radius * (1.1f + 1 - touchPulse.value),
                    center, style = androidx.compose.ui.graphics.drawscope.Stroke(radius * .08f))
                drawCircle(Color.White.copy(alpha = touchPulse.value), radius * .22f,
                    center + Offset(radius * .65f, radius * .65f))
            }
        }
    }
}
