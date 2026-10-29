package com.myrobo.simulator.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.myrobo.simulator.engine.CircuitEngine
import com.myrobo.simulator.model.*
import kotlin.math.max
import kotlin.math.min

class CircuitCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var engine: CircuitEngine? = null

    // Transform Matrix (Zoom & Pan)
    private var scaleFactor = 1.0f
    private var panX = 0f
    private var panY = 0f

    // Multi-touch gestures
    private val scaleDetector: ScaleGestureDetector

    // Interaction states
    private var draggedComponent: Component? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    private var activeInteractiveComponent: Component? = null

    // Wire connection state
    private var wiringStartComponent: Component? = null
    private var wiringStartPin: Pin? = null
    private var wiringCurrentX = 0f
    private var wiringCurrentY = 0f

    // Rendering Paints
    private val bgPaint = Paint().apply {
        color = Color.parseColor("#0F141C")
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#18202E")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val componentPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val wirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }

    private val wiringPreviewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#00E5FF")
        strokeCap = Paint.Cap.ROUND
    }

    private val wirePath = Path()

    // Status callback
    var onWireConnected: ((Wire) -> Unit)? = null
    var onComponentSelected: ((Component) -> Unit)? = null

    init {
        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = max(0.4f, min(scaleFactor, 2.5f))
                invalidate()
                return true
            }
        })
    }

    fun stepSimulation(dt: Float) {
        engine?.tick(dt)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw Background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Save canvas state before pan/zoom
        canvas.save()
        canvas.translate(panX, panY)
        canvas.scale(scaleFactor, scaleFactor)

        // 2. Draw Subtle Engineering Grid
        drawGrid(canvas)

        val activeEngine = engine
        if (activeEngine != null) {
            // 3. Draw All Wires
            for (wire in activeEngine.wires) {
                drawWire(canvas, wire)
            }

            // 4. Draw Active Wiring Line (while user drags between pins)
            if (wiringStartPin != null && wiringStartComponent != null) {
                val (sx, sy) = wiringStartComponent!!.getPinAbsolutePos(wiringStartPin!)
                canvas.drawLine(sx, sy, wiringCurrentX, wiringCurrentY, wiringPreviewPaint)
            }

            // 5. Draw All Components
            for (comp in activeEngine.components) {
                comp.draw(canvas, componentPaint)
            }
        }

        canvas.restore()
    }

    private fun drawGrid(canvas: Canvas) {
        val gridSize = 40f
        val left = -panX / scaleFactor - 200f
        val top = -panY / scaleFactor - 200f
        val right = (width - panX) / scaleFactor + 200f
        val bottom = (height - panY) / scaleFactor + 200f

        var x = (left - (left % gridSize))
        while (x < right) {
            canvas.drawLine(x, top, x, bottom, gridPaint)
            x += gridSize
        }

        var y = (top - (top % gridSize))
        while (y < bottom) {
            canvas.drawLine(left, y, right, y, gridPaint)
            y += gridSize
        }
    }

    private fun drawWire(canvas: Canvas, wire: Wire) {
        val fromComp = engine?.findComponent(wire.fromComponentId) ?: return
        val toComp = engine?.findComponent(wire.toComponentId) ?: return

        val p1 = fromComp.getPinById(wire.fromPinId) ?: return
        val p2 = toComp.getPinById(wire.toPinId) ?: return

        val (x1, y1) = fromComp.getPinAbsolutePos(p1)
        val (x2, y2) = toComp.getPinAbsolutePos(p2)

        wirePaint.color = Color.parseColor(wire.colorHex)
        wirePaint.strokeWidth = if (wire.state == PinState.HIGH) 5f else 3.5f

        // Smooth curved Bezier path for realistic physical wire bend
        wirePath.reset()
        wirePath.moveTo(x1, y1)
        val midX = (x1 + x2) / 2f
        val midY = (y1 + y2) / 2f + 35f // sag downward like real wire
        wirePath.quadTo(midX, midY, x2, y2)

        canvas.drawPath(wirePath, wirePaint)

        // Terminal dots
        componentPaint.style = Paint.Style.FILL
        componentPaint.color = wirePaint.color
        canvas.drawCircle(x1, y1, 4f, componentPaint)
        canvas.drawCircle(x2, y2, 4f, componentPaint)
    }

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isPanning = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        val worldX = (event.x - panX) / scaleFactor
        val worldY = (event.y - panY) / scaleFactor

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y

                val activeEngine = engine
                if (activeEngine != null) {
                    // Check if touching any pin first for wiring
                    for (comp in activeEngine.components.reversed()) {
                        val pin = comp.findPinAt(worldX, worldY, 35f)
                        if (pin != null) {
                            wiringStartComponent = comp
                            wiringStartPin = pin
                            wiringCurrentX = worldX
                            wiringCurrentY = worldY
                            invalidate()
                            return true
                        }
                    }

                    // Check if interacting with component directly (e.g. push button, pot)
                    for (comp in activeEngine.components.reversed()) {
                        if (comp.onTouchInteract(worldX, worldY)) {
                            activeInteractiveComponent = comp
                            invalidate()
                            return true
                        }
                    }

                    // Check if dragging component body
                    for (comp in activeEngine.components.reversed()) {
                        if (comp.containsPoint(worldX, worldY)) {
                            draggedComponent = comp
                            dragOffsetX = worldX - comp.x
                            dragOffsetY = worldY - comp.y
                            onComponentSelected?.invoke(comp)
                            return true
                        }
                    }
                }

                // Nothing hit -> start canvas pan
                isPanning = true
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (scaleDetector.isInProgress) {
                    return true
                }

                if (wiringStartPin != null) {
                    wiringCurrentX = worldX
                    wiringCurrentY = worldY
                    invalidate()
                    return true
                }

                if (activeInteractiveComponent != null) {
                    activeInteractiveComponent?.onTouchInteract(worldX, worldY)
                    invalidate()
                    return true
                }

                if (draggedComponent != null) {
                    draggedComponent!!.x = worldX - dragOffsetX
                    draggedComponent!!.y = worldY - dragOffsetY
                    invalidate()
                    return true
                }

                if (isPanning) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    panX += dx
                    panY += dy
                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (activeInteractiveComponent != null) {
                    activeInteractiveComponent?.onReleaseInteract()
                    activeInteractiveComponent = null
                    invalidate()
                }

                // Complete wiring if dropped on another pin
                if (wiringStartPin != null && wiringStartComponent != null) {
                    val activeEngine = engine
                    if (activeEngine != null) {
                        for (comp in activeEngine.components) {
                            val endPin = comp.findPinAt(worldX, worldY, 35f)
                            if (endPin != null && endPin != wiringStartPin) {
                                val wireColor = when (wiringStartPin!!.type) {
                                    PinType.POWER_5V -> "#FF5252" // Red
                                    PinType.GND -> "#212121"      // Black
                                    PinType.ANALOG -> "#00E5FF"   // Cyan
                                    else -> "#00E676"             // Green
                                }
                                val newWire = Wire(
                                    id = "wire_${System.currentTimeMillis()}",
                                    fromComponentId = wiringStartComponent!!.id,
                                    fromPinId = wiringStartPin!!.id,
                                    toComponentId = comp.id,
                                    toPinId = endPin.id,
                                    colorHex = wireColor
                                )
                                activeEngine.wires.add(newWire)
                                onWireConnected?.invoke(newWire)
                                break
                            }
                        }
                    }
                    wiringStartPin = null
                    wiringStartComponent = null
                    invalidate()
                }

                draggedComponent = null
                isPanning = false
            }
        }
        return super.onTouchEvent(event)
    }

    fun resetView() {
        scaleFactor = 1.0f
        panX = 0f
        panY = 0f
        invalidate()
    }
}
