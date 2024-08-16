package de.thecommcraft.ktgeDemo

import de.thecommcraft.ktge.*
import org.openrndr.KEY_ESCAPE
import org.openrndr.KEY_SPACEBAR
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2

val cursor = sprite {
    val color = ColorRGBa.WHITE
    costume(DrawerCostume {
        drawer.fill = color
        drawer.circle(mouse.position, 50.0)
    }, "mouseDown")
    costume(DrawerCostume {
        drawer.fill = color
        drawer.circle(mouse.position, 10.0)
    }, "mouseUp")

    init {
        costumeName = "mouseUp"
    }

    frame {
        position = mouse.position
    }

    on(mouse.buttonDown) {
        costumeName = "mouseDown"
        createSprite(clickSprite(it.position))
    }

    on(mouse.buttonUp) {
        costumeName = "mouseUp"
    }
}

fun clickSprite(position: Vector2) = sprite {
    costume(DrawerCostume {
        drawer.fill = ColorRGBa.MAGENTA
        drawer.stroke = null
        drawer.circle(position, 15.0)
    })
}

val global = sprite {
    on(keyboard.keyDown) {
        if (it.key == KEY_ESCAPE) application.exit()
        if (it.key == KEY_SPACEBAR) testAudioGroup.playAudio("hit")
        if (it.name == "q") testAudioGroup2.playAudio("hit")
        if (it.name == "a") testAudioGroup2.playAudio("group")
    }
}

val testAudioGroup: AudioGroup = audioGroup {
    audioFile(
        filePath = "data/sounds/hit.wav",
        name = "hit"
    )
}

val testAudioGroup2: AudioGroup = audioGroup {
    audioFile(
        filePath = "data/sounds/hit.wav",
        name = "hit"
    )
    audio(testAudioGroup, name="group")
}

fun main() = ktge(
    sprites = listOf(global, cursor),
    config = {
        windowResizable = true
        title = "KtGe Demo"
        hideCursor = true
    }
)