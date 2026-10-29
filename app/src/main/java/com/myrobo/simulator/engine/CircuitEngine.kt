package com.myrobo.simulator.engine

import com.myrobo.simulator.model.ArduinoUno
import com.myrobo.simulator.model.Component
import com.myrobo.simulator.model.Pin
import com.myrobo.simulator.model.PinMode
import com.myrobo.simulator.model.PinState
import com.myrobo.simulator.model.PinType
import com.myrobo.simulator.model.Wire

class CircuitEngine(
    val components: MutableList<Component>,
    val wires: MutableList<Wire>
) {

    fun tick(dt: Float) {
        // 1. First pass: Update microcontrollers and active components
        for (comp in components) {
            comp.update(dt)
        }

        // 2. Propagate signals through wires
        for (wire in wires) {
            val fromComp = findComponent(wire.fromComponentId)
            val toComp = findComponent(wire.toComponentId)
            if (fromComp == null || toComp == null) continue

            val pFrom = fromComp.getPinById(wire.fromPinId) ?: continue
            val pTo = toComp.getPinById(wire.toPinId) ?: continue

            propagate(pFrom, pTo, wire)
        }

        // 3. Second pass: re-update components with propagated states
        for (comp in components) {
            comp.update(dt)
        }
    }

    private fun propagate(p1: Pin, p2: Pin, wire: Wire) {
        // Power sources take absolute precedence
        if (p1.type == PinType.POWER_5V || (p1.mode == PinMode.OUTPUT && p1.state == PinState.HIGH)) {
            p2.state = PinState.HIGH
            p2.analogValue = if (p1.analogValue > 0) p1.analogValue else 1023
            wire.state = PinState.HIGH
            wire.analogValue = p2.analogValue
            return
        }

        if (p2.type == PinType.POWER_5V || (p2.mode == PinMode.OUTPUT && p2.state == PinState.HIGH)) {
            p1.state = PinState.HIGH
            p1.analogValue = if (p2.analogValue > 0) p2.analogValue else 1023
            wire.state = PinState.HIGH
            wire.analogValue = p1.analogValue
            return
        }

        // Ground connections
        if (p1.type == PinType.GND || (p1.mode == PinMode.OUTPUT && p1.state == PinState.LOW)) {
            p2.state = PinState.LOW
            p2.analogValue = 0
            wire.state = PinState.LOW
            wire.analogValue = 0
            return
        }

        if (p2.type == PinType.GND || (p2.mode == PinMode.OUTPUT && p2.state == PinState.LOW)) {
            p1.state = PinState.LOW
            p1.analogValue = 0
            wire.state = PinState.LOW
            wire.analogValue = 0
            return
        }

        // Bidirectional transfer for passive analog signals (e.g. potentiometer to analog pin)
        if (p1.analogValue > 0 && p2.analogValue == 0) {
            p2.analogValue = p1.analogValue
            p2.state = p1.state
            wire.state = p1.state
            wire.analogValue = p1.analogValue
        } else if (p2.analogValue > 0 && p1.analogValue == 0) {
            p1.analogValue = p2.analogValue
            p1.state = p2.state
            wire.state = p2.state
            wire.analogValue = p2.analogValue
        }
    }

    fun findComponent(id: String): Component? {
        return components.find { it.id == id }
    }

    fun getArduino(): ArduinoUno? {
        return components.filterIsInstance<ArduinoUno>().firstOrNull()
    }
}
