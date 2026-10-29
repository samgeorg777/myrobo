package com.myrobo.simulator.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class PotentiometerComponent(
    id: String,
    x: Float,
    y: Float,
    var value: Int = 512 // 0 to 1023
) : Component(id, "Potentiometer", x, y, 90f, 90f) {

    override val pins: List<Pin> = listOf(
        Pin("${id}_vcc", "VCC", PinType.POWER_5V, relX = 20f, relY = 82f),
        Pin("${id}_sig", "SIG", PinType.ANALOG, relX = 45f, relY = 82f),
        Pin("${id}_gnd", "GND", PinType.GND, relX = 70f, relY = 82f)
    )

    private var isDraggingKnob = false

    override fun onTouchInteract(touchX: Float, touchY: Float): Boolean {
        val centerX = x + 45f
        val centerY = y + 40f
        val dx = touchX - centerX
        val dy = touchY - centerY
        if (dx * dx + dy * dy <= 30f * 30f) {
            isDraggingKnob = true
            updateKnobAngle(touchX, touchY)
            return true
        }
        return false
    }

    private fun updateKnobAngle(touchX: Float, touchY: Float) {
        val centerX = x + 45f
        val centerY = y + 40f
        val angleRad = atan2((touchY - centerY).toDouble(), (touchX - centerX).toDouble())
        var degrees = Math.toDegrees(angleRad) + 90.0
        if (degrees < 0) degrees += 360.0
        val ratio = (degrees / 360.0).coerceIn(0.0, 1.0)
        value = (ratio * 1023).toInt()
    }

    override fun onReleaseInteract() {
        isDraggingKnob = false
    }

    override fun update(dt: Float) {
        // Output analog voltage on wiper pin
        val wiper = pins[1]
        wiper.analogValue = value
        wiper.state = if (value > 200) PinState.HIGH else PinState.LOW
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        val centerX = x + 45f
        val centerY = y + 40f

        // Terminal Legs
        paint.color = Color.parseColor("#90A4AE")
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(x + 20f, y + 60f, x + 20f, y + 82f, paint)
        canvas.drawLine(x + 45f, y + 60f, x + 45f, y + 82f, paint)
        canvas.drawLine(x + 70f, y + 60f, x + 70f, y + 82f, paint)

        // Outer Metallic Housing
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#37474F")
        canvas.drawCircle(centerX, centerY, 28f, paint)

        // Rotary Dial Body (Blue plastic potentiometer head)
        paint.color = Color.parseColor("#1565C0")
        canvas.drawCircle(centerX, centerY, 22f, paint)

        // Notch indicator
        val angleDeg = (value / 1023f) * 360f - 90f
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val notchX = centerX + (cos(angleRad) * 16f).toFloat()
        val notchY = centerY + (sin(angleRad) * 16f).toFloat()

        paint.color = Color.WHITE
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(centerX, centerY, notchX, notchY, paint)

        // Pin Terminals
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#ECEFF1")
        canvas.drawCircle(x + 20f, y + 82f, 3.5f, paint)
        canvas.drawCircle(x + 45f, y + 82f, 3.5f, paint)
        canvas.drawCircle(x + 70f, y + 82f, 3.5f, paint)

        // Text & Value Display
        paint.color = Color.parseColor("#00E5FF")
        paint.textSize = 9f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("$value", centerX, y + 14f, paint)

        paint.color = Color.parseColor("#B0BEC5")
        paint.textSize = 8f
        canvas.drawText("+", x + 20f, y + 78f, paint)
        canvas.drawText("OUT", x + 45f, y + 78f, paint)
        canvas.drawText("-", x + 70f, y + 78f, paint)
    }
}
