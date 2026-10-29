# ⚡ myrobo — Microcontroller & Circuit Simulator for Android

> **For engineering students who don't have the budget to buy microcontrollers and laptops.** Learn embedded systems, C/C++ coding, and circuit wiring right on your Android phone!

[![Build myrobo APK](https://github.com/samgeorg777/myrobo/actions/workflows/build-apk.yml/badge.svg)](https://github.com/samgeorg777/myrobo/actions/workflows/build-apk.yml)
[![GitHub Release](https://img.shields.io/github/v/release/samgeorg777/myrobo?color=00E5FF&label=Latest%20APK)](https://github.com/samgeorg777/myrobo/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-00E676.svg)](LICENSE)

---

## 🎯 Target Audience & Mission

Engineering students often face high financial barriers when beginning electronics and microcontrollers courses:
* Laptops are costly.
* Microcontroller starter kits (Arduino, sensors, breadboards, jumper wires) are expensive and fragile.

**myrobo** solves this by converting **any budget Android phone** into a full microcontroller laboratory with **zero hardware cost**, **zero cloud logins**, and **under 60MB RAM footprint**.

---

## ✨ Features

### 1. 🔌 Interactive Microcontroller & Circuit Board
* **Virtual Arduino Uno (ATmega328P):** 14 Digital I/O pins (with PWM `~` pins 3, 5, 6, 9, 10, 11), 6 Analog inputs (`A0`–`A5`), 5V / 3.3V power rails, multiple GND pins, and built-in Pin 13 LED.
* **Touch Wiring System:** Tap any pin and tap another pin to connect with realistic curved wires. Wires are automatically color-coded (Red for 5V, Black for GND, Cyan for Analog, Green for Digital).
* **Multi-Touch Canvas:** Hardware-accelerated 60 FPS pinch-to-zoom and panning.

### 2. 💻 Offline C/C++ & Arduino Code Engine
* **Full Code Studio:** Monospace IDE with syntax highlighting for keywords, types, numbers, and comments.
* **Arduino C API Support:**
  * `pinMode(pin, INPUT / OUTPUT / INPUT_PULLUP)`
  * `digitalWrite(pin, HIGH / LOW)`
  * `digitalRead(pin)`
  * `analogWrite(pin, 0..255)` (PWM)
  * `analogRead(pin)` (0..1023)
  * `delay(ms)`
  * `millis()`
  * `Serial.begin(baud)` & `Serial.println(msg)`
* **Safe Asynchronous Runtime:** Never freezes the phone or drains excessive battery.

### 3. 🧩 Component Library (Beginner to Intermediate)
* **Arduino Uno Board** (ATmega328P microcontroller)
* **LEDs:** Multi-color (Red, Green, Blue, Yellow) with realistic bloom glow and PWM dimming.
* **Resistors:** 220Ω, 1kΩ, 10kΩ with color band visualization.
* **Tactile Push Button:** Tap the button directly on the canvas during simulation to toggle inputs!
* **Rotary Potentiometer:** Interactive rotary knob generating 0–1023 analog signals.
* **Piezo Buzzer:** Soundwave frequency animation and audio feedback.
* **Ultrasonic Distance Sensor (HC-SR04):** Touch distance slider from 2cm to 400cm to simulate obstacle avoidance.

### 4. 📊 Live Serial Monitor
* Real-time serial terminal showing `Serial.println()` readings for debugging sensors and logic.

### 5. 💾 100% Offline Project Storage (No Login / No Sign-up)
* Save and load unlimited projects locally in private JSON files.
* Includes 4 built-in starter templates:
  1. *Classic Blink (Pin 13)*
  2. *Interactive Pushbutton LED*
  3. *Potentiometer Dimmer (PWM)*
  4. *Ultrasonic Proximity Radar Alarm*

### 6. ⚡ Ultra-Low Memory & Battery Consumption
* Built purely with modern Android Kotlin & vector graphics.
* No heavy webview or multi-gigabyte emulators.
* Works smoothly on 2GB & 3GB RAM budget Android smartphones.

---

## 📱 How to Download & Install

1. Go to the [**GitHub Releases**](https://github.com/samgeorg777/myrobo/releases) page.
2. Under the latest release, download **`app-debug.apk`**.
3. Tap the downloaded file on your Android device to install.

---

## 🛠 Project Architecture

```
myrobo/
├── .github/workflows/
│   └── build-apk.yml               # Automated APK compilation & GitHub Release
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/myrobo/simulator/
│   │   │   ├── model/              # Pin, Wire, Component, ArduinoUno, Led, Resistor...
│   │   │   ├── engine/             # CircuitEngine (wire propagation) & ArduinoCodeEngine
│   │   │   ├── storage/            # ProjectStorage (offline JSON & starter presets)
│   │   │   └── ui/                 # MainActivity, CircuitCanvasView, CodeEditorView
│   │   └── res/
│   │       ├── layout/             # activity_main.xml, item_project.xml
│   │       ├── values/             # strings.xml, colors.xml, themes.xml
│   │       └── drawable/           # ic_launcher.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

---

## 👨‍💻 Contributing & Roadmap
- [x] Arduino Uno ATmega328P virtual board
- [x] Touch wiring with realistic Bezier curves
- [x] Offline C/C++ Arduino parser & executor
- [x] LED, Resistor, Button, Potentiometer, Buzzer, Ultrasonic sensor
- [x] Serial Monitor terminal
- [x] Offline JSON save/load system
- [ ] 16x2 I2C LCD Display component
- [ ] SG90 Micro Servo Motor support

---

## 📄 License
Licensed under the [MIT License](LICENSE).
