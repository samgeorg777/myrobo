package com.myrobo.simulator.model

import org.json.JSONArray
import org.json.JSONObject

data class Project(
    var id: String = "default_project",
    var name: String = "My First Circuit",
    var code: String = DEFAULT_CODE,
    val components: MutableList<Component> = mutableListOf(),
    val wires: MutableList<Wire> = mutableListOf()
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("id", id)
        root.put("name", name)
        root.put("code", code)

        val compArray = JSONArray()
        for (comp in components) {
            val cObj = JSONObject()
            cObj.put("id", comp.id)
            cObj.put("name", comp.name)
            cObj.put("x", comp.x.toDouble())
            cObj.put("y", comp.y.toDouble())

            when (comp) {
                is ArduinoUno -> cObj.put("type", "ArduinoUno")
                is LedComponent -> {
                    cObj.put("type", "Led")
                    cObj.put("color", comp.colorName)
                }
                is ResistorComponent -> {
                    cObj.put("type", "Resistor")
                    cObj.put("ohms", comp.resistanceOhms)
                }
                is PushButtonComponent -> cObj.put("type", "PushButton")
                is PotentiometerComponent -> {
                    cObj.put("type", "Potentiometer")
                    cObj.put("value", comp.value)
                }
                is BuzzerComponent -> cObj.put("type", "Buzzer")
                is UltrasonicSensorComponent -> {
                    cObj.put("type", "Ultrasonic")
                    cObj.put("distance", comp.distanceCm)
                }
            }
            compArray.put(cObj)
        }
        root.put("components", compArray)

        val wireArray = JSONArray()
        for (wire in wires) {
            val wObj = JSONObject()
            wObj.put("id", wire.id)
            wObj.put("fromComp", wire.fromComponentId)
            wObj.put("fromPin", wire.fromPinId)
            wObj.put("toComp", wire.toComponentId)
            wObj.put("toPin", wire.toPinId)
            wObj.put("color", wire.colorHex)
            wireArray.put(wObj)
        }
        root.put("wires", wireArray)

        return root.toString(2)
    }

    companion object {
        const val DEFAULT_CODE = """// myrobo Microcontroller Simulator
// Target: Arduino Uno (ATmega328P)

void setup() {
  pinMode(13, OUTPUT);
  Serial.begin(9600);
  Serial.println("myrobo Simulator Initialized!");
}

void loop() {
  digitalWrite(13, HIGH);
  Serial.println("Pin 13: ON");
  delay(1000);
  
  digitalWrite(13, LOW);
  Serial.println("Pin 13: OFF");
  delay(1000);
}
"""

        fun fromJson(jsonStr: String): Project {
            val root = JSONObject(jsonStr)
            val project = Project(
                id = root.optString("id", "proj_${System.currentTimeMillis()}"),
                name = root.optString("name", "Untitled Project"),
                code = root.optString("code", DEFAULT_CODE)
            )

            val compArray = root.optJSONArray("components")
            if (compArray != null) {
                for (i in 0 until compArray.length()) {
                    val cObj = compArray.getJSONObject(i)
                    val id = cObj.getString("id")
                    val type = cObj.getString("type")
                    val x = cObj.getDouble("x").toFloat()
                    val y = cObj.getDouble("y").toFloat()

                    val comp: Component? = when (type) {
                        "ArduinoUno" -> ArduinoUno(id, x, y)
                        "Led" -> LedComponent(id, x, y, cObj.optString("color", "Red"))
                        "Resistor" -> ResistorComponent(id, x, y, cObj.optInt("ohms", 220))
                        "PushButton" -> PushButtonComponent(id, x, y)
                        "Potentiometer" -> PotentiometerComponent(id, x, y, cObj.optInt("value", 512))
                        "Buzzer" -> BuzzerComponent(id, x, y)
                        "Ultrasonic" -> UltrasonicSensorComponent(id, x, y, cObj.optInt("distance", 25))
                        else -> null
                    }
                    comp?.let { project.components.add(it) }
                }
            }

            val wireArray = root.optJSONArray("wires")
            if (wireArray != null) {
                for (i in 0 until wireArray.length()) {
                    val wObj = wireArray.getJSONObject(i)
                    val wire = Wire(
                        id = wObj.getString("id"),
                        fromComponentId = wObj.getString("fromComp"),
                        fromPinId = wObj.getString("fromPin"),
                        toComponentId = wObj.getString("toComp"),
                        toPinId = wObj.getString("toPin"),
                        colorHex = wObj.optString("color", "#00E5FF")
                    )
                    project.wires.add(wire)
                }
            }

            return project
        }
    }
}
