package org.chubby.github.jamboardclone

import javafx.application.Application
import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.stage.Screen
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.chubby.github.jamboardclone.handler.DrawHandler
import java.io.File
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO


class JamboardClone : Application() {
    private val canvases = mutableListOf<Canvas>()
    private var currentPage = 0

    @FXML
    private lateinit var colorPicker: ColorPicker

    @FXML
    private lateinit var brushSizeSlider: Slider

    @FXML
    private lateinit var eraserSizeSlider: Slider

    @FXML
    private lateinit var eraserToggle: ToggleButton

    @FXML
    private lateinit var pageLabel: Label

    @FXML
    private lateinit var drawingArea: StackPane

    @FXML
    private lateinit var savePdfButton: Button

    private lateinit var dotImageView: ImageView

    override fun start(primaryStage: Stage) {
        val loader = FXMLLoader(javaClass.getResource("JamboardClone.fxml"))
        val root = loader.load<BorderPane>()

        colorPicker = loader.getNamespace()["colorPicker"] as ColorPicker
        brushSizeSlider = loader.getNamespace()["brushSizeSlider"] as Slider
        eraserSizeSlider = loader.getNamespace()["eraserSizeSlider"] as Slider
        eraserToggle = loader.getNamespace()["eraserToggle"] as ToggleButton
        pageLabel = loader.getNamespace()["pageLabel"] as Label
        drawingArea = loader.getNamespace()["drawingArea"] as StackPane
        savePdfButton = loader.getNamespace()["savePdfButton"] as Button

        // Initial drawing area (canvas) setup
        val screenBounds = Screen.getPrimary().bounds
        val initialCanvas = createCanvas(screenBounds.width * 0.8, screenBounds.height * 0.8)
        canvases.add(initialCanvas)

        drawingArea.children.add(initialCanvas)
        drawingArea.style = "-fx-background-color: black;"

        setupCustomCursor()

        setupPageNavigation(screenBounds.width * 0.8, screenBounds.height * 0.8)

        savePdfButton.setOnAction {
            exportToPDF(primaryStage)
        }

        val scene = Scene(root, screenBounds.width * 0.9, screenBounds.height * 0.9)
        primaryStage.scene = scene
        primaryStage.title = "Jamboard Clone"
        primaryStage.show()
    }

    /**
     * Exports the current canvases to a PDF file.
     *
     * @param primaryStage The primary stage of the application.
     */
    private fun exportToPDF(primaryStage: Stage) {
        val fileChooser = FileChooser().apply {
            title = "Save PDF"
            extensionFilters.addAll(
                FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                FileChooser.ExtensionFilter("All Files", "*.*")
            )
        }

        val selectedFile: File? = fileChooser.showSaveDialog(primaryStage)

        if (selectedFile != null) {
            val pdfDocument = PDDocument()
            val pngFile = File("temp_canvas.png")

            try {
                for (canvas in canvases) {
                    val snapshotParameters = SnapshotParameters()
                    val nodeshot: WritableImage = canvas.snapshot(snapshotParameters, null)

                    ImageIO.write(SwingFXUtils.fromFXImage(nodeshot, null), "png", pngFile)

                    if (!pngFile.exists()) {
                        throw IOException("Temporary PNG file not created.")
                    }

                    val pageWidth = 800f
                    val pageHeight = 400f
                    val page = PDPage(PDRectangle(pageWidth, pageHeight))
                    pdfDocument.addPage(page)

                    val pdImage = PDImageXObject.createFromFile(pngFile.absolutePath, pdfDocument)

                    val scaleX = pageWidth / pdImage.width.toFloat()
                    val scaleY = pageHeight / pdImage.height.toFloat()
                    val scale = Math.min(scaleX, scaleY)

                    PDPageContentStream(pdfDocument, page).use { contentStream ->
                        contentStream.drawImage(
                            pdImage,
                            (pageWidth - pdImage.width * scale) / 2,
                            (pageHeight - pdImage.height * scale) / 2,
                            pdImage.width * scale,
                            pdImage.height * scale
                        )
                    }
                }

                pdfDocument.save(selectedFile.absolutePath)
                Alert(Alert.AlertType.INFORMATION, "PDF saved to ${selectedFile.absolutePath}").show()
            } catch (e: IOException) {
                Logger.getLogger(JamboardClone::class.java.name).log(Level.SEVERE, null, e)
                Alert(Alert.AlertType.ERROR, "Error saving PDF: ${e.message}").show()
            } finally {
                if (pngFile.exists()) {
                    pngFile.delete()
                }
                pdfDocument.close()
            }
        }
    }

    /**
     * Sets up the custom cursor using a dot image.
     */
    private fun setupCustomCursor() {
        val dotImage = Image("org/chubby/github/jamboardclone/dot.png")
        dotImageView = ImageView(dotImage).apply {
            fitWidth = 10.0
            fitHeight = 10.0
        }

        drawingArea.cursor = javafx.scene.Cursor.HAND

        drawingArea.setOnMouseMoved { event ->
            dotImageView.x = event.x - (dotImageView.fitWidth / 2)
            dotImageView.y = event.y - (dotImageView.fitHeight / 2)

            if (!drawingArea.children.contains(dotImageView)) {
                drawingArea.children.add(dotImageView)
            }
        }

        drawingArea.setOnMouseExited {
            drawingArea.children.remove(dotImageView)
        }
    }

    /**
     * Creates a new canvas with the specified dimensions and initializes drawing tools.
     *
     * @param width The width of the canvas.
     * @param height The height of the canvas.
     * @return The created Canvas object.
     */
    private fun createCanvas(width: Double, height: Double): Canvas {
        val canvas = Canvas(width, height)
        val gc = canvas.graphicsContext2D
        val drawHandler = DrawHandler()

        drawHandler.setColorPicker(colorPicker)
        drawHandler.setBrushSizeSlider(brushSizeSlider)
        drawHandler.setEraserSizeSlider(eraserSizeSlider)
        drawHandler.setEraserToggle(eraserToggle)
        drawHandler.init(gc)

        return canvas
    }

    /**
     * Sets up the page navigation buttons and their actions.
     *
     * @param canvasWidth The width of the canvas.
     * @param canvasHeight The height of the canvas.
     */
    private fun setupPageNavigation(canvasWidth: Double, canvasHeight: Double) {
        val prevButton = pageLabel.parent.lookup("#prevButton") as Button
        val nextButton = pageLabel.parent.lookup("#nextButton") as Button

        prevButton.setOnAction {
            if (currentPage > 0) {
                currentPage--
                updateCanvas()
            }
        }

        nextButton.setOnAction {
            if (currentPage < canvases.size - 1) {
                currentPage++
                updateCanvas()
            } else {
                val newCanvas = createCanvas(canvasWidth, canvasHeight)
                canvases.add(newCanvas)
                currentPage++
                updateCanvas()
            }
        }
    }

    /**
     * Updates the displayed canvas and the page label.
     */
    private fun updateCanvas() {
        drawingArea.children.setAll(canvases[currentPage])
        pageLabel.text = "Page ${currentPage + 1} / ${canvases.size}"
    }
}

/**
 * Entry point of the application.
 */
fun main() {
    Application.launch(JamboardClone::class.java)
}
