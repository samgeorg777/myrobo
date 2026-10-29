package com.myrobo.simulator.model

enum class PinType {
    DIGITAL,
    ANALOG,
    POWER_5V,
    POWER_3V3,
    GND
}

enum class PinMode {
    INPUT,
    OUTPUT,
    INPUT_PULLUP
}

enum class PinState {
    LOW,
    HIGH
}

data class Pin(
    val id: String,
    val label: String,
    val type: PinType,
    var mode: PinMode = PinMode.INPUT,
    var state: PinState = PinState.LOW,
    var analogValue: Int = 0,
    val relX: Float,
    val relY: Float
) {
    fun isPwmCapable(): Boolean {
        // Arduino Uno PWM pins: D3, D5, D6, D9, D10, D11
        return label in listOf("~3", "~5", "~6", "~9", "~10", "~11", "3", "5", "6", "9", "10", "11")
    }
}
