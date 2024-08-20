package de.thecommcraft.builttoscale

import de.thecommcraft.ktge.ktge
import org.openrndr.math.Vector2

val gameWindow = GameWindow(doRandomColors = false)
val ball = Ball()

val simulatedWindowCorner = Vector2(x = 1.0, y = gameWindow.barHeight) // Used by Utils.toGlobal

fun main() = ktge(
    sprites = listOf(ball, gameWindow),
    config = {
        hideWindowDecorations = true
    }
)
