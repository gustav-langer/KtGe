package de.thecommcraft.ktge

import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.events.Event
import org.openrndr.math.Vector2

typealias BuildFun<T> = T.() -> Unit // TODO find a good name for this
typealias BuiltSprite = (parent: SpriteHost, app: KtgeApp) -> Drawable // TODO also find a name // there are 2 hard things in programming: cache-invalidation, off-by-one errors and naming things
typealias CostumeList = NamedList<Costume, String>
typealias MutableCostumeList = MutableNamedList<Costume, String>

interface Drawable {
    fun draw(program: Program)
    fun update()
}

interface SpriteHost {
    fun createSprite(sprite: BuiltSprite)
    fun removeSprite(sprite: Drawable)
}

open class SpriteState

open class Sprite(
    runOnce: List<BuildFun<Sprite>>,
    private val runEachFrame: List<BuildFun<Sprite>>,
    val costumes: CostumeList, // Must be non-empty, this is ensured when using SpriteBuilder
    val parent: SpriteHost,
    val app: KtgeApp
) : Drawable, SpriteHost {
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

    val childSprites: MutableSet<Drawable> = mutableSetOf()

    init {
        for (f in runOnce) f()
    }

    override fun update() {
        for (f in runEachFrame) f()
        for (spr in childSprites) spr.update()
    }

    override fun draw(program: Program) {
        costumes[costumeIdx].draw(program, position)
        for (spr in childSprites) spr.draw(program)
    }

    override fun createSprite(sprite: BuiltSprite) {
        childSprites.add(sprite(this, app))
    }

    override fun removeSprite(sprite: Drawable) {
        childSprites.remove(sprite)
    }
}

class SpriteBuilder(val parent: SpriteHost, val app: KtgeApp) : KtgeApp by app {
    private val costumes: MutableCostumeList = emptyMutableNamedList()

    private val runOnce: MutableList<BuildFun<Sprite>> = mutableListOf()
    private val runEachFrame: MutableList<BuildFun<Sprite>> = mutableListOf()

    private val eventListeners: MutableList<BuildFun<Sprite>> = mutableListOf()

    fun costume(c: Costume, name: String? = null) {
        costumes.addNullable(c, name)
    }

    fun init(code: Sprite.() -> Unit) = runOnce.add(code)

    fun frame(code: Sprite.() -> Unit) = runEachFrame.add(code)

    fun <E> on(event: Event<E>, code: Sprite.(E) -> Unit) = eventListeners.add {
        event.listen { code(it) }
    }

    fun build(): Sprite {
        if (costumes.isEmpty()) costumes.add(EmptyCostume)
        val sprite = Sprite(runOnce, runEachFrame, costumes, parent, app)
        for (registerEvent in eventListeners) sprite.registerEvent()
        return sprite
    }
}

fun sprite(init: BuildFun<SpriteBuilder>): BuiltSprite = fun(parent: SpriteHost, app: KtgeApp): Sprite {
    val builder = SpriteBuilder(parent, app)
    builder.init()
    return builder.build()
}

interface KtgeApp : Program, SpriteHost

// Main
fun ktge(
    sprites: List<BuiltSprite>, config: BuildFun<Configuration> = {}, background: ColorRGBa? = ColorRGBa.BLACK
) = application {
    configure(config)
    program {
        val spritesActual: MutableList<Drawable> = mutableListOf() // TODO name
        val appImpl = object : KtgeApp, Program by this {
            override fun createSprite(sprite: BuiltSprite) {
                spritesActual.add(sprite(this, this))
            }

            override fun removeSprite(sprite: Drawable) {
                spritesActual.remove(sprite)
            }
        }

        sprites.forEach(appImpl::createSprite)

        backgroundColor = background
        extend {
            for (spr in spritesActual) {
                spr.update()
                spr.draw(this)
            }
        }
    }
}
