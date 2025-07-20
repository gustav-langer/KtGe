package de.thecommcraft.ktge

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import java.util.Collections
import java.util.WeakHashMap
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

typealias ApplicableFun<T> = T.() -> Unit
typealias BuildFun<T> = ApplicableFun<T>
typealias SpriteCode = BuildFun<Sprite>

interface ToInitialize : OwnedResource {
    fun init(parent: SpriteHost, program: Program, app: KtgeApp)
    fun uninit() {}
    override fun cleanUp() {
        uninit()
    }
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

/**
 * A resource owned by exactly one sprite that should be cleaned up with that sprite.
 */
interface OwnedResource {
    fun cleanUp()
}

abstract class Sprite : Drawable, SpriteHost, Positioned {
    lateinit var parent: SpriteHost
    lateinit var program: Program
    lateinit var app: KtgeApp

    private val eventListeners: MutableMap<EventListener<*>, Unit> = Collections.synchronizedMap(WeakHashMap())
    private val ownedResources: MutableSet<OwnedResource> = mutableSetOf()

    private var dead = false
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
        val eventListener = EventListener(event) { this.code(it) }.add()
        eventListeners[eventListener] = Unit
        eventListener.listen()
        return eventListener
    }

    /**
     * Event listeners will be automatically disabled when the sprite is removed from its parent.
     */
    fun <E> onNext(event: Event<E>, code: Sprite.(E) -> Unit): EventListener<E> {
        val eventListener = OneTimeEventListener(event, this) { this.code(it) }.add()
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
        dead = true
        eventListeners.clear()
        childSprites.clear()
        ownedResources.forEach(OwnedResource::cleanUp)
        ownedResources.clear()
        uninitSprite()
    }

    override fun update() {
        check(!dead) { "Tried to update a sprite that was already removed." }
        val scheduledCopy = scheduledCode.toList()
        scheduledCode.clear()
        scheduledCopy.forEach(this::run)
        runEachFrame.forEach(this::run)
        childSprites.forEach(Drawable::update)
    }

    override fun draw() {
        check(!dead) { "Tried to draw a sprite that was already removed." }
        costumes.getOrNull(costumeIdx)?.draw(program, position)
        childSprites.forEach(Drawable::draw)
    }

    override fun createSprite(sprite: Drawable) {
        childSprites.add(sprite.add())
        app.registerSprite(sprite)
        sprite.init(this, program, app)
    }

    override fun removeSprite(sprite: Drawable) {
        removeOwnedResource(sprite)
        childSprites.remove(sprite)
    }

    override fun removeSprites(predicate: (Drawable) -> Boolean) {
        childSprites.removeIf {
            val result = predicate(it)
            if (result) removeOwnedResource(it)
            result
        }
    }

    override fun disableSprite(sprite: Drawable): () -> Unit {
        childSprites.remove(sprite)
        return {
            check(!dead) { "Tried to reenable a sprite whose parent was already removed." }
            childSprites.add(sprite)
        }
    }

    override fun disableSprites(predicate: (Drawable) -> Boolean): Map<Drawable, () -> Unit> {
        return childSprites.filter(predicate).associateWith(this::disableSprite)
    }

    fun addOwnedResource(ownedResource: OwnedResource) {
        ownedResources.add(ownedResource)
    }

    fun<T: OwnedResource> T.add(): T {
        addOwnedResource(this)
        return this
    }

    fun removeOwnedResource(ownedResource: OwnedResource) {
        ownedResource.cleanUp()
        ownedResources.remove(ownedResource)
    }

    companion object {
        // Creates a simple sprite that only overrides the initSprite function
        fun sprite(code: Sprite.(Program) -> Unit) = object : Sprite() {
            override fun initSprite() = code(program)
        }
    }
}

interface KtgeExtension {
    var enabled: Boolean
    fun setup(program: Program) {}
    fun beforeUpdate(app: KtgeApp) {}
    fun afterUpdate(app: KtgeApp) {}
    fun beforeDraw(drawer: Drawer, app: KtgeApp) {}
    fun afterDraw(drawer: Drawer, app: KtgeApp) {}
}

abstract class CollidableSprite : Sprite(), PositionedCollider

interface KtgeApp : Program, SpriteHost {
    val deltaTime: Double
    /**
     * Should be called on creation of any sprite. Should be expected to only work once for every sprite.
     */
    fun registerSprite(sprite: Drawable)

    @Deprecated("Access KtgeApp.currentSprites instead.")
    fun getTotalDepth(): Int

    @Deprecated("Define a SpriteHost with custom draw function instead to control draw order.")
    fun setDepth(sprite: Drawable, depth: Int)

    operator fun Drawable.unaryPlus() {
        createSprite(this)
    }

    operator fun Drawable.unaryMinus() {
        removeSprite(this)
    }
}

// Main
fun ktge(
    sprites: List<Drawable>,
    initialize: List<ToInitialize> = listOf(),
    config: BuildFun<Configuration> = {},
    background: ColorRGBa? = ColorRGBa.BLACK,
    frameRate: Int = 60,
    extensions: List<KtgeExtension> = listOf()
) = application {
    configure(config)

    program {
        val currentSprites: MutableList<Drawable> = mutableListOf()
        var computedDeltaTime = 0.0
        var lastFrameTime = seconds
        val appImpl = object : KtgeApp, Program by this {
            override val deltaTime
                get() = computedDeltaTime
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

        var canvas = renderTarget(width, height, window.contentScale) {
            colorBuffer()
            depthBuffer()
        }

        var lastTime = 0.0
        var secsToNextDraw = 0.0
        backgroundColor = background

        extend {
            if (lastTime == 0.0) {
                lastTime = seconds
            }
            if (width > 0 && height > 0 && (canvas.width != width || canvas.height != height || canvas.contentScale != window.contentScale)) {
                canvas.let {
                    it.colorBuffer(0).destroy()
                    it.detachColorAttachments()
                    it.destroy()
                }
                canvas = renderTarget(width, height, window.contentScale) {
                    colorBuffer()
                    depthBuffer()
                }
            }
            secsToNextDraw -= seconds - lastTime
            lastTime = seconds
            if (secsToNextDraw <= 0.0) {
                val enabledExtensions = extensions.filter(KtgeExtension::enabled)

                enabledExtensions.forEach { it.beforeUpdate(appImpl) }

                for (spr in currentSprites) {
                    spr.update()
                }

                enabledExtensions.reversed().forEach { it.afterUpdate(appImpl) }

                drawer.isolatedWithTarget(canvas) {
                    background?.let { drawer.clear(it) }

                    enabledExtensions.forEach { it.beforeDraw(drawer, appImpl) }

                    for (spr in currentSprites) {
                        spr.draw()
                    }

                    enabledExtensions.reversed().forEach { it.afterDraw(drawer, appImpl) }
                }
                secsToNextDraw = secsToNextDraw.mod(1 / frameRate.toDouble())
            }
            drawer.image(canvas.colorBuffer(0))
        }
    }
}
