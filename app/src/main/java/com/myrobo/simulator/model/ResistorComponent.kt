package com.myrobo.simulator.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class ResistorComponent(
    id: String,
    x: Float,
    y: Float,
    var resistanceOhms: Int = 220
) : Component(id, "Resistor (${resistanceOhms}Ω)", x, y, 90f, 40f) {

    override val pins: List<Pin> = listOf(
        Pin("${id}_p1", "1", PinType.DIGITAL, relX = 8f, relY = 20f),
        Pin("${id}_p2", "2", PinType.DIGITAL, relX = 82f, relY = 20f)
    )

    override fun update(dt: Float) {
        val p1 = pins[0]
        val p2 = pins[1]
        // Resistor conducts bidirectionally
        if (p1.state == PinState.HIGH && p2.state == PinState.LOW) {
            p2.state = PinState.HIGH
            p2.analogValue = p1.analogValue
        } else if (p2.state == PinState.HIGH && p1.state == PinState.LOW) {
            p1.state = PinState.HIGH
            p1.analogValue = p2.analogValue
        }
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        val centerY = y + 20f

        // Axial Wire leads
        paint.color = Color.parseColor("#B0BEC5")
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(x + 8f, centerY, x + 25f, centerY, paint)
        canvas.drawLine(x + 65f, centerY, x + 82f, centerY, paint)

        // Resistor Body (Beige / Carbon film tan)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#D7CCC8")
        val bodyRect = RectF(x + 24f, y + 10f, x + 66f, y + 30f)
        canvas.drawRoundRect(bodyRect, 8f, 8f, paint)

        // Color Bands for 220 Ohm (Red, Red, Brown, Gold)
        val bandWidth = 3.5f
        paint.style = Paint.Style.FILL

        // Band 1: Red
        paint.color = Color.parseColor("#D32F2F")
        canvas.drawRect(x + 30f, y + 10f, x + 30f + bandWidth, y + 30f, paint)

        // Band 2: Red
        paint.color = Color.parseColor("#D32F2F")
        canvas.drawRect(x + 38f, y + 10f, x + 38f + bandWidth, y + 30f, paint)

        // Band 3: Brown (Multiplier x10)
        paint.color = Color.parseColor("#5D4037")
        canvas.drawRect(x + 46f, y + 10f, x + 46f + bandWidth, y + 30f, paint)

        // Band 4: Gold (Tolerance 5%)
        paint.color = Color.parseColor("#FFD700")
        canvas.drawRect(x + 56f, y + 10f, x + 56f + bandWidth, y + 30f, paint)

        // Pin Terminals
        paint.color = Color.parseColor("#ECEFF1")
        canvas.drawCircle(x + 8f, centerY, 4f, paint)
        canvas.drawCircle(x + 82f, centerY, 4f, paint)

        // Text label
        paint.color = Color.parseColor("#90A4AE")
        paint.textSize = 9f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("${resistanceOhms}Ω", x + 45f, y + 40f, paint)
    }
}
