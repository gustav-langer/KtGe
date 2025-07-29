package de.thecommcraft.ktge

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.math.Vector2
import java.util.WeakHashMap

typealias ApplicableFun<T> = T.() -> Unit
typealias BuildFun<T> = ApplicableFun<T>
typealias SpriteCode = BuildFun<Sprite>

fun interface ToInitialize : OwnedResource {
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
fun interface OwnedResource {
    fun cleanUp()
}

interface ResourceHost {
    val ownedResources: List<OwnedResource>

    fun addOwnedResource(resource: OwnedResource): OwnedResource

    fun addOwnedResource(resourceCleanupFun: () -> Unit): OwnedResource {
        return addOwnedResource(OwnedResource(resourceCleanupFun))
    }

    fun removeOwnedResource(resource: OwnedResource)
}

sealed interface OverwriteType

interface OverwriteMouseEvents : OverwriteType {
    fun overwrite(mouseEvents: MouseEvents)
}
interface OverwriteKeyEvents : OverwriteType {
    fun overwrite(keyEvents: KeyEvents)
}

interface KtgeExtension {
    var enabled: Boolean
    fun setup(app: KtgeApp) {}
    fun beforeUpdate(app: KtgeApp) {}
    fun afterUpdate(app: KtgeApp) {}
    fun beforeDraw(drawer: Drawer, app: KtgeApp) {}
    fun afterDraw(drawer: Drawer, app: KtgeApp) {}
}

abstract class CollidableSprite : Sprite(), PositionedCollider

interface KtgeApp : Program, SpriteHost, ResourceHost {

    val deltaTime: Double

    val overwriteKeyEvents: OverwriteKeyEvents

    val overwriteMouseEvents: OverwriteMouseEvents

    val keyTracker: KeyTracker

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

    fun exit()
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
        var shouldExit = false
        val appImpl = object : KtgeApp, Program by this {

            private val ownedResourceSet: MutableSet<OwnedResource> = mutableSetOf()
            override val ownedResources: List<OwnedResource>
                get() = ownedResourceSet.toList()

            override val deltaTime
                get() = computedDeltaTime

            override val program = this

            var currentMouseEvents = this@program.mouse
            override val mouse get() = currentMouseEvents
            override val overwriteMouseEvents = object : OverwriteMouseEvents {
                override fun overwrite(mouseEvents: MouseEvents) {
                    currentMouseEvents = mouseEvents
                }
            }
            var currentKeyEvents = this@program.keyboard
            override val keyboard get() = currentKeyEvents
            override val overwriteKeyEvents = object : OverwriteKeyEvents {
                override fun overwrite(keyEvents: KeyEvents) {
                    currentKeyEvents = keyEvents
                    keyTracker = KeyTracker(currentKeyEvents)
                }
            }
            override var keyTracker = KeyTracker(keyboard)

            override val currentSprites: List<Drawable>
                get() = currentSprites
            val initialized: WeakHashMap<Drawable, Boolean> = WeakHashMap()
            var endedTriggered = false

            init {
                ended.listenOnce {
                    if (endedTriggered) return@listenOnce
                    endedTriggered = true
                    ownedResourceSet.toSet().forEach(::removeOwnedResource)
                }
            }
            override fun createSprite(sprite: Drawable) {
                currentSprites.add(sprite)
                registerSprite(sprite)
                addOwnedResource(sprite)
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

            override fun addOwnedResource(resource: OwnedResource): OwnedResource {
                ownedResourceSet.add(resource)
                return resource
            }

            override fun removeOwnedResource(resource: OwnedResource) {
                resource.cleanUp()
                ownedResourceSet.remove(resource)
            }

            override fun exit() {
                shouldExit = true
            }
        }

        extensions.forEach {
            it.setup(appImpl)
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
                computedDeltaTime = seconds - lastFrameTime
                lastFrameTime = seconds
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
            if (shouldExit) application.exit()
        }
    }
}
