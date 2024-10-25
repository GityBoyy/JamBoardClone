package org.chubby.github.jamboardclone.handler

import javafx.event.EventHandler
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.ColorPicker
import javafx.scene.control.Slider
import javafx.scene.control.ToggleButton
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color

class DrawHandler {
    private var colorPicker: ColorPicker? = null
    private var brushSizeSlider: Slider? = null
    private var eraserSizeSlider: Slider? = null
    private var eraserToggle: ToggleButton? = null

    private var lastX = 0.0
    private var lastY = 0.0
    private var isDrawing = false

    fun init(gc: GraphicsContext) {
        gc.lineWidth = 5.0 // Set a default line width
        gc.stroke = colorPicker?.value ?: Color.BLACK

        val mouseDragged = EventHandler<MouseEvent> { e ->
            if (!isDrawing) {
                isDrawing = true
                lastX = e.x
                lastY = e.y
                gc.beginPath()
                gc.moveTo(lastX, lastY)
            }

            // Set stroke color based on eraser state
            gc.stroke = if (eraserToggle?.isSelected == true) {
                Color.BLACK // Set to background color, assuming a black background
            } else {
                colorPicker?.value ?: Color.BLACK
            }

            // Set the line width based on current tool
            gc.lineWidth = if (eraserToggle?.isSelected == true) {
                eraserSizeSlider?.value ?: 5.0
            } else {
                brushSizeSlider?.value ?: 5.0
            }

            // Draw smooth line using Bezier curves
            val midX = (lastX + e.x) / 2
            val midY = (lastY + e.y) / 2
            gc.quadraticCurveTo(lastX, lastY, midX, midY)
            gc.stroke()

            lastX = e.x
            lastY = e.y
        }

        val mousePressed = EventHandler<MouseEvent> { e ->
            gc.beginPath()
            gc.moveTo(e.x, e.y)
            lastX = e.x
            lastY = e.y
            isDrawing = true
            gc.canvas.cursor = javafx.scene.Cursor.CROSSHAIR // Change cursor to crosshair
        }

        val mouseReleased = EventHandler<MouseEvent> {
            isDrawing = false
            gc.stroke() // Finalize the stroke
            gc.canvas.cursor = javafx.scene.Cursor.DEFAULT // Reset cursor
        }

        // Attach the event listeners to the canvas
        gc.canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, mousePressed)
        gc.canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, mouseDragged)
        gc.canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, mouseReleased)

        // Update brush size dynamically
        brushSizeSlider?.valueProperty()?.addListener { _, _, newValue ->
            if (eraserToggle?.isSelected == false) {
                gc.lineWidth = newValue.toDouble()
            }
        }

        // Update eraser size dynamically
        eraserSizeSlider?.valueProperty()?.addListener { _, _, newValue ->
            if (eraserToggle?.isSelected == true) {
                gc.lineWidth = newValue.toDouble()
            }
        }
    }

    fun setColorPicker(colorPicker: ColorPicker) {
        this.colorPicker = colorPicker
    }

    fun setBrushSizeSlider(brushSizeSlider: Slider) {
        this.brushSizeSlider = brushSizeSlider
        brushSizeSlider.valueProperty().addListener { _, _, newValue ->
            if (eraserToggle?.isSelected == false) {
                brushSizeSlider.value = newValue.toDouble()
            }
        }
    }

    fun setEraserSizeSlider(eraserSizeSlider: Slider) {
        this.eraserSizeSlider = eraserSizeSlider
        eraserSizeSlider.valueProperty().addListener { _, _, newValue ->
            if (eraserToggle?.isSelected == true) {
                // Update line width for eraser
                eraserSizeSlider.value = newValue.toDouble()
            }
        }
    }

    fun setEraserToggle(eraserToggle: ToggleButton) {
        this.eraserToggle = eraserToggle
    }
}
