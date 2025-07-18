package de.thecommcraft.ktge

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import java.util.Collections
import java.util.WeakHashMap
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

typealias ApplicableFun<T> = T.() -> Unit
typealias BuildFun<T> = ApplicableFun<T>
typealias SpriteCode = BuildFun<Sprite>

interface ToInitialize {
    fun init(parent: SpriteHost, program: Program, app: KtgeApp)
    fun uninit() {}
}

interface Positioned {
    var position: Vector2
}

interface Drawable : ToInitialize {
    fun update()
    fun draw()
}

interface SpriteHost {
    val currentSprites: List<Drawable>
    fun createSprite(sprite: Drawable)

    /**
     * This should be final. If you want to readd the sprite later, you should instead disable it.
     */
    fun removeSprite(sprite: Drawable)

    /**
     * This should be final. If you want to readd the sprites later, you should instead disable them.
     */
    fun removeSprites(predicate: (Drawable) -> Boolean)

    /**
     * @return A function to reenable the drawable. This should be expected to only work once.
     */
    fun disableSprite(sprite: Drawable): () -> Unit

    /**
     * @return A mapping of drawable to function to reenable a drawable. Every function should be expected to only work once.
     */
    fun disableSprites(predicate: (Drawable) -> Boolean): Map<Drawable, () -> Unit>
}

abstract class Sprite : Drawable, SpriteHost, Positioned {
    lateinit var parent: SpriteHost
    lateinit var program: Program
    lateinit var app: KtgeApp

    private val eventListeners: MutableMap<EventListener<*>, Unit> = Collections.synchronizedMap(WeakHashMap())

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
    override val currentSprites: List<Drawable>
        get() = childSprites.toList()
    private val runEachFrame: MutableList<SpriteCode> = mutableListOf()
    private val scheduledCode: MutableList<SpriteCode> = mutableListOf()

    fun costume(costume: Costume, name: String? = null) {
        costumes.addNullable(costume, name)
    }

    fun frame(code: SpriteCode) {
        runEachFrame.add(code)
    }

    /**
     * Event listeners will be automatically disabled when the sprite is removed from its parent.
     */
    fun <E> on(event: Event<E>, code: Sprite.(E) -> Unit): EventListener<E> {
        val eventListener = EventListener(event) { this.code(it) }
        eventListeners[eventListener] = Unit
        eventListener.listen()
        return eventListener
    }

    /**
     * Event listeners will be automatically disabled when the sprite is removed from its parent.
     */
    fun <E> onFirst(event: Event<E>, code: Sprite.(E) -> Unit): EventListener<E> {
        var eventListener = EventListener(event) {}
        var wasExecuted = false
        eventListener = EventListener(event) {
            eventListener.unlisten()
            this.code(it)
        }
        eventListeners[eventListener] = Unit
        eventListener.listen()
        return eventListener
    }

    fun schedule(code: SpriteCode) = scheduledCode.add(code)

    protected abstract fun initSprite()

    open fun uninitSprite() {}

    override fun init(parent: SpriteHost, program: Program, app: KtgeApp) {
        this.parent = parent
        this.program = program
        this.app = app
        initSprite()
    }

    override fun uninit() {
        eventListeners.toList().forEach { (t, _) -> t.unlisten() }
        childSprites.toList().forEach(this::removeSprite)
        uninitSprite()
    }

    override fun update() {
        val scheduledCopy = scheduledCode.toList()
        scheduledCode.clear()
        scheduledCopy.forEach(::apply)
        runEachFrame.forEach(::run)
        for (spr in childSprites) spr.update()
    }

    override fun draw() {
        costumes.getOrNull(costumeIdx)?.draw(program, position)
        for (spr in childSprites) spr.draw()
    }

    override fun createSprite(sprite: Drawable) {
        childSprites.add(sprite)
        app.registerSprite(sprite)
        sprite.init(this, program, app)
    }

    override fun removeSprite(sprite: Drawable) {
        sprite.uninit()
        childSprites.remove(sprite)
    }

    override fun removeSprites(predicate: (Drawable) -> Boolean) {
        childSprites.removeIf {
            val result = predicate(it)
            if (result) it.uninit()
            result
        }
    }

    override fun disableSprite(sprite: Drawable): () -> Unit {
        childSprites.remove(sprite)
        return { childSprites.add(sprite) }
    }

    override fun disableSprites(predicate: (Drawable) -> Boolean): Map<Drawable, () -> Unit> {
        return childSprites.filter(predicate).associateWith(this::disableSprite)
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
    /**
     * Should be called on creation of any sprite. Should be expected to only work once for every sprite.
     */
    fun registerSprite(sprite: Drawable)

    @Deprecated("Access SpriteHost::currentSprites instead.")
    fun getTotalDepth(): Int

    @Deprecated("Define a SpriteHost with custom draw function instead to control draw order.")
    fun setDepth(sprite: Drawable, depth: Int)
}

// Main
fun ktge(
    sprites: List<Drawable>,
    initialize: List<ToInitialize> = listOf(),
    config: BuildFun<Configuration> = {},
    background: ColorRGBa? = ColorRGBa.BLACK,
    frameRate: Long? = null,
    extensions: List<Extension> = listOf()
) = application {
    configure(config)

    program {
        val currentSprites: MutableList<Drawable> = mutableListOf()
        val appImpl = object : KtgeApp, Program by this {
            override val currentSprites: List<Drawable>
                get() = currentSprites
            val initialized: WeakHashMap<Drawable, Boolean> = WeakHashMap()
            override fun createSprite(sprite: Drawable) {
                currentSprites.add(sprite)
                registerSprite(sprite)
                sprite.init(this, this, this)
            }

            override fun removeSprite(sprite: Drawable) {
                sprite.uninit()
                currentSprites.remove(sprite)
            }

            override fun removeSprites(predicate: (Drawable) -> Boolean) {
                currentSprites.removeIf {
                    val result = predicate(it)
                    if (result) it.uninit()
                    result
                }
            }

            @Deprecated("Access SpriteHost's currentSprites instead.", replaceWith = ReplaceWith("currentSprites of SpriteHost"))
            override fun getTotalDepth(): Int {
                return currentSprites.size
            }

            @Deprecated("Define a SpriteHost with custom draw function instead to control draw order.")
            override fun setDepth(sprite: Drawable, depth: Int) {
                currentSprites.remove(sprite)
                currentSprites.add(currentSprites.size - depth, sprite)
            }

            override fun disableSprite(sprite: Drawable): () -> Unit {
                currentSprites.remove(sprite)
                return { currentSprites.add(sprite) }
            }

            override fun disableSprites(predicate: (Drawable) -> Boolean): Map<Drawable, () -> Unit> {
                return currentSprites.filter(predicate).associateWith { disableSprite(it) }
            }

            override fun registerSprite(sprite: Drawable) {
                initialized[sprite]?.let { check(!it) }
                initialized[sprite] = true
            }
        }

        sprites.forEach(appImpl::createSprite)
        initialize.forEach { it.init(appImpl, appImpl, appImpl) }

        backgroundColor = background

        extensions.forEach(::extend)
        var lastTime = 0.0
        var secsToNextDraw = 0.0

        extend {
            if (lastTime == 0.0) {
                lastTime = seconds
            }
            frameRate?.let {
                secsToNextDraw -= seconds - lastTime
                lastTime = seconds
                if (secsToNextDraw <= 0.0) {
                    secsToNextDraw += 1 / it.toDouble()
                }
                else {
                    Thread.sleep(secsToNextDraw.seconds.toJavaDuration())
                    secsToNextDraw -= seconds - lastTime
                    lastTime = seconds
                    if (secsToNextDraw <= 0.0) {
                        secsToNextDraw += 1 / it.toDouble()
                    }
                }
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
