package de.thecommcraft.builttoscale

import de.thecommcraft.ktge.Costume
import org.openrndr.Program
import org.openrndr.color.ColorLCHABa
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import kotlin.random.Random

fun min(vararg values: Double): Double = values.min()
fun max(vararg values: Double): Double = values.max()

fun randomColor(): ColorRGBa {
    val l = Random.nextDouble(50.0, 100.0)
    val c = 50.0
    val h = Random.nextDouble(360.0)
    return ColorLCHABa(l, c, h).toRGBa()
}

fun globalPos(costume: Costume): Costume = object : Costume {
    override fun draw(program: Program, position: Vector2) {
        costume.draw(program, position - program.window.position)
    }
}

val simulatedWindowCorner = Vector2(x = 1.0, y = barHeight)
fun Program.toGlobal(position: Vector2): Vector2 = position + window.position + simulatedWindowCorner