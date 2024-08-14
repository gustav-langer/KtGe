package de.thecommcraft.ktge

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.FontMap
import org.openrndr.events.Event
import org.openrndr.math.Vector2

typealias BuildFun<T> = T.() -> Unit // TODO find a good name for this
typealias BuiltSprite = (KtgeApp) -> Sprite // TODO also find a name // there are 3 hard things in programming: off-by-one errors and naming things

class Sprite(
    runOnce: List<BuildFun<Sprite>>,
    val runEachFrame: List<BuildFun<Sprite>>,
    val costumes: List<Pair<Costume, String?>> // Must be non-empty, this is ensured when using SpriteBuilder // TODO replace this with some sort of List<Costume> + BiMap<name, index>
) {
    // Default values: the sprite is in the top left corner with its first costume selected
    var position: Vector2 = Vector2.ZERO
    var costumeIdx: Int = 0
    var costumeName: String?
        get() = costumes[costumeIdx].second
        set(value) {
            val idx = costumes.indexOfFirst { it.second == value }
            if (value != null && idx != -1) costumeIdx = idx
        }

    init {
        for (f in runOnce) f()
    }

    fun update() {
        for (f in runEachFrame) f()
    }

    fun draw(program: Program) {
        costumes[costumeIdx].first.draw(program, position)
    }
}

class SpriteBuilder(app: KtgeApp) : KtgeApp by app {
    private val costumes: MutableList<Pair<Costume, String?>> = mutableListOf()

    private val runOnce: MutableList<BuildFun<Sprite>> = mutableListOf()
    private val runEachFrame: MutableList<BuildFun<Sprite>> = mutableListOf()

    private val eventListeners: MutableList<BuildFun<Sprite>> = mutableListOf()

    fun costume(c: Costume, name: String? = null) = costumes.add(c to name)

    fun init(code: Sprite.() -> Unit) = runOnce.add(code)

    fun frame(code: Sprite.() -> Unit) = runEachFrame.add(code)

    fun <E> on(event: Event<E>, code: Sprite.(E) -> Unit) = eventListeners.add {
        event.listen { code(it) }
    }

    fun build(): Sprite {
        val costumesNonEmpty = if (costumes.size > 0) costumes else listOf(EmptyCostume to null)
        val sprite = Sprite(runOnce, runEachFrame, costumesNonEmpty)
        for (registerEvent in eventListeners) sprite.registerEvent()
        return sprite
    }
}

fun sprite(init: BuildFun<SpriteBuilder>): BuiltSprite = fun(app: KtgeApp): Sprite {
    val builder = SpriteBuilder(app)
    builder.init()
    return builder.build()
}

interface Costume {
    fun draw(program: Program, position: Vector2)
}

class DrawerCostume(val code: Program.(Vector2) -> Unit) : Costume {
    override fun draw(program: Program, position: Vector2) {
        program.code(position)
    }
}

class ImageCostume(val img: ColorBuffer, val drawerConfig: BuildFun<Drawer>) : Costume {
    override fun draw(program: Program, position: Vector2) = program.run {
        drawer.drawerConfig() // TODO find a good name for this
        drawer.image(img, position)
    }
}

class TextCostume(val text: String, val font: FontMap, val drawerConfig: BuildFun<Drawer>) : Costume {
    override fun draw(program: Program, position: Vector2) = program.run {
        drawer.drawerConfig()
        drawer.fontMap = font
        drawer.text(text, position)
    }
}

object EmptyCostume : Costume {
    override fun draw(program: Program, position: Vector2) {}
}

interface KtgeApp : InputEvents, Clock {
    var width: Int
    var height: Int

    fun createSprite(sprite: BuiltSprite)
    fun exit()
}

// Main
fun ktge(
    sprites: List<BuiltSprite>, config: BuildFun<Configuration> = {}, background: ColorRGBa? = ColorRGBa.BLACK
) = application {
    configure(config)
    program {
        val spritesActual: MutableList<Sprite> = mutableListOf() // TODO name
        val appImpl = object : KtgeApp, InputEvents by this, Clock by this {
            override var width: Int = this@program.width
            override var height: Int = this@program.height

            override fun createSprite(sprite: BuiltSprite) {
                spritesActual.add(sprite(this))
            }

            override fun exit() {
                application.exit()
            }
        }

        sprites.mapTo(spritesActual) { it(appImpl) }

        backgroundColor = background
        extend {
            for (spr in spritesActual) {
                spr.update()
                spr.draw(this)
            }
        }
    }
}
