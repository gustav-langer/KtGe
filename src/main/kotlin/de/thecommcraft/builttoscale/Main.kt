package de.thecommcraft.builttoscale

import de.thecommcraft.ktge.DrawerCostume
import de.thecommcraft.ktge.Sprite
import de.thecommcraft.ktge.ktge
import de.thecommcraft.ktge.sprite
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2

val ball = sprite {
    val gravity = 0.2

    var size = 10.0
    var vel: Vector2 = Vector2.ZERO // velocity

    fun Sprite.updateVelocity() {
        val bottomEdge = position.y + size
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
        drawer.circle(it, size)
    })

    init {
        position = Vector2(x = width / 2.0, y = 0.0)
    }

    frame {
        updateVelocity()
        position += vel
    }

    on(mouse.buttonDown) {
        vel = Vector2.ZERO
        position = mouse.position
    }
}

fun main() = ktge(
    sprites = listOf(ball),
    config = {
        windowResizable = true
    }
)