package de.thecommcraft.ktge

import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.events.Event
import org.openrndr.math.Vector2

typealias BuildFun<T> = T.() -> Unit // TODO find a good name for this
typealias SpriteCode = BuildFun<Sprite>

interface Drawable {
    fun draw(program: Program)
    fun update()
}

interface SpriteHost {
    fun createSprite(sprite: Sprite)
    fun removeSprite(sprite: Sprite)
}

// TODO does this need access to SpriteHost parent? where might that be used?
abstract class Sprite : Drawable, SpriteHost {
    lateinit var program: Program

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

    private val costumes: MutableNamedList<Costume, String> = mutableNamedListOf()
    private val childSprites: MutableSet<Sprite> = mutableSetOf()
    private val runEachFrame: MutableList<SpriteCode> = mutableListOf()
    private val scheduledCode: MutableList<SpriteCode> = mutableListOf()

    fun costume(costume: Costume, name: String? = null) {
        costumes.addNullable(costume, name)
    }

    fun frame(code: SpriteCode) {
        runEachFrame.add(code)
    }

    fun <E> on(event: Event<E>, code: Sprite.(E) -> Unit) {
        event.listen { code(it) } // The warning here is a known bug, see https://youtrack.jetbrains.com/issue/KT-21282 TODO remove this comment if the warning is gone
    }

    fun schedule(code: SpriteCode) = scheduledCode.add(code)

    protected abstract fun initSprite()
    fun init(program: Program) {
        this.program = program
        initSprite()
    }

    override fun update() {
        val scheduledCopy = scheduledCode.toList()
        scheduledCode.clear()
        for (f in scheduledCopy) f()
        for (f in runEachFrame) f()
        for (spr in childSprites) spr.update()
    }

    override fun draw(program: Program) {
        costumes.getOrNull(costumeIdx)?.draw(program, position)
        for (spr in childSprites) spr.draw(program)
    }

    override fun createSprite(sprite: Sprite) {
        childSprites.add(sprite)
        sprite.init(program)
    }

    override fun removeSprite(sprite: Sprite) {
        childSprites.remove(sprite)
    }

    companion object {
        // Creates a simple sprite that only overrides the initSprite function
        fun sprite(code: Sprite.(Program) -> Unit) = object : Sprite() {
            override fun initSprite() = code(program)
        }
    }
}

interface KtgeApp : Program, SpriteHost

// Main
fun ktge(
    sprites: List<Sprite>,
    config: BuildFun<Configuration> = {},
    background: ColorRGBa? = ColorRGBa.BLACK
) = application {
    configure(config)

    program {
        val spritesActual: MutableList<Sprite> = mutableListOf() // TODO name
        val appImpl = object : KtgeApp, Program by this {
            override fun createSprite(sprite: Sprite) {
                spritesActual.add(sprite)
                sprite.init(this)
            }

            override fun removeSprite(sprite: Sprite) {
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
