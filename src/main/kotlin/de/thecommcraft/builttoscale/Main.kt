package de.thecommcraft.builttoscale

import de.thecommcraft.ktge.DrawerCostume
import de.thecommcraft.ktge.Sprite
import de.thecommcraft.ktge.ktge
import de.thecommcraft.ktge.sprite
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.presets.DARK_GREEN
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.w3c.dom.css.Rect

enum class DragType {
    MOVE_WINDOW,
    RESIZE_RIGHT,
    RESIZE_DOWN,
    RESIZE_CORNER,
    NONE,
}

val window = sprite {



    val color = ColorRGBa.DARK_GREEN
    val barHeight = 37.0 // pixels

    var bar = Rectangle(corner = Vector2.ZERO, width.toDouble(), barHeight)
    var leftArrow = Rectangle(corner = Vector2(0.0, height.toDouble() / 2 - 16), 32.0, 32.0)
    var rightArrow = Rectangle(corner = Vector2(width.toDouble() - 32, height.toDouble() / 2 - 16), 32.0, 32.0)
    var downArrow = Rectangle(corner = Vector2(width.toDouble() / 2 - 16, height.toDouble() - 32), 32.0, 32.0)
    var cornerArrow = Rectangle(corner = Vector2(width.toDouble() - 32, height.toDouble() - 32), 32.0, 32.0)
    var dragPosition: Vector2? = null
    var dragType: DragType = DragType.NONE
    var dragSize: Vector2? = null

    var windowRect = Rectangle(corner = window.position, window.size[0], window.size[1])

    fun internalResizeWindow(newWidth: Int, newHeight: Int) {
        window.size = Vector2(newWidth.toDouble(), newHeight.toDouble())
        bar = Rectangle(corner = Vector2.ZERO, width.toDouble(), barHeight)
        leftArrow = Rectangle(corner = Vector2(0.0, height.toDouble() / 2 - 16), 32.0, 32.0)
        rightArrow = Rectangle(corner = Vector2(width.toDouble() - 32, height.toDouble() / 2 - 16), 32.0, 32.0)
        downArrow = Rectangle(corner = Vector2(width.toDouble() / 2 - 16, height.toDouble() - 32), 32.0, 32.0)
        cornerArrow = Rectangle(corner = Vector2(width.toDouble() - 32, height.toDouble() - 32), 32.0, 32.0)
    }

    fun resizeWindow() {
        window.position = windowRect.corner
        internalResizeWindow(windowRect.width.toInt(), windowRect.height.toInt())
    }

    costume(DrawerCostume {
        drawer.fill = color
        drawer.stroke = null
        drawer.rectangle(bar)

        drawer.fill = color
        drawer.stroke = null
        drawer.rectangle(rightArrow)

        drawer.fill = color
        drawer.stroke = null
        drawer.rectangle(downArrow)

        drawer.fill = color
        drawer.stroke = null
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
//                DragType.RESIZE_LEFT -> {
//                    var newPos = window.position.x + mouse.position.x - pos.x
//                    if (windowRect.width + windowRect.x - newPos < 128) {
//                        newPos = windowRect.width + windowRect.x - 128
//                    }
//                    windowRect = Rectangle(x = newPos, y = windowRect.y, width = windowRect.width + windowRect.x - newPos, height = windowRect.height)
//                    resizeWindow()
//                }
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
        val belowGround = bottomEdge - window.size.y + 32
        if (belowGround < 0) {
            vel = vel.copy(y = vel.y + gravity)
        } else {
            vel = vel.copy(y = -1.5 * belowGround) //position = position.copy(y = wind.size.y - size)
        }
        val leftEdge = position.x - size - window.position.x
        val inLeftWall = leftEdge
        if (inLeftWall > 0) {

        } else {
            vel = vel.copy(x = vel.x - inLeftWall)
            //position = position.copy(y = window.size.y - size)
        }
        val rightEdge = position.x + size - window.position.x
        val inRightWall = rightEdge - window.size.x
        if (inRightWall < 0) {

        } else {
            vel = vel.copy(x = vel.x - inRightWall)
            //position = position.copy(y = window.size.y - size)
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