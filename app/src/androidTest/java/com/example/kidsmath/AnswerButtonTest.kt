package com.example.kidsmath

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class AnswerButtonTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun enteringDeletingAndConfirmingAnAnswerUpdatesTheKeypad() {
        compose.onNodeWithText("▶  New Game").performClick()
        compose.onNodeWithContentDescription("Confirm answer").assertIsNotEnabled()

        val expression = compose.onNode(hasText(" = ?", substring = true)).fetchSemanticsNode()
            .config[SemanticsProperties.Text].single().text
        val match = Regex("(\\d+) ([+−]) (\\d+) = \\?").matchEntire(expression)!!
        val left = match.groupValues[1].toInt()
        val right = match.groupValues[3].toInt()
        val answer = if (match.groupValues[2] == "+") left + right else left - right

        compose.onNodeWithContentDescription("0").performClick()
        compose.onNodeWithContentDescription("Confirm answer").assertIsEnabled()
        compose.onNodeWithContentDescription("Backspace").performClick()
        compose.onNodeWithContentDescription("Confirm answer").assertIsNotEnabled()

        answer.toString().forEach { digit ->
            compose.onNodeWithContentDescription(digit.toString()).performClick()
        }
        compose.onNodeWithContentDescription("Confirm answer").assertIsEnabled().performClick()
        compose.onNodeWithText("★  1 / 5").assertIsDisplayed()
        compose.waitUntil(timeoutMillis = 5000) {
            compose.onAllNodes(hasText(" = ?", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Confirm answer").assertIsNotEnabled()
        compose.onNodeWithContentDescription("0").assertIsEnabled()
    }

    @Test fun wrongAnswersRemoveHeartsAndShowASadFace() {
        compose.onNodeWithText("▶  New Game").performClick()
        compose.onNodeWithContentDescription("Lives remaining: 3").assertIsDisplayed()
        repeat(2) { mistake ->
            repeat(3) { compose.onNodeWithContentDescription("9").performClick() }
            compose.onNodeWithContentDescription("Confirm answer").performClick()
            compose.onNodeWithContentDescription("Lives remaining: ${2 - mistake}").assertIsDisplayed()
            compose.onAllNodesWithText("♥", useUnmergedTree = true).assertCountEquals(2 - mistake)
            compose.onNodeWithContentDescription("Sad face").assertIsDisplayed()
            compose.onNodeWithContentDescription("Confirm answer").assertIsNotEnabled()
            waitForQuestion()
        }
    }

    @Test fun correctAnswersCelebrateAndNextRoundWaitsForTheButton() {
        compose.onNodeWithText("▶  New Game").performClick()
        repeat(5) { index ->
            enterCorrectAnswer()
            compose.onNodeWithContentDescription("Happy face").assertIsDisplayed()
            compose.onNodeWithContentDescription("Celebration confetti").assertExists()
            if (index < 4) waitForQuestion()
        }
        compose.waitUntil(timeoutMillis = 5000) {
            compose.onAllNodesWithText("▶  Next Round").fetchSemanticsNodes().isNotEmpty()
        }
        // A real elapsed wait catches the previous automatic round transition.
        Thread.sleep(3000)
        compose.onNodeWithText("Round 1 Complete!").assertIsDisplayed()
        compose.onNodeWithText("▶  Next Round").performClick()
        compose.onNodeWithText("Round 2").assertIsDisplayed()
        compose.onNodeWithContentDescription("Lives remaining: 3").assertIsDisplayed()
        compose.onAllNodesWithText("♥", useUnmergedTree = true).assertCountEquals(3)
    }

    private fun waitForQuestion() {
        compose.waitUntil(timeoutMillis = 5000) {
            compose.onAllNodes(hasText(" = ?", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun enterCorrectAnswer() {
        val expression = compose.onNode(hasText(" = ?", substring = true)).fetchSemanticsNode()
            .config[SemanticsProperties.Text].single().text
        val match = Regex("(\\d+) ([+−]) (\\d+) = \\?").matchEntire(expression)!!
        val left = match.groupValues[1].toInt()
        val right = match.groupValues[3].toInt()
        val answer = if (match.groupValues[2] == "+") left + right else left - right
        answer.toString().forEach { compose.onNodeWithContentDescription(it.toString()).performClick() }
        compose.onNodeWithContentDescription("Confirm answer").performClick()
    }
}
