package com.sarim.sceneformgesturestutorial

import android.view.GestureDetector
import android.view.MotionEvent

class GestureDetector : GestureDetector.SimpleOnGestureListener() {
    enum class GestureType {
        NONE, SINGLE_TAP, DOUBLE_TAP, LONG_PRESS
    }

    var gestureType = GestureType.NONE
        private set

    fun resetGestureType() {
        gestureType = GestureType.NONE
    }

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        gestureType = GestureType.SINGLE_TAP
        return super.onSingleTapConfirmed(e)
    }

    override fun onDoubleTap(e: MotionEvent?): Boolean {
        gestureType = GestureType.DOUBLE_TAP
        return super.onDoubleTap(e)
    }

    override fun onLongPress(e: MotionEvent?) {
        super.onLongPress(e)
        gestureType = GestureType.LONG_PRESS
    }
}