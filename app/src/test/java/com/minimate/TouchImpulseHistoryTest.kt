package com.minimate

import com.minimate.touchpad.engine.TouchImpulseHistory
import com.minimate.touchpad.engine.TouchPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchImpulseHistoryTest {
    @Test
    fun reusedAndroidPointerIdDoesNotReplacePreviousReaction() {
        val history = TouchImpulseHistory()
        history.update(listOf(TouchPoint(20f, 30f, id = 0)), 1f)
        val released = history.update(emptyList(), 1.1f).single()
        val both = history.update(listOf(TouchPoint(200f, 300f, id = 0)), 1.2f)

        assertEquals(2, both.size)
        assertFalse(both.first().active)
        assertTrue(both.last().active)
        assertEquals(released.id, both.first().id)
        assertNotEquals(both.first().id, both.last().id)
    }

    @Test
    fun releasedReactionRemainsAsPersistentSceneMemory() {
        val history = TouchImpulseHistory()
        history.update(listOf(TouchPoint(1f, 2f, id = 0)), 0f)
        history.update(emptyList(), .1f)
        assertEquals(1, history.update(emptyList(), 2.2f).size)
        assertEquals(1, history.update(emptyList(), 200f).size)
    }

    @Test
    fun oldestSceneMemoryIsReplacedOnlyAtCapacity() {
        val history = TouchImpulseHistory(capacity = 2)
        history.update(listOf(TouchPoint(1f, 2f, id = 0)), 0f)
        history.update(emptyList(), .1f)
        history.update(listOf(TouchPoint(3f, 4f, id = 0)), .2f)
        history.update(emptyList(), .3f)
        val newest = history.update(listOf(TouchPoint(5f, 6f, id = 0)), .4f)

        assertEquals(2, newest.size)
        assertEquals(3f, newest.first().x)
        assertEquals(5f, newest.last().x)
    }
}
