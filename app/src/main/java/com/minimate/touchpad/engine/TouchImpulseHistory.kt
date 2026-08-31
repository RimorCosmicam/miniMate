package com.minimate.touchpad.engine

/**
 * Keeps shader impulses independent from Android pointer IDs, which are reused between gestures.
 * Released impulses remain in the ring until their visual decay is complete.
 */
internal class TouchImpulseHistory(
    private val capacity: Int = 8,
    // A released contact is an impulse that decays, not a permanent mark. Keeping them forever
    // left a fixed disturbance everywhere the screen had ever been touched; they are dropped once
    // their visual decay has run out, slightly after it is no longer visible.
    private val retentionSeconds: Float = 1.8f
) {
    private val history = LinkedHashMap<Int, TouchPoint>()
    private val activePointers = mutableMapOf<Int, Int>()
    private var nextImpulseId = 1

    fun update(activePoints: List<TouchPoint>, nowSeconds: Float): List<TouchPoint> {
        val activePointerIds = activePoints.mapTo(HashSet()) { it.id }
        activePointers.keys.filterNot { it in activePointerIds }.forEach { pointerId ->
            val impulseId = activePointers.remove(pointerId) ?: return@forEach
            history[impulseId]?.let { point ->
                history[impulseId] = point.copy(startedAtSeconds = nowSeconds, active = false)
            }
        }
        activePoints.forEach { point ->
            val impulseId = activePointers.getOrPut(point.id) { nextImpulseId++ }
            val old = history[impulseId]
            val start = if (old == null || !old.active) nowSeconds else old.startedAtSeconds
            history[impulseId] = point.copy(id = impulseId, startedAtSeconds = start, active = true)
        }
        history.entries.removeAll { (_, point) ->
            !point.active && nowSeconds - point.startedAtSeconds > retentionSeconds
        }
        while (history.size > capacity) history.remove(history.keys.first())
        return history.values.toList()
    }
}
