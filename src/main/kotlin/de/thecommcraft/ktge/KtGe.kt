package de.thecommcraft.ktge

import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.FontMap
import org.openrndr.draw.loadImage
import org.openrndr.math.Vector2

// General Types
interface Drawable {
    fun draw(program: Program)
}

typealias BuildFun<T> = T.() -> Unit

interface Builder<T> {
    fun build(): T
}

fun <T, B : Builder<T>> build(initial: () -> B): (BuildFun<B>) -> T {
    return fun(init: BuildFun<B>): T {
        val t = initial()
        t.init()
        return t.build()
    }
}

// Sprites
class Sprite(
    val costumes: List<Costume>,
    val costumeNames: Map<String, Int>,
    val stateFun: SpriteStateFun,
    initialState: SpriteState
) : Drawable {
    var state: SpriteState = initialState
    var currentCostumeNum: Int = 0 // initially, the first costume is selected

    fun update(program: Program) {
        state = program.stateFun(state)
    }

    override fun draw(program: Program) {
        costumes[currentCostumeNum].draw(state, program)
    }
}

data class SpriteState(val position: Vector2, val size: Double)

typealias SpriteStateFun = Program.(SpriteState) -> SpriteState

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

val sprite: (BuildFun<SpriteBuilder>) -> Sprite = build(::SpriteBuilder)

// Costumes
interface Costume {
    fun draw(state: SpriteState, program: Program)
}

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

class TextCostume(val text: String, val font: Lazy<FontMap>, val config: Drawer.() -> Unit) : Costume {
    override fun draw(state: SpriteState, program: Program) = with(program) {
        drawer.fontMap = font.value
        drawer.config()
        drawer.text(text, state.position)
    }
}

// Backgrounds
interface Background : Drawable

class ImageBackground : Background {
    val bg: Lazy<ColorBuffer>
    val config: BuildFun<Drawer>

    constructor(image: ColorBuffer, config: BuildFun<Drawer>) {
        bg = lazy { image }
        this.config = config
    }

    constructor(path: String, config: BuildFun<Drawer>) {
        bg = lazy { loadImage(path) }
        this.config = config
    }

    override fun draw(program: Program) {
        program.drawer.config()
        program.drawer.image(bg.value)
    }
}

// Main
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