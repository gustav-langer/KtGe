package de.thecommcraft.ktge

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import kotlin.random.Random
import kotlinx.coroutines.delay

typealias BuildFun<T> = T.() -> Unit // TODO find a good name for this
typealias BuiltSprite = (KtgeApp) -> Drawable // TODO also find a name // there are 2 hard things in programming: cache-invalidation, off-by-one errors and naming things
typealias CostumeList = NamedList<Costume, String>
typealias MutableCostumeList = MutableNamedList<Costume, String>

interface Drawable {
    fun draw(program: Program)
    fun update()
}

open class SpriteState

open class Sprite(
    runOnce: List<BuildFun<Sprite>>,
    private val runEachFrame: List<BuildFun<Sprite>>,
    val costumes: CostumeList // Must be non-empty, this is ensured when using SpriteBuilder
) : Drawable {
    // Default values: the sprite is in the top left corner with its first costume selected
    val spriteState: SpriteState = SpriteState()
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

interface KtgeApp : Program {
    fun createSprite(sprite: BuiltSprite)
}

// Main
fun ktge(
    sprites: List<BuiltSprite>, config: BuildFun<Configuration> = {}, background: ColorRGBa? = ColorRGBa.BLACK, frameRate: Long = 60L
) = application {
    configure(config)
    program {
        window.presentationMode = PresentationMode.MANUAL
        var lastTime = seconds

        val spritesActual: MutableList<Drawable> = mutableListOf() // TODO name
        val appImpl = object : KtgeApp, Program by this {
            override fun createSprite(sprite: BuiltSprite) {
                spritesActual.add(sprite(this))
            }
        }

        sprites.mapTo(spritesActual) { it(appImpl) }

        backgroundColor = background
        extend {
            launch {
                val t = 1000.0 / frameRate
                val t0 = seconds - lastTime
                lastTime = seconds
                val d = (t - t0 * 1000).toLong() // in millis

                if (d > 0L) delay(d)

                window.requestDraw()
            }
            for (spr in spritesActual) {
                spr.update()
                spr.draw(this)
            }
        }
    }
}
