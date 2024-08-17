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

enum class DragType {
    MOVE_WINDOW,
    RESIZE_LEFT,
    RESIZE_RIGHT,
    RESIZE_DOWN,
    RESIZE_UP,
    NONE,
}

val window = sprite {



    val color = ColorRGBa.DARK_GREEN
    val barHeight = 37.0 // pixels

    var bar = Rectangle(corner = Vector2.ZERO, width.toDouble(), barHeight)
    var leftArrow = Rectangle(corner = Vector2(0.0, height.toDouble() / 2 - 16), 32.0, 32.0)
    var rightArrow = Rectangle(corner = Vector2(width.toDouble() - 32, height.toDouble() / 2 - 16), 32.0, 32.0)
    var downArrow = Rectangle(corner = Vector2(width.toDouble() / 2 - 16, height.toDouble() - 32), 32.0, 32.0)
    var dragPosition: Vector2? = null
    var dragType: DragType = DragType.NONE

    fun resizeWindow(newWidth: Int, newHeight: Int) {
        window.size = Vector2(newWidth.toDouble(), newHeight.toDouble())
        bar = Rectangle(corner = Vector2.ZERO, width.toDouble(), barHeight)
        leftArrow = Rectangle(corner = Vector2(0.0, height.toDouble() / 2 - 16), 32.0, 32.0)
        rightArrow = Rectangle(corner = Vector2(width.toDouble() - 32, height.toDouble() / 2 - 16), 32.0, 32.0)
        downArrow = Rectangle(corner = Vector2(width.toDouble() / 2 - 16, height.toDouble() - 32), 32.0, 32.0)
    }

    costume(DrawerCostume {
        drawer.fill = color
        drawer.stroke = null
        drawer.rectangle(bar)

        drawer.fill = color
        drawer.stroke = null
        drawer.rectangle(leftArrow)

        drawer.fill = color
        drawer.stroke = null
        drawer.rectangle(rightArrow)

        drawer.fill = color
        drawer.stroke = null
        drawer.rectangle(downArrow)

        drawer.fill = null
        drawer.stroke = color
        drawer.rectangle(Vector2.ZERO, width.toDouble(), height.toDouble())
    })

    on(mouse.buttonDown) {
        if (bar.contains(it.position)) {
            dragPosition = it.position
            dragType = DragType.MOVE_WINDOW
        }
        if (leftArrow.contains(it.position)) {
            dragPosition = it.position
            dragType = DragType.RESIZE_LEFT
        }
        if (rightArrow.contains(it.position)) {
            dragPosition = it.position
            dragType = DragType.RESIZE_RIGHT
        }
        if (downArrow.contains(it.position)) {
            dragPosition = it.position
            dragType = DragType.RESIZE_DOWN
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
                DragType.MOVE_WINDOW -> window.position += mouse.position - pos
                DragType.RESIZE_LEFT -> {
                    resizeWindow((width - mouse.position.x + pos.x).toInt(), height)
                    window.position = window.position.copy(x = window.position.x + mouse.position.x - pos.x)
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
        val belowGround = bottomEdge - window.size.y
        if (belowGround < 0) {
            vel = vel.copy(y = vel.y + gravity)
        } else {
            vel = vel.copy(y = -belowGround)
            //position = position.copy(y = window.size.y - size)
        }
    }

    costume(DrawerCostume {
        drawer.fill = ColorRGBa.CYAN
        drawer.circle(it - window.position, size)
    })

    init {
        position = Vector2(x = width / 2.0, y = 0.0)
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