package com.myrobo.simulator.engine

import com.myrobo.simulator.model.ArduinoUno
import com.myrobo.simulator.model.PinMode
import com.myrobo.simulator.model.PinState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArduinoCodeEngine(
    private val getArduino: () -> ArduinoUno?
) {

    private var executionJob: Job? = null
    var isRunning: Boolean = false
        private set

    var onSerialOutput: ((String) -> Unit)? = null
    var onError: ((String, Int) -> Unit)? = null
    var onStatusChanged: ((Boolean) -> Unit)? = null

    private val variables = mutableMapOf<String, Any>()
    private var startTimeMillis = 0L

    fun start(code: String, scope: CoroutineScope) {
        stop()
        variables.clear()
        startTimeMillis = System.currentTimeMillis()

        executionJob = scope.launch(Dispatchers.Default) {
            try {
                isRunning = true
                withContext(Dispatchers.Main) { onStatusChanged?.invoke(true) }

                // 1. Pre-process and validate code
                val (setupCode, loopCode) = parseFunctions(code)

                // 2. Execute setup()
                executeBlock(setupCode)

                // 3. Repeatedly execute loop()
                while (isActive && isRunning) {
                    executeBlock(loopCode)
                    delay(10) // Prevents 100% CPU lock in tight loops
                }

            } catch (e: CancellationException) {
                // Normal termination
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError?.invoke(e.message ?: "Execution error", -1)
                }
            } finally {
                isRunning = false
                withContext(Dispatchers.Main) { onStatusChanged?.invoke(false) }
            }
        }
    }

    fun stop() {
        isRunning = false
        executionJob?.cancel()
        executionJob = null
        onStatusChanged?.invoke(false)
    }

    private fun parseFunctions(source: String): Pair<List<String>, List<String>> {
        // Strip block and line comments
        val cleanCode = source
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .lines()
            .map { it.replace(Regex("//.*$"), "").trim() }

        val setupLines = mutableListOf<String>()
        val loopLines = mutableListOf<String>()

        var currentMode = 0 // 0: outside, 1: setup, 2: loop
        var bracketDepth = 0

        for (line in cleanCode) {
            if (line.isEmpty()) continue

            if (line.contains("void setup()")) {
                currentMode = 1
            } else if (line.contains("void loop()")) {
                currentMode = 2
            }

            for (ch in line) {
                if (ch == '{') bracketDepth++
                else if (ch == '}') {
                    bracketDepth--
                    if (bracketDepth == 0) currentMode = 0
                }
            }

            // Exclude the header line itself
            if (currentMode == 1 && !line.contains("void setup()")) {
                val cleaned = line.replace("{", "").replace("}", "").trim()
                if (cleaned.isNotEmpty()) setupLines.add(cleaned)
            } else if (currentMode == 2 && !line.contains("void loop()")) {
                val cleaned = line.replace("{", "").replace("}", "").trim()
                if (cleaned.isNotEmpty()) loopLines.add(cleaned)
            }
        }

        return Pair(setupLines, loopLines)
    }

    private suspend fun executeBlock(lines: List<String>) {
        var i = 0
        while (i < lines.size && isRunning) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("//")) {
                i++
                continue
            }

            // Handle delays
            if (line.startsWith("delay(")) {
                val msStr = line.substringAfter("delay(").substringBefore(")").trim()
                val ms = resolveInt(msStr)
                delay(ms.toLong())
                i++
                continue
            }

            // Handle pinMode
            if (line.startsWith("pinMode(")) {
                val args = line.substringAfter("pinMode(").substringBefore(")").split(",")
                if (args.size == 2) {
                    val pinId = parsePinId(args[0].trim())
                    val mode = when (args[1].trim().uppercase()) {
                        "OUTPUT" -> PinMode.OUTPUT
                        "INPUT_PULLUP" -> PinMode.INPUT_PULLUP
                        else -> PinMode.INPUT
                    }
                    getArduino()?.getPinById(pinId)?.mode = mode
                }
                i++
                continue
            }

            // Handle digitalWrite
            if (line.startsWith("digitalWrite(")) {
                val args = line.substringAfter("digitalWrite(").substringBefore(")").split(",")
                if (args.size == 2) {
                    val pinId = parsePinId(args[0].trim())
                    val valStr = args[1].trim().uppercase()
                    val state = if (valStr == "HIGH" || valStr == "1") PinState.HIGH else PinState.LOW
                    val pin = getArduino()?.getPinById(pinId)
                    pin?.state = state
                    pin?.analogValue = if (state == PinState.HIGH) 255 else 0
                }
                i++
                continue
            }

            // Handle analogWrite (PWM)
            if (line.startsWith("analogWrite(")) {
                val args = line.substringAfter("analogWrite(").substringBefore(")").split(",")
                if (args.size == 2) {
                    val pinId = parsePinId(args[0].trim())
                    val value = resolveInt(args[1].trim()).coerceIn(0, 255)
                    val pin = getArduino()?.getPinById(pinId)
                    pin?.analogValue = value
                    pin?.state = if (value > 0) PinState.HIGH else PinState.LOW
                }
                i++
                continue
            }

            // Handle Serial prints
            if (line.startsWith("Serial.println(") || line.startsWith("Serial.print(")) {
                val isLn = line.startsWith("Serial.println(")
                val content = line.substringAfter("(").substringBeforeLast(")").trim()
                val output = resolveString(content) + if (isLn) "\n" else ""
                withContext(Dispatchers.Main) {
                    onSerialOutput?.invoke(output)
                }
                i++
                continue
            }

            if (line.startsWith("Serial.begin(")) {
                // Serial initialized
                i++
                continue
            }

            // Variable assignments: e.g. int val = digitalRead(2);
            if (line.contains("=")) {
                executeAssignment(line)
            }

            i++
        }
    }

    private fun executeAssignment(line: String) {
        val parts = line.split("=", limit = 2)
        if (parts.size != 2) return

        var varName = parts[0].trim()
        if (varName.startsWith("int ")) varName = varName.substring(4).trim()
        else if (varName.startsWith("bool ")) varName = varName.substring(5).trim()
        else if (varName.startsWith("float ")) varName = varName.substring(6).trim()
        else if (varName.startsWith("String ")) varName = varName.substring(7).trim()

        val expr = parts[1].replace(";", "").trim()

        // digitalRead(pin)
        if (expr.startsWith("digitalRead(")) {
            val pinId = parsePinId(expr.substringAfter("digitalRead(").substringBefore(")"))
            val pin = getArduino()?.getPinById(pinId)
            val res = if (pin?.state == PinState.HIGH) 1 else 0
            variables[varName] = res
            return
        }

        // analogRead(pin)
        if (expr.startsWith("analogRead(")) {
            val pinId = parsePinId(expr.substringAfter("analogRead(").substringBefore(")"))
            val pin = getArduino()?.getPinById(pinId)
            val res = pin?.analogValue ?: 0
            variables[varName] = res
            return
        }

        // millis()
        if (expr == "millis()") {
            variables[varName] = (System.currentTimeMillis() - startTimeMillis).toInt()
            return
        }

        // Literal int or variable
        variables[varName] = resolveInt(expr)
    }

    private fun resolveInt(token: String): Int {
        val t = token.trim()
        t.toIntOrNull()?.let { return it }
        val v = variables[t]
        if (v is Number) return v.toInt()
        return 0
    }

    private fun resolveString(token: String): String {
        val t = token.trim()
        if (t.startsWith("\"") && t.endsWith("\"") && t.length >= 2) {
            return t.substring(1, t.length - 1)
        }
        val v = variables[t]
        if (v != null) return v.toString()
        return t
    }

    private fun parsePinId(pinToken: String): String {
        val clean = pinToken.trim().lowercase().replace("d", "")
        return when (clean) {
            "a0" -> "a0"
            "a1" -> "a1"
            "a2" -> "a2"
            "a3" -> "a3"
            "a4" -> "a4"
            "a5" -> "a5"
            else -> "d${clean.toIntOrNull() ?: 13}"
        }
    }
}
