package de.thecommcraft.builttoscale

import de.thecommcraft.ktge.*
import org.openrndr.draw.loadImage
import org.openrndr.math.Vector2
import kotlin.io.path.Path
import kotlin.random.Random

val gameWindow = GameWindow(doRandomColors = false)
val ball = Ball()

val simulatedWindowCorner = Vector2(x = 1.0, y = gameWindow.barHeight) // Used by Utils.toGlobal

fun loadTile(tileName: String) = "data/images/$tileName.png"
    .path()
    .editableImage()
    .scaleBy(3)
    .complete()
    .toCostume()

val tileBackground = TileGrid(tileSize = 50, gridWidth = 16, gridHeight = 12) {
    tileType(0, loadTile("tile0"))
    tileType(1, loadTile("tile1"))

    frame {
        val x = Random.nextInt(gridWidth)
        val y = Random.nextInt(gridHeight)
        val tile = Random.nextInt(2) // selects either 0 or 1
        this@TileGrid[x, y] = tile
    }
}

fun main() = ktge(
    sprites = listOf(tileBackground, ball, gameWindow),
    config = {
        hideWindowDecorations = true
    }
)
