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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.platform.LocalView
import android.os.Build
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.inputmethod.EditorInfoCompat
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

@OptIn(ExperimentalMaterial3Api::class)
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
                // The buttons already animate their press. Avoid native RippleDrawable,
                // which crashes the API 35 x86_64/16 KB emulator during navigation.
                CompositionLocalProvider(LocalRippleConfiguration provides null, LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = RoundedFont)) {
                    KidsMathApp(onExit = { finishAndRemoveTask() })
                }
            }
        }
    }
}

enum class Screen { MENU, SETTINGS, MAP, GAME }

class AppState(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("settings", 0)
    private val campaignStore = CampaignStore(application)
    var campaign by mutableStateOf(campaignStore.load()); private set
    var settings by mutableStateOf(readSettings()); private set
    var screen by mutableStateOf(Screen.MENU)
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
        settings = value; screen = Screen.MENU
    }
    fun openMap() { game = null; screen = Screen.MAP; revision++ }
    fun start(level: Int) {
        if (!campaign.canStart(level)) return
        game = Game(settings, CampaignConfig.levels[level - 1]); screen = Screen.GAME; revision++
    }
    fun retry() { game?.config?.level?.let(::start) }
    fun resetProgress() { campaign = CampaignProgress(); campaignStore.save(campaign) }
    fun leaveGame() { game = null; screen = Screen.MAP; revision++ }
    fun update(action: (Game) -> Unit) { game?.let {
            action(it)
            if (it.phase == Phase.LEVEL_COMPLETE) {
                val next = campaign.complete(it.config.level)
                if (next != campaign) { campaignStore.save(next); campaign = next }
            }
        }; revision++ }
}

@Composable
private fun KidsMathApp(onExit: () -> Unit, state: AppState = viewModel()) {
    @Suppress("UNUSED_VARIABLE") val revision = state.revision
    val confirmLeave = state.game?.pausedAt != null
    val requestLeave = {
        if (state.game?.phase == Phase.LEVEL_COMPLETE || state.game?.phase == Phase.GAME_OVER) state.leaveGame()
        else state.update { it.pause(SystemClock.elapsedRealtime()) }
    }
    val cancelLeave = { state.update { it.resume(SystemClock.elapsedRealtime()) } }
    BackHandler(state.screen != Screen.MENU) {
        if (state.screen == Screen.SETTINGS || state.screen == Screen.MAP) state.screen = Screen.MENU else requestLeave()
    }
    Box(Modifier.fillMaxSize().background(Navy)) {
        SpaceBackground(vibrant = state.screen == Screen.MAP)
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            when (state.screen) {
                Screen.SETTINGS -> SettingsScreen(state.settings, state::save, state::resetProgress) { state.screen = Screen.MENU }
                Screen.MAP -> SpaceMap(state)
                Screen.GAME -> GameScreen(state, state::leaveGame, requestLeave)
                Screen.MENU -> MainMenu(state.settings, state::openMap, { state.screen = Screen.SETTINGS }, onExit)
            }
        }
    }
    if (confirmLeave) AlertDialog(onDismissRequest = cancelLeave,
        title = { Text("Return to Space Map?") },
        text = { Text("Are you sure you want to stop playing? Energy in this attempt will be lost. Unlocked levels stay available.") },
        confirmButton = { TextButton(onClick = state::leaveGame) { Text("Space Map") } },
        dismissButton = { TextButton(onClick = cancelLeave) { Text("Keep Playing") } })
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
            SpaceButton("Start Game", Green, Modifier.width(250.dp), start)
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
@OptIn(ExperimentalComposeUiApi::class)
private fun SettingsScreen(initial: Settings, save: (Settings) -> Unit, reset: () -> Unit, back: () -> Unit) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false },
        title = { Text("Reset progress?") },
        text = { Text("All completed levels and your current progress will be lost. You'll start your space adventure again from Level 1.") },
        confirmButton = { TextButton(onClick = { reset(); confirmReset = false }) { Text("Reset", color = Color(0xFFFF5268)) } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } })
    val keyboard = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    DisposableEffect(view) {
        val wasEnabled = if (Build.VERSION.SDK_INT >= 33) view.isAutoHandwritingEnabled else false
        if (Build.VERSION.SDK_INT >= 33) view.setAutoHandwritingEnabled(false)
        onDispose { if (Build.VERSION.SDK_INT >= 33) view.setAutoHandwritingEnabled(wasEnabled) }
    }
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var operations by rememberSaveable { mutableStateOf(initial.operations) }
    var min by rememberSaveable { mutableStateOf(initial.min.toString()) }
    var max by rememberSaveable { mutableStateOf(initial.max.toString()) }
    val minNumber = min.toIntOrNull()
    val maxNumber = max.toIntOrNull()
    val valid = minNumber != null && maxNumber != null && minNumber in 0..100 && maxNumber in minNumber..100
    InterceptPlatformTextInput(interceptor = { request, nextHandler ->
        nextHandler.startInputMethod { attributes ->
            request.createInputConnection(attributes).also {
                attributes.imeOptions = attributes.imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
                EditorInfoCompat.setStylusHandwritingEnabled(attributes, false)
            }
        }
    }) {
        Column(Modifier.widthIn(max = 900.dp).fillMaxSize().imePadding()) {
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true, colors = fields, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(1f)) {
                        PolishedButton("Reset Progress", Color(0xFFBE2944), onClick = { confirmReset = true })
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
}

@Composable
private fun GameScreen(state: AppState, menu: () -> Unit, requestLeave: () -> Unit) {
    val game = state.game ?: return
    // The revision makes the small, pure Kotlin game model observable without Compose dependencies.
    @Suppress("UNUSED_VARIABLE") val revision = state.revision
    val lives = game.lives
    val answerAnimation = remember(game) { Animatable(0f) }
    LaunchedEffect(game.phase, game.question) {
        answerAnimation.snapTo(0f)
        if (game.phase == Phase.FEEDBACK) answerAnimation.animateTo(1f, tween(GameRules.ANSWER_FEEDBACK_MS.toInt() - 100, easing = LinearEasing))
    }
    LaunchedEffect(game) {
        while (true) {
            state.update { it.tick(SystemClock.elapsedRealtime()) }
            delay(50)
        }
    }
    when (game.phase) {
        Phase.LEVEL_COMPLETE -> CompletionScreen(game, menu)
        Phase.GAME_OVER -> GameOverScreen(game, menu, state::retry)
        else -> Column(Modifier.widthIn(max = 1050.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).semantics { contentDescription = "Lives remaining: $lives" }) {
                    repeat(lives) { Text("♥", color = Color(0xFFFF5268),
                        fontSize = 32.sp, style = TitleStyle, modifier = Modifier.padding(end = 6.dp)) }
                }
                Text("Level ${game.config.level}", Modifier.shadow(4.dp, RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF244E9B), Color(0xFF13316A))), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF7399D9).copy(alpha = .5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 22.dp, vertical = 3.dp), color = Color.White, style = TitleStyle, fontSize = 21.sp)
                Text("★  ${game.correct} / ${game.config.requiredCorrectAnswers}", Modifier.weight(1f), color = Gold, fontWeight = FontWeight.Bold,
                    fontSize = 23.sp, style = TitleStyle, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Spacer(Modifier.width(12.dp))
                SpaceButton("⌂ Map", Blue, onClick = requestLeave)
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
            Box(Modifier.weight(1f).fillMaxWidth()) {
                EnergyScene(game, answerAnimation.value)
                if (game.phase == Phase.FEEDBACK) FeedbackOverlay(game.feedback!!, when (game.feedback) {
                    Feedback.CORRECT -> game.settings.greeting("Great job")
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
private fun FeedbackOverlay(feedback: Feedback, message: String) {
    val happy = feedback == Feedback.CORRECT
    val animation = remember(feedback) { Animatable(0f) }
    LaunchedEffect(feedback) {
        animation.animateTo(1f, tween(GameRules.feedbackDurationMs(feedback).toInt(), easing = LinearEasing))
    }
    Box(Modifier.fillMaxSize()) {
        if (happy) Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Celebration confetti" }) {
            val beats = animation.value * 3f
            confetti(beats % 1f, 55, seed = 23 + beats.toInt())
            repeat(18) { index ->
                val random = Random(402 + index)
                val point = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height * .8f)
                val flash = (.5f + .5f * sin(animation.value * 18f + index)).coerceIn(0f, 1f)
                star(point, (3f + flash * 7f).dp.toPx(), if (index % 2 == 0) Gold else Color(0xFF91FFFF))
            }
            repeat(7) { index ->
                val angle = index * Math.PI * 2 / 7
                val radius = animation.value * size.height * .65f
                val center = Offset(size.width * .82f, size.height * .18f) +
                    Offset(cos(angle).toFloat(), sin(angle).toFloat()) * radius
                star(center, (1f - animation.value) * 10.dp.toPx(), Gold)
            }
        }
        Row(Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 8.dp)
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
private fun GameOverScreen(game: Game, map: () -> Unit, retry: () -> Unit) {
    Row(Modifier.widthIn(max = 950.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.weight(1f).fillMaxHeight()) {
            planet(Offset(size.width * .5f, size.height * 1.08f), size.minDimension * .45f, Color(0xFF9967E5))
            astronaut(Offset(size.width * .5f, size.height * .48f), size.minDimension * .65f)
        }
        Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Game Over", color = Gold, fontSize = 38.sp, style = TitleStyle)
            Text(game.settings.greeting("Keep exploring"), color = Color.White, fontSize = 25.sp)
            Text("Level ${game.config.level} · Let's try again!", color = Pale, fontSize = 20.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SpaceButton("Back to Map", Blue, onClick = map)
                SpaceButton("Retry", Green, onClick = retry)
            }
        }
    }
}

@Composable
private fun SpaceBackground(vibrant: Boolean = false) {
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
        if (vibrant) {
            drawRect(Brush.radialGradient(listOf(Color(0xFF2459D4).copy(alpha = .45f), Color.Transparent),
                Offset(size.width * .35f, size.height * .45f), size.width * .65f))
            drawRect(Brush.radialGradient(listOf(Color(0xFFCF39E9).copy(alpha = .18f), Color.Transparent),
                Offset(size.width * .8f, size.height * .3f), size.width * .4f))
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

// Stable irregular coordinates keep the route and its clickable planets aligned.
private fun mapLevelY(index: Int) = .34f + Random(1307 + index * 97).nextFloat() * .36f
private val MapPlanetColors = listOf(Color(0xFFFF6C27), Color(0xFF00F5C8), Color(0xFFFF42C4),
    Color(0xFFAD65FF), Color(0xFFFFD600), Color(0xFF00CFFF), Color(0xFF8CFF36))

@Composable
private fun SpaceMap(state: AppState) {
    val orbit = rememberInfiniteTransition(label = "Map orbit")
    val motion by orbit.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "Current planet aura")
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SpaceButton("‹ Menu", Blue, onClick = { state.screen = Screen.MENU })
            Text("Space Map", Modifier.weight(1f), color = Gold, style = TitleStyle, fontSize = 30.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("Swipe to explore →", color = Pale)
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val mapHeight = maxHeight
            val spacing = maxWidth / 6
            val scroll = rememberScrollState()
            val density = LocalDensity.current
            LaunchedEffect(state.campaign.highestUnlockedLevel, maxWidth) {
                scroll.scrollTo(with(density) { (spacing * (state.campaign.highestUnlockedLevel - 3).coerceAtLeast(0)).roundToPx() })
            }
            Box(Modifier.fillMaxSize().horizontalScroll(scroll).semantics { contentDescription = "Campaign route" }) {
                Box(Modifier.width(spacing * CampaignConfig.levels.size).fillMaxHeight()) {
                    Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Current rocket at Level ${state.campaign.highestUnlockedLevel}" }) {
                        val step = size.width / CampaignConfig.levels.size
                        fun point(t: Float): Offset {
                            val i = t.toInt()
                            val fraction = t - i
                            val smooth = fraction * fraction * (3f - 2f * fraction)
                            return Offset(step * (t + .5f), size.height * (mapLevelY(i) + (mapLevelY(i + 1) - mapLevelY(i)) * smooth))
                        }
                        repeat(CampaignConfig.levels.size * 16 - 16) { i ->
                            drawLine(Pale.copy(alpha = .45f), point(i / 16f), point((i + .55f) / 16f), 2.dp.toPx())
                        }
                        // Sparse scenery has its own positions, sizes and colours, independent of level nodes.
                        val scenery = Random(918)
                        repeat(CampaignConfig.levels.size / 2) { i ->
                            val p = Offset(scenery.nextFloat() * size.width,
                                size.height * (if (i % 3 == 0) .87f else .09f + scenery.nextFloat() * .10f))
                            val radius = (8f + scenery.nextFloat() * 17f).dp.toPx()
                            val color = MapPlanetColors[(i * 3 + 2) % MapPlanetColors.size]
                            planet(p + Offset(0f, sin(motion * 6.28f + i) * 3.dp.toPx()), radius, color)
                            if (i % 3 == 0) drawOval(color.copy(alpha = .5f), p - Offset(radius * 1.6f, radius * .35f),
                                Size(radius * 3.2f, radius * .7f), style = Stroke(2.dp.toPx()))
                        }
                        repeat(CampaignConfig.levels.size) { i ->
                            val p = point(i.toFloat())
                            if (i + 1 == state.campaign.highestUnlockedLevel) {
                                val angle = motion * Math.PI.toFloat() * 2f
                                val ship = p + Offset(cos(angle) * 67.dp.toPx(), sin(angle) * 61.dp.toPx())
                                rotate(motion * 360f + 90f, ship) {
                                    rocket(ship, 42.dp.toPx(), .6f + .15f * sin(motion * 70f), enginesOn = true)
                                }
                            }
                        }
                    }
                    CampaignConfig.levels.forEachIndexed { i, level ->
                        val unlocked = state.campaign.canStart(level.level)
                        val completed = state.campaign.isCompleted(level.level)
                        val current = level.level == state.campaign.highestUnlockedLevel
                        val nodeSize = if (current) 92.dp else 76.dp
                        Column(Modifier.offset(x = spacing * i, y = mapHeight * mapLevelY(i) - nodeSize / 2)
                            .width(spacing), horizontalAlignment = Alignment.CenterHorizontally) {
                            val color = if (unlocked) MapPlanetColors[(i * 3) % MapPlanetColors.size] else Color(0xFF677493)
                            Box(Modifier.size(nodeSize).semantics {
                                contentDescription = "Level ${level.level}, ${if (completed) "completed" else if (unlocked) "unlocked" else "locked"}"
                            }.clickable(enabled = unlocked) { state.start(level.level) }, contentAlignment = Alignment.Center) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val center = Offset(size.width / 2, size.height / 2)
                                    if (current) {
                                        val pulse = .65f + .25f * sin(motion * 6.28f)
                                        drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = pulse), Color(0xFFEE6AFF).copy(alpha = .25f), Color.Transparent), center, size.width * .65f), size.width * .65f, center)
                                        drawCircle(Gold, size.minDimension * .47f, center, style = Stroke(3.dp.toPx()))
                                        repeat(5) { spark ->
                                            val angle = motion * 6.28f + spark * 6.28f / 5
                                            star(center + Offset(cos(angle), sin(angle)) * size.width * .49f, 4.dp.toPx(), if (spark % 2 == 0) Gold else Color(0xFF88FFFF))
                                        }
                                    }
                                    planet(center, size.minDimension * .40f, color, vivid = unlocked)
                                    if (current) drawCircle(Color.White.copy(alpha = .25f * (.5f + .5f * sin(motion * 18.85f))),
                                        size.minDimension * .40f, center)
                                }
                                Text(if (unlocked) level.level.toString() else "🔒", color = Color.White,
                                    fontSize = 25.sp, style = TitleStyle)
                                if (completed) Text("★", Modifier.align(Alignment.BottomCenter), color = Gold, fontSize = 22.sp)
                            }
                            Text("Level ${level.level}", color = if (unlocked) Color.White else Pale.copy(alpha = .6f), fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnergyScene(game: Game, collection: Float, launch: Float = 0f) {
    val collecting = game.phase == Phase.FEEDBACK && game.feedback == Feedback.CORRECT
    val previous = game.correct - if (collecting) 1 else 0
    val transfer = ((collection - .80f) / .12f).coerceIn(0f, 1f)
    val energy = (previous + if (collecting) transfer else 0f) / game.config.requiredCorrectAnswers
    val launching = game.phase == Phase.LEVEL_COMPLETE
    Canvas(Modifier.fillMaxSize().semantics {
        contentDescription = "Rocket energy: ${game.correct} of ${game.config.requiredCorrectAnswers}"
        stateDescription = if (launching) "Full tank, launching" else if (collecting) "Collecting crystal" else "Landed rocket"
    }) {
        val ground = Offset(size.width * .50f, size.height * 1.38f)
        planet(ground, size.height * .76f, Color(0xFF8466CC))
        val width = minOf(size.height * .70f, 130.dp.toPx())
        val joy = if (collecting) sin(collection * Math.PI.toFloat() * 12f) * (1f - collection) else 0f
        val base = Offset(size.width * .5f, size.height * .43f)
        val fuelTransfer = (launch / .25f).coerceIn(0f, 1f)
        val rise = ((launch - .28f) / .17f).coerceIn(0f, 1f)
        val loop = ((launch - .45f) / .35f).coerceIn(0f, 1f)
        val departure = ((launch - .80f) / .20f).coerceIn(0f, 1f)
        val angle = loop * Math.PI.toFloat() * 2f
        val ship = base + Offset(joy * 3.dp.toPx(), -kotlin.math.abs(joy) * 2.dp.toPx()) + Offset(
            (1f - cos(angle)) * size.width * .18f + departure * size.width * .45f +
                if (launch in .25f.. .28f) sin(launch * 600f) * 3.dp.toPx() else 0f,
            -rise * size.height * .13f - sin(angle) * size.height * .18f - departure * (size.height + width))
        val heading = -90f + if (launch > .45f) loop * 360f + departure * 35f else joy * 3f
        val tankSize = Size(width * .32f, width * .46f)
        val tank = base + Offset(-width * .85f - tankSize.width / 2, width * .03f)
        val tankCenter = tank + Offset(tankSize.width / 2, tankSize.height / 2)
        if (launch > .25f) {
            val engine = ship + Offset(0f, width * .46f)
            drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = .7f), Color.Transparent), engine, width * .65f), width * .65f, engine)
        }
        rotate(heading, ship) { rocket(ship, width, 1f + .3f * sin(launch * 180f), enginesOn = launch > .25f) }
        // The freestanding collection tank stays connected until the rocket launches.
        if (!launching || launch < .28f) {
            val inlet = base + Offset(-width * .16f, width * .17f)
            val outlet = tank + Offset(tankSize.width, tankSize.height * .8f)
            val hose = Path().apply {
                moveTo(outlet.x, outlet.y)
                cubicTo(outlet.x + width * .25f, outlet.y + width * .3f,
                    inlet.x - width * .25f, inlet.y + width * .3f, inlet.x, inlet.y)
            }
            drawPath(hose, Navy, style = Stroke(10.dp.toPx()))
            drawPath(hose, Color(0xFF57FBE2), style = Stroke(5.dp.toPx()))
            if (launching && fuelTransfer < 1f) repeat(9) { i ->
                val t = (fuelTransfer * 4f + i / 9f) % 1f
                val v = 1f - t
                val p = outlet * (v * v * v) + (outlet + Offset(width * .25f, width * .3f)) * (3f * v * v * t) +
                    (inlet + Offset(-width * .25f, width * .3f)) * (3f * v * t * t) + inlet * (t * t * t)
                drawCircle(Color.White, 2.dp.toPx(), p)
            }
            if (launching) drawCircle(Brush.radialGradient(listOf(Color(0xFF54FFE4).copy(alpha = fuelTransfer * .7f), Color.Transparent), base, width * .55f), width * .55f, base)
        }
        val tankEnergy = if (launching) 1f - fuelTransfer else energy
        drawRoundRect(Color(0xFF153B65).copy(alpha = .72f), tank, tankSize, androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
        drawRoundRect(Color(0xFF76C9ED), tank + Offset(-4.dp.toPx(), tankSize.height),
            Size(tankSize.width + 8.dp.toPx(), 5.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
        val inner = tank + Offset(3.dp.toPx(), 3.dp.toPx())
        val innerSize = Size(tankSize.width - 6.dp.toPx(), tankSize.height - 6.dp.toPx())
        drawRect(Brush.verticalGradient(listOf(Color(0xFF8EFFF1), Color(0xFF0ABFC7))),
            inner + Offset(0f, innerSize.height * (1f - tankEnergy)), Size(innerSize.width, innerSize.height * tankEnergy))
        drawRoundRect(Pale, tank, tankSize, androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()), style = Stroke(2.dp.toPx()))
        drawLine(Color.White.copy(alpha = .7f), tank + Offset(tankSize.width * .2f, tankSize.height * .12f),
            tank + Offset(tankSize.width * .2f, tankSize.height * .78f), 2.dp.toPx())
        repeat(4) { i ->
            val y = tank.y + tankSize.height * (i + 1) / 5
            drawLine(Pale.copy(alpha = .65f), Offset(tank.x + tankSize.width * .74f, y), Offset(tank.x + tankSize.width * .94f, y), 1.dp.toPx())
        }
        val count = game.config.requiredCorrectAnswers
        fun crystalPosition(i: Int): Offset {
            val t = (i + .5f) / count
            return Offset(size.width * (.09f + .82f * t), size.height * (.77f + .09f * sin(i * 2.1f)))
        }
        for (i in previous until count) {
            if (collecting && i == previous) continue
            energyCrystal(crystalPosition(i), minOf(15.dp.toPx(), size.width / (count * 2.8f)))
        }
        // A permanent shoulder makes the collection arm clearly belong to the rocket.
        val origin = ship + Offset(width * .24f, width * .16f)
        var hand = origin + Offset(width * .16f, width * .08f)
        if (collecting) {
            val destination = crystalPosition(previous)
            hand = when {
                collection < .20f -> hand
                collection < .48f -> origin + (destination - origin) * ((collection - .20f) / .28f)
                collection < .55f -> destination
                collection < .83f -> destination + (tankCenter - destination) * ((collection - .55f) / .28f)
                else -> tankCenter + (origin - tankCenter) * ((collection - .83f) / .17f)
            }
            if (collection < .83f) {
                val crystal = if (collection < .55f) destination else hand
                val pulse = if (collection < .20f) (.5f + .5f * cos(collection / .20f * Math.PI * 6).toFloat()) else 1f
                energyCrystal(crystal, (12f + 4f * pulse).dp.toPx())
                if (collection < .20f) drawCircle(Color(0xFFBFFFF7).copy(alpha = .65f * pulse),
                    (20f + 9f * pulse).dp.toPx(), crystal, style = Stroke(2.dp.toPx()))
            }
        }
        if (!launching) {
        val joint = origin + (hand - origin) * .50f + Offset(width * .10f, -width * .10f)
        drawLine(Navy, origin, joint, 9.dp.toPx()); drawLine(Navy, joint, hand, 8.dp.toPx())
        drawLine(Color(0xFFE7EAF2), origin, joint, 5.dp.toPx()); drawLine(Color(0xFFE7EAF2), joint, hand, 4.dp.toPx())
        drawCircle(Color(0xFFFF855C), 6.dp.toPx(), origin)
        drawCircle(Gold, 4.dp.toPx(), joint)
        drawArc(Pale, 35f, 290f, false, hand - Offset(7.dp.toPx(), 7.dp.toPx()), Size(14.dp.toPx(), 14.dp.toPx()), style = Stroke(2.dp.toPx()))
        }
    }
}

@Composable
private fun CompletionScreen(game: Game, map: () -> Unit) {
    val flight = remember(game) { Animatable(0f) }
    LaunchedEffect(game) {
        delay(400)
        flight.animateTo(1f, tween(8000, easing = LinearEasing))
        delay(1800)
        map()
    }
    Box(Modifier.fillMaxSize().semantics {
        contentDescription = "Level ${game.config.level} complete"
        stateDescription = when {
            flight.value < .25f -> "Transferring fuel"
            flight.value < .45f -> "Lifting off"
            flight.value < .80f -> "Celebration loop"
            else -> "Flying to the next planet"
        }
    }) {
        EnergyScene(game, 1f, flight.value)
        if (flight.value > .30f) Canvas(Modifier.fillMaxSize()) {
            confetti(((flight.value - .30f) * 8f) % 1f, 100, seed = 23 + ((flight.value - .30f) * 8f).toInt())
            repeat(4) { burst ->
                val progress = ((flight.value - .38f - burst * .10f) / .25f).coerceIn(0f, 1f)
                if (progress > 0f && progress < 1f) repeat(14) { ray ->
                    val angle = ray * Math.PI.toFloat() * 2 / 14
                    val center = Offset(size.width * (.17f + burst * .22f), size.height * (.3f + .13f * (burst % 2)))
                    val direction = Offset(cos(angle), sin(angle))
                    drawLine(MapPlanetColors[burst], center + direction * progress * size.height * .18f,
                        center + direction * progress * size.height * .26f, (1f - progress) * 4.dp.toPx())
                }
            }
            repeat(9) { index -> star(Offset(size.width * (index + .5f) / 9, size.height * (.32f + .14f * sin(index.toFloat()))), 12.dp.toPx(), Gold) }
        }
        Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (flight.value < .25f) "Fuelling up!" else if (flight.value < .45f) "Rocket ready!" else game.settings.greeting("Great job"), color = Gold, fontSize = 36.sp, style = TitleStyle)
            Text("Level ${game.config.level} completed!", color = Color.White, fontSize = 24.sp)
        }
        if (flight.value >= 1f) SpaceButton("Back to Map", Green, Modifier.align(Alignment.BottomCenter).padding(16.dp), map)
    }
}
