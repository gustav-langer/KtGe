package de.thecommcraft.ktge

import org.openrndr.Program
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.renderTarget
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2

class TileGrid(val tileSize: Int, val gridWidth: Int, val gridHeight: Int=gridWidth, private val initFun: BuildFun<TileGrid> = {}) :
    Drawable {
    lateinit var program: Program

    private val tileTypes: MutableMap<Int, Costume> = mutableMapOf()
    private val runEachFrame: MutableList<BuildFun<TileGrid>> = mutableListOf()

    var position: Vector2 = Vector2.ZERO
    val tiles: MutableList<MutableList<Int>> = MutableList(gridWidth) { MutableList(gridHeight) { 0 } }

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
    }

    fun drawTile(gridX: Int, gridY: Int) {
        program.drawer.withTarget(renderTarget) {
            val position = (IntVector2(gridX, gridY) * tileSize).vector2
            tileTypes[tiles[gridX][gridY]]?.draw(program, position)
        }
    }

    override fun init(parent: SpriteHost, program: Program) {
        this.program = program
        renderTarget = renderTarget(tileSize * gridWidth, tileSize * gridHeight) { colorBuffer() }
        initFun()
        (0..<gridWidth).forEach { x -> (0..<gridHeight).forEach { y -> drawTile(x, y) } }
    }

    override fun update() {
        for (f in runEachFrame) f()
    }

    override fun draw() {
        program.drawer.image(renderTarget.colorBuffer(0), position)
    }
}