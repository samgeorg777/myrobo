package com.myrobo.simulator.model

data class Wire(
    val id: String,
    val fromComponentId: String,
    val fromPinId: String,
    val toComponentId: String,
    val toPinId: String,
    var colorHex: String = "#00E5FF",
    var state: PinState = PinState.LOW,
    var analogValue: Int = 0
)
