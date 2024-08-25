package de.thecommcraft.builttoscale

import de.thecommcraft.ktge.DrawerCostume
import de.thecommcraft.ktge.ImageCostume
import de.thecommcraft.ktge.Sprite
import de.thecommcraft.ktge.SpriteCode
import org.openrndr.CursorType
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.pow

enum class DragType {
    MOVE_WINDOW,
    RESIZE_RIGHT,
    RESIZE_DOWN,
    RESIZE_CORNER,
    NONE,
}

class GameWindow(
    val doRandomColors: Boolean = true,
    val minWidth: Double = 128.0, val minHeight: Double = 128.0
) : Sprite() {
    var color: ColorRGBa = if (doRandomColors) randomColor() else ColorRGBa(0.42, 0.51, 1.0)

    var resizable = true
    val barHeight = 37.0 // pixels
    val arrowHeight = 15.0 // pixels

    var bar: Rectangle = Rectangle.EMPTY
        private set
    var resizeRight: Rectangle = Rectangle.EMPTY
        private set
    var resizeDown: Rectangle = Rectangle.EMPTY
        private set
    var resizeCorner: Rectangle = Rectangle.EMPTY
        private set

    private var dragType: DragType = DragType.NONE
    private var dragPosition: Vector2 = Vector2.ZERO // Mouse position when drag started
    private var dragWindowPosition: Vector2 = Vector2.ZERO
    private var dragSize: Vector2 = Vector2.ZERO // Size of the window when drag started

    lateinit var windowRect: Rectangle

    override fun initSprite() = program.run {
        setUIElements()

        windowRect = Rectangle(corner = window.position, window.size.x, window.size.y)

        costume(DrawerCostume {
            drawer.fill = color
            drawer.stroke = null
            drawer.rectangle(bar)
            drawer.rectangle(resizeRight)
            drawer.rectangle(resizeDown)
            drawer.rectangle(resizeCorner)
        })

        on(mouse.buttonDown) {
            dragPosition = toGlobal(it.position)
            dragWindowPosition = window.position
            dragSize = window.size
            dragType = when {
                bar.contains(it.position) -> DragType.MOVE_WINDOW
                resizeRight.contains(it.position) -> DragType.RESIZE_RIGHT
                resizeDown.contains(it.position) -> DragType.RESIZE_DOWN
                resizeCorner.contains(it.position) -> DragType.RESIZE_CORNER
                else -> DragType.NONE
            }
        }

        on(mouse.buttonUp) {
            dragType = DragType.NONE
        }

        frame {
            val mouseChange = toGlobal(mouse.position) - dragPosition
            val newWidth = max(dragSize.x + mouseChange.x, minWidth)
            val newHeight = max(dragSize.y + mouseChange.y, minHeight)

            windowRect = if (resizable)
                when (dragType) {
                    DragType.NONE -> windowRect
                    DragType.MOVE_WINDOW -> windowRect.copy(corner = dragWindowPosition + mouseChange)
                    DragType.RESIZE_RIGHT -> windowRect.copy(width = newWidth)
                    DragType.RESIZE_DOWN -> windowRect.copy(height = newHeight)
                    DragType.RESIZE_CORNER -> windowRect.copy(width = newWidth, height = newHeight)
                }
            else
                when (dragType) {
                    DragType.MOVE_WINDOW -> windowRect.copy(corner = dragWindowPosition + mouseChange)
                    else -> windowRect
                }
            resizeWindow()
        }

        frame {
            application.cursorType = when {
                resizeRight.contains(mouse.position) -> CursorType.HRESIZE_CURSOR
                resizeDown.contains(mouse.position) -> CursorType.VRESIZE_CURSOR
                else -> CursorType.ARROW_CURSOR // TODO does the diagonal cursor type exist yet? if so, add it
            }
        }

        if (doRandomColors) frame {
            if (frameCount % 300 == 0) color = randomColor()
        }

        createSprite(Icon("close.png", 1) { exit() })
    }

    private fun setUIElements() = program.run {
        bar = Rectangle(corner = Vector2.ZERO, width.toDouble(), barHeight)
        resizeRight =
            Rectangle(
                corner = Vector2(width.toDouble() - arrowHeight, barHeight),
                arrowHeight,
                height - barHeight - arrowHeight
            )
        resizeDown = Rectangle(corner = Vector2(0.0, height.toDouble() - arrowHeight), width - arrowHeight, arrowHeight)
        resizeCorner = Rectangle(
            corner = Vector2(width.toDouble() - arrowHeight, height.toDouble() - arrowHeight),
            arrowHeight,
            arrowHeight
        )
    }

    fun resizeWindow() = program.run {
        setUIElements()
        window.position = windowRect.corner
        window.size = windowRect.dimensions
    }

    fun exit() = program.run {
        resizable = false
        val animLength = 30.0 // frames
        val dSize = (1.0 / kotlin.math.min(width, height)).pow(1 / animLength)
        val dPos = -window.position / animLength
        fun animFrame(): SpriteCode = {
            windowRect = windowRect.movedBy(dPos)
            windowRect = windowRect.scaledBy(dSize, 0.0, 0.0)
            resizeWindow()
            if (kotlin.math.min(width, height) > 2) schedule(animFrame()) else application.exit()
        }
        schedule(animFrame())
    }

    inner class Icon(val path: String, val index: Int, val action: SpriteCode) : Sprite() {
        override fun initSprite() = program.run {
            position = Vector2(x = window.size.x - index * barHeight, y = 0.0)

            costume(ImageCostume(img = loadImage("data/images/icons/$path")))

            on(window.sized) {
                position = Vector2(x = width - index * barHeight, y = 0.0)
            }

            on(mouse.buttonDown) {
                if (Rectangle(position, barHeight, barHeight).contains(it.position)) action()
            }
        }
    }
}
