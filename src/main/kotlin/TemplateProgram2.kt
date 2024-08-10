import org.openrndr.ApplicationBuilder
import org.openrndr.KEY_ESCAPE
import org.openrndr.KeyEvent
import org.openrndr.application
import org.openrndr.color.ColorRGBa
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

fun main() = application(demoBuilder)
