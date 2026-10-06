package com.example.kidsmath

import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val Navy = Color(0xFF071745)
private val Blue = Color(0xFF009CFA)
private val Green = Color(0xFF12B832)
private val Gold = Color(0xFFFFD34B)
private val Pale = Color(0xFFD4EEFF)
private val RoundedFont = FontFamily(androidx.compose.ui.text.font.Typeface(
    android.graphics.Typeface.create("sans-serif-rounded", android.graphics.Typeface.NORMAL)))
private val RoundedBold = FontFamily(androidx.compose.ui.text.font.Typeface(
    android.graphics.Typeface.create("sans-serif-rounded", android.graphics.Typeface.BOLD)))
private val TitleStyle = TextStyle(fontFamily = RoundedBold, fontWeight = FontWeight.Black,
    shadow = Shadow(Color(0xFF020B2B), Offset(0f, 3f), 3f))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Blue, background = Navy)) {
                CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = RoundedFont)) {
                    KidsMathApp(onExit = { finishAndRemoveTask() })
                }
            }
        }
    }
}

class AppState(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("settings", 0)
    var settings by mutableStateOf(readSettings()); private set
    var screen by mutableStateOf("menu")
    var game by mutableStateOf<Game?>(null); private set
    var revision by mutableIntStateOf(0); private set
    private fun readSettings(): Settings {
        val min = prefs.getInt("min", 0)
        val max = prefs.getInt("max", 10)
        if (min !in 0..100 || max !in min..100) return Settings()
        return Settings(prefs.getString("name", "") ?: "",
            Operations.entries.firstOrNull { it.name == prefs.getString("operations", "BOTH") }
                ?: Operations.BOTH, min, max)
    }
    fun save(value: Settings) {
        prefs.edit().putString("name", value.name).putString("operations", value.operations.name)
            .putInt("min", value.min).putInt("max", value.max).apply()
        settings = value; screen = "menu"
    }
    fun start() { game = Game(settings, SystemClock.elapsedRealtime()); screen = "game"; revision++ }
    fun update(action: (Game) -> Unit) { game?.let(action); revision++ }
}

@Composable
private fun KidsMathApp(onExit: () -> Unit, state: AppState = viewModel()) {
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    BackHandler(state.screen != "menu") {
        if (state.screen == "settings") state.screen = "menu" else confirmLeave = true
    }
    Box(Modifier.fillMaxSize().background(Navy)) {
        SpaceBackground()
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            when (state.screen) {
                "settings" -> SettingsScreen(state.settings, state::save) { state.screen = "menu" }
                "game" -> GameScreen(state) { state.screen = "menu" }
                else -> MainMenu(state.settings, state::start, { state.screen = "settings" }, onExit)
            }
        }
    }
    if (confirmLeave) AlertDialog(onDismissRequest = { confirmLeave = false },
        title = { Text("Leave this space adventure?") },
        text = { Text("A new game will start from round 1.") },
        confirmButton = { TextButton(onClick = { confirmLeave = false; state.screen = "menu" }) { Text("Main Menu") } },
        dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Keep Playing") } })
}

@Composable
private fun MainMenu(settings: Settings, start: () -> Unit, configure: () -> Unit, exit: () -> Unit) {
    val floating = rememberInfiniteTransition(label = "Astronaut floating")
    val bob by floating.animateFloat(-1f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing),
        RepeatMode.Reverse), label = "Gentle floating")
    Row(Modifier.widthIn(max = 950.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.weight(0.9f).fillMaxHeight()) {
            val hero = Offset(size.width * .55f, size.height * .56f + bob * 6.dp.toPx())
            val width = size.minDimension * .77f
            star(Offset(size.width * .20f, size.height * .29f), 14.dp.toPx(), Gold)
            star(Offset(size.width * .81f, size.height * .78f), 10.dp.toPx(), Gold)
            planet(Offset(size.width * .30f, size.height * .96f), size.minDimension * .22f, Color(0xFF9561E7))
            rotate(-18f + bob * 2f, hero) {
                astronaut(hero + Offset(-width * .08f, -width * .23f), width * .67f)
                rocket(hero + Offset(width * .02f, width * .10f), width, 1f + bob * .15f)
            }
        }
        Column(Modifier.weight(1.2f).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Welcome to", color = Color.White, fontSize = 24.sp, style = TitleStyle)
            Text("Kids Math Game${if (settings.name.isBlank()) "!" else ","}", color = Gold,
                fontSize = 36.sp, style = TitleStyle, maxLines = 1)
            if (settings.name.isNotBlank()) Text("${settings.name}!", color = Color(0xFFFF8ED4), fontSize = 26.sp,
                style = TitleStyle, maxLines = 1)
            SpaceButton("▶  New Game", Green, Modifier.width(250.dp), start)
            SpaceButton("⚙  Settings", Blue, Modifier.width(250.dp), configure)
            SpaceButton("Exit", Color(0xFFEE4058), Modifier.width(250.dp), exit)
        }
    }
}

@Composable
private fun SpaceButton(label: String, color: Color, modifier: Modifier = Modifier,
                        onClick: () -> Unit) {
    PolishedButton(label, color, modifier.heightIn(min = 48.dp), onClick = onClick)
}

@Composable
private fun PolishedButton(label: String, color: Color, modifier: Modifier = Modifier,
                           enabled: Boolean = true, description: String? = null,
                           keypad: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) .96f else 1f, tween(100), label = "Button press")
    val shape = RoundedCornerShape(if (keypad) 12.dp else 17.dp)
    Button(onClick = onClick, enabled = enabled, interactionSource = interaction,
        modifier = modifier.graphicsLayer { scaleX = pressScale; scaleY = pressScale; translationY = if (pressed) 2f else 0f }
            .shadow(if (pressed) 2.dp else 6.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape).background(Brush.verticalGradient(listOf(lerp(color, Color.White, .28f),
                color, lerp(color, Color.Black, .25f))))
            .border(1.5.dp, Color.White.copy(alpha = if (enabled) .48f else .2f), shape)
            .drawWithContent {
                drawContent()
                if (!enabled) drawRect(Navy.copy(alpha = .36f))
            }.semantics { if (description != null) contentDescription = description },
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent,
            contentColor = Color.White, disabledContainerColor = Color.Transparent, disabledContentColor = Color.White),
        shape = shape, contentPadding = if (keypad) PaddingValues(0.dp) else PaddingValues(horizontal = 14.dp, vertical = 7.dp)) {
        Text(label, fontSize = if (keypad) 25.sp else 21.sp, style = TitleStyle.copy(
            shadow = Shadow(Color.Black.copy(alpha = .3f), Offset(0f, 2f), 2f)), maxLines = 1)
    }
}

@Composable
private fun SettingsScreen(initial: Settings, save: (Settings) -> Unit, back: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var operations by rememberSaveable { mutableStateOf(initial.operations) }
    var min by rememberSaveable { mutableStateOf(initial.min.toString()) }
    var max by rememberSaveable { mutableStateOf(initial.max.toString()) }
    val minNumber = min.toIntOrNull()
    val maxNumber = max.toIntOrNull()
    val valid = minNumber != null && maxNumber != null && minNumber in 0..100 && maxNumber in minNumber..100
    Column(Modifier.widthIn(max = 900.dp).fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SpaceButton("‹ Back", Blue, onClick = { keyboard?.hide(); back() })
            Text("Settings", Modifier.weight(1f), color = Color.White, fontSize = 32.sp,
                style = TitleStyle, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        Column(Modifier.padding(top = 8.dp).weight(1f).fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFF1FAFF), Pale)))
            .border(2.dp, Color.White.copy(alpha = .9f), RoundedCornerShape(24.dp))
            .verticalScroll(rememberScrollState()).padding(16.dp)) {
            val fields = OutlinedTextFieldDefaults.colors(focusedTextColor = Navy, unfocusedTextColor = Navy,
                focusedLabelColor = Navy, unfocusedLabelColor = Navy, cursorColor = Navy,
                focusedBorderColor = Blue, unfocusedBorderColor = Color(0xFF7A9BC4),
                focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
            OutlinedTextField(name, { name = it }, label = { Text("Player name (optional)") },
                singleLine = true, colors = fields, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Type of operations", color = Navy, fontFamily = RoundedBold, fontWeight = FontWeight.Bold)
                    Operations.entries.forEach { option ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(operations == option, { operations = option },
                                colors = RadioButtonDefaults.colors(selectedColor = Blue, unselectedColor = Navy))
                            Text(when (option) { Operations.ADDITION -> "Addition"; Operations.SUBTRACTION -> "Subtraction"
                                Operations.BOTH -> "Addition and Subtraction" }, color = Navy)
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Result range (0–100)", color = Navy, fontFamily = RoundedBold, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(min, { min = it }, label = { Text("MIN") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = fields,
                            modifier = Modifier.weight(1f), isError = !valid)
                        OutlinedTextField(max, { max = it }, label = { Text("MAX") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = fields,
                            modifier = Modifier.weight(1f), isError = !valid)
                    }
                    if (!valid) Text("Enter whole numbers from 0 to 100. MIN must be ≤ MAX.", color = Color(0xFFAA1838), fontSize = 13.sp)
                    PolishedButton("Save Settings", Green, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = valid, onClick = {
                        if (valid) { keyboard?.hide(); save(Settings(name.trim(), operations, minNumber!!, maxNumber!!)) }
                    })
                }
            }
        }
    }
}

@Composable
private fun GameScreen(state: AppState, menu: () -> Unit) {
    val game = state.game ?: return
    // The revision makes the small, pure Kotlin game model observable without Compose dependencies.
    @Suppress("UNUSED_VARIABLE") val revision = state.revision
    val lives = game.lives
    val answerAnimation = remember { Animatable(0f) }
    LaunchedEffect(game.phase, game.question) {
        answerAnimation.snapTo(0f)
        if (game.phase == Phase.FEEDBACK) answerAnimation.animateTo(1f, tween(650))
    }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(game) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            state.update { it.tick(now) }
            delay(50)
        }
    }
    when (game.phase) {
        Phase.ROUND_COMPLETE, Phase.VICTORY, Phase.GAME_OVER -> EndScreen(game, menu,
            { state.update { it.nextRound(SystemClock.elapsedRealtime()) } }, state::start)
        else -> Column(Modifier.widthIn(max = 1050.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).semantics { contentDescription = "Lives remaining: $lives" }) {
                    repeat(lives) { Text("♥", color = Color(0xFFFF5268),
                        fontSize = 32.sp, style = TitleStyle, modifier = Modifier.padding(end = 6.dp)) }
                }
                Text("Round ${game.round}", Modifier.shadow(4.dp, RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF244E9B), Color(0xFF13316A))), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF7399D9).copy(alpha = .5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 22.dp, vertical = 3.dp), color = Color.White, style = TitleStyle, fontSize = 21.sp)
                Text("★  ${game.correct} / 5", Modifier.weight(1f), color = Gold, fontWeight = FontWeight.Bold,
                    fontSize = 23.sp, style = TitleStyle, textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
            Box(Modifier.padding(top = 5.dp).widthIn(max = 420.dp).fillMaxWidth(.62f)
                .graphicsLayer {
                    val value = answerAnimation.value
                    if (game.phase == Phase.FEEDBACK && game.feedback != Feedback.CORRECT)
                        translationX = sin(value * Math.PI * 6).toFloat() * 7f * (1f - value)
                    else { scaleX = 1f + sin(value * Math.PI).toFloat() * .025f; scaleY = scaleX }
                }.shadow(8.dp, RoundedCornerShape(19.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFF4FCFF),
                    if (game.phase == Phase.FEEDBACK && game.feedback != Feedback.CORRECT) Color(0xFFFFDCE4) else Pale)), RoundedCornerShape(19.dp))
                .border(2.dp, Color.White.copy(alpha = .85f), RoundedCornerShape(19.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                Text("${game.question.expression} = ${game.input.ifEmpty { "?" }}", color = when {
                    game.phase != Phase.FEEDBACK -> Navy
                    game.feedback == Feedback.CORRECT -> Color(0xFF087C27)
                    else -> Color(0xFFBE2944)
                }, fontSize = 36.sp, fontFamily = RoundedBold, fontWeight = FontWeight.Black)
            }
            if (game.round > 1) {
                val remaining = (game.deadline - now).coerceAtLeast(0)
                Row(Modifier.widthIn(max = 400.dp).fillMaxWidth(.62f).padding(top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LinearProgressIndicator(progress = { (remaining.toFloat() / GameRules.timeLimitMs(game.round)).coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f).height(12.dp).border(1.dp, Color(0xFFB9DBFF), RoundedCornerShape(8.dp)), color = if (remaining < 10_000) Color(0xFFFF6688) else Gold,
                        trackColor = Color(0xFF365688))
                    Text("${(remaining + 999) / 1000}s", color = Color.White, fontFamily = RoundedBold, fontWeight = FontWeight.Bold)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Journey(game.correct, game.round)
                if (game.phase == Phase.FEEDBACK) FeedbackOverlay(game.feedback!!, when (game.feedback) {
                    Feedback.CORRECT -> game.settings.greeting("Great job")
                    Feedback.TIMEOUT -> "Time's up! Keep going!"
                    else -> "Keep going! You can do it!"
                })
            }
            Keypad(game.phase, game.input, state)
        }
    }
}

@Composable
private fun Keypad(phase: Phase, input: String, state: AppState) {
    Row(Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(16.dp))
        .background(Brush.verticalGradient(listOf(Color(0xEE17316B), Color(0xEE050F31))), RoundedCornerShape(16.dp))
        .border(1.dp, Color(0xFF5687CC).copy(alpha = .45f), RoundedCornerShape(16.dp)).padding(6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Key("⌫", "Backspace", Color(0xFFFF941D), Modifier.weight(1.15f), phase == Phase.PLAYING) {
            state.update { it.tick(SystemClock.elapsedRealtime()); it.backspace() }
        }
        Spacer(Modifier.width(12.dp))
        repeat(10) { digit ->
            Key(digit.toString(), digit.toString(), Blue, Modifier.weight(1f), phase == Phase.PLAYING) {
                state.update { it.tick(SystemClock.elapsedRealtime()); it.digit(digit) }
            }
            if (digit < 9) Spacer(Modifier.width(3.dp))
        }
        Spacer(Modifier.width(12.dp))
        Key("✓", "Confirm answer", Green, Modifier.weight(1.3f), phase == Phase.PLAYING && input.isNotEmpty()) {
            state.update { it.confirm(SystemClock.elapsedRealtime()) }
        }
    }
}

@Composable
private fun Key(label: String, description: String, color: Color, modifier: Modifier, enabled: Boolean, action: () -> Unit) {
    PolishedButton(label, color, modifier.height(52.dp), enabled, description, keypad = true, onClick = action)
}

@Composable
private fun Journey(correct: Int, round: Int) {
    val progress by animateFloatAsState(correct / 5f, tween(650, easing = FastOutSlowInEasing), label = "Rocket progress")
    val thruster = rememberInfiniteTransition(label = "Rocket thruster")
    val flame by thruster.animateFloat(.85f, 1.15f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "Flame flicker")
    Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Rocket journey: $correct of 5 steps" }) {
        val start = size.width * .12f
        val end = size.width * .85f
        val y = size.height * .64f
        fun route(t: Float) = Offset(start + (end - start) * t,
            y + sin(t * Math.PI * 3).toFloat() * minOf(size.height * .13f, 16.dp.toPx()))
        for (i in 0..47) {
            drawLine(Color(0xFF9FDBFF).copy(alpha = .7f), route(i / 48f), route((i + .45f) / 48f), 2.dp.toPx())
        }
        for (i in 1..5) star(route(i / 5f), size.height.coerceAtMost(150.dp.toPx()) * .11f,
            if (i <= correct) Color(0xFFFFF2A1) else Gold)
        planet(Offset(end + size.width * .04f, y), minOf(size.height * .34f, size.width * .073f),
            when (round) { 1 -> Color(0xFFFFAE53); 2 -> Color(0xFF35BBD9); else -> Color(0xFFFF7288) })
        val position = route(progress)
        rotate(cos(progress * Math.PI * 3).toFloat() * 7f, position) {
            rocket(position, minOf(size.height * .57f, 112.dp.toPx()), flame)
        }
    }
}

@Composable
private fun FeedbackOverlay(feedback: Feedback, message: String) {
    val happy = feedback == Feedback.CORRECT
    val animation = remember(feedback) { Animatable(0f) }
    LaunchedEffect(feedback) {
        animation.animateTo(1f, tween(GameRules.feedbackDurationMs(feedback).toInt(), easing = LinearEasing))
    }
    Box(Modifier.fillMaxSize()) {
        if (happy) Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Celebration confetti" }) {
            confetti(animation.value, 46)
            repeat(7) { index ->
                val angle = index * Math.PI * 2 / 7
                val radius = animation.value * size.height * .65f
                val center = Offset(size.width * .5f, size.height * .48f) +
                    Offset(cos(angle).toFloat(), sin(angle).toFloat()) * radius
                star(center, (1f - animation.value) * 10.dp.toPx(), Gold)
            }
        }
        Row(Modifier.align(Alignment.TopCenter).padding(top = 4.dp)
            .shadow(5.dp, RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0xF02B4385), Color(0xF00E2051))), RoundedCornerShape(18.dp))
            .border(1.dp, (if (happy) Gold else Color(0xFFFFA9B7)).copy(alpha = .7f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Canvas(Modifier.size(44.dp).semantics {
                contentDescription = if (happy) "Happy face" else "Sad face"
            }) {
                val center = Offset(size.width / 2, size.height / 2)
                val bounce = sin(animation.value * Math.PI).toFloat()
                val radius = size.minDimension * (.39f + .06f * bounce)
                val tilt = sin(animation.value * 3f * Math.PI).toFloat() * 10f * (1f - animation.value)
                rotate(tilt, center) {
                    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF2A1), Gold, Color(0xFFFFAE38)),
                        center - Offset(radius * .3f, radius * .4f), radius * 1.8f), radius, center)
                    drawCircle(Color(0xFFDD831D), radius, center, style = Stroke(1.dp.toPx()))
                    drawCircle(Navy, radius * .09f, center + Offset(-radius * .33f, -radius * .2f))
                    drawCircle(Navy, radius * .09f, center + Offset(radius * .33f, -radius * .2f))
                    drawArc(Navy, if (happy) 0f else 180f, 180f, false,
                        center + Offset(-radius * .45f, if (happy) -radius * .08f else radius * .23f),
                        Size(radius * .9f, radius * .6f), style = Stroke(2.dp.toPx()))
                }
            }
            Text(message, color = if (happy) Gold else Color.White, fontSize = 19.sp, style = TitleStyle)
        }
    }
}

@Composable
private fun EndScreen(game: Game, menu: () -> Unit, next: () -> Unit, again: () -> Unit) {
    val victory = game.phase == Phase.VICTORY
    val lost = game.phase == Phase.GAME_OVER
    val celebration = rememberInfiniteTransition(label = "Celebration")
    val cycle by celebration.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "Confetti shower")
    val bob by celebration.animateFloat(-1f, 1f, infiniteRepeatable(tween(1900), RepeatMode.Reverse), label = "Celebration floating")
    Box(Modifier.widthIn(max = 950.dp).fillMaxSize()) {
        if (!lost) Canvas(Modifier.fillMaxSize()) {
            confetti(cycle, if (victory) 95 else 42, seed = 34)
            if (victory) {
                val origin = Offset(size.width * .24f, size.height * .44f)
                repeat(12) { index ->
                    val angle = index * Math.PI / 6 + cycle * .3
                    val point = origin + Offset(cos(angle).toFloat() * size.height * .42f,
                        sin(angle).toFloat() * size.height * .42f)
                    star(point, (8f + 3f * sin(cycle * Math.PI * 2 + index).toFloat()).dp.toPx(), Gold)
                }
            }
        }
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.weight(1f).fillMaxHeight()) {
                val ground = Offset(size.width * .51f, size.height * .96f)
                val radius = size.minDimension * .38f
                rotate(bob * 3f, ground) {
                    planet(ground, radius, if (victory) Color(0xFFFFBE42) else Color(0xFF9967E5))
                }
                val hero = Offset(size.width * .52f, size.height * .50f + bob * 4.dp.toPx())
                astronaut(hero, size.minDimension * .68f, celebrating = !lost)
                val ship = Offset(size.width * .17f, size.height * .70f - bob * 5.dp.toPx())
                rotate(-27f + bob * 4f, ship) { rocket(ship, size.minDimension * .32f, 1f + bob * .1f) }
                if (!lost) star(Offset(size.width * .84f, size.height * .24f),
                    size.minDimension * (if (victory) .105f else .075f) * (1f + bob * .05f), Gold)
                if (victory) trophy(hero + Offset(size.minDimension * .22f, -size.minDimension * .1f), size.minDimension * .20f)
            }
            Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(game.settings.greeting(if (lost) "Keep exploring" else if (victory) "Amazing" else "Great job"),
                    modifier = Modifier.graphicsLayer { rotationZ = bob * .6f; scaleX = 1f + if (victory) bob * .018f else 0f; scaleY = scaleX },
                    color = Gold, style = TitleStyle, fontSize = if (victory) 42.sp else 36.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text(if (lost) "Let's try a new adventure!" else if (victory) "You completed the game!" else "Round ${game.round} Complete!",
                    color = Color.White, fontSize = 23.sp, style = TitleStyle,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                if (!lost) Canvas(Modifier.fillMaxWidth(.76f).height(if (victory) 54.dp else 46.dp)) {
                    val count = if (victory) 5 else 3
                    repeat(count) { index ->
                        val center = Offset(size.width * (index + .5f) / count,
                            size.height * .5f + sin(cycle * Math.PI * 2 + index).toFloat() * 3.dp.toPx())
                        star(center, size.height * .34f, Gold)
                    }
                }
                if (game.phase == Phase.ROUND_COMPLETE) {
                    SpaceButton("▶  Next Round", Green, onClick = next)
                    Text("Ready? Press Next Round!", color = Pale, fontSize = 13.sp)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SpaceButton("Main Menu", Blue, onClick = menu)
                        SpaceButton("Play Again", Green, onClick = again)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpaceBackground() {
    val twinkle = rememberInfiniteTransition(label = "Starfield")
    val shimmer by twinkle.animateFloat(0f, 1f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "Starlight")
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF030D35), Color(0xFF071E59), Color(0xFF050E31))))
        drawRect(Brush.radialGradient(listOf(Color(0xFF164EAE).copy(alpha = .42f), Color.Transparent),
            Offset(size.width * .34f, size.height * .48f), size.width * .5f))
        repeat(5) { index ->
            val center = Offset(size.width * (.15f + index * .17f), size.height * (.32f + sin(index * 1.7f) * .20f))
            val radius = size.height * .43f
            drawCircle(Brush.radialGradient(listOf((if (index % 2 == 0) Color(0xFF346ECD) else Color(0xFF8D43BF))
                .copy(alpha = .11f), Color.Transparent), center, radius), radius, center)
        }
        val random = Random(71)
        repeat(230) { index ->
            val point = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
            val pulse = .5f + .25f * sin(shimmer * Math.PI * 2 + index).toFloat()
            drawCircle((if (index % 4 == 0) Color(0xFF64C5FF) else Color(0xFFDEEDFF)).copy(alpha = pulse),
                (.5f + random.nextFloat() * 1.1f).dp.toPx(), point)
            if (index % 27 == 0) star(point, (5f + pulse * 4f).dp.toPx(), Gold)
            if (index % 17 == 0) {
                drawLine(Color(0xFF78CEFF).copy(alpha = pulse * .45f), point - Offset(4.dp.toPx(), 0f),
                    point + Offset(4.dp.toPx(), 0f), 1.dp.toPx())
                drawLine(Color(0xFF78CEFF).copy(alpha = pulse * .45f), point - Offset(0f, 4.dp.toPx()),
                    point + Offset(0f, 4.dp.toPx()), 1.dp.toPx())
            }
        }
        planet(Offset(size.width * .035f, size.height * .14f), size.height * .16f, Color(0xFFE6758D))
        planet(Offset(size.width * 1.01f, size.height * .80f), size.height * .27f, Color(0xFF29B7CF))
        planet(Offset(size.width * .84f, -size.height * .055f), size.height * .16f, Color(0xFF8265D7))
        planet(Offset(-size.width * .035f, size.height * .83f), size.height * .18f, Color(0xFFBC76BA))
        planet(Offset(size.width * .27f, size.height * .94f), size.height * .06f, Color(0xFF456FA8))
        if (shimmer < .22f) {
            val point = Offset(size.width * (.33f + shimmer * 1.2f), size.height * (.05f + shimmer * .5f))
            drawLine(Color(0xFF7ADCFF).copy(alpha = .28f), point - Offset(38.dp.toPx(), 8.dp.toPx()), point, 1.dp.toPx())
            drawCircle(Color(0xFFC0F0FF).copy(alpha = .6f), 1.2.dp.toPx(), point)
        }
    }
}
