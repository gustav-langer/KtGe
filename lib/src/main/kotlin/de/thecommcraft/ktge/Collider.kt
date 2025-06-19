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
    var width: Double,
    var height: Double,
    initialPosition: Vector2 = Vector2.ZERO
) : PositionedCollider {

    companion object {
        val EMPTY = BoxCollider(0.0, 0.0)
    }

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
    var radius: Double,
    initialPosition: Vector2 = Vector2.ZERO
) : PositionedCollider {

    companion object {
        val EMPTY = CircleCollider(0.0)
    }

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



open class RotatedRectangleCollider(
    width: Double,
    height: Double,
    initialPosition: Vector2 = Vector2.ZERO,
    var rotation: Double = 0.0
) : BoxCollider(width, height, initialPosition) {

    companion object {
        val EMPTY = RotatedRectangleCollider(0.0, 0.0)
    }

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

open class DisplacedPositionedCollider(private val collider: PositionedCollider, var offset: Vector2 = Vector2.ZERO, initialPosition: Vector2 = Vector2.ZERO) : PositionedCollider {
    companion object {
        val EMPTY = DisplacedPositionedCollider(EmptyCollider())
    }

    override var position: Vector2 = initialPosition

    override fun collides(other: Collider): Boolean {
        collider.position = position - offset
        return collider.collides(other)
    }
}

infix fun PositionedCollider.offset(offset: Vector2): DisplacedPositionedCollider {
    return DisplacedPositionedCollider(this, offset)
}

open class InvertedPositionedCollider(private val collider: PositionedCollider, initialPosition: Vector2 = Vector2.ZERO) : PositionedCollider {
    companion object {
        val EMPTY = InvertedPositionedCollider(InvertedPositionedCollider(EmptyCollider()))
    }

    override var position: Vector2 = initialPosition

    override fun collides(other: Collider): Boolean {
        collider.position = position
        return !collider.collides(other)
    }
}

operator fun PositionedCollider.not(): InvertedPositionedCollider {
    return InvertedPositionedCollider(this)
}

open class PolyPositionedCollider(
    val subColliders: MutableList<PositionedCollider>,
    val requiredColliders: MutableList<PositionedCollider> = mutableListOf(),
    initialPosition: Vector2 = Vector2.ZERO
) : PositionedCollider {

    companion object {
        val EMPTY = PolyPositionedCollider(mutableListOf())
    }

    override var position: Vector2 = initialPosition

    override fun collides(other: Collider): Boolean {
        return subColliders.any { subCollider ->
            subCollider.position = this.position
            subCollider.collides(other)
        } && requiredColliders.all { subCollider ->
            subCollider.position = this.position
            subCollider.collides(other)
        }
    }
}

interface AnyColliderBuilder<T> {
    fun collider(collider: PositionedCollider): Unit
    fun collider(colliderBuildFun: BuildFun<T>): Unit
    fun invertedCollider(collider: PositionedCollider): Unit
    fun invertedCollider(colliderBuildFun: BuildFun<T>): Unit
    fun required(collider: PositionedCollider): Unit
    fun required(colliderBuildFun: BuildFun<T>): Unit
    fun invertedRequired(collider: PositionedCollider): Unit
    fun invertedRequired(colliderBuildFun: BuildFun<T>): Unit
    fun build(): PolyPositionedCollider
}

open class ColliderBuilder(private val buildFun: BuildFun<ColliderBuilder>) : AnyColliderBuilder<ColliderBuilder> {
    private val colliders: MutableList<PositionedCollider> = mutableListOf()
    private val colliderBuilders: MutableList<ColliderBuilder> = mutableListOf()
    private val invertedColliders: MutableList<PositionedCollider> = mutableListOf()
    private val invertedColliderBuilders: MutableList<ColliderBuilder> = mutableListOf()
    private val requiredColliders: MutableList<PositionedCollider> = mutableListOf()
    private val requiredColliderBuilders: MutableList<ColliderBuilder> = mutableListOf()
    private val invertedRequiredColliders: MutableList<PositionedCollider> = mutableListOf()
    private val invertedRequiredColliderBuilders: MutableList<ColliderBuilder> = mutableListOf()
    private var built: Boolean = false
    protected fun checkNotBuilt(): Unit {
        check(!built) { "Builder must not be used after being built." }
    }
    override fun collider(collider: PositionedCollider): Unit {
        checkNotBuilt()
        colliders.add(collider)
    }

    override fun collider(colliderBuildFun: BuildFun<ColliderBuilder>): Unit {
        checkNotBuilt()
        colliderBuilders.add(ColliderBuilder(colliderBuildFun))
    }

    override fun invertedCollider(collider: PositionedCollider): Unit {
        checkNotBuilt()
        invertedColliders.add(collider)
    }

    override fun invertedCollider(colliderBuildFun: BuildFun<ColliderBuilder>): Unit {
        checkNotBuilt()
        invertedColliderBuilders.add(ColliderBuilder(colliderBuildFun))
    }

    override fun required(collider: PositionedCollider): Unit {
        checkNotBuilt()
        requiredColliders.add(collider)
    }

    override fun required(colliderBuildFun: BuildFun<ColliderBuilder>): Unit {
        checkNotBuilt()
        requiredColliderBuilders.add(ColliderBuilder(colliderBuildFun))
    }

    override fun invertedRequired(collider: PositionedCollider): Unit {
        checkNotBuilt()
        invertedRequiredColliders.add(collider)
    }

    override fun invertedRequired(colliderBuildFun: BuildFun<ColliderBuilder>): Unit {
        checkNotBuilt()
        invertedRequiredColliderBuilders.add(ColliderBuilder(colliderBuildFun))
    }

    override fun build(): PolyPositionedCollider {
        checkNotBuilt()
        built = true
        colliderBuilders.forEach {
            colliders.add(it.build())
        }
        invertedColliderBuilders.forEach {
            invertedColliders.add(it.build())
        }
        requiredColliderBuilders.forEach {
            requiredColliders.add(it.build())
        }
        invertedRequiredColliderBuilders.forEach {
            invertedRequiredColliders.add(it.build())
        }
        colliders.addAll(invertedColliders.map(::InvertedPositionedCollider))
        requiredColliders.addAll(invertedRequiredColliders.map(::InvertedPositionedCollider))
        return PolyPositionedCollider(colliders, requiredColliders)
    }
}

fun collider(buildFun: BuildFun<ColliderBuilder>): PolyPositionedCollider {
    return ColliderBuilder(buildFun).build()
}

open class EmptyCollider(initialPosition: Vector2 = Vector2.ZERO) : PositionedCollider {

    companion object {
        val EMPTY = EmptyCollider()
    }

    override var position: Vector2 = initialPosition
    override fun collides(other: Collider): Boolean = false
}
