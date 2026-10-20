package com.example.kidsmath

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toPixelMap
import android.graphics.PathMeasure
import android.graphics.Region
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class StarHintFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val state get() = ViewModelProvider(compose.activity)[AppState::class.java]
    @Before fun prepare() {
        compose.runOnIdle {
            state.activatePremium(); state.configureLimits(true, 5, 1)
            CampaignStore(compose.activity.application).save(CampaignProgress(30))
            compose.activity.viewModelStore.clear()
        }
        compose.activityRule.scenario.recreate()
    }
    private fun start(max: Int = 20, level: Int = 1, operation: Operations = Operations.BOTH) {
        compose.runOnIdle { state.save(Settings(operations = operation, max = max)); state.start(level) }
    }
    private fun gridCount(count: Int) = compose.onNodeWithTag("hint-grid").assert(
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "5 columns, 4 rows, $count stars"))
    private fun stars(color: String) = compose.onAllNodesWithContentDescription("$color counting star")
    private fun harness(q: Question, level: Int): MutableState<Boolean> {
        val opened = mutableStateOf(true)
        compose.runOnUiThread {
            compose.activity.setContent {

                MaterialTheme {
                    if (opened.value) StarHintScreen(q, "", level, close = { opened.value = false }) {}
                }
            }
        }
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(32)
        return opened
    }
    private fun travel() { compose.mainClock.advanceTimeBy(600); compose.waitForIdle() }
    private fun drag(source: SemanticsNodeInteraction, destination: SemanticsNodeInteraction) {
        val from = source.fetchSemanticsNode().boundsInRoot.center
        val to = destination.fetchSemanticsNode().boundsInRoot.center
        compose.onRoot().performTouchInput { swipe(from, to, 350) }
        travel()
    }
    @Test fun availabilityDependsOnMaxNotQuestionOrLevel() {
        start(20, 10); compose.onNodeWithContentDescription("Star counting hint").assertIsDisplayed().performClick()
        compose.onNodeWithTag("star-hint").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Automatic counting"))
        compose.onNodeWithContentDescription("Close hint").performClick()
        start(20, 11); compose.onNodeWithContentDescription("Star counting hint").performClick()
        compose.onNodeWithTag("star-hint").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Interactive counting"))
        gridCount(0)
        compose.onNodeWithContentDescription("Close hint").performClick()
        start(21); compose.onNodeWithContentDescription("Star counting hint").assertDoesNotExist()
    }
    @Test fun xAndAndroidBackPreserveGameInputAndPassAndReopenFresh() {
        start(level = 11)
        compose.onNodeWithContentDescription("1").performClick()
        lateinit var question: Question
        lateinit var pass: SpacePass
        compose.runOnIdle { question = state.game!!.question; pass = state.spacePass }
        repeat(3) {
            compose.onNodeWithContentDescription("Star counting hint").performClick(); gridCount(0)
            if (it == 1) Espresso.pressBack() else compose.onNodeWithContentDescription("Close hint").performClick()
            compose.onNodeWithTag("star-hint").assertDoesNotExist()
            compose.runOnIdle {
                assertEquals(question, state.game!!.question); assertEquals("1", state.game!!.input)
                assertEquals(3, state.game!!.lives); assertEquals(0, state.game!!.correct)
                assertNull(state.game!!.pausedAt); assertEquals(pass, state.spacePass)
            }
        }
    }
    @Test fun automaticAdditionTravelsInOrderAndUsesOneFixedGrid() {
        harness(Question(3, 5, true), 10)
        compose.mainClock.advanceTimeBy(1200)
        stars("yellow").assertCountEquals(3); stars("pink").assertCountEquals(0)
        compose.mainClock.advanceTimeBy(4000)
        gridCount(8); stars("yellow").assertCountEquals(3); stars("pink").assertCountEquals(5)
        val positions = (0..7).map { compose.onNodeWithTag("hint-star-$it").fetchSemanticsNode().boundsInRoot }
        assertEquals(positions[0].width, positions[7].width, .1f)
        assertEquals(positions[0].top, positions[4].top, .1f)
        assertTrue(positions[5].top > positions[4].top)
    }
    @Test fun automaticSubtractionCrossesWithoutRemovingAndCloseCancelsTimeline() {
        harness(Question(7, 2, false), 1)
        compose.mainClock.advanceTimeBy(6000)
        gridCount(7); stars("yellow").assertCountEquals(7)
        compose.onNodeWithTag("hint-star-0").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Dimmed"))
        compose.onNodeWithTag("hint-star-1").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Dimmed"))
        compose.onNodeWithContentDescription("Close hint").performClick()
        compose.mainClock.advanceTimeBy(20_000)
        compose.onNodeWithTag("star-hint").assertDoesNotExist(); stars("yellow").assertCountEquals(0)
    }
    @Test fun largeAutomaticAdditionAndZeroAreBounded() {
        harness(Question(12, 7, true), 10)
        compose.mainClock.advanceTimeBy(11_000)
        gridCount(19); stars("yellow").assertCountEquals(12); stars("pink").assertCountEquals(7)
        // Row wrapping follows absolute slot indexes, even when color changes mid-row.
        val yellow = compose.onNodeWithTag("hint-star-11").fetchSemanticsNode().boundsInRoot
        val pink = compose.onNodeWithTag("hint-star-12").fetchSemanticsNode().boundsInRoot
        assertEquals(yellow.top, pink.top, .1f); assertTrue(pink.left > yellow.left)
    }
    @Test fun interactiveAdditionDragReturnWrongBagCompactionAndCapacity() {
        harness(Question(3, 5, true), 11); gridCount(0)
        val pink = compose.onNodeWithTag("hint-bag-pink")
        val yellow = compose.onNodeWithTag("hint-bag-yellow")
        val grid = compose.onNodeWithTag("hint-grid")
        drag(pink, grid); drag(yellow, grid); drag(pink, grid)
        gridCount(3)
        drag(compose.onNodeWithTag("hint-star-1"), pink); gridCount(3)
        val firstBefore = compose.onNodeWithTag("hint-star-0").fetchSemanticsNode().boundsInRoot
        val secondBefore = compose.onNodeWithTag("hint-star-2").fetchSemanticsNode().boundsInRoot
        drag(compose.onNodeWithTag("hint-star-1"), yellow); gridCount(2)
        val first = compose.onNodeWithTag("hint-star-0").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithTag("hint-star-2").fetchSemanticsNode().boundsInRoot
        assertEquals(firstBefore, first); assertEquals(secondBefore, second)
        repeat(18) { drag(yellow, grid) }; gridCount(20)
        drag(pink, grid); gridCount(20)
        drag(compose.onNodeWithTag("hint-star-0"), pink); gridCount(19)
    }
    @Test fun interactiveSubtractionTapToggleDragAndPreservedCrosses() {
        harness(Question(7, 2, false), 11)
        val bag = compose.onNodeWithTag("hint-bag-yellow"); val grid = compose.onNodeWithTag("hint-grid")
        repeat(3) { drag(bag, grid) }
        val first = compose.onNodeWithTag("hint-star-0")
        first.performClick(); travel()
        first.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Dimmed"))
        first.performClick(); travel()
        first.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Lit"))
        compose.onNodeWithTag("hint-star-1").performClick(); compose.onNodeWithTag("hint-star-2").performClick(); travel()
        drag(compose.onNodeWithTag("hint-star-1"), bag); gridCount(2)
        compose.onNodeWithTag("hint-star-2").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Dimmed"))
        // Dragging an uncrossed star to a non-bag target cannot accidentally toggle it.
        drag(first, grid)
        first.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Lit"))
        drag(first, bag); gridCount(1)
    }
    @Test fun confirmingFromHintUsesExistingFeedbackOnceInBothModes() {
        for (level in listOf(10, 11)) for (correct in listOf(true, false)) {
            start(level = level)
            var expected = 0
            compose.runOnIdle { expected = if (correct) state.game!!.question.result else 999 }
            compose.onNodeWithContentDescription("Star counting hint").performClick()
            expected.toString().forEach { compose.onNodeWithContentDescription(it.toString()).performClick() }
            compose.onNodeWithContentDescription("Confirm answer").performTouchInput { doubleClick() }
            compose.onNodeWithTag("star-hint").assertDoesNotExist()
            compose.waitUntil(1500) { state.game!!.phase == Phase.FEEDBACK }
            compose.onNodeWithContentDescription(if (correct) "Happy face" else "Sad face").assertIsDisplayed()
            compose.runOnIdle {
                assertEquals(if (correct) 1 else 0, state.game!!.correct)
                assertEquals(if (correct) 3 else 2, state.game!!.lives)
                assertEquals(if (correct) Feedback.CORRECT else Feedback.WRONG, state.game!!.feedback)
            }
        }
    }
    @Test fun reopeningAutomaticSessionStartsFromZeroWithoutOldJobs() {
        val opened = harness(Question(20, 0, true), 10)
        compose.mainClock.advanceTimeBy(1700)
        assertTrue(stars("yellow").fetchSemanticsNodes().size in 3..4)
        compose.runOnUiThread { opened.value = false }
        compose.mainClock.advanceTimeBy(6000)
        stars("yellow").assertCountEquals(0)
        compose.runOnUiThread { opened.value = true }
        compose.mainClock.advanceTimeBy(64)
        assertTrue(stars("yellow").fetchSemanticsNodes().size <= 1)
        compose.mainClock.advanceTimeBy(11_000); gridCount(20)
        compose.runOnUiThread { opened.value = false }; compose.mainClock.advanceTimeBy(32)
        compose.runOnUiThread { opened.value = true }; compose.mainClock.advanceTimeBy(64)
        assertTrue(stars("yellow").fetchSemanticsNodes().size <= 1)
    }
    @Test fun cancelledGesturesDoNotAddOrCrossAndZeroRemainsEmpty() {
        harness(Question(0, 0, false), 11)
        val bag = compose.onNodeWithTag("hint-bag-yellow")
        bag.performTouchInput { down(center); moveBy(Offset(30f, -30f)); cancel() }
        travel(); gridCount(0)
        drag(bag, compose.onNodeWithTag("hint-grid")); gridCount(1)
        compose.onNodeWithTag("hint-star-0").performTouchInput { down(center); moveBy(Offset(30f, 30f)); cancel() }
        travel(); gridCount(1)
        compose.onNodeWithTag("hint-star-0").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Lit"))
    }
    private fun swipeBetween(from: Offset, to: Offset) {
        compose.onRoot().performTouchInput { swipe(from, to, 350) }
        travel()
    }
    @Test fun nearBagPickupAndNearGridDropWorkForBothColors() {
        harness(Question(3, 5, true), 11)
        val grid = compose.onNodeWithTag("hint-grid").fetchSemanticsNode().boundsInRoot
        val cell = grid.width / 5f
        listOf("yellow", "pink").forEach { color ->
            val bag = compose.onNodeWithTag("hint-bag-$color").fetchSemanticsNode().boundsInRoot
            val from = Offset(if (color == "yellow") bag.left + 4 else bag.right - 4, bag.center.y)
            val to = Offset(if (color == "yellow") grid.left - cell * .45f else grid.right + cell * .45f, grid.center.y)
            swipeBetween(from, to)
            stars(color).assertCountEquals(1)
        }
        gridCount(2)
        val yellow = compose.onNodeWithTag("hint-bag-yellow").fetchSemanticsNode().boundsInRoot
        swipeBetween(yellow.center, Offset(grid.left - cell, grid.center.y))
        gridCount(2) // Clearly outside the generous margin still rejects the drop.
    }
    @Test fun bagTapsAndShortMovementsDoNotCreateStars() {
        harness(Question(7, 2, false), 11)
        val bag = compose.onNodeWithTag("hint-bag-yellow")
        bag.performTouchInput { click(center) }; travel(); gridCount(0)
        val grid = compose.onNodeWithTag("hint-grid").fetchSemanticsNode().boundsInRoot
        val bounds = bag.fetchSemanticsNode().boundsInRoot
        val density = compose.activity.resources.displayMetrics.density
        val from = Offset(bounds.right - 2, grid.bottom - 4)
        swipeBetween(from, from + Offset(12 * density, 0f))
        gridCount(0)
        drag(bag, compose.onNodeWithTag("hint-grid")); gridCount(1)
        val star = compose.onNodeWithTag("hint-star-0")
        star.performClick(); travel()
        star.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Dimmed"))
        val point = star.fetchSemanticsNode().boundsInRoot.center
        swipeBetween(point, point + Offset(16 * density, 0f))
        star.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Dimmed"))
        gridCount(1)
        star.performClick(); travel()
        star.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Lit"))
        // Return near the sack, outside its visible silhouette.
        swipeBetween(star.fetchSemanticsNode().boundsInRoot.center, Offset(bounds.right - grid.width / 5f * .20f, bounds.center.y))
        gridCount(0)
    }
    private fun slotCenter(index: Int): Offset {
        val grid = compose.onNodeWithTag("hint-grid").fetchSemanticsNode().boundsInRoot
        val cell = grid.width / 5f
        return grid.topLeft + Offset((index % 5 + .5f) * cell, (index / 5 + .5f) * cell)
    }
    private fun assertAtSlot(id: Long, slot: Int) {
        val center = compose.onNodeWithTag("hint-star-$id").fetchSemanticsNode().boundsInRoot.center
        assertTrue("Star $id at slot $slot: $center", (center - slotCenter(slot)).getDistance() < 2f)
    }
    @Test fun releaseContinuesFromFingerAndStarsCanBeArrangedOnBothSides() {
        harness(Question(3, 5, true), 11)
        val cell = compose.onNodeWithTag("hint-grid").fetchSemanticsNode().boundsInRoot.width / 5f
        val bag = compose.onNodeWithTag("hint-bag-pink").fetchSemanticsNode().boundsInRoot.center
        val drop = slotCenter(4) + Offset(cell * .25f, cell * .15f)
        compose.onRoot().performTouchInput {
            down(bag); moveTo((bag + drop) / 2f, 100); moveTo(drop, 200)
        }
        compose.mainClock.advanceTimeByFrame()
        val held = compose.onNodeWithTag("hint-drag-pink").fetchSemanticsNode().boundsInRoot.center
        assertTrue("Dragged star follows the finger", (held - drop).getDistance() < 2f)
        compose.onRoot().performTouchInput { up() }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("hint-drag-pink").assertDoesNotExist()
        val released = compose.onNodeWithTag("hint-star-0").fetchSemanticsNode().boundsInRoot.center
        assertTrue("Released star must not restart at the bag", (released - held).getDistance() < cell * .12f)
        compose.mainClock.advanceTimeBy(160)
        val halfway = compose.onNodeWithTag("hint-star-0").fetchSemanticsNode().boundsInRoot.center
        assertTrue((halfway - slotCenter(4)).getDistance() < (held - slotCenter(4)).getDistance())
        travel(); assertAtSlot(0, 4)
        val yellow = compose.onNodeWithTag("hint-bag-yellow").fetchSemanticsNode().boundsInRoot.center
        swipeBetween(yellow, slotCenter(0)); assertAtSlot(1, 0)
        swipeBetween(bag, slotCenter(9)); assertAtSlot(2, 9)
        // Occupied drops choose the next nearest free slot without disturbing its occupant.
        swipeBetween(yellow, slotCenter(4)); assertAtSlot(0, 4); gridCount(4)
        val moved = compose.onNodeWithTag("hint-star-1").fetchSemanticsNode().boundsInRoot.center
        swipeBetween(moved, slotCenter(15)); assertAtSlot(1, 15)
        swipeBetween(compose.onNodeWithTag("hint-star-2").fetchSemanticsNode().boundsInRoot.center, bag)
        gridCount(3); assertAtSlot(0, 4); assertAtSlot(1, 15)
    }
    private fun assertNoGroupLabels() {
        listOf("yellow", "pink").forEach { color ->
            compose.onNodeWithTag("hint-group-$color").assertDoesNotExist()
            compose.onNodeWithTag("hint-count-$color").assertDoesNotExist()
        }
    }
    @Test fun dimmedStarIsGrayAndCanRelightWithoutCountLabels() {
        harness(Question(7, 2, false), 11)
        assertNoGroupLabels()
        drag(compose.onNodeWithTag("hint-bag-yellow"), compose.onNodeWithTag("hint-grid"))
        val star = compose.onNodeWithTag("hint-star-0")
        val bounds = star.fetchSemanticsNode().boundsInRoot
        fun centerColor() = star.captureToImage().let { it.toPixelMap()[it.width / 2, it.height / 2] }
        val lit = centerColor()
        star.performClick(); travel()
        val dim = centerColor()
        assertTrue("Dimmed star loses yellow saturation", kotlin.math.abs(dim.red - dim.blue) < .2f)
        assertTrue("Dimmed star is darker", dim.red + dim.green < lit.red + lit.green - .3f)
        assertEquals(bounds, star.fetchSemanticsNode().boundsInRoot)
        gridCount(1); assertNoGroupLabels()
        star.performClick(); travel()
        // The standard touch ripple may still darken the surface briefly.
        val restored = centerColor()
        assertTrue("Relit star is gold", restored.red - restored.blue > .5f)
        assertTrue("Relit star is brighter", restored.red + restored.green > dim.red + dim.green + .3f)
        star.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Lit"))
    }
    @Test fun labelsOnlyAppearInAutomaticAddition() {
        val opened = harness(Question(3, 5, true), 10)
        compose.mainClock.advanceTimeBy(6000)
        compose.onNodeWithTag("hint-group-yellow").assertIsDisplayed()
        compose.onNodeWithTag("hint-group-pink").assertIsDisplayed()
        compose.runOnUiThread { opened.value = false }; travel()
        harness(Question(7, 2, false), 10)
        compose.mainClock.advanceTimeBy(6000); assertNoGroupLabels()
        compose.runOnUiThread { compose.activity.setContent {} }; travel()
        harness(Question(3, 5, true), 11)
        assertNoGroupLabels()
        listOf("yellow", "pink", "yellow", "pink").forEach { color ->
            drag(compose.onNodeWithTag("hint-bag-$color"), compose.onNodeWithTag("hint-grid"))
            assertNoGroupLabels()
        }
        gridCount(4)
    }
    @Test fun groupOutlinesAreSingleContoursAndExcludeOtherColorCenters() {
        val geometry = HintGeometry(1200f, 600f)
        val examples = listOf((0..11).toList(), (12..18).toList(), (0..19 step 2).toList(),
            listOf(0,1,2,5,7,10,11,12), listOf(0,2,6,8,10,12,16,18))
        examples.forEach { indices ->
            HintColor.entries.forEach { color ->
                val path = hintGroupPath(indices, geometry, color).asAndroidPath()
                val measure = PathMeasure(path, false)
                var contours = 1
                while (measure.nextContour()) contours++
                assertEquals("$color $indices", 1, contours)
                val region = Region().apply { setPath(path, Region(0,0,1200,600)) }
                repeat(20) { index ->
                    val center = geometry.center(index)
                    assertEquals("$color center $index in $indices", index in indices,
                        region.contains(center.x.toInt(), center.y.toInt()))
                }
            }
        }
    }}
