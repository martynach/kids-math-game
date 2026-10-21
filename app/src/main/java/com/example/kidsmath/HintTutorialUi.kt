package com.example.kidsmath

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.sin
import kotlinx.coroutines.delay

@Stable
internal class HintTutorialUi {
    private val timers = HintTutorialStage.entries.associateWith { HintTutorialTimer() }
    var progress by mutableStateOf<Float?>(null); private set
    var touching by mutableStateOf(false); private set
    var activity by mutableIntStateOf(0); private set
    fun cancel() { timers.values.forEach { it.interrupt() }; progress = null }
    fun interact(pressed: Boolean = false) {
        cancel(); touching = pressed; activity++
    }
    fun frame(stage: HintTutorialStage, now: Long) { progress = timers.getValue(stage).frame(now) }
    fun hasCycles(stage: HintTutorialStage) = timers.getValue(stage).cycles < HINT_TUTORIAL_CYCLES
}

@Composable
internal fun rememberHintTutorial(stage: HintTutorialStage?, enabled: Boolean = true): HintTutorialUi {
    val tutorial = remember { HintTutorialUi() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (!resumed) tutorial.interact()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); tutorial.interact() }
    }
    LaunchedEffect(stage, enabled, resumed, tutorial.activity, tutorial.touching) {
        tutorial.cancel()
        if (stage == null || !enabled || !resumed || tutorial.touching) {
            return@LaunchedEffect
        }
        withFrameMillis { tutorial.frame(stage, it) }
        while (tutorial.hasCycles(stage)) {
            delay(HINT_IDLE_TIMEOUT_MS)
            do { withFrameMillis { tutorial.frame(stage, it) } } while (tutorial.progress != null)
        }
    }
    return tutorial
}

/** Observe the initial pass without consuming events or competing with child gestures. */
internal fun Modifier.hintActivity(tutorial: HintTutorialUi): Modifier = pointerInput(tutorial) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            tutorial.interact(event.changes.any { it.pressed })
        }
    }
}

/** A small space-glove finger. Canvas has no input handler and performs no action. */
@Composable
internal fun TutorialFinger(progress: Float?, modifier: Modifier, target: (Size, Float) -> Offset) {
    if (progress == null) return
    Canvas(modifier.testTag("hint-tutorial-finger")) {
        val wave = sin(progress * Math.PI).toFloat()
        val tip = target(size, wave)
        val unit = 24.dp.toPx()
        val alpha = (minOf(progress * 8, (1 - progress) * 8)).coerceIn(0f, 1f)
        if (progress > .35f && progress < .7f)
            drawCircle(HintGold.copy(alpha = alpha * .5f), unit * (.25f + wave * .25f), tip,
                style = Stroke(1.5.dp.toPx()))
        translate(tip.x, tip.y) {
            val glove = Path().apply {
                moveTo(0f, 0f)
                cubicTo(-unit * .2f, -unit * .08f, -unit * .25f, unit * .12f, -unit * .23f, unit * .32f)
                lineTo(-unit * .23f, unit * .67f)
                lineTo(-unit * .42f, unit * .52f)
                cubicTo(-unit * .64f, unit * .4f, -unit * .68f, unit * .65f, -unit * .51f, unit * .84f)
                lineTo(-unit * .17f, unit * 1.23f)
                lineTo(unit * .45f, unit * 1.23f)
                cubicTo(unit * .7f, unit * .9f, unit * .6f, unit * .4f, unit * .29f, unit * .42f)
                lineTo(unit * .13f, unit * .46f)
                lineTo(unit * .13f, unit * .14f)
                quadraticTo(unit * .13f, 0f, 0f, 0f)
                close()
            }
            drawPath(glove, Color(0xFFEAF5FF).copy(alpha = alpha))
            drawPath(glove, Color(0xFF739DCF).copy(alpha = alpha), style = Stroke(1.5.dp.toPx()))
            drawLine(HintGold.copy(alpha = alpha), Offset(-unit * .14f, unit * 1.13f),
                Offset(unit * .43f, unit * 1.13f), 3.dp.toPx())
        }
    }
}
