package de.thecommcraft.ktge

import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.shape.IntRectangle

interface Costume {
    fun draw(program: Program, position: Vector2)
}

class DrawerCostume(val code: Program.(Vector2) -> Unit) : Costume {
    override fun draw(program: Program, position: Vector2) {
        program.code(position)
    }
}

class ImageCostume(
    img: ColorBuffer,
    val drawerConfig: BuildFun<Drawer> = {},
    scale: Int = 1,
    scaleType: MagnifyingFilter = MagnifyingFilter.NEAREST
) : Costume {
    val img = colorBuffer(img.width * scale, img.height * scale)

    init {
        img.copyTo(
            img,
            sourceRectangle = IntRectangle(0, 0, img.width, img.height),
            targetRectangle = IntRectangle(0, 0, img.width * scale, img.height * scale),
            filter = scaleType
        )
    }

    override fun draw(program: Program, position: Vector2) = program.run {
        drawer.drawerConfig() // TODO find a good name for this
        drawer.image(img, position)
    }
}

class TextCostume(val text: String, val font: FontMap, val drawerConfig: BuildFun<Drawer>) : Costume {
    override fun draw(program: Program, position: Vector2) = program.run {
        drawer.drawerConfig()
        drawer.fontMap = font
        drawer.text(text, position)
    }
}

object EmptyCostume : Costume {
    override fun draw(program: Program, position: Vector2) {}
}