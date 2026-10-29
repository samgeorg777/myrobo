package com.myrobo.simulator.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class PushButtonComponent(
    id: String,
    x: Float,
    y: Float
) : Component(id, "Push Button", x, y, 70f, 70f) {

    override val pins: List<Pin> = listOf(
        Pin("${id}_1a", "1A", PinType.DIGITAL, relX = 6f, relY = 24f),
        Pin("${id}_1b", "1B", PinType.DIGITAL, relX = 6f, relY = 46f),
        Pin("${id}_2a", "2A", PinType.DIGITAL, relX = 64f, relY = 24f),
        Pin("${id}_2b", "2B", PinType.DIGITAL, relX = 64f, relY = 46f)
    )

    var isPressed: Boolean = false

    override fun onTouchInteract(touchX: Float, touchY: Float): Boolean {
        // Center button tactile disc
        val btnCenterX = x + 35f
        val btnCenterY = y + 35f
        val dx = touchX - btnCenterX
        val dy = touchY - btnCenterY
        if (dx * dx + dy * dy <= 24f * 24f) {
            isPressed = true
            return true
        }
        return false
    }

    override fun onReleaseInteract() {
        isPressed = false
    }

    override fun update(dt: Float) {
        val p1 = pins[0] // 1A
        val p2 = pins[2] // 2A
        if (isPressed) {
            // Closed contact: pass signal between side 1 and side 2
            if (p1.state == PinState.HIGH) {
                p2.state = PinState.HIGH
                p2.analogValue = p1.analogValue
            } else if (p2.state == PinState.HIGH) {
                p1.state = PinState.HIGH
                p1.analogValue = p2.analogValue
            }
        }
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        // Metallic Leads
        paint.color = Color.parseColor("#90A4AE")
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(x + 6f, y + 24f, x + 16f, y + 24f, paint)
        canvas.drawLine(x + 6f, y + 46f, x + 16f, y + 46f, paint)
        canvas.drawLine(x + 54f, y + 24f, x + 64f, y + 24f, paint)
        canvas.drawLine(x + 54f, y + 46f, x + 64f, y + 46f, paint)

        // Switch Base Housing (Dark tactile button package)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#263238")
        val baseRect = RectF(x + 14f, y + 14f, x + 56f, y + 56f)
        canvas.drawRoundRect(baseRect, 6f, 6f, paint)

        // Tactile Round Button
        paint.style = Paint.Style.FILL
        if (isPressed) {
            // Depressed state
            paint.color = Color.parseColor("#00E5FF")
            canvas.drawCircle(x + 35f, y + 35f, 13f, paint)
            paint.color = Color.parseColor("#00B0FF")
            canvas.drawCircle(x + 35f, y + 35f, 10f, paint)
        } else {
            // Normal state
            paint.color = Color.parseColor("#37474F")
            canvas.drawCircle(x + 35f, y + 35f, 15f, paint)
            paint.color = Color.parseColor("#455A64")
            canvas.drawCircle(x + 35f, y + 35f, 11f, paint)
        }

        // Terminal dots
        paint.color = Color.parseColor("#ECEFF1")
        canvas.drawCircle(x + 6f, y + 24f, 3.5f, paint)
        canvas.drawCircle(x + 6f, y + 46f, 3.5f, paint)
        canvas.drawCircle(x + 64f, y + 24f, 3.5f, paint)
        canvas.drawCircle(x + 64f, y + 46f, 3.5f, paint)

        // Status text
        paint.color = Color.WHITE
        paint.textSize = 8f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(if (isPressed) "PRESSED" else "PUSH", x + 35f, y + 66f, paint)
    }
}
