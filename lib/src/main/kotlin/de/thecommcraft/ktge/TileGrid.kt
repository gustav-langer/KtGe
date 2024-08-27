package de.thecommcraft.ktge

import org.openrndr.Program
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.renderTarget
import org.openrndr.events.Event
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2

class TileGrid(val tileSize: Int, var gridWidth: Int, var gridHeight: Int=gridWidth, tiles: MutableList<MutableList<Int>>? = null, private val initFun: BuildFun<TileGrid> = {}) :
    Drawable {
    lateinit var program: Program

    private val tileTypes: MutableMap<Int, Costume> = mutableMapOf()
    private val runEachFrame: MutableList<BuildFun<TileGrid>> = mutableListOf()
    private val runOnChange: MutableList<TileGrid.() -> Unit> = mutableListOf()

    var position: Vector2 = Vector2.ZERO
    val tiles: MutableList<MutableList<Int>> = tiles ?: MutableList(gridWidth) { MutableList(gridHeight) { 0 } }

    private lateinit var renderTarget: RenderTarget

    fun tileType(id: Int, costume: Costume) {
        tileTypes[id] = costume
    }

    fun frame(code: BuildFun<TileGrid>) = runEachFrame.add(code)

    operator fun get(x: Int, y: Int): Int {
        return tiles[x][y]
    }

    operator fun set(x: Int, y: Int, value: Int) {
        tiles[x][y] = value
        drawTile(x, y)
        runOnChange.forEach { it() }
    }

    fun loadNew(width: Int, height: Int, tiles: MutableList<MutableList<Int>>) {
        gridWidth = width
        gridHeight = height
        this.tiles.removeIf { true }
        tiles.forEach(this.tiles::add)
        regenerateRenderTarget()
    }

    private fun drawTile(gridX: Int, gridY: Int) {
        program.drawer.withTarget(renderTarget) {
            val position = (IntVector2(gridX, gridY) * tileSize).vector2
            tileTypes[tiles[gridX][gridY]]?.draw(program, position)
        }
    }

    override fun init(parent: SpriteHost, program: Program) {
        this.program = program
        regenerateRenderTarget(initFun)
    }

    private fun regenerateRenderTarget(initFun: BuildFun<TileGrid> = {}) {
        renderTarget = renderTarget(tileSize * gridWidth, tileSize * gridHeight) { colorBuffer() }
        initFun()
        (0..<gridWidth).forEach { x -> (0..<gridHeight).forEach { y -> drawTile(x, y) } }
    }

    fun onChange(changeFun: TileGrid.() -> Unit): Unit {
        runOnChange.add(changeFun)
    }

    fun <E> on(event: Event<E>, code: TileGrid.(E) -> Unit) {
        event.listen { code(it) }
    }

    override fun update() {
        for (f in runEachFrame) f()
    }

    override fun draw() {
        program.drawer.image(renderTarget.colorBuffer(0), position)
    }
}