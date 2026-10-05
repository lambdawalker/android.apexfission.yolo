package com.apexfission.android.yolo.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NumThreadsTest {

    @Test
    fun testDefaultNumThreadsReturnsAtLeastOne() {
        val count = NumThreads.Default.toInt()
        assertTrue("Default thread count should be >= 1", count >= 1)
        assertTrue("Default thread count should be <= available processors", count <= Runtime.getRuntime().availableProcessors())
    }

    @Test
    fun testQuarterNumThreadsReturnsAtLeastOne() {
        val count = NumThreads.Quarter.toInt()
        assertTrue("Quarter thread count should be >= 1", count >= 1)
    }

    @Test
    fun testHalfNumThreadsReturnsAtLeastOne() {
        val count = NumThreads.Half.toInt()
        assertTrue("Half thread count should be >= 1", count >= 1)
    }

    @Test
    fun testThreeQuartersNumThreadsReturnsAtLeastOne() {
        val count = NumThreads.ThreeQuarters.toInt()
        assertTrue("ThreeQuarters thread count should be >= 1", count >= 1)
    }

    @Test
    fun testCustomPercentageNumThreads() {
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        val customPercentage = 0.5f
        val expected = (availableProcessors * customPercentage).toInt().coerceAtLeast(1)

        val count = NumThreads.CustomPercentage(customPercentage).toInt()
        assertEquals(expected, count)
    }

    @Test
    fun testNegativeCustomPercentageCoercesToOne() {
        val count = NumThreads.CustomPercentage(-0.5f).toInt()
        assertEquals(1, count)
    }

    @Test
    fun testCustomCountNumThreads() {
        val count = NumThreads.CustomCount(4).toInt()
        assertEquals(4, count)
    }

    @Test
    fun testCustomCountZeroOrNegativeCoercesToOne() {
        assertEquals(1, NumThreads.CustomCount(0).toInt())
        assertEquals(1, NumThreads.CustomCount(-5).toInt())
    }

    @Test
    fun testToStringRepresentations() {
        assertEquals("Default", NumThreads.Default.toString())
        assertEquals("25%", NumThreads.Quarter.toString())
        assertEquals("50%", NumThreads.Half.toString())
        assertEquals("75%", NumThreads.ThreeQuarters.toString())
        assertEquals("CustomPercentage(percentage=0.6)", NumThreads.CustomPercentage(0.6f).toString())
        assertEquals("CustomCount(count=4)", NumThreads.CustomCount(4).toString())
    }
}
