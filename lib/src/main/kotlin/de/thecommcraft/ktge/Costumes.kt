package de.thecommcraft.ktge

import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.shape.IntRectangle

interface Costume {
    fun draw(program: Program, position: Vector2)
}

open class DrawerCostume(val code: Program.(Vector2) -> Unit) : Costume {
    override fun draw(program: Program, position: Vector2) {
        program.code(position)
    }
}

open class ImageCostume(
    img: ColorBuffer,
    val drawerConfig: BuildFun<Drawer> = {},
    scale: Int = 1,
    scaleType: MagnifyingFilter = MagnifyingFilter.NEAREST
) : Costume {
    val buf = colorBuffer(img.width * scale, img.height * scale)

    init {
        img.copyTo(
            buf,
            sourceRectangle = IntRectangle(0, 0, img.width, img.height),
            targetRectangle = IntRectangle(0, 0, img.width * scale, img.height * scale),
            filter = scaleType
        )
    }

    override fun draw(program: Program, position: Vector2) = program.run {
        drawer.drawerConfig() // TODO find a good name for this
        drawer.image(buf, position)
    }
}

open class TextCostume(val text: String, val font: FontMap, val drawerConfig: BuildFun<Drawer>) : Costume {
    override fun draw(program: Program, position: Vector2) = program.run {
        drawer.drawerConfig()
        drawer.fontMap = font
        drawer.text(text, position)
    }
}
