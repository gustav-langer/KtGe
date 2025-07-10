package de.thecommcraft.ktge

import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.math.abs

interface ColliderPrototype {
    fun collides(myPos: Vector2, other: ColliderPrototype, otherPos: Vector2): Boolean {
        when (this) {
            is RotatedRectangleColliderPrototype -> when (other) {
                is RotatedRectangleColliderPrototype -> return this.collides(myPos, other, otherPos)
                is BoxColliderPrototype -> return this.collides(myPos, other, otherPos)
                is CircleColliderPrototype -> return this.collides(myPos, other, otherPos)
                is DisplacedPositionedColliderPrototype -> return other.collides(otherPos, this, myPos)
                is PolyColliderPrototype -> return other.collides(otherPos, this, myPos)
                is InvertedPositionedColliderPrototype -> return other.collides(otherPos, this, myPos)
            }
            is BoxColliderPrototype -> when (other) {
                is RotatedRectangleColliderPrototype -> return other.collides(otherPos, this, myPos)
                is BoxColliderPrototype -> return this.collides(myPos, other, otherPos)
                is CircleColliderPrototype -> return other.collides(otherPos, this, myPos)
                is DisplacedPositionedColliderPrototype -> return other.collides(otherPos, this, myPos)
                is PolyColliderPrototype -> return other.collides(otherPos, this, myPos)
                is InvertedPositionedColliderPrototype -> return other.collides(otherPos, this, myPos)
            }
            is CircleColliderPrototype -> when (other) {
                is RotatedRectangleColliderPrototype -> return other.collides(otherPos, this, myPos)
                is BoxColliderPrototype -> return this.collides(myPos, other, otherPos)
                is CircleColliderPrototype -> return this.collides(myPos, other, otherPos)
                is DisplacedPositionedColliderPrototype -> return other.collides(otherPos, this, myPos)
                is PolyColliderPrototype -> return other.collides(otherPos, this, myPos)
                is InvertedPositionedColliderPrototype -> return other.collides(otherPos, this, myPos)
            }
        }
        return false
    }
}

open class BoxColliderPrototype(
    var width: Double,
    var height: Double
) : ColliderPrototype {
    companion object {
        val EMPTY = BoxColliderPrototype(0.0, 0.0)
    }

    fun correspondingRectangle(position: Vector2) = Rectangle.fromCenter(position, width, height)

    open fun collides(myPos: Vector2, other: BoxColliderPrototype, otherPos: Vector2): Boolean {
        if (abs(myPos.x - otherPos.x) > (this.width + other.width) / 2.0) {
            return false
        }
        if (abs(myPos.y - otherPos.y) > (this.height + other.height) / 2.0) {
            return false
        }
        return true
    }

    open fun asRotated(): RotatedRectangleColliderPrototype =
        RotatedRectangleColliderPrototype(width, height)

    open fun project(myPos: Vector2, axis: Vector2): Projection {
        val centerProjection = myPos.dot(axis)
        val radiusProjection = (this.width / 2.0) * abs(axis.x) + (this.height / 2.0) * abs(axis.y)
        return Projection(centerProjection - radiusProjection, centerProjection + radiusProjection)
    }
}


open class CircleColliderPrototype(
    var radius: Double
) : ColliderPrototype {
    companion object {
        val EMPTY = CircleColliderPrototype(0.0)
    }

    fun correspondingCircle(position: Vector2) = Circle(position, radius)

    open fun collides(myPos: Vector2, other: CircleColliderPrototype, otherPos: Vector2): Boolean {
        val distanceSq = (myPos - otherPos).squaredLength
        val radiiSumSq = (this.radius + other.radius).squared()
        return distanceSq <= radiiSumSq
    }

    open fun collides(myPos: Vector2, other: BoxColliderPrototype, otherPos: Vector2): Boolean {
        val otherRect = other.correspondingRectangle(otherPos)
        val closestPoint = Vector2(
            x = myPos.x.coerceIn(otherRect.corner.x, otherRect.corner.x + other.width),
            y = myPos.y.coerceIn(otherRect.corner.y, otherRect.corner.y + other.height)
        )
        val distanceSq = (myPos - closestPoint).squaredLength
        return distanceSq < this.radius.squared()
    }
}



open class RotatedRectangleColliderPrototype(
    width: Double,
    height: Double,
    var rotation: Double = 0.0
) : BoxColliderPrototype(width, height) {
    companion object {
        val EMPTY = RotatedRectangleColliderPrototype(0.0, 0.0)
    }

    private fun correspondingVertices(position: Vector2): List<Vector2> {
        val halfW = width / 2.0
        val halfH = height / 2.0
        return listOf(
            Vector2(-halfW, -halfH), Vector2(halfW, -halfH),
            Vector2(halfW, halfH), Vector2(-halfW, halfH)
        ).map { it.rotate(rotation) + position }
    }

    private fun correspondingAxes(position: Vector2): List<Vector2> {
        val vertices = correspondingVertices(position)
        return listOf(
            (vertices[1] - vertices[0]).normalized.perpendicular(),
            (vertices[2] - vertices[1]).normalized.perpendicular()
        )
    }

    fun collides(myPos: Vector2, other: CircleColliderPrototype, otherPos: Vector2): Boolean {
        val localCircleCenter = (otherPos - myPos).rotate(-rotation)

        val closestPoint = Vector2(
            x = localCircleCenter.x.coerceIn(-width / 2.0, width / 2.0),
            y = localCircleCenter.y.coerceIn(-height / 2.0, height / 2.0)
        )

        val distanceSq = (localCircleCenter - closestPoint).squaredLength
        return distanceSq < other.radius.squared()
    }

    fun collides(myPos: Vector2, other: RotatedRectangleColliderPrototype, otherPos: Vector2): Boolean {
        val axes = correspondingAxes(myPos)
        val allAxes = axes + other.correspondingAxes(otherPos)
        val myVertices = correspondingVertices(myPos)
        val otherVertices = other.correspondingVertices(otherPos)

        for (axis in allAxes) {
            val p1 = this.project(myVertices, axis)
            val p2 = other.project(otherVertices, axis)
            if (!p1.overlaps(p2)) {
                return false
            }
        }
        return true
    }

    override fun collides(myPos: Vector2, other: BoxColliderPrototype, otherPos: Vector2): Boolean {
        val axes = correspondingAxes(myPos)
        val allAxes = axes + listOf(Vector2.UNIT_X, Vector2.UNIT_Y)
        val myVertices = correspondingVertices(myPos)

        for (axis in allAxes) {
            val p1 = this.project(myVertices, axis)
            val p2 = other.project(otherPos, axis)
            if (!p1.overlaps(p2)) {
                return false
            }
        }
        return true
    }

    override fun project(myPos: Vector2, axis: Vector2): Projection {
        return project(correspondingVertices(myPos), axis)
    }

    fun project(vertices: List<Vector2>, axis: Vector2): Projection {
        val projections = vertices.map { it.dot(axis) }
        return Projection(projections.minOrNull()!!, projections.maxOrNull()!!)
    }

    override fun asRotated(): RotatedRectangleColliderPrototype {
        return RotatedRectangleColliderPrototype(width, height, rotation)
    }
}

open class DisplacedPositionedColliderPrototype(private val collider: ColliderPrototype, var offset: Vector2 = Vector2.ZERO) : ColliderPrototype {

    companion object {
        val EMPTY = DisplacedPositionedColliderPrototype(EmptyColliderPrototype())
    }

    override fun collides(myPos: Vector2, other: ColliderPrototype, otherPos: Vector2): Boolean {
        return collider.collides(myPos + offset, other, otherPos)
    }
}

infix fun ColliderPrototype.offset(offset: Vector2): DisplacedPositionedColliderPrototype {
    return DisplacedPositionedColliderPrototype(this, offset)
}

open class InvertedPositionedColliderPrototype(private val collider: ColliderPrototype) : ColliderPrototype {

    companion object {
        val EMPTY = InvertedPositionedCollider(InvertedPositionedCollider(EmptyCollider()))
    }

    override fun collides(myPos: Vector2, other: ColliderPrototype, otherPos: Vector2): Boolean {
        return !collider.collides(myPos, other, otherPos)
    }
}

operator fun ColliderPrototype.not(): InvertedPositionedColliderPrototype {
    return InvertedPositionedColliderPrototype(this)
}

open class PolyColliderPrototype(
    val subColliders: MutableList<ColliderPrototype>,
    val requiredColliders: MutableList<ColliderPrototype> = mutableListOf(),
    pos: vecDelegate = zeroVecDelegate()
) : ColliderPrototype {
    companion object {
        val EMPTY = PolyPositionedCollider(mutableListOf())
    }

    override fun collides(myPos: Vector2, other: ColliderPrototype, otherPos: Vector2): Boolean {
        return subColliders.any { subCollider ->
            subCollider.collides(myPos, other, otherPos)
        } && requiredColliders.all { subCollider ->
            subCollider.collides(myPos, other, otherPos)
        }
    }
}

open class EmptyColliderPrototype(pos: vecDelegate = zeroVecDelegate()) : ColliderPrototype {

    companion object {
        val EMPTY = EmptyCollider()
    }

    override fun collides(myPos: Vector2, other: ColliderPrototype, otherPos: Vector2): Boolean = false
}

open class PointColliderPrototype : BoxColliderPrototype(0.0, 0.0)