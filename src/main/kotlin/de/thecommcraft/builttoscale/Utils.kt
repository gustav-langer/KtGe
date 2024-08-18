package de.thecommcraft.builttoscale

import de.thecommcraft.ktge.Costume
import org.openrndr.Program
import org.openrndr.math.Vector2

fun min(vararg values: Double): Double = values.min()
fun max(vararg values: Double): Double = values.max()

fun globalPos(costume: Costume): Costume = object: Costume {
    override fun draw(program: Program, position: Vector2) {
        costume.draw(program, position - program.window.position)
    }
}

val simulatedWindowCorner = Vector2(x=1.0,y=barHeight)
fun Program.toGlobal(position: Vector2): Vector2 = position + window.position + simulatedWindowCorner