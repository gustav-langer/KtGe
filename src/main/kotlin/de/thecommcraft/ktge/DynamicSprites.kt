package de.thecommcraft.ktge

/*import org.openrndr.Program
import org.openrndr.draw.ResizableRenderTarget
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import kotlin.random.Random
import org.openrndr.draw.resizableRenderTarget as rT

open class DynamicSprite<T : DynamicSpriteState>(
    costumes: CostumeList,
    val parent: DynamicSpriteGroup<T>,
    var spriteId: Int,
) : Sprite(listOf(), listOf(), costumes) {

    fun getState(): SpriteState {
        return parent.getSpriteState(spriteId)
    }

    override fun draw(program: Program) {
        parent.getDynamicSpriteState(spriteId).draw(program)
    }
}

abstract class DynamicSpriteState(
    val spriteId: Int,
    open val parent: DynamicSpriteGroup<*>,
    costumes: MutableCostumeList = emptyMutableNamedList(),
    costumeIdx: Int = 0
) {
    val costumeData: CostumeData = CostumeData(costumes, costumeIdx)
    abstract fun getSpriteState(): SpriteState
    abstract fun getPosition(): Vector2
    abstract fun draw(program: Program)
}

class CostumeData(var costumes: CostumeList, var costumeIdx: Int = 0)

abstract class DynamicSpriteGroup<T : DynamicSpriteState> : Drawable {
    protected val dynamicSpriteStates: MutableMap<Int, T> = mutableMapOf()

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

class TileState(spriteId: Int, override val parent: TileGrid, costumes: CostumeList) :
    DynamicSpriteState(spriteId, parent, costumes.toMutable()) {
    var tileX: Int? = null
    var tileY: Int? = null
    var tileType: Int?
        get() = parent.tiles.getOrNull(tileX!!)?.getOrNull(tileY!!)
        set(value) {
            parent.tiles[tileX!!][tileY!!] = value
        }
    var costumeIdx: Int = 0
    var costumeName: String?
        get() = costumeData.costumes.nameOf(costumeIdx)
        set(value) {
            value?.let {
                costumeData.costumes.indexOfName(value)?.let {
                    costumeIdx = it
                }
            }
        }

    override fun getSpriteState(): SpriteState {
        return parent.getSpriteState(spriteId)
    }

    override fun getPosition(): Vector2 {
        return Vector2((parent.tileSize * tileX!!).toDouble(), (parent.tileSize * tileY!!).toDouble())
    }

    fun drawToParent(program: Program) {
        program.drawer.withTarget(parent.renderTarget.renderTarget) {
            costumeName?.let { (costumeData.costumes[it]?.draw(program, getPosition())) }
        }
    }

    fun determineCostumes() {
        costumeData.costumes =
            parent.tileTypeCostumes(tileType) ?: MapNamedList(listOf(EmptyCostume), listOf())
    }

    override fun draw(program: Program) {
        drawToParent(program)
    }
}

open class TileGrid(
    val width: Int,
    val height: Int,
    val tileSize: Int,
    val costumes: Map<Int, MutableCostumeList>,
    tiles: MutableList<MutableList<Int?>> = mutableListOf(),
    runOnce: MutableList<BuildFun<TileGrid>> = mutableListOf(),
    val runEachFrame: MutableList<BuildFun<TileGrid>> = mutableListOf(),
    val app: KtgeApp,
) : DynamicSpriteGroup<TileState>() {
    val tiles: MutableList<MutableList<Int?>> = ((0..<width).map { i: Int ->
        ((0..<height).map { j: Int ->
            tiles.getOrNull(i)?.getOrNull(j)
        }).toMutableList()
    }).toMutableList()
    var position: Vector2 = Vector2.ZERO
    val totalWidth: Int = width * tileSize
    val totalHeight: Int = height * tileSize
    val renderTarget: ResizableRenderTarget = rT(totalWidth, totalHeight, 1.0) {
        colorBuffer()
    }
    var drawn: Boolean = false
    val spriteIds: MutableList<MutableList<Int>> =
        MutableList(width) { MutableList(height) { 0 } }

    init {
        for (i: Int in 0..<width) {
            for (j: Int in 0..<height) {
                val spriteId: Int = Random.nextInt()
                val sprite = addSprite(spriteId) {
                    tileX = i
                    tileY = j
                }
                spriteIds[i][j] = spriteId
            }
        }
        for (f in runOnce) f()
    }

    fun getSpriteId(tileX: Int, tileY: Int): Int {
        return spriteIds[tileX][tileY]
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

    fun tileTypeCostumes(tileType: Int?): CostumeList? {
        return costumes[tileType]
    }

    override fun newSpriteState(spriteId: Int, init: BuildFun<TileState>): TileState {
        val spriteState = TileState(spriteId, this, emptyNamedList())
        spriteState.init()
        spriteState.tileX!!
        spriteState.tileY!!
        spriteState.determineCostumes()
        return spriteState
    }

    override fun update() {
        for (f in runEachFrame) f()
    }

    override fun draw(program: Program) {
        if (!drawn) {
            for (i: Int in 0..<width) {
                for (j: Int in 0..<height) {
                    getSprite(getSpriteId(i, j)).draw(program)
                }
            }
            drawn = true
        }
        program.drawer.image(renderTarget.renderTarget.colorBuffer(0), position)
    }
}

class TileTypeCostumeBuilder {
    val costumes: MutableCostumeList = emptyMutableNamedList()
    fun costume(c: Costume, name: String? = null) = costumes.add(c, name ?: Random.nextInt(16777216).toString())
}

class TileGridBuilder(val app: KtgeApp) : KtgeApp by app {
    var gridWidth: Int? = null
    var gridHeight: Int? = null
    var tileSize: Int? = null

    private val costumes: MutableMap<Int, CostumeList> = mutableMapOf()

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
        val tileGrid = TileGrid(
            gridWidth!!,
            gridHeight!!,
            tileSize!!,
            costumes.toMap().mapValues { it.value.toMutable() },
            mutableListOf(),
            runOnce,
            runEachFrame,
            app
        )
        for (registerEvent in eventListeners) tileGrid.registerEvent()
        return tileGrid
    }
}

fun tileGrid(init: BuildFun<TileGridBuilder>): BuiltSprite = fun(app: KtgeApp): TileGrid {
    val builder = TileGridBuilder(app)
    builder.init()
    return builder.build()
}*/
