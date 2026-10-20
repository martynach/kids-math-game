package com.example.kidsmath

import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class QuestionGeneratorTest {
    private val evaluator = DifficultyEvaluator()
    private fun a(x:Int,y:Int) = Question(x,y,true)
    private fun s(x:Int,y:Int) = Question(x,y,false)
    private fun valid(q:Question,min:Int,max:Int) {
        assertTrue(q.left in 0..max && q.right in 0..max)
        assertTrue(q.result in (if(q.addition) min else 0)..max)
    }
    @Test fun indexedUniverseIsExact() {
        for(min in 0..12) for(max in min..12) for(op in Operations.entries) {
            val sampler=UniformQuestionSampler(min,max,op)
            val expected=mutableSetOf<Question>()
            for(x in 0..max) for(y in 0..max) {
                if(op!=Operations.SUBTRACTION && x+y in min..max) expected.add(a(x,y))
                if(op!=Operations.ADDITION && y<=x) expected.add(s(x,y))
            }
            assertEquals(expected.size.toLong(),sampler.count)
            assertEquals(expected,(0 until sampler.count).map { sampler.questionAt(it) }.toSet())
        }
    }
    @Test fun uniformExpressionsAndBalancedOperations() {
        for(op in Operations.entries) {
            val sampler=UniformQuestionSampler(2,4,op,Random(901))
            val counts=mutableMapOf<Question,Int>()
            repeat(200_000) { val q=sampler.next(); counts[q]=(counts[q]?:0)+1 }
            assertEquals(sampler.count.toInt(),counts.size)
            if(op==Operations.BOTH) assertTrue(abs(counts.filterKeys{it.addition}.values.sum()-100_000)<1500)
            counts.forEach { (q,n) ->
                val size=if(q.addition) sampler.additionCount else sampler.subtractionCount
                val expected=200_000.0/size/(if(op==Operations.BOTH) 2 else 1)
                assertTrue("$op $q $n vs $expected",abs(n-expected)<expected*.06)
            }
        }
    }
    @Test fun difficultyScoresAndDecimalTransfers() {
        val cases=mapOf(a(4,1) to 0.0,a(3,2) to 18.75,a(6,4) to 49.5,a(19,1) to 57.0,
            a(12,8) to 87.0,s(20,0) to 50.0,s(20,13) to 87.0,s(0,0) to 0.0,
            a(8,7) to 68.25,s(13,7) to 60.75,s(20,20) to 50.0)
        cases.forEach { (q,score) -> assertEquals(q.toString(),score,evaluator.score(q,20),1e-9) }
        assertEquals(3,evaluator.carryBorrowCount(a(999,1)))
        assertEquals(6,evaluator.carryBorrowCount(s(1_000_000,1)))
        assertEquals(0,evaluator.carryBorrowCount(a(20,30)))
        assertEquals(0,evaluator.carryBorrowCount(s(18,5)))
        assertEquals(1,evaluator.carryBorrowCount(s(82,37)))
        assertEquals(0.0,evaluator.score(a(0,0),0),0.0)
        assertEquals(0.0,evaluator.score(s(0,0),0),0.0)
        assertEquals(50.0,evaluator.score(a(0,1),1),0.0)
        assertTrue(evaluator.score(a(19,1),20)>evaluator.score(a(4,1),20))
        assertTrue(evaluator.score(a(19,1),20)<evaluator.score(a(12,8),20))
        for(x in 0..100) for(y in 0..x) {
            assertTrue(evaluator.score(a(y,x-y),100) in 0.0..100.0)
            assertTrue(evaluator.score(s(x,y),100) in 0.0..100.0)
        }
        // Isolate each configurable contribution.
        assertEquals(0.0,DifficultyEvaluator(DifficultyWeights(0.0,0.0,0.0,0.0)).score(a(8,7),20),0.0)
        assertEquals(12.0,DifficultyEvaluator(DifficultyWeights(0.0,0.0,0.0,12.0)).score(a(8,7),20),0.0)
    }
    @Test fun campaignNormalizationAndCaps() {
        val p=ProgressionPolicy()
        for(total in listOf(1,5,10,30,100)) for(level in 1..total) {
            val progress=if(total==1) 0.0 else (level-1).toDouble()/(total-1)
            assertEquals(progress,p.progress(level,total),0.0)
            assertEquals(8+84*progress,p.target(progress),0.0)
            assertEquals(if(progress<.2) 30+70*progress/.2 else 100.0,p.cap(progress),1e-9)
        }
        assertEquals(50.0,p.target(.5),0.0)
        assertEquals(1.0,AcceptancePolicy().probability(50.0,50.0),0.0)
        assertTrue(AcceptancePolicy().probability(0.0,92.0)>=.025)
    }
    @Test fun allCategoriesOverlapAndExtend() {
        val t=QuestionLimitTracker()
        t.record(s(0,0))
        assertEquals(1,t.count("subtraction-zero")); assertEquals(1,t.count("subtraction-equal"))
        assertFalse(t.allows(s(5,0))); assertFalse(t.allows(s(5,5)))
        for(q in listOf(a(0,5),a(1,5),s(5,1))) {
            assertTrue(t.allows(q)); t.record(q); assertFalse(t.allows(q))
        }
        val overlap=QuestionLimitTracker(); overlap.record(a(0,1))
        assertFalse(overlap.allows(a(0,8))); assertFalse(overlap.allows(a(1,8)))
        assertTrue(QuestionLimitTracker().allows(s(0,0)))
        val custom=QuestionLimitTracker(listOf(QuestionLimitRule("two",2){it.left==2}))
        repeat(2) { assertTrue(custom.allows(a(2,3))); custom.record(a(2,3)) }
        assertFalse(custom.allows(a(2,3)))
    }
    @Test fun boundedFallbackStagesAndExhaustion() {
        fun g(settings:Settings)=QuestionGenerator(settings,Random(3),selection=SelectionConfig(attempts=0))
        val preference=g(Settings(max=20)); preference.next()
        assertEquals(SelectionStage.PREFERENCE_RELAXED,preference.lastStage)
        val cap=g(Settings(min=20,max=20,operations=Operations.ADDITION)); cap.next()
        assertEquals(SelectionStage.CAP_RELAXED,cap.lastStage)
        val categories=g(Settings(min=1,max=1,operations=Operations.ADDITION))
        categories.next(); categories.next(); assertEquals(SelectionStage.LIMITS_RELAXED,categories.lastStage)
        categories.next(); assertEquals(SelectionStage.REPEAT,categories.lastStage)
        for((min,max) in listOf(0 to 0,1 to 1,20 to 20,2 to 20)) for(op in Operations.entries) {
            val generator=g(Settings(min=min,max=max,operations=op))
            repeat(600) { valid(generator.next(),min,max) }
        }
        val stuckRandom=object:Random() { override fun nextBits(bitCount:Int)=0 }
        val large=QuestionGenerator(Settings(max=100),stuckRandom,selection=SelectionConfig(100,0,10))
        val seen=mutableSetOf<Question>()
        repeat(500) { assertTrue(seen.add(large.next())) }
    }
    @Test fun normalRestrictionsAndHistoryAtEveryLevel() {
        for(level in listOf(1,15,30)) {
            val g=QuestionGenerator(Settings(max=100),Random(level),level)
            val t=QuestionLimitTracker(); val seen=mutableSetOf<Question>()
            repeat(50) {
                val q=g.next()
                if(g.lastStage<SelectionStage.LIMITS_RELAXED) assertTrue(t.allows(q))
                if(g.lastStage<SelectionStage.REPEAT) assertTrue(q !in seen)
                if(g.lastStage<SelectionStage.CAP_RELAXED) assertTrue(evaluator.score(q,100)<=g.cap)
                t.record(q); seen.add(q)
            }
        }
    }
    @Test fun mistakesRepeatWithoutIssuingNewQuestions() {
        val settings=Settings(max=20)
        val game=Game(settings,LevelConfig(1,2),Random(42)); val first=game.question
        repeat(2) { i ->
            type(game,999); game.confirm(i*2600L); game.tick((i+1)*2600L)
            assertEquals(first,game.question); assertEquals(0,game.correct)
            assertEquals(2-i,game.lives); assertEquals("",game.input)
        }
        type(game,first.result); game.confirm(5200); game.tick(7800)
        assertEquals(1,game.correct); assertNotEquals(first,game.question)
        val retry=Game(settings,LevelConfig(1,2),Random(42))
        assertEquals(first,retry.question); assertEquals(3,retry.lives)
        // Same seed and same correct answers yield identical subsequent selection even
        // when one game had mistakes: neither RNG nor restrictions were consumed.
        type(retry,first.result); retry.confirm(0); retry.tick(2600)
        assertEquals(retry.question,game.question)
        val tiny=Game(Settings(max=0),LevelConfig(1,20))
        repeat(20) { i -> type(tiny,0); tiny.confirm(i*2600L); tiny.tick((i+1)*2600L) }
        assertEquals(Phase.LEVEL_COMPLETE,tiny.phase)
    }
    @Test fun largeRangeAndIntBoundaryCounts() {
        val sampler=UniformQuestionSampler(0,1_000_000,Operations.BOTH,Random(77))
        assertEquals(500_001_500_001L,sampler.additionCount)
        assertEquals(1_000_003_000_002L,sampler.count)
        assertEquals(a(1_000_000,0),sampler.questionAt(sampler.additionCount-1,true))
        assertEquals(s(1_000_000,1_000_000),sampler.questionAt(sampler.subtractionCount-1,false))
        repeat(100_000) { valid(sampler.next(),0,1_000_000) }
        val generator=QuestionGenerator(ArithmeticRange(500_000,1_000_000,Operations.BOTH),Random(4))
        repeat(200) { valid(generator.next(),500_000,1_000_000) }
        val forced=QuestionGenerator(ArithmeticRange(1_000_000,1_000_000,Operations.ADDITION),
            Random(4),selection=SelectionConfig(0,0,0))
        repeat(200) { valid(forced.next(),1_000_000,1_000_000) }
        val extreme=UniformQuestionSampler(Int.MAX_VALUE,Int.MAX_VALUE,Operations.ADDITION,Random(2))
        repeat(1000) { valid(extreme.next(),Int.MAX_VALUE,Int.MAX_VALUE) }
    }
    @Test fun independentCampaignsShowDifficultyTrend() {
        for((levels,max) in listOf(5 to 20,10 to 20,30 to 20,10 to 100,10 to 1000)) {
            val sums=DoubleArray(levels); val histogram=IntArray(11)
            val categoryCounts=mutableMapOf<String,Int>()
            var additions=0; var duplicates=0; var fallbacks=0; var issued=0
            repeat(100) { seed -> for(level in 1..levels) {
                val g=QuestionGenerator(ArithmeticRange(0,max,Operations.BOTH),Random(seed*101+level),level,levels)
                val seen=mutableSetOf<Question>(); val limits=QuestionLimitTracker()
                repeat(10) {
                    val q=g.next(); val score=evaluator.score(q,max)
                    sums[level-1]+=score; histogram[(score/10).toInt()]++; issued++
                    if(q.addition) additions++
                    if(!seen.add(q)) duplicates++
                    if(g.lastStage!=SelectionStage.NORMAL) fallbacks++
                    if(g.lastStage<SelectionStage.CAP_RELAXED) assertTrue(score<=g.cap)
                    if(g.lastStage<SelectionStage.LIMITS_RELAXED) assertTrue(limits.allows(q))
                    QuestionLimits.defaults.filter { it.matches(q) }.forEach {
                        categoryCounts[it.id]=(categoryCounts[it.id]?:0)+1
                    }
                    limits.record(q)
                }
            } }
            assertTrue("$levels/$max ${sums.toList()}",sums.last()>sums.first()+15_000)
            println("Campaign levels=$levels max=$max mean=${sums.map{it/1000}} histogram=${histogram.toList()} additions=$additions duplicates=$duplicates fallbacks=$fallbacks categories=$categoryCounts issued=$issued")
            assertEquals(issued,histogram.sum()); assertEquals(0,duplicates)
            assertTrue(additions.toDouble()/issued in .2.. .8)
            assertTrue(fallbacks.toDouble()/issued<.2)
        }
    }
    private fun type(g:Game,n:Int) { n.toString().forEach { g.digit(it.digitToInt()) } }
}
