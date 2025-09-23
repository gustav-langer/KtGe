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
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.WeakHashMap

abstract class GridTileMetaData<T>(val metaDataId: String) {
    private val instanceValues: WeakHashMap<GridOfTiles, MutableMap<Pair<Int, Int>, T?>> = WeakHashMap()
    abstract fun associateData(gridOfTiles: GridOfTiles, data: MutableMap<Pair<Int, Int>, String>)
    abstract fun getAssociatedData(gridOfTiles: GridOfTiles): MutableMap<Pair<Int, Int>, String>
    operator fun get(gridTile: GridTile): T? {
        val value = instanceValues[gridTile.tileGrid]?.get(gridTile.x to gridTile.y)
        return value
    }
    operator fun set(gridTile: GridTile, value: T?) {
        val metaDataGroup = instanceValues.getOrPut(gridTile.tileGrid, ::mutableMapOf)
        if (value == null) {
            metaDataGroup.remove(gridTile.x to gridTile.y)
            return
        }
        metaDataGroup[gridTile.x to gridTile.y] = value
    }
}

infix fun<T> GridTileMetaData<T>.of(gridTile: GridTile) = get(gridTile)

interface GridTile {
    val x: Int
    val y: Int
    val tile: Int
    val tileGrid: GridOfTiles
    operator fun<T> get(gridTileMetaData: GridTileMetaData<T>): T? =
        gridTileMetaData[this]
    operator fun<T> set(gridTileMetaData: GridTileMetaData<T>, value: T?) {
        gridTileMetaData[this] = value
    }
    fun setTile(tile: Int)
}

internal class BoundGridTile(
    startTile: Int,
    override val x: Int,
    override val y: Int,
    override val tileGrid: GridOfTiles
) : GridTile {
    override var tile: Int = startTile
        internal set

    override fun setTile(tile: Int) {
        this.tile = tile
        tileGrid[x, y] = tile
    }
}

interface GridOfTiles {
    var gridWidth: Int
    var gridHeight: Int
    val tiles: MutableList<MutableList<Int>>
    val metaData: MutableMap<String, GridTileMetaData<*>>

    operator fun contains(pos: IntVector2): Boolean {
        if (pos.x < 0) return false
        if (pos.y < 0) return false
        if (pos.x >= gridWidth) return false
        if (pos.y >= gridHeight) return false
        return true
    }

    fun tile(x: Int, y: Int): GridTile? {
        return getOrNull(x, y)?.let { BoundGridTile(it, x, y, this) }
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
    override val tiles: MutableList<MutableList<Int>> = MutableList(width) { MutableList(height) { 0 } },
    override val metaData: MutableMap<String, GridTileMetaData<*>> = mutableMapOf()
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
    private val initFun: BuildFun<TileGrid> = {},
    override val metaData: MutableMap<String, GridTileMetaData<*>> = mutableMapOf()
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
        const val MAX_SUPPORTED_ENCODING_VERSION = 0
        const val VERSION_ZERO = 0
        @Suppress("unused")
        fun makeSerializer(
            tileTypes: MutableMap<Int, Costume> = mutableMapOf(),
            gridTileMetaData: List<GridTileMetaData<*>>
        ) =
            object : KSerializer<TileGrid> {
                override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("de.thecommcraft.ktge.TileGrid", PrimitiveKind.STRING)

                override fun serialize(encoder: Encoder, value: TileGrid) {
                    val b64Encoder = Base64.getEncoder()
                    ByteArrayOutputStream().use { baos ->
                        DataOutputStream(baos).use { dos ->
                            dos.writeInt(MAX_SUPPORTED_ENCODING_VERSION)
                            dos.writeInt(value.tileSize)
                            dos.writeInt(value.gridWidth)
                            dos.writeInt(value.gridHeight)
                            dos.writeBoolean(true)
                            value.tiles.forEach { it1 ->
                                it1.forEach { it2 ->
                                    dos.writeInt(it2)
                                }
                            }
                            val allMetaData = value.metaData.map { (t, u) ->
                                t to u.getAssociatedData(value)
                            }.toMap()
                            dos.writeInt(allMetaData.size)
                            allMetaData.forEach { (t, u) ->
                                dos.writeChars(
                                    URLEncoder.encode(t, StandardCharsets.UTF_8)+";"
                                )
                                dos.writeInt(u.size)
                                u.forEach { (k, v) ->
                                    dos.writeInt(k.first)
                                    dos.writeInt(k.second)
                                    dos.writeChars(
                                        URLEncoder.encode(v, StandardCharsets.UTF_8)+";"
                                    )
                                }
                            }
                        }
                        val string = b64Encoder.encodeToString(baos.toByteArray())
                        encoder.encodeString(string)
                    }
                }

                fun decodeVersionZero(dis: DataInputStream): TileGrid {
                    val tileSize = dis.readInt()
                    val gridWidth = dis.readInt()
                    val gridHeight = dis.readInt()
                    val filled = dis.readBoolean()
                    val tiles = if (filled) MutableList(gridWidth) { _ ->
                        MutableList(gridHeight) { _ ->
                            dis.readInt()
                        }
                    } else MutableList(gridWidth) { MutableList(gridHeight) { 0 } }
                    val metaDataTypeEntryCount = dis.readInt()
                    val metaData = gridTileMetaData.associateBy(
                        GridTileMetaData<*>::metaDataId
                    ).toMutableMap()
                    val result = TileGrid(
                        tileSize,
                        gridWidth,
                        gridHeight,
                        tiles,
                        tileTypes,
                        metaData = metaData
                    )
                    repeat(metaDataTypeEntryCount) {
                        var currentChar = dis.readChar()
                        var metaDataTypeName = ""
                        while (currentChar != ';') {
                            metaDataTypeName += currentChar
                            currentChar = dis.readChar()
                        }
                        metaDataTypeName = URLDecoder.decode(metaDataTypeName, StandardCharsets.UTF_8)
                        val metaDataEntryCount = dis.readInt()
                        val metaDataEntries = mutableMapOf<Pair<Int, Int>, String>()
                        repeat(metaDataEntryCount) {
                            currentChar = dis.readChar()
                            val x = dis.readInt()
                            val y = dis.readInt()
                            var metaDataValue = ""
                            while (currentChar != ';') {
                                metaDataValue += currentChar
                                currentChar = dis.readChar()
                            }
                            metaDataValue = URLDecoder.decode(metaDataValue, StandardCharsets.UTF_8)
                            metaDataEntries[x to y] = metaDataValue
                        }
                        metaData[metaDataTypeName]?.associateData(result, metaDataEntries)
                    }
                    result.event.attachedMetaData.trigger(result)
                    result.event.ready.trigger(result)
                    return result
                }

                override fun deserialize(decoder: Decoder): TileGrid {
                    val string = decoder.decodeString()
                    val b64Decoder = Base64.getDecoder()
                    val b64Decoded = b64Decoder.decode(string)
                    return b64Decoded.inputStream().use { iS -> DataInputStream(iS).use { dis ->
                        val encodingVersion = dis.readInt()
                        if (encodingVersion == VERSION_ZERO) {
                            decodeVersionZero(dis)
                        } else throw IllegalArgumentException("Version not recognized.")
                    } }
                }
            }
        class TileEvent {
            val change = Event<TileGrid>("change")
            val attachedMetaData = Event<TileGrid>("attachedMetaData")
            val ready = Event<TileGrid>("ready")
        }
    }
    override operator fun set(x: Int, y: Int, value: Int) {
        val prev = this[x, y]
        super.set(x, y, value)
        if (prev == value) return
        drawTile(x, y)
        hasChangedThisFrame = true
    }

    @Transient
    val event = TileEvent()

    private var hasChangedThisFrame = false
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
        this.tiles.clear()
        tiles.forEach { this.tiles.add(it.toMutableList()) }
        regenerateRenderTarget()
        hasChangedThisFrame = true
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
        frame {
            if (hasChangedThisFrame) event.change.trigger(this@TileGrid)
            hasChangedThisFrame = false
        }
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

data class MetaDataValue<T>(
    val gridTileMetaData: GridTileMetaData<T>,
    val value: T?
) {
    fun addTo(gridTile: GridTile) {
        gridTileMetaData[gridTile] = value
    }
}

fun<T> GridTileMetaData<T>.exportValue(gridTile: GridTile) =
    MetaDataValue(this, this[gridTile])

typealias TileMetaData = List<MetaDataValue<out Any?>>
typealias BoundTileData = Pair<TileMetaData, GridTile>

fun GridOfTiles.getBoundsData(bounds: IntRectangle): List<List<BoundTileData?>> {
    val metaDataTypes = metaData.values
    val boundA = bounds.corner
    return List(bounds.width + 1) { dx ->
        val x = boundA.x + dx
        List(bounds.height + 1) { dy ->
            val y = boundA.y + dy
            tile(x, y)?.let { gridTile ->
                metaDataTypes.map {
                    it.exportValue(gridTile)
                } to gridTile
            }
        }
    }
}

fun GridOfTiles.setBoundsData(bounds: IntRectangle, boundsData: List<List<BoundTileData?>>) {
    val boundA = bounds.corner
    repeat(bounds.width + 1) { dx ->
        val x = boundA.x + dx
        repeat(bounds.height + 1) { dy ->
            val y = boundA.y + dy
            tile(x, y)?.let { gridTile ->
                boundsData[dx][dy]?.first?.let { metaData ->
                    gridTile.allMetaData = metaData
                }
            }
        }
    }
}

fun GridOfTiles.copyBoundsDataFrom(
    gridOfTiles: GridOfTiles,
    myBounds: IntRectangle,
    otherBounds: IntRectangle
) {
    setBoundsData(myBounds, gridOfTiles.getBoundsData(otherBounds))
}

fun GridOfTiles.copyBoundsDataTo(
    gridOfTiles: GridOfTiles,
    myBounds: IntRectangle,
    otherBounds: IntRectangle
) {
    gridOfTiles.copyBoundsDataFrom(this, otherBounds, myBounds)
}

fun GridOfTiles.exportBoundsData(bounds: IntRectangle): OffscreenTileGrid {
    val offscreenTileGrid = OffscreenTileGrid(gridWidth, gridHeight)
    copyBoundsDataTo(offscreenTileGrid, bounds, bounds.copy(corner = IntVector2.ZERO))
    return offscreenTileGrid
}

var GridTile.allMetaData: TileMetaData
    get() =
        tileGrid.metaData.values.map {
            it.exportValue(this)
        }
    set(value) {
        value.forEach {
            it.addTo(this)
        }
    }

fun GridTile.clearMetaData() {
    allMetaData = allMetaData.map { MetaDataValue(it.gridTileMetaData, null) }
}

@Suppress("unused")
val GridOfTiles.bounds
    get() = IntRectangle(0, 0, gridWidth - 1, gridHeight - 1)

fun GridOfTiles.getArea(boundA: IntVector2, boundB: IntVector2) =
    List(boundB.x - boundA.x + 1) { dx ->
        List(boundB.y - boundA.y + 1) { dy ->
            getOrNull(boundA.x + dx, boundA.y + dy)
        }
    }

@Suppress("unused")
fun GridOfTiles.getArea(bounds: IntRectangle) =
    getArea(
        bounds.corner,
        bounds.corner + IntVector2(bounds.width, bounds.height)
    )

fun GridOfTiles.getDefaultedArea(boundA: IntVector2, boundB: IntVector2, default: Int) =
    List(boundB.x - boundA.x + 1) { dx ->
        List(boundB.y - boundA.y + 1) { dy ->
            getOrNull(boundA.x + dx, boundA.y + dy) ?: default
        }
    }

@Suppress("unused")
fun GridOfTiles.getDefaultedArea(bounds: IntRectangle, default: Int) =
    getDefaultedArea(
        bounds.corner,
        bounds.corner + IntVector2(bounds.width, bounds.height),
        default
    )

/**
 * Sets an entire rectangle on the TileGrid.
 *
 * Tiles that are passed as null will not be overwritten.
 * @return A rectangle that has all the previous tiles at the positions of the input rectangle.
 */
fun GridOfTiles.setArea(boundA: IntVector2, boundB: IntVector2, area: List<List<Int?>>, keepMetaData: Boolean = false) =
    List(boundB.x - boundA.x + 1) { dx ->
        val x = boundA.x + dx
        List(boundB.y - boundA.y + 1) { dy ->
            val y = boundA.y + dy
            val previous = getOrNull(x, y)
            val new = area.getOrNull(dx)?.getOrNull(dy)
            if (new != null) {
                this.tile(x, y)?.let { tile ->
                    tile.setTile(new)
                    if (!keepMetaData) tile.clearMetaData()
                }
            }
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
fun GridOfTiles.setArea(bounds: IntRectangle, area: List<List<Int?>>, keepMetaData: Boolean = false) =
    setArea(
        bounds.corner,
        bounds.corner + IntVector2(bounds.width, bounds.height),
        area,
        keepMetaData
    )

/**
 * Sets an entire rectangle to a specific tile on the TileGrid.
 *
 * @return A rectangle that has all the previous tiles at the positions of the input rectangle.
 */
@Suppress("unused")
fun GridOfTiles.setArea(boundA: IntVector2, boundB: IntVector2, value: Int, keepMetaData: Boolean = false) =
    List(boundB.x - boundA.x + 1) { dx ->
        val x = boundA.x + dx
        List(boundB.y - boundA.y + 1) { dy ->
            val y = boundA.y + dy
            val previous = getOrNull(x, y)
            this.tile(x, y)?.let { tile ->
                tile.setTile(value)
                if (!keepMetaData) tile.clearMetaData()
            }
            previous
        }
    }

/**
 * Sets an entire rectangle to a specific tile on the TileGrid.
 *
 * @return A rectangle that has all the previous tiles at the positions of the input rectangle.
 */
@Suppress("unused")
fun GridOfTiles.setArea(bounds: IntRectangle, value: Int, keepMetaData: Boolean = false) =
    setArea(
        bounds.corner,
        bounds.corner + IntVector2(bounds.width, bounds.height),
        value,
        keepMetaData
    )

fun GridOfTiles.getOrNull(x: Int, y: Int) =
    if (IntVector2(x, y) !in this) null else get(x, y)
