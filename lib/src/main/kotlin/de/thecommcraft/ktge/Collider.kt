package de.thecommcraft.ktge

import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.math.*

private fun Double.squared(): Double = this * this


interface Collider {
    fun collides(other: Collider): Boolean
}


interface PositionedCollider : Collider {
    var position: Vector2
}

data class Projection(val min: Double, val max: Double) {
    fun overlaps(other: Projection): Boolean = this.max > other.min && other.max > this.min
}

open class BoxCollider(
    val width: Double,
    val height: Double,
    initialPosition: Vector2 = Vector2.ZERO
) : PositionedCollider {

    override var position: Vector2 = initialPosition

    val rectangle: Rectangle
        get() = Rectangle.fromCenter(position, width, height)

    open fun collides(other: RotatedRectangleCollider): Boolean = other.collides(this)

    open fun collides(other: CircleCollider): Boolean = other.collides(this)

    open fun collides(other: BoxCollider): Boolean {
        if (abs(this.position.x - other.position.x) > (this.width + other.width) / 2.0) {
            return false
        }
        if (abs(this.position.y - other.position.y) > (this.height + other.height) / 2.0) {
            return false
        }
        return true
    }

    override fun collides(other: Collider): Boolean {
        return false
    }

    open fun asRotated(): RotatedRectangleCollider =
        RotatedRectangleCollider(width, height, position)

    open fun project(axis: Vector2): Projection {
        val centerProjection = this.position.dot(axis)
        val radiusProjection = (this.width / 2.0) * abs(axis.x) + (this.height / 2.0) * abs(axis.y)
        return Projection(centerProjection - radiusProjection, centerProjection + radiusProjection)
    }
}

open class CircleCollider(
    val radius: Double,
    initialPosition: Vector2 = Vector2.ZERO
) : PositionedCollider {

    override var position: Vector2 = initialPosition

    val circle: Circle
        get() = Circle(position, radius)

    override fun collides(other: Collider): Boolean {
        return false
    }

    open fun collides(other: CircleCollider): Boolean {
        val distanceSq = (this.position - other.position).squaredLength
        val radiiSumSq = (this.radius + other.radius).squared()
        return distanceSq <= radiiSumSq
    }

    open fun collides(other: BoxCollider): Boolean {
        val closestPoint = Vector2(
            x = this.position.x.coerceIn(other.rectangle.corner.x, other.rectangle.corner.x + other.width),
            y = this.position.y.coerceIn(other.rectangle.corner.y, other.rectangle.corner.y + other.height)
        )
        val distanceSq = (this.position - closestPoint).squaredLength
        return distanceSq < this.radius.squared()
    }

    open fun collides(other: RotatedRectangleCollider): Boolean = other.collides(this)
}


class RotatedRectangleCollider(
    width: Double,
    height: Double,
    initialPosition: Vector2 = Vector2.ZERO,
    var rotation: Double = 0.0
) : BoxCollider(width, height, initialPosition) {

    private val vertices: List<Vector2>
        get() {
            val halfW = width / 2.0
            val halfH = height / 2.0
            return listOf(
                Vector2(-halfW, -halfH), Vector2(halfW, -halfH),
                Vector2(halfW, halfH), Vector2(-halfW, halfH)
            ).map { it.rotate(rotation) + position }
        }

    private val axes: List<Vector2>
        get() = listOf(
            (vertices[1] - vertices[0]).normalized.perpendicular(),
            (vertices[2] - vertices[1]).normalized.perpendicular()
        )

    override fun collides(other: CircleCollider): Boolean {
        val localCircleCenter = (other.position - this.position).rotate(-this.rotation)

        val closestPoint = Vector2(
            x = localCircleCenter.x.coerceIn(-width / 2.0, width / 2.0),
            y = localCircleCenter.y.coerceIn(-height / 2.0, height / 2.0)
        )

        val distanceSq = (localCircleCenter - closestPoint).squaredLength
        return distanceSq < other.radius.squared()
    }

    override fun collides(other: RotatedRectangleCollider): Boolean {
        val allAxes = this.axes + other.axes

        for (axis in allAxes) {
            val p1 = this.project(axis)
            val p2 = other.project(axis)
            if (!p1.overlaps(p2)) {
                return false
            }
        }
        return true
    }

    override fun collides(other: BoxCollider): Boolean {
        val allAxes = axes + listOf(Vector2.UNIT_X, Vector2.UNIT_Y)

        for (axis in allAxes) {
            val p1 = project(axis)
            val p2 = other.project(axis)
            if (!p1.overlaps(p2)) {
                return false
            }
        }
        return true
    }

    override fun project(axis: Vector2): Projection {
        val projections = vertices.map { it.dot(axis) }
        return Projection(projections.minOrNull()!!, projections.maxOrNull()!!)
    }

    override fun asRotated(): RotatedRectangleCollider {
        return RotatedRectangleCollider(width, height, position, rotation)
    }
}


class PolyPositionedCollider(
    private val subColliders: Iterable<PositionedCollider>,
    initialPosition: Vector2 = Vector2.ZERO
) : PositionedCollider {

    override var position: Vector2 = initialPosition

    override fun collides(other: Collider): Boolean {
        return subColliders.any { subCollider ->
            subCollider.position = this.position
            subCollider.collides(other)
        }
    }
}

class EmptyCollider(initialPosition: Vector2 = Vector2.ZERO) : PositionedCollider {
    override var position: Vector2 = initialPosition
    override fun collides(other: Collider): Boolean = false
}
