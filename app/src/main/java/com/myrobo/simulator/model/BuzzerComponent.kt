package com.myrobo.simulator.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class BuzzerComponent(
    id: String,
    x: Float,
    y: Float
) : Component(id, "Buzzer", x, y, 70f, 90f) {

    override val pins: List<Pin> = listOf(
        Pin("${id}_pos", "+", PinType.DIGITAL, relX = 22f, relY = 82f),
        Pin("${id}_neg", "-", PinType.GND, relX = 48f, relY = 82f)
    )

    var isBeeping: Boolean = false
    private var wavePhase: Float = 0f

    override fun update(dt: Float) {
        val pos = pins[0]
        val neg = pins[1]
        isBeeping = pos.state == PinState.HIGH || pos.analogValue > 50
        if (isBeeping) {
            wavePhase += dt * 10f
            if (wavePhase > 1f) wavePhase -= 1f
        }
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        val centerX = x + 35f
        val centerY = y + 35f

        // Terminals
        paint.color = Color.parseColor("#90A4AE")
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(x + 22f, y + 55f, x + 22f, y + 82f, paint)
        canvas.drawLine(x + 48f, y + 55f, x + 48f, y + 82f, paint)

        // Sound Waves when active
        if (isBeeping) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = Color.argb((220 * (1f - wavePhase)).toInt(), 0, 229, 255)
            canvas.drawCircle(centerX, centerY, 28f + wavePhase * 16f, paint)
        }

        // Buzzer Body (Black cylinder)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#1E272E")
        canvas.drawCircle(centerX, centerY, 26f, paint)

        // Center Sound Hole
        paint.color = Color.parseColor("#0A0E17")
        canvas.drawCircle(centerX, centerY, 7f, paint)

        // Pin Terminals
        paint.color = Color.parseColor("#ECEFF1")
        canvas.drawCircle(x + 22f, y + 82f, 3.5f, paint)
        canvas.drawCircle(x + 48f, y + 82f, 3.5f, paint)

        // Text
        paint.color = Color.WHITE
        paint.textSize = 8f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("+", x + 22f, y + 78f, paint)
        canvas.drawText("-", x + 48f, y + 78f, paint)
        canvas.drawText("BUZZER", centerX, y + 16f, paint)
    }
}
