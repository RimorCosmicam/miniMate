package com.minimate.touchpad.model

sealed interface GestureEvent {
    data class Move(val dx: Int, val dy: Int, val isDragging: Boolean = false) : GestureEvent
    data class Click(val button: Byte, val down: Boolean) : GestureEvent
    data class Scroll(val vScroll: Int, val hScroll: Int) : GestureEvent
    data class ScrollFling(val velocityX: Float, val velocityY: Float) : GestureEvent
    data class Pinch(val scaleFactor: Float) : GestureEvent
    object TapClick : GestureEvent
    object RightClick : GestureEvent
    object DoubleTapDragStart : GestureEvent
    object DragEnd : GestureEvent
}
