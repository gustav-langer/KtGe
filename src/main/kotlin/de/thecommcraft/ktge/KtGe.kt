package de.thecommcraft.ktge

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.math.Vector2

// Demo application. Press ESCAPE to exit.
val demoBuilder: ApplicationBuilder.() -> Unit = {
    configure {
        //width = 1280
        //height = 720
        windowResizable = true
        //fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
        title = "KtGe Demo"
        hideCursor = true
    }

    program {
        var mouseDown = false
        val clicks: MutableSet<Vector2> = mutableSetOf()

        mouse.buttonDown.listen {
            mouseDown = true
            clicks.add(mouse.position)
        }
        mouse.buttonUp.listen { mouseDown = false }
        keyboard.keyDown.listen { e: KeyEvent -> if (e.key == KEY_ESCAPE) application.exit() }

        drawer.stroke = null
        extend {
            // loop
            drawer.fill = ColorRGBa.MAGENTA
            clicks.forEach {
                drawer.circle(it, 15.0)
            }
            drawer.fill = ColorRGBa.WHITE
            drawer.circle(mouse.position, if (mouseDown) 50.0 else 10.0)
        }
    }
}

typealias Builder<T> = T.() -> Unit

data class SpriteState(val position: Vector2)

interface Costume {
    fun draw(state: SpriteState, program: Program)
}

class ImageCostume(val buffer: ColorBuffer) : Costume {
    override fun draw(state: SpriteState, program: Program) {
        program.drawer.image(buffer, position = state.position)
    }
}

class Sprite {
    var costumes: List<Costume> = listOf()
}

fun <T> build(initial: T): (Builder<T>) -> T {
    return fun(init: Builder<T>): T {
        initial.init()
        return initial
    }
}

val sprite: (Builder<Sprite>) -> Sprite = build(Sprite())

fun ktge(sprites: List<Sprite> /*background: BackgroundOptions*/) {
    application(demoBuilder) // runs demo application for now
}