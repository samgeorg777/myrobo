package com.myrobo.simulator.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.myrobo.simulator.R
import com.myrobo.simulator.databinding.ActivityMainBinding
import com.myrobo.simulator.databinding.ItemProjectBinding
import com.myrobo.simulator.engine.ArduinoCodeEngine
import com.myrobo.simulator.engine.CircuitEngine
import com.myrobo.simulator.model.*
import com.myrobo.simulator.storage.ProjectStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var projectStorage: ProjectStorage
    private lateinit var circuitEngine: CircuitEngine
    private lateinit var codeEngine: ArduinoCodeEngine

    private var currentProject = Project()
    private var simulationLoopJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        projectStorage = ProjectStorage(this)

        // Initialize with default or starter project
        val defaultProject = projectStorage.createPresetProjects().first()
        loadProjectIntoWorkspace(defaultProject)

        setupEngines()
        setupBottomNavigation()
        setupToolbarControls()
        setupCodeSnippets()
        setupProjectList()
    }

    private fun setupEngines() {
        circuitEngine = CircuitEngine(currentProject.components, currentProject.wires)
        binding.circuitCanvas.engine = circuitEngine

        codeEngine = ArduinoCodeEngine { circuitEngine.getArduino() }

        codeEngine.onSerialOutput = { output ->
            runOnUiThread {
                binding.tvSerialLog.append(output)
                binding.scrollSerial.post {
                    binding.scrollSerial.fullScroll(View.FOCUS_DOWN)
                }
            }
        }

        codeEngine.onError = { error, _ ->
            runOnUiThread {
                binding.tvSerialLog.append("\n[COMPILER ERROR] $error\n")
                Toast.makeText(this, "Simulation Error: $error", Toast.LENGTH_SHORT).show()
                stopSimulation()
            }
        }

        codeEngine.onStatusChanged = { isRunning ->
            runOnUiThread {
                updateRunButtonUi(isRunning)
            }
        }

        binding.circuitCanvas.onWireConnected = { wire ->
            Toast.makeText(this, "Wire Connected!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupToolbarControls() {
        binding.btnRunStop.setOnClickListener {
            if (codeEngine.isRunning) {
                stopSimulation()
            } else {
                startSimulation()
            }
        }

        binding.btnExamples.setOnClickListener {
            showExamplesDialog()
        }

        binding.fabAddComponent.setOnClickListener {
            showAddComponentDialog()
        }

        binding.btnClearSerial.setOnClickListener {
            binding.tvSerialLog.text = "[Serial Monitor Cleared]\n"
        }

        binding.btnSaveCurrent.setOnClickListener {
            showSaveDialog()
        }
    }

    private fun startSimulation() {
        // Synchronize editor code into project
        currentProject.code = binding.codeEditor.text.toString()

        binding.tvSerialLog.append("\n--- Starting Simulation ---\n")
        codeEngine.start(currentProject.code, lifecycleScope)

        // Start hardware refresh loop (60 FPS = 16ms)
        simulationLoopJob?.cancel()
        simulationLoopJob = lifecycleScope.launch {
            while (isActive) {
                binding.circuitCanvas.stepSimulation(0.016f)
                delay(16)
            }
        }
    }

    private fun stopSimulation() {
        codeEngine.stop()
        simulationLoopJob?.cancel()
        simulationLoopJob = null
        binding.circuitCanvas.stepSimulation(0f)
        binding.tvSerialLog.append("\n--- Simulation Stopped ---\n")
    }

    private fun updateRunButtonUi(isRunning: Boolean) {
        if (isRunning) {
            binding.btnRunStop.text = "■ STOP"
            binding.btnRunStop.backgroundTintList = ContextCompat.getColorStateList(this, R.color.stop_red)
        } else {
            binding.btnRunStop.text = "▶ RUN"
            binding.btnRunStop.backgroundTintList = ContextCompat.getColorStateList(this, R.color.run_green)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            binding.circuitCanvas.visibility = View.GONE
            binding.codeStudioLayout.visibility = View.GONE
            binding.serialMonitorLayout.visibility = View.GONE
            binding.projectsLayout.visibility = View.GONE
            binding.fabAddComponent.visibility = View.GONE

            when (item.itemId) {
                R.id.nav_circuit -> {
                    binding.circuitCanvas.visibility = View.VISIBLE
                    binding.fabAddComponent.visibility = View.VISIBLE
                    true
                }
                R.id.nav_code -> {
                    binding.codeStudioLayout.visibility = View.VISIBLE
                    true
                }
                R.id.nav_serial -> {
                    binding.serialMonitorLayout.visibility = View.VISIBLE
                    true
                }
                R.id.nav_projects -> {
                    binding.projectsLayout.visibility = View.VISIBLE
                    refreshProjectList()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupCodeSnippets() {
        binding.chipSnippetDigitalWrite.setOnClickListener {
            insertSnippetAtCursor("digitalWrite(13, HIGH);\n")
        }
        binding.chipSnippetDigitalRead.setOnClickListener {
            insertSnippetAtCursor("int state = digitalRead(2);\n")
        }
        binding.chipSnippetAnalogRead.setOnClickListener {
            insertSnippetAtCursor("int sensorVal = analogRead(A0);\n")
        }
        binding.chipSnippetDelay.setOnClickListener {
            insertSnippetAtCursor("delay(1000);\n")
        }
        binding.chipSnippetSerial.setOnClickListener {
            insertSnippetAtCursor("Serial.println(\"Hello myrobo\");\n")
        }
    }

    private fun insertSnippetAtCursor(snippet: String) {
        val start = binding.codeEditor.selectionStart.coerceAtLeast(0)
        val end = binding.codeEditor.selectionEnd.coerceAtLeast(0)
        binding.codeEditor.text?.replace(start, end, snippet)
    }

    private fun showAddComponentDialog() {
        val items = arrayOf(
            "LED (Red)",
            "LED (Green)",
            "LED (Blue)",
            "LED (Yellow)",
            "Resistor (220Ω)",
            "Push Button",
            "Potentiometer",
            "Piezo Buzzer",
            "Ultrasonic Sensor (HC-SR04)",
            "Arduino Uno (MCU)"
        )

        AlertDialog.Builder(this)
            .setTitle("Add Component to Board")
            .setItems(items) { _, which ->
                val newComp: Component = when (which) {
                    0 -> LedComponent("led_${System.currentTimeMillis()}", 300f, 300f, "Red")
                    1 -> LedComponent("led_${System.currentTimeMillis()}", 300f, 300f, "Green")
                    2 -> LedComponent("led_${System.currentTimeMillis()}", 300f, 300f, "Blue")
                    3 -> LedComponent("led_${System.currentTimeMillis()}", 300f, 300f, "Yellow")
                    4 -> ResistorComponent("res_${System.currentTimeMillis()}", 300f, 300f, 220)
                    5 -> PushButtonComponent("btn_${System.currentTimeMillis()}", 300f, 300f)
                    6 -> PotentiometerComponent("pot_${System.currentTimeMillis()}", 300f, 300f, 512)
                    7 -> BuzzerComponent("buzzer_${System.currentTimeMillis()}", 300f, 300f)
                    8 -> UltrasonicSensorComponent("sonar_${System.currentTimeMillis()}", 300f, 300f, 25)
                    else -> ArduinoUno("uno_${System.currentTimeMillis()}", 100f, 150f)
                }
                currentProject.components.add(newComp)
                binding.circuitCanvas.invalidate()
                Toast.makeText(this, "${newComp.name} Added!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showExamplesDialog() {
        val presets = projectStorage.createPresetProjects()
        val names = presets.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Load Example Circuit")
            .setItems(names) { _, which ->
                stopSimulation()
                loadProjectIntoWorkspace(presets[which])
                Toast.makeText(this, "Loaded: ${presets[which].name}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSaveDialog() {
        val input = EditText(this).apply {
            setText(currentProject.name)
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle("Save Project Locally")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                currentProject.name = input.text.toString().trim()
                currentProject.code = binding.codeEditor.text.toString()
                projectStorage.saveProject(currentProject)
                binding.tvProjectTitle.text = currentProject.name
                refreshProjectList()
                Toast.makeText(this, "Saved project '${currentProject.name}'", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadProjectIntoWorkspace(project: Project) {
        currentProject = project
        binding.tvProjectTitle.text = project.name
        binding.codeEditor.setText(project.code)

        circuitEngine = CircuitEngine(currentProject.components, currentProject.wires)
        binding.circuitCanvas.engine = circuitEngine
        binding.circuitCanvas.resetView()
    }

    private fun setupProjectList() {
        binding.rvProjects.layoutManager = LinearLayoutManager(this)
        refreshProjectList()
    }

    private fun refreshProjectList() {
        val projects = projectStorage.listProjects()
        binding.rvProjects.adapter = ProjectAdapter(projects,
            onOpen = { id ->
                stopSimulation()
                val loaded = projectStorage.loadProject(id)
                if (loaded != null) {
                    loadProjectIntoWorkspace(loaded)
                    binding.bottomNavigation.selectedItemId = R.id.nav_circuit
                    Toast.makeText(this, "Opened '${loaded.name}'", Toast.LENGTH_SHORT).show()
                }
            },
            onDelete = { id ->
                projectStorage.deleteProject(id)
                refreshProjectList()
                Toast.makeText(this, "Project deleted", Toast.LENGTH_SHORT).show()
            }
        )
    }

    inner class ProjectAdapter(
        private val list: List<Pair<String, String>>,
        private val onOpen: (String) -> Unit,
        private val onDelete: (String) -> Unit
    ) : RecyclerView.Adapter<ProjectAdapter.ViewHolder>() {

        inner class ViewHolder(val itemBinding: ItemProjectBinding) :
            RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemProjectBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (id, name) = list[position]
            holder.itemBinding.tvProjectName.text = name
            holder.itemBinding.tvProjectId.text = "Saved locally"
            holder.itemBinding.btnLoad.setOnClickListener { onOpen(id) }
            holder.itemBinding.btnDelete.setOnClickListener { onDelete(id) }
        }

        override fun getItemCount() = list.size
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSimulation()
    }
}
