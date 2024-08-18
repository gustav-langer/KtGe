package de.thecommcraft.builttoscale

import de.thecommcraft.ktge.DrawerCostume
import de.thecommcraft.ktge.Sprite
import de.thecommcraft.ktge.ktge
import de.thecommcraft.ktge.sprite
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

    var bar: Rectangle = Rectangle.EMPTY
    var rightArrow: Rectangle = Rectangle.EMPTY
    var downArrow: Rectangle = Rectangle.EMPTY
    var cornerArrow: Rectangle = Rectangle.EMPTY

    fun setUIElements() {
        bar = Rectangle(corner = Vector2.ZERO, width.toDouble(), barHeight)
        rightArrow =
            Rectangle(corner = Vector2(width.toDouble() - arrowHeight, barHeight), arrowHeight, height - arrowHeight)
        downArrow = Rectangle(corner = Vector2(0.0, height.toDouble() - arrowHeight), width - arrowHeight, arrowHeight)
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