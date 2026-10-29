package com.myrobo.simulator.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class UltrasonicSensorComponent(
    id: String,
    x: Float,
    y: Float,
    var distanceCm: Int = 25 // 2cm to 400cm
) : Component(id, "Ultrasonic (HC-SR04)", x, y, 140f, 90f) {

    override val pins: List<Pin> = listOf(
        Pin("${id}_vcc", "VCC", PinType.POWER_5V, relX = 35f, relY = 82f),
        Pin("${id}_trig", "TRIG", PinType.DIGITAL, relX = 55f, relY = 82f),
        Pin("${id}_echo", "ECHO", PinType.DIGITAL, relX = 75f, relY = 82f),
        Pin("${id}_gnd", "GND", PinType.GND, relX = 95f, relY = 82f)
    )

    private var isDraggingSlider = false

    override fun onTouchInteract(touchX: Float, touchY: Float): Boolean {
        // Slider area below sensor
        if (touchX >= x && touchX <= x + width && touchY >= y - 20f && touchY <= y + 25f) {
            isDraggingSlider = true
            updateDistance(touchX)
            return true
        }
        return false
    }

    private fun updateDistance(touchX: Float) {
        val fraction = ((touchX - x) / width).coerceIn(0f, 1f)
        distanceCm = (2 + fraction * 398).toInt()
    }

    override fun onReleaseInteract() {
        isDraggingSlider = false
    }

    override fun update(dt: Float) {
        // Provide simulated distance signal via analog or pulse
        val echoPin = pins[2]
        echoPin.analogValue = distanceCm
        val trigPin = pins[1]
        if (trigPin.state == PinState.HIGH) {
            echoPin.state = PinState.HIGH
        }
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        // PCB Body (Classic blue HC-SR04)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#1565C0")
        val boardRect = RectF(x + 10f, y + 20f, x + width - 10f, y + 70f)
        canvas.drawRoundRect(boardRect, 6f, 6f, paint)

        // Two Ultrasonic Transducer Barrels (Transmitter T & Receiver R)
        paint.color = Color.parseColor("#CFD8DC")
        canvas.drawCircle(x + 38f, y + 45f, 20f, paint)
        canvas.drawCircle(x + 102f, y + 45f, 20f, paint)

        // Inner Mesh Grids
        paint.color = Color.parseColor("#455A64")
        canvas.drawCircle(x + 38f, y + 45f, 14f, paint)
        canvas.drawCircle(x + 102f, y + 45f, 14f, paint)

        // Labels T and R
        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("T", x + 38f, y + 48f, paint)
        canvas.drawText("R", x + 102f, y + 48f, paint)

        // Header Pins
        for (pin in pins) {
            val px = x + pin.relX
            val py = y + pin.relY

            paint.color = Color.parseColor("#90A4AE")
            paint.strokeWidth = 2.5f
            canvas.drawLine(px, y + 70f, px, py, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#ECEFF1")
            canvas.drawCircle(px, py, 3f, paint)

            paint.textSize = 7f
            canvas.drawText(pin.label, px, py - 6f, paint)
        }

        // Distance Slider / Readout Header
        paint.color = Color.parseColor("#00E5FF")
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("HC-SR04: ${distanceCm}cm", x + width / 2f, y + 14f, paint)
        paint.isFakeBoldText = false
    }
}
