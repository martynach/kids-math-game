package com.example.kidsmath

import org.junit.Assert.*
import org.junit.Test

class StarHintStateTest {
    @Test fun availabilityModeAndDefensiveValidation() {
        assertTrue(StarHintRules.available(20)); assertFalse(StarHintRules.available(21))
        assertTrue(StarHintRules.available(0)); assertFalse(StarHintRules.available(-1))
        assertTrue(StarHintRules.automatic(10, 30)); assertFalse(StarHintRules.automatic(11, 30))
        assertFalse(StarHintRules.supports(Question(20, 20, true)))
        assertFalse(StarHintRules.supports(Question(2, 3, false)))
        assertThrows(IllegalArgumentException::class.java) { StarHintState(Question(-1, 0, true), 1, 30) }
    }
    @Test fun fixedGridHasFiveColumnsFourRowsAndTwentyPositions() {
        assertEquals(5, StarHintRules.COLUMNS); assertEquals(4, StarHintRules.ROWS)
        for (count in listOf(0, 1, 5, 6, 19, 20)) {
            val state = StarHintState(Question(0, 0, true), 11, 30)
            repeat(count) { state.add(HintColor.YELLOW) }
            assertEquals(count, state.stars.size)
            state.stars.indices.forEach { i -> assertEquals(i % 5 to i / 5, StarHintRules.cell(i)) }
        }
        assertThrows(IllegalArgumentException::class.java) { StarHintRules.cell(20) }
    }
    private fun replay(q: Question): StarHintState {
        val state = StarHintState(q, 10, 30)
        hintTimeline(q).forEach { cue ->
            when (val action = cue.action) {
                is HintAction.Add -> state.add(action.color)
                is HintAction.Cross -> state.toggle(state.stars[action.index].id)
            }
        }
        return state
    }
    @Test fun automaticAdditionSequenceAndZeroOperands() {
        for ((left, right) in listOf(3 to 5, 12 to 7, 0 to 0, 0 to 20, 20 to 0, 19 to 1)) {
            val q = Question(left, right, true)
            val state = replay(q)
            assertEquals(left, state.count(HintColor.YELLOW)); assertEquals(right, state.count(HintColor.PINK))
            assertEquals(List(left) { HintColor.YELLOW } + List(right) { HintColor.PINK }, state.stars.map { it.color })
            hintTimeline(q).forEachIndexed { i, cue -> assertEquals(i * 500L, cue.atMs) }
        }
    }
    @Test fun subtractionTimelineRetainsCrossedCellsAndPropagatesNoGameState() {
        for ((left, right) in listOf(7 to 2, 0 to 0, 20 to 20, 20 to 0)) {
            val state = replay(Question(left, right, false))
            assertEquals(left, state.stars.size); assertEquals(right, state.stars.count { it.crossed })
            assertTrue(state.stars.all { it.color == HintColor.YELLOW })
            assertNull(state.add(HintColor.PINK))
            val cues = hintTimeline(state.question)
            assertTrue(cues.zipWithNext().all { (a, b) -> b.atMs - a.atMs >= 500 })
            if (right > 0) assertEquals(left * 500L + 350, cues[left].atMs)
        }
    }
    @Test fun interactiveCountsAreNotConstrainedByOperandsAndNeverExceedTwenty() {
        val state = StarHintState(Question(3, 5, true), 11, 30)
        repeat(19) { assertNotNull(state.add(HintColor.YELLOW)) }
        assertNotNull(state.add(HintColor.PINK))
        repeat(200) { assertNull(state.add(if (it % 2 == 0) HintColor.PINK else HintColor.YELLOW)) }
        assertEquals(19, state.count(HintColor.YELLOW)); assertEquals(1, state.count(HintColor.PINK))
        val full = StarHintState(Question(0, 0, true), 30, 30)
        repeat(20) { full.add(HintColor.YELLOW) }; assertNull(full.add(HintColor.PINK))
    }
    @Test fun orderCorrectBagsAndCompactionPreserveIdentity() {
        val state = StarHintState(Question(3, 5, true), 11, 30)
        val p = state.add(HintColor.PINK)!!
        val y = state.add(HintColor.YELLOW)!!
        val p2 = state.add(HintColor.PINK)!!
        assertEquals(listOf(p, y, p2), state.stars.map { it.id })
        assertFalse(state.beginReturn(y, HintColor.PINK)); assertFalse(state.finishReturn(y))
        assertTrue(state.beginReturn(y, HintColor.YELLOW))
        assertFalse(state.beginReturn(y, HintColor.YELLOW))
        assertTrue(state.finishReturn(y)); assertFalse(state.finishReturn(y))
        assertEquals(listOf(p, p2), state.stars.map { it.id })
        assertTrue(state.beginReturn(p, HintColor.PINK)); state.finishReturn(p)
        assertEquals(listOf(p2), state.stars.map { it.id })
    }
    @Test fun crossingTogglesAndReturnKeepsOtherCrosses() {
        val state = StarHintState(Question(7, 2, false), 11, 30)
        val ids = List(7) { state.add(HintColor.YELLOW)!! }
        assertTrue(state.toggle(ids[0])); assertTrue(state.stars[0].crossed)
        assertTrue(state.toggle(ids[0])); assertFalse(state.stars[0].crossed)
        state.toggle(ids[1]); state.toggle(ids[5])
        state.beginReturn(ids[1], HintColor.YELLOW)
        assertFalse(state.toggle(ids[1])); state.finishReturn(ids[1])
        assertEquals(1, state.stars.count { it.crossed }); assertTrue(state.stars.first { it.id == ids[5] }.crossed)
        state.beginReturn(ids[0], HintColor.YELLOW); state.finishReturn(ids[0])
        assertEquals(ids.drop(2), state.stars.map { it.id })
        assertFalse(StarHintState(Question(3, 5, true), 11, 30).toggle(0))
    }
    @Test fun returningStarReservesCapacityUntilItsAnimationFinishes() {
        val state = StarHintState(Question(0, 0, true), 11, 30)
        val ids = List(20) { state.add(HintColor.YELLOW)!! }
        state.beginReturn(ids[0], HintColor.YELLOW)
        assertNull(state.add(HintColor.PINK))
        state.finishReturn(ids[0]); assertNotNull(state.add(HintColor.PINK))
        assertEquals(20, state.stars.size)
    }
    @Test fun chosenSlotsStayUniqueAndReturningStarsReserveTheirPlace() {
        val state = StarHintState(Question(3, 5, true), 11, 30)
        val order = listOf(4, 9, 14, 19) + (0 until 20).toList()
        val pink = state.add(HintColor.PINK, order)!!
        val yellow = state.add(HintColor.YELLOW, order)!!
        assertEquals(listOf(4, 9), state.stars.map { it.slot })
        assertTrue(state.move(yellow, listOf(4, 0, 1)))
        assertEquals(listOf(4, 0), state.stars.map { it.slot })
        state.beginReturn(pink, HintColor.PINK)
        assertFalse(state.move(pink, listOf(3)))
        state.add(HintColor.YELLOW, order)
        assertEquals(listOf(4, 0, 9), state.stars.map { it.slot })
        state.finishReturn(pink)
        assertEquals(listOf(0, 9), state.stars.map { it.slot })
        state.add(HintColor.PINK, order)
        assertEquals(4, state.stars.last().slot)
        repeat(30) { state.add(HintColor.YELLOW, (0 until 20).toList()) }
        assertEquals(20, state.stars.size)
        assertEquals(20, state.stars.map { it.slot }.distinct().size)
    }
    @Test fun movingSubtractionStarPreservesItsIdentityAndLight() {
        val state = StarHintState(Question(7, 2, false), 11, 30)
        val id = state.add(HintColor.YELLOW, listOf(19))!!
        state.toggle(id)
        assertTrue(state.move(id, listOf(0)))
        assertEquals(id, state.stars.single().id)
        assertEquals(0, state.stars.single().slot)
        assertTrue(state.stars.single().crossed)
        assertFalse(state.move(id, listOf(-1, 20)))
        assertFalse(StarHintState(Question(3, 5, true), 10, 30).move(0, listOf(19)))
    }
    @Test fun reopenedSessionsAlwaysStartEmptyAndTimelineRestarts() {
        for (level in listOf(1, 10, 11, 30)) {
            val q = Question(7, 2, false)
            val old = StarHintState(q, level, 30); old.add(HintColor.YELLOW); old.toggle(0)
            val fresh = StarHintState(q, level, 30)
            assertTrue(fresh.stars.isEmpty()); assertEquals(0, fresh.count(HintColor.YELLOW))
            assertEquals(hintTimeline(q), hintTimeline(q))
        }
    }
}
