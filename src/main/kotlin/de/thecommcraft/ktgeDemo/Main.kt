package de.thecommcraft.ktgeDemo

import de.thecommcraft.ktge.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.tint
import org.openrndr.math.Vector2
import kotlin.math.cos
import kotlin.math.sin

val pinkCircle = sprite {
    costume(DrawerCostume {
        drawer.fill = ColorRGBa.PINK
        drawer.circle(it.position, 140.0)
    })
    stateFun = StateFun.position {
        Vector2(
            x = cos(seconds) * width / 2.0 + width / 2.0,
            y = sin(0.5 * seconds) * height / 2.0 + height / 2.0
        )
    }
}

val openrndrText = sprite {
    val font = lazy { loadFont("data/fonts/default.otf", 64.0) }
    costume(TextCostume("OPENRNDR", font = font, preDraw = {
        fill = ColorRGBa.WHITE
    }))
    stateFun = StateFun.position { Vector2(width / 2.0, height / 2.0) }
}

fun main() = ktge(
    sprites = listOf(pinkCircle, openrndrText),
    config = {
        width = 768
        height = 576
    },
    background = ImageBackground(
        "data/images/pm5544.png",
        preDraw = { drawStyle.colorMatrix = tint(ColorRGBa.WHITE.shade(0.2)) })
)