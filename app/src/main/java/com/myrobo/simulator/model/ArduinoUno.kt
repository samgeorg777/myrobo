package com.myrobo.simulator.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class ArduinoUno(
    id: String = "mcu_arduino_uno",
    x: Float = 100f,
    y: Float = 100f
) : Component(id, "Arduino Uno (ATmega328P)", x, y, 380f, 260f) {

    // Pins list
    override val pins: List<Pin> = listOf(
        // Digital Header (Top Row: from right to left or left to right)
        Pin("d0", "0 (RX)", PinType.DIGITAL, relX = 40f, relY = 16f),
        Pin("d1", "1 (TX)", PinType.DIGITAL, relX = 64f, relY = 16f),
        Pin("d2", "2", PinType.DIGITAL, relX = 88f, relY = 16f),
        Pin("d3", "~3", PinType.DIGITAL, relX = 112f, relY = 16f),
        Pin("d4", "4", PinType.DIGITAL, relX = 136f, relY = 16f),
        Pin("d5", "~5", PinType.DIGITAL, relX = 160f, relY = 16f),
        Pin("d6", "~6", PinType.DIGITAL, relX = 184f, relY = 16f),
        Pin("d7", "7", PinType.DIGITAL, relX = 208f, relY = 16f),
        Pin("d8", "8", PinType.DIGITAL, relX = 240f, relY = 16f),
        Pin("d9", "~9", PinType.DIGITAL, relX = 264f, relY = 16f),
        Pin("d10", "~10", PinType.DIGITAL, relX = 288f, relY = 16f),
        Pin("d11", "~11", PinType.DIGITAL, relX = 312f, relY = 16f),
        Pin("d12", "12", PinType.DIGITAL, relX = 336f, relY = 16f),
        Pin("d13", "13", PinType.DIGITAL, relX = 360f, relY = 16f),

        // Power Header (Bottom Left)
        Pin("rst", "RST", PinType.DIGITAL, relX = 40f, relY = 244f),
        Pin("3v3", "3.3V", PinType.POWER_3V3, relX = 64f, relY = 244f),
        Pin("5v", "5V", PinType.POWER_5V, state = PinState.HIGH, analogValue = 1023, relX = 88f, relY = 244f),
        Pin("gnd1", "GND", PinType.GND, state = PinState.LOW, relX = 112f, relY = 244f),
        Pin("gnd2", "GND", PinType.GND, state = PinState.LOW, relX = 136f, relY = 244f),
        Pin("vin", "Vin", PinType.POWER_5V, relX = 160f, relY = 244f),

        // Analog Header (Bottom Right)
        Pin("a0", "A0", PinType.ANALOG, relX = 220f, relY = 244f),
        Pin("a1", "A1", PinType.ANALOG, relX = 244f, relY = 244f),
        Pin("a2", "A2", PinType.ANALOG, relX = 268f, relY = 244f),
        Pin("a3", "A3", PinType.ANALOG, relX = 292f, relY = 244f),
        Pin("a4", "A4", PinType.ANALOG, relX = 316f, relY = 244f),
        Pin("a5", "A5", PinType.ANALOG, relX = 340f, relY = 244f)
    )

    var isPowerLedOn: Boolean = true
    var isPin13LedOn: Boolean = false

    override fun update(dt: Float) {
        val pin13 = getPinById("d13")
        isPin13LedOn = pin13?.state == PinState.HIGH || (pin13?.analogValue ?: 0) > 100
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        // PCB Board Background (Classic Arduino Teal)
        paint.color = Color.parseColor("#00796B")
        paint.style = Paint.Style.FILL
        val boardRect = RectF(x, y, x + width, y + height)
        canvas.drawRoundRect(boardRect, 16f, 16f, paint)

        // PCB Border
        paint.color = Color.parseColor("#004D40")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(boardRect, 16f, 16f, paint)

        // USB Connector (Silver box on left)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#B0BEC5")
        canvas.drawRoundRect(RectF(x - 12f, y + 30f, x + 36f, y + 80f), 4f, 4f, paint)

        // DC Power Barrel (Black box bottom left)
        paint.color = Color.parseColor("#263238")
        canvas.drawRoundRect(RectF(x - 12f, y + 140f, x + 44f, y + 190f), 4f, 4f, paint)

        // ATmega328P Chip (DIP package in middle)
        paint.color = Color.parseColor("#1A202C")
        val chipRect = RectF(x + 130f, y + 90f, x + 270f, y + 150f)
        canvas.drawRoundRect(chipRect, 6f, 6f, paint)

        // Chip Label
        paint.color = Color.parseColor("#E2E8F0")
        paint.textSize = 12f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("ATmega328P", x + 200f, y + 124f, paint)

        // Arduino Uno Silkscreen
        paint.color = Color.WHITE
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("ARDUINO UNO", x + 200f, y + 65f, paint)
        paint.isFakeBoldText = false

        // Built-in 'L' LED (Pin 13)
        paint.style = Paint.Style.FILL
        if (isPin13LedOn) {
            paint.color = Color.parseColor("#FFD600") // Glowing amber
            canvas.drawCircle(x + 300f, y + 70f, 9f, paint)
            paint.color = Color.parseColor("#FFF59D")
            canvas.drawCircle(x + 300f, y + 70f, 5f, paint)
        } else {
            paint.color = Color.parseColor("#5D4037")
            canvas.drawCircle(x + 300f, y + 70f, 6f, paint)
        }
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("L", x + 300f, y + 88f, paint)

        // 'ON' Power LED
        if (isPowerLedOn) {
            paint.color = Color.parseColor("#00E676") // Green
            canvas.drawCircle(x + 270f, y + 70f, 7f, paint)
        } else {
            paint.color = Color.parseColor("#1B5E20")
            canvas.drawCircle(x + 270f, y + 70f, 5f, paint)
        }
        paint.color = Color.WHITE
        paint.textSize = 10f
        canvas.drawText("ON", x + 270f, y + 88f, paint)

        // Draw Header Rails (Black plastic strips)
        paint.color = Color.parseColor("#212121")
        // Top header strip
        canvas.drawRect(x + 30f, y + 8f, x + 370f, y + 26f, paint)
        // Bottom left header strip
        canvas.drawRect(x + 30f, y + height - 26f, x + 170f, y + height - 8f, paint)
        // Bottom right header strip
        canvas.drawRect(x + 210f, y + height - 26f, x + 350f, y + height - 8f, paint)

        // Draw Individual Pin Holes and Labels
        for (pin in pins) {
            val pinX = x + pin.relX
            val pinY = y + pin.relY

            // Pin metallic socket
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#CFD8DC")
            canvas.drawCircle(pinX, pinY, 5f, paint)

            paint.color = Color.parseColor("#37474F")
            canvas.drawCircle(pinX, pinY, 2.5f, paint)

            // Pin label text
            paint.color = Color.WHITE
            paint.textSize = 9f
            paint.textAlign = Paint.Align.CENTER
            if (pin.relY < height / 2) {
                // Top pins label below socket
                canvas.drawText(pin.label, pinX, pinY + 20f, paint)
            } else {
                // Bottom pins label above socket
                canvas.drawText(pin.label, pinX, pinY - 10f, paint)
            }
        }
    }
}
