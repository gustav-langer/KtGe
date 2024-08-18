package de.thecommcraft.builttoscale

import de.thecommcraft.ktge.DrawerCostume
import de.thecommcraft.ktge.Sprite
import de.thecommcraft.ktge.ktge
import de.thecommcraft.ktge.sprite
import org.openrndr.CursorType
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.presets.DARK_GREEN
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

enum class DragType {
    MOVE_WINDOW,
    RESIZE_RIGHT,
    RESIZE_DOWN,
    RESIZE_CORNER,
    NONE,
}

const val barHeight = 37.0 // pixels
const val arrowHeight = 15.0 // pixels

val window = sprite {
    val color = ColorRGBa.DARK_GREEN
    val minWidth = 128.0
    val minHeight = 128.0

    var bar: Rectangle = Rectangle.EMPTY
    var resizeRight: Rectangle = Rectangle.EMPTY
    var resizeDown: Rectangle = Rectangle.EMPTY
    var resizeCorner: Rectangle = Rectangle.EMPTY

    fun setUIElements() {
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

    setUIElements()

    var dragType: DragType = DragType.NONE
    var dragPosition: Vector2 = toGlobal(mouse.position) // Mouse position when drag started
    var dragWindowPosition: Vector2 = window.position
    var dragSize: Vector2 = window.size // Size of the window when drag started

    var windowRect = Rectangle(corner = window.position, window.size[0], window.size[1])

    fun resizeWindow() {
        setUIElements()
        window.position = windowRect.corner
        window.size = windowRect.dimensions
    }

    costume(DrawerCostume {
        drawer.fill = color
        drawer.stroke = null
        drawer.rectangle(bar)
        drawer.rectangle(resizeRight)
        drawer.rectangle(resizeDown)
        drawer.rectangle(resizeCorner)

        drawer.fill = null
        drawer.stroke = color
        drawer.rectangle(Vector2.ZERO, width.toDouble(), height.toDouble())
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
        windowRect = when (dragType) {
            DragType.NONE -> windowRect
            DragType.MOVE_WINDOW -> windowRect.copy(corner = dragWindowPosition + mouseChange)
            DragType.RESIZE_RIGHT -> windowRect.copy(width = newWidth)
            DragType.RESIZE_DOWN -> windowRect.copy(height = newHeight)
            DragType.RESIZE_CORNER -> windowRect.copy(width = newWidth, height = newHeight)
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
}

val ball = sprite {
    val gravity = 0.2
    val bounceX = 0.5
    val bounceY = 0.5
    val frictionX = 0.99
    val frictionY = 1

    var size = 10.0
    var vel: Vector2 = Vector2.ZERO // velocity

    // In this sprite, position is calculated globally, i.e. from the top left of the screen and not the window.
    // Use the Program.toGlobal() function whenever setting the position.
    costume(globalPos(DrawerCostume {
        drawer.fill = ColorRGBa.CYAN
        drawer.circle(it, size)
    }))

    fun Sprite.updateVelocity() {
        // window sizes
        val wTop = barHeight + window.position.y
        val wBottom = window.size.y - arrowHeight + window.position.y
        val wLeft = 1.0 + window.position.x // left border is just 1 pixel
        val wRight = window.size.x - arrowHeight + window.position.x

        // sprite sizes
        val sTop = position.y - size
        val sBottom = position.y + size
        val sLeft = position.x - size
        val sRight = position.x + size

        // Gravity & bounces
        val belowGround = sBottom - wBottom
        if (belowGround < 0) {
            vel = vel.copy(y = vel.y + gravity) // ball is above ground so gravity applies
        } else {
            val newVelY = min(
                -bounceY * vel.y, // bounce
                -belowGround, // bring the ball back above ground next frame
                vel.y // continue motion
            )
            vel = vel.copy(y = newVelY)
        }

        val aboveCeiling = wTop - sTop
        if (aboveCeiling >= 0) {
            val newVelY = max(
                -bounceY * vel.y, // bounce
                aboveCeiling, // bring the ball back in the window next frame
                vel.y // continue motion
            )
            vel = vel.copy(y = newVelY)
        }

        val inLeftWall = wLeft - sLeft
        if (inLeftWall >= 0) {
            val newVelX = max(
                -bounceX * vel.x, // bounce
                inLeftWall, // bring the ball back out of the wall next frame
                vel.x // continue motion
            )
            vel = vel.copy(x = newVelX)
        }

        val inRightWall = sRight - wRight
        if (inRightWall >= 0) {
            val newVelX = min(
                -bounceX * vel.x, // bounce
                -inRightWall, // bring the ball back out of the wall next frame
                vel.x // continue motion
            )
            vel = vel.copy(x = newVelX)
        }

        // Friction
        vel = vel.copy(x = frictionX * vel.x)
        vel = vel.copy(y = frictionY * vel.y)
    }

    init {
        position = toGlobal(Vector2(x = width / 2.0, y = size))
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