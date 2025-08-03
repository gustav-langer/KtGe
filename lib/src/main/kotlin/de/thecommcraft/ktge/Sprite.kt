package de.thecommcraft.ktge

import org.openrndr.Program
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import java.util.*

abstract class Sprite : Drawable, SpriteHost, Positioned, ResourceHost {
    open lateinit var parent: SpriteHost
    open lateinit var program: Program
    open lateinit var app: KtgeApp

    private val eventListeners: MutableMap<EventListener<*>, Unit> = Collections.synchronizedMap(WeakHashMap())
    private val ownedResourceSet: MutableSet<OwnedResource> = mutableSetOf()
    override val ownedResources: List<OwnedResource>
        get() = ownedResourceSet.toList()

    private var dead = false
    override var position: Vector2 = Vector2.ZERO
    open var costumeIdx: Int = 0
    open var costumeName: String?
        get() = costumes.nameOf(costumeIdx)
        set(value) {
            value?.let {
                costumes.indexOfName(value)?.let {
                    costumeIdx = it
                }
            }
        }
    open val currentCostume: Costume?
        get() = costumes.getOrNull(costumeIdx)

    private val costumes: MutableNamedList<Costume, String> = mutableNamedListOf()
    private val childSprites: MutableSet<Drawable> = mutableSetOf()
    override val currentSprites: List<Drawable>
        get() = childSprites.toList()
    private val runEachFrame: MutableList<SpriteCode> = mutableListOf()
    private val scheduledCode: MutableList<SpriteCode> = mutableListOf()

    open var reactivate: (() -> Unit)? = null
        protected set

    open fun costume(costume: Costume, name: String? = null) {
        costumes.addNullable(costume, name)
    }

    open fun ownedImage(image: OwnedImage, name: String? = null) {
        addOwnedResource(image)
    }

    open fun frame(code: SpriteCode) {
        runEachFrame.add(code)
    }

    /**
     * Event listeners will be automatically disabled when the sprite is removed from its parent.
     */
    open fun <E> on(event: Event<E>, code: Sprite.(E) -> Unit): EventListener<E> {
        val eventListener = EventListener(event) { this.code(it) }.add()
        eventListeners[eventListener] = Unit
        eventListener.listen()
        return eventListener
    }

    /**
     * Event listeners will be automatically disabled when the sprite is removed from its parent.
     */
    open fun <E> onNext(event: Event<E>, code: Sprite.(E) -> Unit): EventListener<E> {
        val eventListener = OneTimeEventListener(event, this) { this.code(it) }.add()
        eventListeners[eventListener] = Unit
        eventListener.listen()
        return eventListener
    }

    open fun schedule(code: SpriteCode) = scheduledCode.add(code)

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
        ownedResourceSet.forEach(OwnedResource::cleanUp)
        ownedResourceSet.clear()
        uninitSprite()
    }

    override fun update() {
        check(!dead) { "Tried to update a sprite that was already removed." }
        val scheduledCopy = scheduledCode.toList()
        scheduledCode.clear()
        scheduledCopy.forEach(this::run)
        runEachFrame.forEach(this::run)
        childSprites.toList().forEach(Drawable::update)
    }

    override fun draw() {
        check(!dead) { "Tried to draw a sprite that was already removed." }
        currentCostume?.draw(program, position)
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

    fun disable() {
        val reactivationFun = parent.disableSprite(this)
        val reactivationResource = addOwnedResource {
            reactivate = null
        }
        reactivate = {
            removeOwnedResource(reactivationResource)
            reactivationFun()
        }
    }

    override fun disableSprites(predicate: (Drawable) -> Boolean): Map<Drawable, () -> Unit> {
        return childSprites.filter(predicate).associateWith(this::disableSprite)
    }

    override fun addOwnedResource(resource: OwnedResource): OwnedResource {
        ownedResourceSet.add(resource)
        return resource
    }

    open fun<T: OwnedResource> T.add(): T {
        addOwnedResource(this)
        return this
    }

    override fun removeOwnedResource(resource: OwnedResource) {
        resource.cleanUp()
        ownedResourceSet.remove(resource)
    }

    companion object {
        // Creates a simple sprite that only overrides the initSprite function
        fun sprite(code: Sprite.(Program) -> Unit) = object : Sprite() {
            override fun initSprite() = code(program)
        }
    }
}