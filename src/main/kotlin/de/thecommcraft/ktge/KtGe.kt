package de.thecommcraft.ktge

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.FontMap
import org.openrndr.draw.loadImage
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

interface Builder<T> {
    fun build(): T
}

typealias BuildFun<T> = T.() -> Unit

data class SpriteState(val position: Vector2, val size: Double)

interface Drawable {
    fun draw(program: Program)
}
/*abstract class PreDrawable: Drawable {
    private var _preDraw: Drawer.() -> Unit = {}
    abstract fun preDraw(code: Drawer.()->Unit)
    override fun draw(program: Program) {
        program.drawer._preDraw()

    }
}*/

interface Costume {
    fun draw(state: SpriteState, program: Program)
}

interface Background : Drawable

class ImageCostume(val buffer: ColorBuffer) : Costume {
    override fun draw(state: SpriteState, program: Program) {
        program.drawer.image(
            buffer,
            position = state.position,
            width = buffer.width * state.size,
            height = buffer.height * state.size
        )
    }
}

class DrawerCostume(val drawCostume: Program.(SpriteState) -> Unit) : Costume {
    override fun draw(state: SpriteState, program: Program) {
        program.drawCostume(state)
    }
}

class TextCostume(val text: String, val font: Lazy<FontMap>, val preDraw: Drawer.() -> Unit) : Costume {
    override fun draw(state: SpriteState, program: Program) = with(program) {
        drawer.fontMap = font.value
        drawer.preDraw()
        drawer.text(text, state.position)
    }
}

class ImageBackground : Background {
    val bg: Lazy<ColorBuffer>
    val preDraw: BuildFun<Drawer>

    constructor(image: ColorBuffer, preDraw: BuildFun<Drawer>) {
        bg = lazy { image }
        this.preDraw = preDraw
    }

    constructor(path: String, preDraw: BuildFun<Drawer>) {
        bg = lazy { loadImage(path) }
        this.preDraw = preDraw
    }

    override fun draw(program: Program) {
        program.drawer.preDraw()
        program.drawer.image(bg.value)
    }
}

typealias SpriteStateFun = Program.(SpriteState) -> SpriteState

//fun static(x: Double, y: Double): PositionLogic = { Vector2(x, y) }

class Sprite(
    val costumes: List<Costume>,
    val costumeNames: Map<String, Int>,
    val stateFun: SpriteStateFun,
    initialState: SpriteState
) : Drawable {
    var state: SpriteState = initialState
    var costumeNum: Int = 0

    fun update(program: Program) {
        state = program.stateFun(state)
    }

    override fun draw(program: Program) {
        costumes[costumeNum].draw(state, program)
    }
}

object StateFun {
    fun fixed(x: Double, y: Double, size: Double = 1.0): SpriteStateFun =
        { SpriteState(position = Vector2(x, y), size = size) }

    fun position(posFun: Program.(SpriteState) -> Vector2): SpriteStateFun =
        { SpriteState(position = posFun(it), size = it.size) }
}

class SpriteBuilder : Builder<Sprite> {
    private val _costumes: MutableList<Costume> = mutableListOf()
    private val _costumeNames: MutableMap<String, Int> = mutableMapOf()
    var stateFun: SpriteStateFun = { it }
    var initialState: SpriteState = SpriteState(Vector2.ZERO, 1.0)

    fun costume(c: Costume, name: String? = null) {
        name?.let { _costumeNames[it] = _costumes.size }
        _costumes.add(c)
    }

    override fun build(): Sprite =
        Sprite(costumes = _costumes, costumeNames = _costumeNames, stateFun = stateFun, initialState = initialState)
}

fun <T, B : Builder<T>> build(initial: () -> B): (BuildFun<B>) -> T {
    return fun(init: BuildFun<B>): T {
        val t = initial()
        t.init()
        return t.build()
    }
}

val sprite: (BuildFun<SpriteBuilder>) -> Sprite = build(::SpriteBuilder)

fun ktge(sprites: List<Sprite>, config: BuildFun<Configuration>, background: Background) {
    //application(demoBuilder) // runs demo application for now
    application {
        configure(config)
        program {
            extend {
                background.draw(this)
                for (spr in sprites) {
                    spr.update(this)
                    spr.draw(this)
                }
            }
        }
    }
}