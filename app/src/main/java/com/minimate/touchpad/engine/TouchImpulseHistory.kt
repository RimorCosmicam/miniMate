package com.minimate.touchpad.engine

/**
 * Keeps shader impulses independent from Android pointer IDs, which are reused between gestures.
 * Released impulses remain in the ring until their visual decay is complete.
 */
internal class TouchImpulseHistory(
    private val capacity: Int = 8,
    // The last eight released contacts are scene memory, not short-lived cursor particles.
    // They remain until newer contacts replace them so themes can keep dents, currents,
    // scrambled cells, planted objects, and other persistent changes.
    private val retentionSeconds: Float = Float.POSITIVE_INFINITY
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
