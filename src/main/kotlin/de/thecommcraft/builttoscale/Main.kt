package de.thecommcraft.builttoscale

import de.thecommcraft.ktge.DrawerCostume
import de.thecommcraft.ktge.Sprite
import de.thecommcraft.ktge.ktge
import de.thecommcraft.ktge.sprite
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.presets.DARK_GREEN
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

val window = sprite {
    val color = ColorRGBa.DARK_GREEN
    val barHeight = 37.0 // pixels

    val bar = Rectangle(corner = Vector2.ZERO, width.toDouble(), barHeight)
    var dragPosition: Vector2? = null

    costume(DrawerCostume {
        drawer.fill = color
        drawer.stroke = null
        drawer.rectangle(bar)

        drawer.fill = null
        drawer.stroke = color
        drawer.rectangle(Vector2.ZERO, width.toDouble(), height.toDouble())
    })

    on(mouse.buttonDown) {
        if (bar.contains(it.position)) dragPosition = it.position
    }
    on(mouse.buttonUp) {
        if (bar.contains(it.position)) dragPosition = null
    }

    frame {
        dragPosition?.let { pos ->
            window.position += mouse.position - pos
        }
    }
}

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
    sprites = listOf(ball, window),
    config = {
        hideWindowDecorations = true
    }
)