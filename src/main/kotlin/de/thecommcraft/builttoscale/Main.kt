package de.thecommcraft.builttoscale

import de.thecommcraft.ktge.DrawerCostume
import de.thecommcraft.ktge.ktge
import de.thecommcraft.ktge.sprite
import org.openrndr.color.ColorRGBa

val ball = sprite {
    var size = 10.0

    costume(DrawerCostume {
        drawer.fill = ColorRGBa.CYAN
        drawer.circle(it, size)
    })

    frame {
        position = mouse.position
    }

    on(mouse.scrolled) {
        //window.
    }
}

fun main() = ktge(
    sprites = listOf(ball),
    config = {
        windowResizable = true
    }
)