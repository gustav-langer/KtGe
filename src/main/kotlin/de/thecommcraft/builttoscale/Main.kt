package de.thecommcraft.builttoscale

import de.thecommcraft.ktge.DrawerCostume
import de.thecommcraft.ktge.Sprite
import de.thecommcraft.ktge.ktge
import de.thecommcraft.ktge.sprite
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.presets.DARK_GREEN
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.w3c.dom.css.Rect
import kotlin.math.abs

enum class DragType {
    MOVE_WINDOW,
    RESIZE_RIGHT,
    RESIZE_DOWN,
    RESIZE_CORNER,
    NONE,
}

val barHeight = 37.0 // pixels
val arrowHeight = 15.0 // pixels

val window = sprite {
    val color = ColorRGBa.DARK_GREEN

    var bar: Rectangle = Rectangle.EMPTY
    var rightArrow: Rectangle = Rectangle.EMPTY
    var downArrow: Rectangle = Rectangle.EMPTY
    var cornerArrow: Rectangle = Rectangle.EMPTY

    fun setUIElements() {
        bar = Rectangle(corner = Vector2.ZERO, width.toDouble(), barHeight)
        rightArrow = Rectangle(corner = Vector2(width.toDouble() - arrowHeight, barHeight), arrowHeight, height-arrowHeight)
        downArrow = Rectangle(corner = Vector2(0.0, height.toDouble() - arrowHeight), width-arrowHeight, arrowHeight)
        cornerArrow = Rectangle(
            corner = Vector2(width.toDouble() - arrowHeight, height.toDouble() - arrowHeight),
            arrowHeight,
            arrowHeight
        )
    }

    setUIElements()

    var dragPosition: Vector2? = null
    var dragType: DragType = DragType.NONE
    var dragSize: Vector2? = null

    var windowRect = Rectangle(corner = window.position, window.size[0], window.size[1])

    fun internalResizeWindow(newWidth: Int, newHeight: Int) {
        window.size = Vector2(newWidth.toDouble(), newHeight.toDouble())
        setUIElements()
    }

    fun resizeWindow() {
        window.position = windowRect.corner
        internalResizeWindow(windowRect.width.toInt(), windowRect.height.toInt())
    }

    costume(DrawerCostume {
        drawer.fill = color
        drawer.stroke = null
        drawer.rectangle(bar)
        drawer.rectangle(rightArrow)
        drawer.rectangle(downArrow)
        drawer.rectangle(cornerArrow)

        drawer.fill = null
        drawer.stroke = color
        drawer.rectangle(Vector2.ZERO, width.toDouble(), height.toDouble())
    })

    on(mouse.buttonDown) {
        if (bar.contains(it.position)) {
            dragPosition = it.position
            dragType = DragType.MOVE_WINDOW
        }
        if (rightArrow.contains(it.position)) {
            dragPosition = it.position
            dragSize = window.size
            dragType = DragType.RESIZE_RIGHT
        }
        if (downArrow.contains(it.position)) {
            dragPosition = it.position
            dragSize = window.size
            dragType = DragType.RESIZE_DOWN
        }
        if (cornerArrow.contains(it.position)) {
            dragPosition = it.position
            dragSize = window.size
            dragType = DragType.RESIZE_CORNER
        }
        if (bar.contains(it.position)) {
            dragPosition = it.position
            dragType = DragType.MOVE_WINDOW
        }
    }

    on(mouse.buttonUp) {
        dragType = DragType.NONE
        if (bar.contains(it.position)) dragPosition = null
    }

    frame {
        dragPosition?.let { pos ->
            when (dragType) {
                DragType.MOVE_WINDOW -> {
                    windowRect = windowRect.copy(corner = window.position + mouse.position - pos)
                    resizeWindow()
                }

                DragType.RESIZE_RIGHT -> {
                    dragSize?.let { size ->
                        var newSize = size.x + mouse.position.x - pos.x
                        if (newSize < 128) {
                            newSize = 128.0
                        }
                        windowRect = Rectangle(corner = windowRect.corner, width = newSize, height = windowRect.height)
                        resizeWindow()
                    }
                }

                DragType.RESIZE_DOWN -> {
                    dragSize?.let { size ->
                        var newSize = size.y + mouse.position.y - pos.y
                        if (newSize < 128) {
                            newSize = 128.0
                        }
                        windowRect = Rectangle(corner = windowRect.corner, width = windowRect.width, height = newSize)
                        resizeWindow()
                    }
                }

                DragType.RESIZE_CORNER -> {
                    dragSize?.let { size ->
                        var newSize = size + mouse.position - pos
                        if (newSize.x < 128) {
                            newSize = newSize.copy(x = 128.0)
                        }
                        if (newSize.y < 128) {
                            newSize = newSize.copy(y = 128.0)
                        }
                        windowRect = Rectangle(corner = windowRect.corner, width = newSize.x, height = newSize.y)
                        resizeWindow()
                    }
                }

                else -> {}
            }
        }
    }
}

val ball = sprite {
    val gravity = 0.2

    var size = 10.0
    var vel: Vector2 = Vector2.ZERO // velocity

    fun Sprite.updateVelocity() {
        val bottomEdge = position.y + size - window.position.y
        val belowGround = bottomEdge - window.size.y + arrowHeight
        if (belowGround < 0) {
            vel = vel.copy(y = vel.y + gravity)
        } else {
            vel = vel.copy(y = -0.5 * vel.y)
            position = position.copy(y = position.y - belowGround)
        }
        val leftEdge = position.x - size - window.position.x
        val inLeftWall = leftEdge
        if (inLeftWall > 0) {

        } else {
            if (abs(vel.x) < abs(inLeftWall)) vel = vel.copy(x = vel.x + inLeftWall * 0.4)
            vel = vel.copy(x = -0.8 * vel.x)
            position = position.copy(x = position.x - inLeftWall)
        }
        val rightEdge = position.x + size - window.position.x
        val inRightWall = rightEdge - window.size.x
        if (inRightWall < 0) {

        } else {
            if (abs(vel.x) < abs(inRightWall)) vel = vel.copy(x = vel.x + inRightWall * 0.4)
            vel = vel.copy(x = -0.8 * vel.x)
            position = position.copy(x = position.x - inRightWall)
        }
        vel = vel.copy(x = vel.x * 0.99)
    }

    costume(DrawerCostume {
        drawer.fill = ColorRGBa.CYAN
        drawer.circle(it - window.position, size)
    })

    init {
        position = Vector2(x = window.position.x + width / 2.0, y = window.position.y)
    }



    frame {
        updateVelocity()
        position += vel
    }

    on(mouse.buttonDown) {
        //vel = Vector2.ZERO
        //position = mouse.position
    }
}

fun main() = ktge(
    sprites = listOf(ball, window),
    config = {
        hideWindowDecorations = true
        windowResizable = true
    }
)