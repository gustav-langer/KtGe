package de.thecommcraft.ktge

import kotlinx.coroutines.delay
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.events.Event
import org.openrndr.math.Vector2

typealias ApplicableFun<T> = T.() -> Unit
typealias BuildFun<T> = ApplicableFun<T>
typealias SpriteCode = BuildFun<Sprite>

interface ToInitialize {
    fun init(parent: SpriteHost, program: Program, app: KtgeApp)
}

interface Positioned {
    var position: Vector2
}

interface Drawable : ToInitialize {
    fun update()
    fun draw()
}

interface SpriteHost {
    fun createSprite(sprite: Drawable)
    fun removeSprite(sprite: Drawable)
    fun removeSprites(predicate: (Drawable) -> Boolean)
}

class EventListener<E>(val event: Event<E>, val eventListener: (E) -> Unit) {
    fun unlisten() {
        event.cancel(eventListener)
    }
    fun listen() {
        event.listen(eventListener)
    }
    fun listenOnce() {
        event.listenOnce(eventListener)
    }
}

abstract class Sprite : Drawable, SpriteHost, Positioned {
    lateinit var parent: SpriteHost
    lateinit var program: Program
    lateinit var app: KtgeApp

    override var position: Vector2 = Vector2.ZERO
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
    val currentCostume: Costume?
        get() = costumes.getOrNull(costumeIdx)

    private val costumes: MutableNamedList<Costume, String> = mutableNamedListOf()
    private val childSprites: MutableSet<Drawable> = mutableSetOf()
    private val runEachFrame: MutableList<SpriteCode> = mutableListOf()
    private val scheduledCode: MutableList<SpriteCode> = mutableListOf()

    fun costume(costume: Costume, name: String? = null) {
        costumes.addNullable(costume, name)
    }

    fun frame(code: SpriteCode) {
        runEachFrame.add(code)
    }

    fun <E> on(event: Event<E>, code: Sprite.(E) -> Unit): EventListener<E> {
        val eventListener = EventListener(event) { this.code(it) }
        eventListener.listen()
        return eventListener
    }

    fun <E> onFirst(event: Event<E>, code: Sprite.(E) -> Unit): EventListener<E> {
        val eventListener = EventListener(event) { this.code(it) }
        eventListener.listenOnce()
        return eventListener
    }

    fun schedule(code: SpriteCode) = scheduledCode.add(code)

    protected abstract fun initSprite()
    override fun init(parent: SpriteHost, program: Program, app: KtgeApp) {
        this.parent = parent
        this.program = program
        this.app = app
        initSprite()
    }

    override fun update() {
        val scheduledCopy = scheduledCode.toList()
        scheduledCode.clear()
        for (f in scheduledCopy) f()
        for (f in runEachFrame) f()
        for (spr in childSprites) spr.update()
    }

    override fun draw() {
        costumes.getOrNull(costumeIdx)?.draw(program, position)
        for (spr in childSprites) spr.draw()
    }

    override fun createSprite(sprite: Drawable) {
        childSprites.add(sprite)
        sprite.init(this, program, app)
    }

    override fun removeSprite(sprite: Drawable) {
        childSprites.remove(sprite)
    }

    override fun removeSprites(predicate: (Drawable) -> Boolean) {
        childSprites.removeIf(predicate)
    }

    companion object {
        // Creates a simple sprite that only overrides the initSprite function
        fun sprite(code: Sprite.(Program) -> Unit) = object : Sprite() {
            override fun initSprite() = code(program)
        }
    }
}

abstract class CollidableSprite : Sprite(), PositionedCollider

interface KtgeApp : Program, SpriteHost {
    fun getTotalDepth(): Int

    fun setDepth(sprite: Drawable, depth: Int)
}

// Main
fun ktge(
    sprites: List<Drawable>,
    initialize: List<ToInitialize> = listOf(),
    config: BuildFun<Configuration> = {},
    background: ColorRGBa? = ColorRGBa.BLACK,
    frameRate: Long = 60L,
    extensions: List<Extension> = listOf()
) = application {
    configure(config)

    program {
        window.presentationMode = PresentationMode.MANUAL

        val currentSprites: MutableList<Drawable> = mutableListOf()
        val appImpl = object : KtgeApp, Program by this {
            override fun createSprite(sprite: Drawable) {
                currentSprites.add(sprite)
                sprite.init(this, this, this)
            }

            override fun removeSprite(sprite: Drawable) {
                currentSprites.remove(sprite)
            }

            override fun removeSprites(predicate: (Drawable) -> Boolean) {
                currentSprites.removeIf(predicate)
            }

            override fun getTotalDepth(): Int {
                return currentSprites.size
            }

            override fun setDepth(sprite: Drawable, depth: Int) {
                removeSprite(sprite)
                currentSprites.add(getTotalDepth() - depth, sprite)
            }
        }

        sprites.forEach(appImpl::createSprite)
        initialize.forEach { it.init(appImpl, appImpl, appImpl) }

        backgroundColor = background

        extensions.forEach(::extend)

        extend {
            launch {
                val msPerFrame = 1000.0 / frameRate
                val msGoneBy = seconds.mod(msPerFrame / 1000.0) * 1000.0
                val d = (msPerFrame - msGoneBy).toLong()

                if (d > 0L) delay(d)

                window.requestDraw()
            }
            for (spr in currentSprites) {
                spr.update()
            }
            for (spr in currentSprites) {
                spr.draw()
            }
        }
    }
}
