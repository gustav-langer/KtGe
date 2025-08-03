package de.thecommcraft.ktge

import org.openrndr.Program
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.events.Event
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Base64
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

open class TileGrid(
    val tileSize: Int,
    var gridWidth: Int,
    var gridHeight: Int = gridWidth,
    val tiles: MutableList<MutableList<Int>> = MutableList(gridWidth) { MutableList(gridHeight) { 0 } },
    private val tileTypes: MutableMap<Int, Costume> = mutableMapOf(),
    @Transient
    private val initFun: BuildFun<TileGrid> = {}
) : Sprite() {

    companion object {
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
                    return DataInputStream(string.byteInputStream()).use {
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



    @Transient
    val event = TileEvent()

    private var renderTarget: OwnedRenderTarget? = null

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
