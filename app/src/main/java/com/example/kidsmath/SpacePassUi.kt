package com.example.kidsmath

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PassGold = Color(0xFFFFD34B)
private val PassBlue = Color(0xFF009CFA)
private val PassNavy = Color(0xFF071745)
private val PassPale = Color(0xFFD4EEFF)

@Composable
internal fun SpacePassSection(premium: Boolean, unlimited: Boolean, attempts: String, hours: String,
    onPremiumChange: (Boolean) -> Unit, onUnlimitedChange: (Boolean) -> Unit,
    onAttemptsChange: (String) -> Unit, onHoursChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)
        .background(Brush.verticalGradient(listOf(Color(0xFF123C7D), PassNavy)), RoundedCornerShape(20.dp))
        .border(1.dp, PassBlue, RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("★ SPACE PASS", color = PassGold, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text("Choose your plan", color = PassPale)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PlanCard("FREE", !premium, PassBlue, Modifier.weight(1f)) {
                Text("${SpacePassConfig.FREE_ATTEMPTS} missions", color = Color.White)
                Text("${formatCooldown(SpacePassConfig.FREE_COOLDOWN_MS)} rest after all attempts", color = PassPale)

                if (premium) SpaceButton("Switch to Free", PassBlue, onClick = { onPremiumChange(false) })
                if (!premium) Text("✓ Selected plan", color = Color(0xFF71FF9B), fontWeight = FontWeight.Bold)
            }
            PlanCard("PREMIUM ★", premium, PassGold, Modifier.weight(1f)) {
                Text("Custom play limits", color = Color.White)
                Text("Unlimited by default", color = PassPale)
                Text("More space adventures!", color = PassPale)
                if (premium) Text("✓ Selected plan", color = Color(0xFF71FF9B), fontWeight = FontWeight.Bold)
                else SpaceButton("Switch to Premium", Color(0xFFDA9500), Modifier.fillMaxWidth(), { onPremiumChange(true) })
            }
        }
        if (premium) {
            Text("PLAY LIMITS", color = PassGold, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Unlimited attempts", color = Color.White)
                    Text(if (unlimited) "Your child can play without limits." else "Custom limit", color = PassPale)
                }
                Switch(unlimited, onUnlimitedChange, Modifier.semantics { contentDescription = "Unlimited attempts" })
            }
            val fields = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = PassPale,
                focusedLabelColor = PassGold, unfocusedLabelColor = PassPale,
                focusedBorderColor = PassBlue, unfocusedBorderColor = PassPale.copy(alpha = .5f),
                focusedContainerColor = PassNavy, unfocusedContainerColor = PassNavy,
                disabledContainerColor = PassNavy.copy(alpha = .5f),
                disabledTextColor = PassPale.copy(alpha = .4f), disabledLabelColor = PassPale.copy(alpha = .4f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(attempts, onAttemptsChange, label = { Text("Attempts before rest") },
                    enabled = !unlimited, singleLine = true, colors = fields,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(hours, onHoursChange, label = { Text("Rest duration (hours)") },
                    enabled = !unlimited, singleLine = true, colors = fields,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            val count = attempts.toIntOrNull()
            val duration = hours.toIntOrNull()
            val valid = count != null && count > 0 && duration != null && duration in 1..SpacePassConfig.MAX_REST_HOURS
            if (!unlimited) {
                if (!valid) Text("Enter positive whole numbers; rest duration must be 1–${SpacePassConfig.MAX_REST_HOURS} hours.", color = Color(0xFFFF8CA1))
                Text("Changing the allowance starts a fresh cycle after saving.", color = PassPale)
            }
        }
        Text("Changes take effect after Save Settings.", color = PassPale, fontSize = 12.sp)
        Text("DEVELOPMENT ONLY · Premium activation is a local mock. No payment is taken.",
            color = PassPale, fontSize = 12.sp)
    }
}

@Composable
private fun PlanCard(title: String, current: Boolean, color: Color, modifier: Modifier,
                     content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.background(Brush.verticalGradient(listOf(color.copy(alpha = .22f), PassNavy)), RoundedCornerShape(18.dp))
        .border(if (current) 2.dp else 1.dp, color.copy(alpha = if (current) 1f else .5f), RoundedCornerShape(18.dp))
        .padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Canvas(Modifier.size(40.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                if (title == "FREE") rotate(-50f, center) { rocket(center, size.width * .9f) }
                else star(center, size.width * .4f, PassGold)
            }
            Text(title, color = if (title == "FREE") Color.White else PassGold, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        content()
    }
}

@Composable
internal fun RestScreen(state: AppState) {
    val available = state.spacePass.canStart(state.passNow)
    val breathing = rememberInfiniteTransition(label = "Astronaut resting")
    val bob by breathing.animateFloat(-1f, 1f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "Peaceful breathing")
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(.85f).fillMaxHeight()) {
            Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Sleeping astronaut and parked rocket" }) {
                planet(Offset(size.width * .5f, size.height * 1.12f), size.height * .5f, Color(0xFF9970E8))
                val ship = Offset(size.width * .48f, size.height * .45f)
                rotate(-90f, ship) { rocket(ship, size.minDimension * .62f, enginesOn = false) }
                val hero = Offset(size.width * .59f, size.height * .78f + bob * 2.dp.toPx())
                rotate(55f, hero) { astronaut(hero, size.minDimension * .48f, sleeping = true) }
                energyCrystal(Offset(size.width * .19f, size.height * .86f), 18.dp.toPx())
                energyCrystal(Offset(size.width * .82f, size.height * .92f), 13.dp.toPx())
                star(Offset(size.width * .2f, size.height * .2f), 9.dp.toPx(), PassGold)
            }
            Text("Zzz…", Modifier.align(Alignment.CenterEnd).padding(end = 16.dp), color = PassPale,
                fontSize = (27 + bob).sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1.35f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Great space training today!", color = PassGold, fontSize = 30.sp,
                fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text("Come back later for more missions!", color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center)
            Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF285FB7), Color(0xFF183675))), RoundedCornerShape(20.dp))
                .border(1.dp, PassBlue, RoundedCornerShape(20.dp)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (available) "Ready for more adventures!" else "New missions in", color = PassPale, fontSize = 18.sp)
                Text(if (available) "Missions available again!" else formatCooldown(state.spacePass.remainingCooldown(state.passNow)),
                    Modifier.semantics { contentDescription = "Mission cooldown" }, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            SpaceButton("Back to map", PassBlue, Modifier.fillMaxWidth(), state::openMap)
            Text("Want to play without limits?", color = PassPale, fontSize = 14.sp)
            Text("Ask a parent to check the Space Pass settings.", color = PassPale, fontSize = 13.sp, textAlign = TextAlign.Center)
            SpaceButton("⚙ Settings", Color(0xFF293C78), onClick = state::openSettings)
        }
    }
}
