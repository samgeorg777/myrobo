package com.myrobo.simulator.model

import android.graphics.Canvas
import android.graphics.Paint

abstract class Component(
    val id: String,
    var name: String,
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float
) {
    abstract val pins: List<Pin>

    fun containsPoint(px: Float, py: Float): Boolean {
        return px >= x && px <= x + width && py >= y && py <= y + height
    }

    fun findPinAt(px: Float, py: Float, radius: Float = 40f): Pin? {
        for (pin in pins) {
            val pinAbsX = x + pin.relX
            val pinAbsY = y + pin.relY
            val dx = px - pinAbsX
            val dy = py - pinAbsY
            if (dx * dx + dy * dy <= radius * radius) {
                return pin
            }
        }
        return null
    }

    fun getPinAbsolutePos(pin: Pin): Pair<Float, Float> {
        return Pair(x + pin.relX, y + pin.relY)
    }

    fun getPinById(pinId: String): Pin? {
        return pins.find { it.id == pinId }
    }

    abstract fun draw(canvas: Canvas, paint: Paint)
    
    open fun onTouchInteract(touchX: Float, touchY: Float): Boolean {
        return false
    }

    open fun onReleaseInteract() {}

    open fun update(dt: Float) {}
}
