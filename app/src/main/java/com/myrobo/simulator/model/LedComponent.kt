package com.myrobo.simulator.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader

class LedComponent(
    id: String,
    x: Float,
    y: Float,
    var colorName: String = "Red"
) : Component(id, "LED ($colorName)", x, y, 64f, 100f) {

    override val pins: List<Pin> = listOf(
        Pin("${id}_anode", "+", PinType.DIGITAL, relX = 20f, relY = 90f),
        Pin("${id}_cathode", "-", PinType.GND, relX = 44f, relY = 90f)
    )

    var isLit: Boolean = false
    var brightness: Float = 0f // 0.0 to 1.0

    private val baseColor: Int
        get() = when (colorName.lowercase()) {
            "green" -> Color.parseColor("#00E676")
            "blue" -> Color.parseColor("#2979FF")
            "yellow" -> Color.parseColor("#FFEA00")
            else -> Color.parseColor("#FF1744") // Red
        }

    override fun update(dt: Float) {
        val anode = pins[0]
        val cathode = pins[1]
        // LED conducts if anode is positive relative to cathode
        val isAnodeHigh = anode.state == PinState.HIGH || anode.analogValue > 100
        val isCathodeLow = cathode.state == PinState.LOW && cathode.type != PinType.POWER_5V
        isLit = isAnodeHigh && isCathodeLow

        brightness = if (isLit) {
            if (anode.analogValue > 0) {
                (anode.analogValue / 255f).coerceIn(0.2f, 1f)
            } else {
                1f
            }
        } else {
            0f
        }
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        val centerX = x + 32f
        val bulbCenterY = y + 36f

        // Draw Metal Legs
        paint.color = Color.parseColor("#B0BEC5")
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        // Anode leg (slight kink / longer)
        canvas.drawLine(x + 20f, y + 54f, x + 20f, y + 90f, paint)
        // Cathode leg (straight)
        canvas.drawLine(x + 44f, y + 54f, x + 44f, y + 90f, paint)

        // Draw LED Base Rim (plastic rim)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#37474F")
        canvas.drawRoundRect(x + 12f, y + 50f, x + 52f, y + 56f, 3f, 3f, paint)

        // Glow effect when ON
        if (isLit) {
            val glowRadius = 38f * brightness
            val glowShader = RadialGradient(
                centerX, bulbCenterY, glowRadius,
                baseColor, Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            glowPaint.shader = glowShader
            canvas.drawCircle(centerX, bulbCenterY, glowRadius, glowPaint)
        }

        // Draw Bulb Dome
        paint.shader = null
        paint.style = Paint.Style.FILL
        if (isLit) {
            paint.color = baseColor
        } else {
            // Dim translucent tint
            paint.color = Color.argb(140, Color.red(baseColor) / 2, Color.green(baseColor) / 2, Color.blue(baseColor) / 2)
        }
        canvas.drawCircle(centerX, bulbCenterY, 20f, paint)

        // Dome highlight reflection
        paint.color = Color.argb(160, 255, 255, 255)
        canvas.drawCircle(centerX - 6f, bulbCenterY - 6f, 4f, paint)

        // Draw Pin Terminals
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#ECEFF1")
        canvas.drawCircle(x + 20f, y + 90f, 4f, paint)
        canvas.drawCircle(x + 44f, y + 90f, 4f, paint)

        // Labels
        paint.textSize = 9f
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("+", x + 20f, y + 84f, paint)
        canvas.drawText("-", x + 44f, y + 84f, paint)
    }
}
