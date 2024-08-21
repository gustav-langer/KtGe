package de.thecommcraft.builttoscale

import de.thecommcraft.ktge.DrawerCostume
import de.thecommcraft.ktge.Sprite
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2

class Ball(val color: ColorRGBa = ColorRGBa.CYAN) : Sprite() {

    private val gravity = 0.2
    private val bounceX = 0.8
    private val bounceY = 0.8
    private val frictionX = 1
    private val frictionY = 1
    private val dSize = 10.0 // size change when pressing + or -

    var size = 10.0
    private var vel: Vector2 = Vector2.ZERO // velocity

    override fun initSprite() = program.run {
        costume(globalCostume(DrawerCostume {
            drawer.fill = color
            drawer.circle(it, size)
        }))

        position = toGlobal(Vector2(x = width / 2.0, y = size))

        frame {
            updateVelocity()
            position += vel
        }

        on(keyboard.keyDown) {
            if (it.modifiers.isEmpty()) {
                when (it.name) {
                    "+" -> size += dSize
                    "-" -> if (size > dSize) size -= dSize
                }
            }
        }
    }

    // In this sprite, position is calculated globally, i.e. from the top left of the screen and not the window.
    // Use the Program.toGlobal() function whenever setting the position.

    fun updateVelocity() = program.run {
        // window sizes
        val wTop = gameWindow.barHeight + window.position.y
        val wBottom = window.size.y - gameWindow.arrowHeight + window.position.y
        val wLeft = 1.0 + window.position.x // left border is just 1 pixel
        val wRight = window.size.x - gameWindow.arrowHeight + window.position.x

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
}
