package de.thecommcraft.ktge

import org.openrndr.Program
import org.openrndr.math.Vector2
import org.openrndr.draw.RenderTarget
import org.openrndr.events.Event
import org.openrndr.draw.renderTarget as rT
import kotlin.random.Random

open class DynamicSprite<T: DynamicSpriteState>(
    costumes: List<Pair<Costume, String?>>,
    val parent: DynamicSpriteGroup<T>,
    var spriteId: Int,
) : Sprite(listOf(), listOf(), costumes) {

    fun getState(): SpriteState {
        return parent.getSpriteState(spriteId)
    }

    override fun draw(program: Program): Unit {
        costumes[costumeIdx].first.draw(program, position)
    }
}

abstract class DynamicSpriteState (val spriteId: Int, open val parent: DynamicSpriteGroup<*>, costumes: List<Pair<Costume, String?>> = listOf(), costumeIdx: Int = 0) {
    val costumeData: CostumeData = CostumeData(costumes.toList(), costumeIdx)
    abstract fun getSpriteState(): SpriteState
    abstract fun getPosition(): Vector2
}

class CostumeData(var costumes: List<Pair<Costume, String?>>, var costumeIdx: Int = 0)

abstract class DynamicSpriteGroup<T: DynamicSpriteState> : Drawable() {
    protected val dynamicSpriteStates : MutableMap<Int, T> = mutableMapOf()

    abstract fun getDynamicSpriteState(spriteId: Int): DynamicSpriteState

    abstract fun getSpriteState(spriteId: Int): SpriteState

    abstract fun getCostumeData(spriteId: Int): CostumeData

    open fun getSprite(spriteId: Int): DynamicSprite<T> {
        val costumeData = getCostumeData(spriteId)
        return DynamicSprite(costumeData.costumes, this, spriteId)
    }

    open fun addSprite(spriteId: Int, init: BuildFun<T>): DynamicSprite<T> {
        if (spriteId in dynamicSpriteStates) {
            throw RuntimeException("Id exists")
        }
        dynamicSpriteStates[spriteId] = newSpriteState(spriteId, init)
        return getSprite(spriteId)
    }

    abstract fun newSpriteState(spriteId: Int, init: BuildFun<T>): T
}

class TileState(spriteId: Int, override val parent: TileGrid, costumes: List<Pair<Costume, String?>>) : DynamicSpriteState(spriteId, parent, costumes) {
    var tileX: Int? = null
    var tileY: Int? = null
    var tileType: Int?
        get() = parent.tiles?.get(tileX!!)?.get(tileY!!)
        set(value) {parent.tiles?.get(tileX!!)?.set(tileY!!, value)}

    override fun getSpriteState(): SpriteState {
        return parent.getSpriteState(spriteId)
    }

    override fun getPosition(): Vector2 {
        return Vector2((parent.tileSize * tileX!!).toDouble(), (parent.tileSize * tileY!!).toDouble())
    }

    fun determineCostumes(): Unit {
        costumeData.costumes = parent.tileTypeCostumes(tileType) ?: listOf(EmptyCostume to null)
    }
}

open class TileGrid(
    val width: Int,
    val height: Int,
    val tileSize: Int,
    val costumes: Map<Int, List<Pair<Costume, String?>>>,
    val tiles: MutableList<MutableList<Int?>> = mutableListOf(),
    val runOnce: MutableList<BuildFun<TileGrid>> = mutableListOf(),
    val runEachFrame: MutableList<BuildFun<TileGrid>> = mutableListOf(),
) : DynamicSpriteGroup<TileState>() {
    var position: Vector2 = Vector2.ZERO
    val totalWidth: Int = width * tileSize
    val totalHeight: Int = height * tileSize
    val renderTarget: RenderTarget = rT(totalWidth, totalHeight) {
        colorBuffer()
    }

    init {
        for (i: Int in 0..width) {
            for (j: Int in 0..height) {
                val spriteId: Int = Random.nextInt()
                val sprite = addSprite(spriteId) {
                    tileX = i
                    tileY = j
                }
            }
        }
        for (f in runOnce) f()
    }
    override fun getDynamicSpriteState(spriteId: Int): TileState {
        return dynamicSpriteStates[spriteId]!!
    }
    override fun getSpriteState(spriteId: Int): SpriteState {
        return SpriteState()
    }
    override fun getCostumeData(spriteId: Int): CostumeData {
        return getDynamicSpriteState(spriteId).costumeData
    }
    fun tileTypeCostumes(tileType: Int?): List<Pair<Costume, String?>>? {
        return costumes[tileType]
    }
    override fun newSpriteState(spriteId: Int, init: BuildFun<TileState>): TileState {
        val spriteState = TileState(spriteId, this, listOf())
        spriteState.init()
        spriteState.tileX!!
        spriteState.tileY!!
        spriteState.determineCostumes()
        return spriteState
    }
    override fun update() {
        for (f in runEachFrame) f()
    }
    override fun draw(program: Program): Unit {
        program.drawer.image(renderTarget.colorBuffer(0), position)
    }
}

class TileTypeCostumeBuilder {
    val costumes: MutableList<Pair<Costume, String?>> = mutableListOf()
    fun costume(c: Costume, name: String? = null) = costumes.add(c to name)
}

class TileGridBuilder(app: KtgeApp) : KtgeApp by app {
    private var gridWidth: Int? = null
    private var gridHeight: Int? = null
    private var tileSize: Int? = null

    private val costumes: MutableMap<Int, List<Pair<Costume, String?>>> = mutableMapOf()

    private val runOnce: MutableList<BuildFun<TileGrid>> = mutableListOf()
    private val runEachFrame: MutableList<BuildFun<TileGrid>> = mutableListOf()

    private val eventListeners: MutableList<BuildFun<TileGrid>> = mutableListOf()

    fun tileType(t: Int, code: TileTypeCostumeBuilder.() -> Unit) {
        val tileTypeCostumeBuilder = TileTypeCostumeBuilder()
        tileTypeCostumeBuilder.code()
        costumes[t] = tileTypeCostumeBuilder.costumes
    }

    fun init(code: TileGrid.() -> Unit) = runOnce.add(code)

    fun frame(code: TileGrid.() -> Unit) = runEachFrame.add(code)

    fun <E> on(event: Event<E>, code: TileGrid.(E) -> Unit) = eventListeners.add {
        event.listen { code(it) }
    }

    fun build(): TileGrid {
        val tileGrid = TileGrid(gridWidth!!, gridHeight!!, tileSize!!, costumes, mutableListOf(), runOnce, runEachFrame)
        for (registerEvent in eventListeners) tileGrid.registerEvent()
        return tileGrid
    }
}