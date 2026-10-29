package com.myrobo.simulator.storage

import android.content.Context
import com.myrobo.simulator.model.*
import java.io.File

class ProjectStorage(private val context: Context) {

    private val projectsDir: File
        get() {
            val dir = File(context.filesDir, "projects")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun saveProject(project: Project) {
        val file = File(projectsDir, "${project.id}.json")
        file.writeText(project.toJson())
    }

    fun loadProject(id: String): Project? {
        val file = File(projectsDir, "$id.json")
        if (!file.exists()) return null
        return try {
            Project.fromJson(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    fun listProjects(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        projectsDir.listFiles { f -> f.extension == "json" }?.forEach { f ->
            try {
                val proj = Project.fromJson(f.readText())
                list.add(Pair(proj.id, proj.name))
            } catch (e: Exception) {
                // skip corrupted
            }
        }
        return list
    }

    fun deleteProject(id: String): Boolean {
        val file = File(projectsDir, "$id.json")
        return if (file.exists()) file.delete() else false
    }

    fun createPresetProjects(): List<Project> {
        return listOf(
            createBlinkPreset(),
            createButtonPreset(),
            createPotentiometerPreset(),
            createUltrasonicPreset()
        )
    }

    private fun createBlinkPreset(): Project {
        val p = Project(
            id = "preset_blink",
            name = "1. Classic Blink (Pin 13)",
            code = """// 1. Classic Blink Example
// Controls the built-in LED on Pin 13

void setup() {
  pinMode(13, OUTPUT);
  Serial.begin(9600);
  Serial.println("Starting Blink Demo...");
}

void loop() {
  digitalWrite(13, HIGH);
  Serial.println("LED State: HIGH");
  delay(1000);

  digitalWrite(13, LOW);
  Serial.println("LED State: LOW");
  delay(1000);
}
"""
        )
        val uno = ArduinoUno("uno_1", 120f, 150f)
        val led = LedComponent("led_red", 550f, 150f, "Red")
        val res = ResistorComponent("res_220", 550f, 280f, 220)

        // Wire 1: Arduino D13 -> LED Anode
        val w1 = Wire("w1", "uno_1", "d13", "led_red", "led_red_anode", "#FF5252")
        // Wire 2: LED Cathode -> Resistor Pin 1
        val w2 = Wire("w2", "led_red", "led_red_cathode", "res_220", "res_220_p1", "#448AFF")
        // Wire 3: Resistor Pin 2 -> Arduino GND
        val w3 = Wire("w3", "res_220", "res_220_p2", "uno_1", "gnd1", "#212121")

        p.components.addAll(listOf(uno, led, res))
        p.wires.addAll(listOf(w1, w2, w3))
        return p
    }

    private fun createButtonPreset(): Project {
        val p = Project(
            id = "preset_button",
            name = "2. Pushbutton Interactive",
            code = """// 2. Interactive Pushbutton
// Tap the button on the canvas to turn on the LED!

int buttonPin = 2;
int ledPin = 13;

void setup() {
  pinMode(buttonPin, INPUT);
  pinMode(ledPin, OUTPUT);
  Serial.begin(9600);
  Serial.println("Press the button on screen!");
}

void loop() {
  int buttonState = digitalRead(buttonPin);
  if (buttonState == HIGH) {
    digitalWrite(ledPin, HIGH);
    Serial.println("Button PRESSED -> LED ON");
  } else {
    digitalWrite(ledPin, LOW);
  }
  delay(50);
}
"""
        )
        val uno = ArduinoUno("uno_1", 100f, 150f)
        val btn = PushButtonComponent("btn_1", 540f, 180f)
        val led = LedComponent("led_green", 540f, 300f, "Green")

        // 5V to button 1A
        val w1 = Wire("w1", "uno_1", "5v", "btn_1", "btn_1_1a", "#FF5252")
        // Button 2A to Arduino D2
        val w2 = Wire("w2", "btn_1", "btn_1_2a", "uno_1", "d2", "#00E676")
        // Arduino D13 to LED Anode
        val w3 = Wire("w3", "uno_1", "d13", "led_green", "led_green_anode", "#FFD600")
        // LED Cathode to GND
        val w4 = Wire("w4", "led_green", "led_green_cathode", "uno_1", "gnd1", "#212121")

        p.components.addAll(listOf(uno, btn, led))
        p.wires.addAll(listOf(w1, w2, w3, w4))
        return p
    }

    private fun createPotentiometerPreset(): Project {
        val p = Project(
            id = "preset_pot",
            name = "3. Potentiometer Dimmer (PWM)",
            code = """// 3. Potentiometer Analog Read & PWM Dimmer
// Rotate the dial on canvas to dim or brighten the LED!

int potPin = A0;
int ledPin = 11;

void setup() {
  pinMode(ledPin, OUTPUT);
  Serial.begin(9600);
}

void loop() {
  int val = analogRead(potPin);
  int brightness = val / 4; // Map 0-1023 to 0-255 PWM
  analogWrite(ledPin, brightness);

  Serial.print("Pot ADC: ");
  Serial.println(val);
  delay(100);
}
"""
        )
        val uno = ArduinoUno("uno_1", 100f, 150f)
        val pot = PotentiometerComponent("pot_1", 530f, 160f, 600)
        val led = LedComponent("led_blue", 530f, 300f, "Blue")

        // Pot VCC to 5V
        val w1 = Wire("w1", "uno_1", "5v", "pot_1", "pot_1_vcc", "#FF5252")
        // Pot GND to GND
        val w2 = Wire("w2", "uno_1", "gnd1", "pot_1", "pot_1_gnd", "#212121")
        // Pot OUT to A0
        val w3 = Wire("w3", "pot_1", "pot_1_sig", "uno_1", "a0", "#00E5FF")
        // D11 (PWM) to LED Anode
        val w4 = Wire("w4", "uno_1", "d11", "led_blue", "led_blue_anode", "#448AFF")
        // LED Cathode to GND
        val w5 = Wire("w5", "led_blue", "led_blue_cathode", "uno_1", "gnd2", "#212121")

        p.components.addAll(listOf(uno, pot, led))
        p.wires.addAll(listOf(w1, w2, w3, w4, w5))
        return p
    }

    private fun createUltrasonicPreset(): Project {
        val p = Project(
            id = "preset_ultrasonic",
            name = "4. Ultrasonic Distance Alarm",
            code = """// 4. Ultrasonic Proximity Radar
// Slide the distance slider on canvas to test alarm!

int trigPin = 9;
int echoPin = 10;
int buzzerPin = 8;

void setup() {
  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);
  pinMode(buzzerPin, OUTPUT);
  Serial.begin(9600);
  Serial.println("Radar System Armed");
}

void loop() {
  int distance = analogRead(echoPin);
  Serial.print("Target Distance: ");
  Serial.println(distance);

  if (distance < 20) {
    digitalWrite(buzzerPin, HIGH); // Alarm ON
    Serial.println("WARNING: OBJECT TOO CLOSE!");
  } else {
    digitalWrite(buzzerPin, LOW);
  }
  delay(150);
}
"""
        )
        val uno = ArduinoUno("uno_1", 80f, 150f)
        val sensor = UltrasonicSensorComponent("sonar_1", 510f, 130f, 15)
        val buzzer = BuzzerComponent("buzzer_1", 520f, 270f)

        // 5V to VCC
        val w1 = Wire("w1", "uno_1", "5v", "sonar_1", "sonar_1_vcc", "#FF5252")
        // GND to GND
        val w2 = Wire("w2", "uno_1", "gnd1", "sonar_1", "sonar_1_gnd", "#212121")
        // D9 to TRIG
        val w3 = Wire("w3", "uno_1", "d9", "sonar_1", "sonar_1_trig", "#00E676")
        // D10 to ECHO
        val w4 = Wire("w4", "sonar_1", "sonar_1_echo", "uno_1", "d10", "#00E5FF")
        // D8 to Buzzer +
        val w5 = Wire("w5", "uno_1", "d8", "buzzer_1", "buzzer_1_pos", "#FFD600")
        // Buzzer - to GND
        val w6 = Wire("w6", "buzzer_1", "buzzer_1_neg", "uno_1", "gnd2", "#212121")

        p.components.addAll(listOf(uno, sensor, buzzer))
        p.wires.addAll(listOf(w1, w2, w3, w4, w5, w6))
        return p
    }
}
