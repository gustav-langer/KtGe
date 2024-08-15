package de.thecommcraft.ktge

import org.openrndr.Program
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.FontMap
import org.openrndr.math.Vector2

interface Costume {
    fun draw(program: Program, position: Vector2)
}

class DrawerCostume(val code: Program.(Vector2) -> Unit) : Costume {
    override fun draw(program: Program, position: Vector2) {
        program.code(position)
    }
}

class ImageCostume(val img: ColorBuffer, val drawerConfig: BuildFun<Drawer>) : Costume {
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