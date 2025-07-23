package de.thecommcraft.ktge

import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.rectangleBounds
import org.openrndr.writer
import java.nio.file.Path
import kotlin.io.path.Path
import java.io.File
import kotlin.io.path.pathString

fun loadImage(path: Path): ColorBuffer {
    return loadImage(path.toFile())
}

suspend fun loadImageSuspend(path: Path): ColorBuffer {
    return loadImageSuspend(path.toString())
}

suspend fun loadImageSuspend(file: File): ColorBuffer {
    return loadImageSuspend(file.toString())
}

interface Costume {
    fun draw(program: Program, position: Vector2)
}

open class DrawerCostume(val code: Program.(Vector2) -> Unit) : Costume {
    override fun draw(program: Program, position: Vector2) {
        program.code(position)
    }
}

internal fun ColorBuffer.scaledTo(width: Int, height: Int, scaleType: MagnifyingFilter = MagnifyingFilter.NEAREST): ColorBuffer {
    val buf = colorBuffer(width, height, contentScale, format, type, multisample, levels, session)
    copyTo(
        buf,
        sourceRectangle = bounds.toInt(),
        targetRectangle = buf.bounds.toInt(),
        filter = scaleType
    )
    return buf
}

internal fun ColorBuffer.scaledBy(scale: Int, scaleType: MagnifyingFilter = MagnifyingFilter.NEAREST) = scaledTo(width * scale, height * scale, scaleType)

fun ColorBuffer.toOwnedImage(): OwnedImage = OwnedImage.Companion.OwnedImageImplementation(this)

fun OwnedImage.toCostume(drawerConfig: BuildFun<Drawer> = {}) = ImageCostume.from(this, drawerConfig)

open class ImageCostume internal constructor(
    img: ColorBuffer,
    val drawerConfig: BuildFun<Drawer> = {}
) : Costume {
    companion object {
        fun from(
            path: Path,
            drawerConfig: BuildFun<Drawer> = {},
        ): ImageCostume {
            val img = loadImage(path)
            val costume = ImageCostume(img, drawerConfig)
            return costume
        }
        fun from(
            ownedImage: OwnedImage,
            drawerConfig: BuildFun<Drawer> = {}
        ): ImageCostume = ImageCostume(ownedImage.buf, drawerConfig)
    }

    private val buf = img
    val width: Int by buf::width
    val height: Int by buf::height

    override fun draw(program: Program, position: Vector2) = program.run {
        drawer.isolated(drawerConfig)
        drawer.image(buf, position)
    }
}

open class TextCostume(var text: String, val font: FontMap, val drawerConfig: BuildFun<Drawer>) : Costume {
    override fun draw(program: Program, position: Vector2) = program.run {
        drawer.isolated(drawerConfig)
        drawer.fontMap = font
        drawer.text(text, position)
    }
}

open class MultiLineTextCostume(var text: String, val font: FontMap, var size: Vector2, val drawerConfig: BuildFun<Drawer>) : Costume {
    override fun draw(program: Program, position: Vector2) = program.run {
        drawer.isolated(drawerConfig)
        drawer.fontMap = font
        writer {
            box = Rectangle(position, size.x, size.y)
            val lines = text.lines()
            lines.forEach {
                text(it)
                newLine()
            }
        }
    }
}
