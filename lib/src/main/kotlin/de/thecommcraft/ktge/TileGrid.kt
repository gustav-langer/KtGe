package de.thecommcraft.ktge

import org.openrndr.Program
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.events.Event
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

class TileEvent {
    val change = Event<Unit>("change")
}

open class TileGrid(val tileSize: Int, var gridWidth: Int, var gridHeight: Int = gridWidth, tiles: MutableList<MutableList<Int>>? = null, private val initFun: BuildFun<TileGrid> = {}) :
    Sprite() {

    private val tileTypes: MutableMap<Int, Costume> = mutableMapOf()

    val event = TileEvent()
    val tiles: MutableList<MutableList<Int>> = tiles ?: MutableList(gridWidth) { MutableList(gridHeight) { 0 } }

    private var renderTarget: RenderTarget? = null

    val rect
        get() = Rectangle(position, gridWidth.toDouble() * tileSize, gridHeight.toDouble() * tileSize)

    fun tileType(id: Int, costume: Costume) {
        tileTypes[id] = costume
    }

    operator fun get(x: Int, y: Int): Int {
        return tiles[x][y]
    }

    operator fun set(x: Int, y: Int, value: Int) {
        tiles[x][y] = value
        drawTile(x, y)
        event.change.trigger(Unit)
    }

    fun copyTo(tileGrid: TileGrid) {
        tileGrid.loadFrom(this)
    }

    fun loadFrom(tileGrid: TileGrid) {
        loadNew(tileGrid.gridWidth, tileGrid.gridHeight, (tileGrid.tiles.map { it.toMutableList() }).toMutableList())
    }

    fun loadNew(width: Int, height: Int, tiles: MutableList<MutableList<Int>>) {
        gridWidth = width
        gridHeight = height
        this.tiles.removeIf { true }
        tiles.forEach(this.tiles::add)
        regenerateRenderTarget()
        event.change.trigger(Unit)
    }

    private fun drawTile(gridX: Int, gridY: Int) {
        renderTarget?.let {
            program.drawer.isolatedWithTarget(it) {
                program.drawer.ortho(it)
                drawTileHere(gridX, gridY)
            }
        }
    }

    private fun drawTileHere(gridX: Int, gridY: Int) {
        val position = (IntVector2(gridX, gridY) * tileSize).vector2
        tileTypes[tiles[gridX][gridY]]?.draw(program, position)
    }

    override fun uninit() {
        this.renderTarget?.let {
            it.colorBuffer(0).destroy()
            it.detachColorAttachments()
            it.destroy()
        }
        super.uninit()
    }

    override fun initSprite() {
        regenerateRenderTarget(initFun)
        costume(DrawerCostume {
            renderTarget?.let { it1 -> program.drawer.image(it1.colorBuffer(0), it) }
        })
    }

    private fun regenerateRenderTarget(initFun: BuildFun<TileGrid> = {}) {
        renderTarget?.let {
            it.colorBuffer(0).destroy()
            it.detachColorAttachments()
            it.destroy()
        }
        renderTarget = renderTarget(tileSize * gridWidth, tileSize * gridHeight, 1.0) { colorBuffer() }
        initFun()
        renderTarget?.let {
            program.drawer.isolatedWithTarget(it) {
                program.drawer.ortho(it)
                (0..<gridWidth).forEach { x -> (0..<gridHeight).forEach { y -> drawTileHere(x, y) } }
            }
        }
    }
}
