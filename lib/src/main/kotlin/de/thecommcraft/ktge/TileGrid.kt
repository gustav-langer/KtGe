package de.thecommcraft.ktge

import org.openrndr.draw.isolatedWithTarget
import org.openrndr.events.Event
import org.openrndr.math.IntVector2
import org.openrndr.shape.Rectangle
import kotlinx.serialization.Transient
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.openrndr.shape.IntRectangle
import java.util.Base64
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

interface GridOfTiles {
    var gridWidth: Int
    var gridHeight: Int
    val tiles: MutableList<MutableList<Int>>

    operator fun contains(pos: IntVector2): Boolean {
        if (pos.x < 0) return false
        if (pos.y < 0) return false
        if (pos.x >= gridWidth) return false
        if (pos.y >= gridHeight) return false
        return true
    }

    operator fun get(x: Int, y: Int): Int {
        return tiles[x][y]
    }

    operator fun set(x: Int, y: Int, value: Int) {
        tiles[x][y] = value
    }
}

@Suppress("unused")
open class OffscreenTileGrid(
    width: Int,
    height: Int = width,
    override val tiles: MutableList<MutableList<Int>>
) : GridOfTiles {

    override var gridWidth: Int = width
        set(value) {
            field = value
            if (tiles.size < value) repeat(value - tiles.size) {
                tiles.add(MutableList(gridHeight) { 0 })
            }
            if (tiles.size > value) repeat(tiles.size - value) {
                tiles.removeLast()
            }
        }
    override var gridHeight: Int = height
        set(value) {
            field = value
            tiles.forEach { col ->
                if (col.size < value) repeat(value - col.size) {
                    col.add(0)
                }
                if (col.size > value) repeat(col.size - value) {
                    col.removeLast()
                }
            }
        }
}

open class TileGrid(
    val tileSize: Int,
    width: Int,
    height: Int = width,
    override val tiles: MutableList<MutableList<Int>> = MutableList(width) { MutableList(height) { 0 } },
    private val tileTypes: MutableMap<Int, Costume> = mutableMapOf(),
    @Transient
    private val initFun: BuildFun<TileGrid> = {}
) : Sprite(), GridOfTiles {

    override var gridWidth: Int = width
        set(value) {
            field = value
            if (tiles.size < value) repeat(value - tiles.size) {
                tiles.add(MutableList(gridHeight) { 0 })
            }
            if (tiles.size > value) repeat(tiles.size - value) {
                tiles.removeLast()
            }
            regenerateRenderTarget()
        }
    override var gridHeight: Int = height
        set(value) {
            field = value
            tiles.forEach { col ->
                if (col.size < value) repeat(value - col.size) {
                    col.add(0)
                }
                if (col.size > value) repeat(col.size - value) {
                    col.removeLast()
                }
            }
            regenerateRenderTarget()
        }

    companion object {
        @Suppress("unused")
        fun makeSerializer(tileTypes: MutableMap<Int, Costume> = mutableMapOf()) =
            object : KSerializer<TileGrid> {
                override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("de.thecommcraft.ktge.TileGrid", PrimitiveKind.STRING)

                override fun serialize(encoder: Encoder, value: TileGrid) {
                    val b64Encoder = Base64.getEncoder()
                    val baos = ByteArrayOutputStream()
                    DataOutputStream(baos).use {
                        it.writeInt(value.tileSize)
                        it.writeInt(value.gridWidth)
                        it.writeInt(value.gridHeight)
                        it.writeBoolean(true)
                        value.tiles.forEach { it1 ->
                            it1.forEach { it2 ->
                                it.writeInt(it2)
                            }
                        }
                    }
                    val string = b64Encoder.encodeToString(baos.toByteArray())
                    encoder.encodeString(string)
                }

                override fun deserialize(decoder: Decoder): TileGrid {
                    val string = decoder.decodeString()
                    val b64Decoder = Base64.getDecoder()
                    val b64Decoded = b64Decoder.decode(string)
                    return DataInputStream(b64Decoded.inputStream()).use {
                        val tileSize = it.readInt()
                        val gridWidth = it.readInt()
                        val gridHeight = it.readInt()
                        val filled = it.readBoolean()
                        val tiles = if (filled) MutableList(gridWidth) { _ ->
                            MutableList(gridHeight) { _ ->
                                it.readInt()
                            }
                        } else MutableList(gridWidth) { MutableList(gridHeight) { 0 } }
                        TileGrid(tileSize, gridWidth, gridHeight, tiles, tileTypes)
                    }
                }
            }
        class TileEvent {
            val change = Event<Unit>("change")
        }
    }
    override operator fun set(x: Int, y: Int, value: Int) {
        val prev = this[x, y]
        super.set(x, y, value)
        if (prev == value) return
        drawTile(x, y)
        event.change.trigger(Unit)
    }

    @Transient
    val event = TileEvent()

    private var renderTarget: OwnedRenderTarget? = null

    @Suppress("unused")
    val rect
        get() = Rectangle(position, gridWidth.toDouble() * tileSize, gridHeight.toDouble() * tileSize)

    fun tileType(id: Int, costume: Costume) {
        tileTypes[id] = costume
    }

    @Suppress("unused")
    fun copyTo(tileGrid: TileGrid) {
        tileGrid.loadFrom(this)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun loadFrom(tileGrid: TileGrid) {
        loadNew(tileGrid.gridWidth, tileGrid.gridHeight, (tileGrid.tiles.map { it.toMutableList() }).toMutableList())
    }

    @Suppress("MemberVisibilityCanBePrivate")
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

    override fun initSprite() {
        regenerateRenderTarget(initFun)
        costume(DrawerCostume {
            renderTarget?.let { it1 -> program.drawer.image(it1.colorBuffer(0), it) }
        })
    }

    private fun regenerateRenderTarget(initFun: BuildFun<TileGrid> = {}) {
        renderTarget?.let { removeOwnedResource(it) }
        renderTarget = OwnedRenderTarget.of(tileSize * gridWidth, tileSize * gridHeight, 1.0) {
            colorBuffer()
        }.add()
        this.initFun()
        renderTarget?.let {
            program.drawer.isolatedWithTarget(it) {
                program.drawer.ortho(it)
                (0..<gridWidth).forEach { x -> (0..<gridHeight).forEach { y -> drawTileHere(x, y) } }
            }
        }
    }
}

fun GridOfTiles.getArea(boundA: IntVector2, boundB: IntVector2) =
    List(boundB.x - boundA.x) { dx ->
        List(boundB.y - boundA.y) { dy ->
            getOrNull(boundA.x + dx, boundA.y + dy)
        }
    }

@Suppress("unused")
fun GridOfTiles.getArea(bounds: IntRectangle) =
    getArea(
        bounds.corner,
        bounds.corner + IntVector2(bounds.width, bounds.height)
    )

@Suppress("unused")
fun GridOfTiles.getDefaultedArea(bounds: IntRectangle, default: Int) =
    getDefaultedArea(
        bounds.corner,
        bounds.corner + IntVector2(bounds.width, bounds.height),
        default
    )

fun GridOfTiles.getDefaultedArea(boundA: IntVector2, boundB: IntVector2, default: Int) =
    List(boundB.x - boundA.x) { dx ->
        List(boundB.y - boundA.y) { dy ->
            getOrNull(boundA.x + dx, boundA.y + dy) ?: default
        }
    }

/**
 * Sets an entire rectangle on the TileGrid.
 *
 * Tiles that are passed as null will not be overwritten.
 * @return A rectangle that has all the previous tiles at the positions of the input rectangle.
 */
fun GridOfTiles.setArea(boundA: IntVector2, boundB: IntVector2, area: List<List<Int?>>) =
    List(boundB.x - boundA.x) { dx ->
        val x = boundA.x + dx
        List(boundB.y - boundA.y) { dy ->
            val y = boundA.y + dy
            val previous = getOrNull(x, y)
            val new = area.getOrNull(dx)?.getOrNull(dy)
            if (previous != null && new != null) this[x, y] = new
            previous
        }
    }

/**
 * Sets an entire rectangle on the TileGrid.
 *
 * Tiles that are passed as null will not be overwritten.
 * @return A rectangle that has all the previous tiles at the positions of the input rectangle.
 */
@Suppress("unused")
fun GridOfTiles.setArea(bounds: IntRectangle, area: List<List<Int?>>) =
    setArea(
        bounds.corner,
        bounds.corner + IntVector2(bounds.width, bounds.height),
        area
    )

/**
 * Sets an entire rectangle to a specific tile on the TileGrid.
 *
 * @return A rectangle that has all the previous tiles at the positions of the input rectangle.
 */
@Suppress("unused")
fun GridOfTiles.setArea(boundA: IntVector2, boundB: IntVector2, value: Int) =
    List(boundB.x - boundA.x) { dx ->
        val x = boundA.x + dx
        List(boundB.y - boundA.y) { dy ->
            val y = boundA.y + dy
            val previous = getOrNull(x, y)
            if (previous != null) this[x, y] = value
            previous
        }
    }

/**
 * Sets an entire rectangle to a specific tile on the TileGrid.
 *
 * @return A rectangle that has all the previous tiles at the positions of the input rectangle.
 */
@Suppress("unused")
fun GridOfTiles.setArea(bounds: IntRectangle, value: Int) =
    setArea(
        bounds.corner,
        bounds.corner + IntVector2(bounds.width, bounds.height),
        value
    )

fun GridOfTiles.getOrNull(x: Int, y: Int) =
    if (IntVector2(x, y) !in this) null else get(x, y)
