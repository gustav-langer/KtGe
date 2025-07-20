package de.thecommcraft.ktge

import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
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

interface Costume : OwnedResource {
    fun draw(program: Program, position: Vector2)
    override fun cleanUp() {}
}

open class DrawerCostume(val code: Program.(Vector2) -> Unit) : Costume {
    override fun draw(program: Program, position: Vector2) {
        program.code(position)
    }
}

open class ImageCostume internal constructor(
    img: ColorBuffer,
    val drawerConfig: BuildFun<Drawer> = {},
    scale: Int = 1,
    scaleType: MagnifyingFilter = MagnifyingFilter.NEAREST
) : Costume {
    companion object {
        fun from(
            path: String,
            drawerConfig: BuildFun<Drawer> = {},
            scale: Int = 1,
            scaleType: MagnifyingFilter = MagnifyingFilter.NEAREST
        ): ImageCostume {
            val img = loadImage(path)
            val costume = ImageCostume(img, drawerConfig, scale, scaleType)
            img.destroy()
            return costume
        }
        fun from(
            path: Path,
            drawerConfig: BuildFun<Drawer> = {},
            scale: Int = 1,
            scaleType: MagnifyingFilter = MagnifyingFilter.NEAREST
        ): ImageCostume = from(path.pathString, drawerConfig, scale, scaleType)
        fun from(
            path: File,
            drawerConfig: BuildFun<Drawer> = {},
            scale: Int = 1,
            scaleType: MagnifyingFilter = MagnifyingFilter.NEAREST
        ): ImageCostume = from(path.path, drawerConfig, scale, scaleType)

        /**
         * Only use when the original [ColorBuffer] stays alive.
         */
        fun from(
            colorBuffer: ColorBuffer,
            drawerConfig: BuildFun<Drawer> = {},
            scale: Int = 1,
            scaleType: MagnifyingFilter = MagnifyingFilter.NEAREST
        ): ImageCostume = ImageCostume(colorBuffer, drawerConfig, scale, scaleType)
    }

    private val buf = colorBuffer(img.width * scale, img.height * scale)

    init {
        img.copyTo(
            buf,
            sourceRectangle = IntRectangle(0, 0, img.width, img.height),
            targetRectangle = IntRectangle(0, 0, img.width * scale, img.height * scale),
            filter = scaleType
        )
    }

    override fun draw(program: Program, position: Vector2) = program.run {
        drawer.drawerConfig()
        drawer.image(buf, position)
    }

    override fun cleanUp() {
        buf.destroy()
    }
}

open class TextCostume(var text: String, val font: FontMap, val drawerConfig: BuildFun<Drawer>) : Costume {
    override fun draw(program: Program, position: Vector2) = program.run {
        drawer.drawerConfig()
        drawer.fontMap = font
        drawer.text(text, position)
    }
}

open class MultiLineTextCostume(var text: String, val font: FontMap, var size: Vector2, val drawerConfig: BuildFun<Drawer>) : Costume {
    override fun draw(program: Program, position: Vector2) = program.run {
        drawer.drawerConfig()
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
