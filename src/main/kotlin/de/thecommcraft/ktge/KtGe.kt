package de.thecommcraft.ktge

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import kotlin.random.Random

typealias BuildFun<T> = T.() -> Unit // TODO find a good name for this
typealias BuiltSprite = (KtgeApp) -> Drawable // TODO also find a name // there are 2 hard things in programming: cache-invalidation, off-by-one errors and naming things
typealias CostumeList = NamedList<Costume, String>
typealias MutableCostumeList = MutableNamedList<Costume, String>

open class SpriteState
abstract class Drawable {
    abstract fun draw(program: Program)
    abstract fun update()
}

open class Sprite(
    runOnce: List<BuildFun<Sprite>>,
    private val runEachFrame: List<BuildFun<Sprite>>,
    val costumes: CostumeList // Must be non-empty, this is ensured when using SpriteBuilder
) : Drawable() {
    // Default values: the sprite is in the top left corner with its first costume selected
    val spriteState : SpriteState = SpriteState()
    var position: Vector2 = Vector2.ZERO
    var costumeIdx: Int = 0
    var costumeName: String?
        get() = costumes.nameOf(costumeIdx)
        set(value) {
            value?.let {
                costumes.indexOfName(value)?.let {
                    costumeIdx = it
                }
            }
        }

    init {
        for (f in runOnce) f()
    }

    override fun update() {
        for (f in runEachFrame) f()
    }

    override fun draw(program: Program) {
        costumes[costumeIdx].draw(program, position)
    }
}

class SpriteBuilder(app: KtgeApp) : KtgeApp by app {
    private val costumes: MutableCostumeList = emptyMutableNamedList()

    private val runOnce: MutableList<BuildFun<Sprite>> = mutableListOf()
    private val runEachFrame: MutableList<BuildFun<Sprite>> = mutableListOf()

    private val eventListeners: MutableList<BuildFun<Sprite>> = mutableListOf()

    fun costume(c: Costume, name: String? = null) {
        costumes.add(c, name ?: Random.nextInt(16777216).toString())
    }

    fun init(code: Sprite.() -> Unit) = runOnce.add(code)

    fun frame(code: Sprite.() -> Unit) = runEachFrame.add(code)

    fun <E> on(event: Event<E>, code: Sprite.(E) -> Unit) = eventListeners.add {
        event.listen { code(it) }
    }

    fun build(): Sprite {
        if (costumes.size == 0) costumes.add(EmptyCostume)
        val sprite = Sprite(runOnce, runEachFrame, costumes)
        for (registerEvent in eventListeners) sprite.registerEvent()
        return sprite
    }
}

fun sprite(init: BuildFun<SpriteBuilder>): BuiltSprite = fun(app: KtgeApp): Sprite {
    val builder = SpriteBuilder(app)
    builder.init()
    return builder.build()
}

interface KtgeApp : InputEvents, Clock {
    var width: Int
    var height: Int
    val program: Program

    fun createSprite(sprite: BuiltSprite)
    fun exit()
}

// Main
fun ktge(
    sprites: List<BuiltSprite>, config: BuildFun<Configuration> = {}, background: ColorRGBa? = ColorRGBa.BLACK
) = application {
    configure(config)
    program {
        val spritesActual: MutableList<Drawable> = mutableListOf() // TODO name
        val appImpl = object : KtgeApp, InputEvents by this, Clock by this {
            override var width: Int = this@program.width
            override var height: Int = this@program.height
            override val program: Program = this@program

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
