package de.thecommcraft.ktge

import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.properties.Delegates.observable
import kotlin.properties.ReadWriteProperty
import kotlin.math.*
import kotlin.reflect.KProperty

internal fun Double.squared(): Double = this * this

fun <T>unobservedDelegate(initialValue: T): ReadWriteProperty<Any?, T> {
    return observable(initialValue) { _, _, _ -> }
}

typealias vecDelegate = ReadWriteProperty<Any?, Vector2>

fun zeroVecDelegate(): vecDelegate = unobservedDelegate(Vector2.ZERO)

fun positionedDelegate(positioned: Positioned): vecDelegate {
    return object : vecDelegate {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Vector2 {
            return positioned.position
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Vector2) {
            positioned.position = value
        }
    }
}

interface Collider {
    fun collides(other: Collider): Boolean {
        return false
    }
}

interface PositionedCollider : Collider, Positioned {
    val colliderPrototype: ColliderPrototype
    override fun collides(other: Collider): Boolean {
        if (other is PositionedCollider) return colliderPrototype.collides(position, other.colliderPrototype, other.position)
        return super.collides(other)
    }
}

data class Projection(val min: Double, val max: Double) {
    fun overlaps(other: Projection): Boolean = this.max > other.min && other.max > this.min
}

interface BoxCollider : PositionedCollider {
    var width: Double
    var height: Double
    val rectangle: Rectangle
    companion object {
        class BoxColliderBuilder internal constructor() {
            var width: Double = 0.0
            var height: Double = 0.0
            internal var positionVecDelegate: vecDelegate = unobservedDelegate(Vector2.ZERO)
            var position: Vector2 by positionVecDelegate
            var rectangle: Rectangle
                get() = Rectangle.fromCenter(position, width, height)
                set(value) {
                    position = value.center
                    width = value.width
                    height = value.height
                }
            var parent: Positioned? = null
                set(value) {
                    value?.let { positionVecDelegate = positionedDelegate(it) }
                    field = value
                }
            fun setPositionDelegate(vecDelegate: vecDelegate) {
                positionVecDelegate = vecDelegate
            }
        }
        fun build(buildFun: BuildFun<BoxColliderBuilder>): BoxCollider {
            val builder = BoxColliderBuilder()
            builder.buildFun()
            return object : BoxCollider {
                override val colliderPrototype = BoxColliderPrototype(builder.width, builder.height)
                override var position: Vector2 by builder.positionVecDelegate
                override val rectangle: Rectangle
                    get() = colliderPrototype.correspondingRectangle(position)
                override var width: Double by colliderPrototype::width
                override var height: Double by colliderPrototype::height
            }
        }
    }
}

fun BoxCollider.asRotated(rotation: Double = 0.0) {}

fun boxColliderOf(width: Double, height: Double, position: Vector2 = Vector2.ZERO) =
    object : BoxCollider {
        override val colliderPrototype = BoxColliderPrototype(width, height)
        override var width: Double by colliderPrototype::width
        override var height: Double by colliderPrototype::height
        override var position: Vector2 = position
        override val rectangle: Rectangle
            get() = colliderPrototype.correspondingRectangle(position)
    }

interface CircleCollider : PositionedCollider {
    var radius: Double
    val circle: Circle
}

fun circleColliderOf(radius: Double, position: Vector2 = Vector2.ZERO) =
    object : CircleCollider {
        override val colliderPrototype = CircleColliderPrototype(radius)
        override var position: Vector2 = position
        override val circle: Circle
            get() = Circle(position, radius)
        override var radius: Double by colliderPrototype::radius
    }

interface Rotation {
    val degrees: Double
    val radians: Double

    companion object {
        val ZERO = object : Rotation {
            override val degrees: Double = 0.0
            override val radians: Double = 0.0
        }
        fun degrees(degrees: Double): Rotation {
            return Degrees(degrees=degrees)
        }
        fun radians(radians: Double): Rotation {
            return Radians(radians=radians)
        }
    }
}

@JvmInline
value class Degrees(override val degrees: Double) : Rotation {
    override val radians: Double
        get() = degrees / 180.0 * PI
}

@JvmInline
value class Radians(override val radians: Double) : Rotation {
    override val degrees: Double
        get() = radians / PI * 180.0
}

interface RotatedBoxCollider : BoxCollider {
    @Deprecated(
        "Returns unrotated Rectangle. As this is not entirely clear from the name, it is recommended to use the extension attribute `.nonRotatedRectangle` instead.",
        replaceWith = ReplaceWith(".nonRotatedRectangle")
    )
    override val rectangle: Rectangle
        get() = nonRotatedRectangle
    var rotation: Rotation
}

fun RotatedBoxCollider.asNonRotated() {}

val RotatedBoxCollider.nonRotatedRectangle: Rectangle
    get() = Rectangle.fromCenter(position, width, height)

fun rotatedBoxColliderOf(width: Double, height: Double, rotation: Rotation = Rotation.ZERO, position: Vector2 = Vector2.ZERO) =
    object : RotatedBoxCollider {
        override val colliderPrototype = RotatedBoxColliderPrototype(width, height, rotation)
        override var rotation: Rotation by colliderPrototype::rotation
        override var width: Double by colliderPrototype::width
        override var height: Double by colliderPrototype::height
        override var position: Vector2 = position
    }

interface DisplacedPositionedCollider : PositionedCollider {
    val collider: PositionedCollider
    var offset: Vector2
}

/**
 * @param[collider] The position of this [Collider] is ignored.
 * If you want to use the position of this Collider, pass [InheritPosition] or [InheritDisplacedPosition] as position into the function.
 *
 * InheritPosition will cause the position of the [DisplacedPositionedCollider] to be the same as the collider's position.
 *
 * InheritDisplacedPosition will cause the position of the DisplacedPositionedCollider to be offset from the collider's position.
 */
fun displacedPositionedColliderOf(collider: PositionedCollider, offset: Vector2 = Vector2.ZERO, position: Vector2 = Vector2.ZERO) =
    object : DisplacedPositionedCollider {
        override val colliderPrototype = DisplacedPositionedColliderPrototype(collider.colliderPrototype, offset)
        override val collider: PositionedCollider = collider
        override var offset: Vector2 by colliderPrototype::offset
        override var position: Vector2 = position
    }

object InheritPosition

object InheritDisplacedPosition

enum class DisplacementOffsetChangeBehavior {
    MOVE_CHILD_COLLIDER,
    MOVE_PARENT_COLLIDER
}

fun displacedPositionedColliderOf(collider: PositionedCollider, offset: Vector2 = Vector2.ZERO, position: InheritPosition) =
    object : DisplacedPositionedCollider {
        override val colliderPrototype = DisplacedPositionedColliderPrototype(collider.colliderPrototype, offset)
        override val collider: PositionedCollider = collider
        override var offset: Vector2 by colliderPrototype::offset
        override var position: Vector2 by collider::position
    }

fun displacedPositionedColliderOf(collider: PositionedCollider, offset: Vector2 = Vector2.ZERO, position: InheritDisplacedPosition, displacementOffsetChangeBehavior: DisplacementOffsetChangeBehavior = DisplacementOffsetChangeBehavior.MOVE_CHILD_COLLIDER) =
    object : DisplacedPositionedCollider {
        override val colliderPrototype = DisplacedPositionedColliderPrototype(collider.colliderPrototype, offset)
        override val collider: PositionedCollider = collider
        override var offset: Vector2
            get() = colliderPrototype.offset
            set(value) {
                if (displacementOffsetChangeBehavior == DisplacementOffsetChangeBehavior.MOVE_CHILD_COLLIDER) {
                    collider.position -= offset - value
                }
                colliderPrototype.offset = value
            }
        override var position: Vector2
            get() = collider.position - offset
            set(value) { collider.position = value + offset }
    }

infix fun PositionedCollider.offset(offset: Vector2): DisplacedPositionedCollider {
    return displacedPositionedColliderOf(this, offset, InheritDisplacedPosition)
}

interface InvertedPositionedCollider : PositionedCollider {
    val collider: PositionedCollider
}

/**
 * @param[collider] The position of this [Collider] is ignored.
 * If you want to use the position of this Collider, pass [InheritPosition] as position into the function.
 */
fun invertedPositionedColliderOf(collider: PositionedCollider, position: Vector2 = Vector2.ZERO) =
    object : InvertedPositionedCollider {
        override val colliderPrototype = InvertedPositionedColliderPrototype(collider.colliderPrototype)
        override val collider: PositionedCollider = collider
        override var position: Vector2 = position
    }

fun invertedPositionedColliderOf(collider: PositionedCollider, position: InheritPosition) =
    object : InvertedPositionedCollider {
        override val colliderPrototype = InvertedPositionedColliderPrototype(collider.colliderPrototype)
        override val collider: PositionedCollider = collider
        override var position: Vector2 by collider::position
    }

operator fun PositionedCollider.not(): InvertedPositionedCollider {
    return invertedPositionedColliderOf(this, InheritPosition)
}

interface PolyPositionedCollider : PositionedCollider {
    val subColliders: MutableList<PositionedCollider>
    val requiredColliders: MutableList<PositionedCollider>
}

/**
 * The positions of the [subColliders] and [requiredColliders] are ignored.
 */
fun polyPositionedColliderOf(subColliders: MutableList<PositionedCollider>, requiredColliders: MutableList<PositionedCollider>, position: Vector2 = Vector2.ZERO) =
    object : PolyPositionedCollider {
        override val colliderPrototype = object : ColliderPrototype {
            override fun collides(myPos: Vector2, other: ColliderPrototype, otherPos: Vector2): Boolean {
                return subColliders.any { subCollider ->
                    subCollider.colliderPrototype.collides(myPos, other, otherPos)
                } && requiredColliders.all { subCollider ->
                    subCollider.colliderPrototype.collides(myPos, other, otherPos)
                }
            }
        }
        override val subColliders: MutableList<PositionedCollider> = subColliders
        override val requiredColliders: MutableList<PositionedCollider> = requiredColliders
        override var position: Vector2 = position
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
        colliders.addAll(invertedColliders.map(::invertedPositionedColliderOf))
        requiredColliders.addAll(invertedRequiredColliders.map(::invertedPositionedColliderOf))
        return polyPositionedColliderOf(colliders, requiredColliders)
    }
}

fun collider(buildFun: BuildFun<ColliderBuilder>): PolyPositionedCollider {
    return ColliderBuilder(buildFun).build()
}

open class EmptyCollider(override var position: Vector2 = Vector2.ZERO) : PositionedCollider {

    override val colliderPrototype = EmptyColliderPrototype()

    companion object {
        val EMPTY = EmptyCollider()
    }

    override fun collides(other: Collider): Boolean = false
}

interface PointCollider : PositionedCollider

fun pointColliderOf(position: Vector2): PointCollider =
    object : PointCollider, BoxCollider by boxColliderOf(0.0, 0.0, position) {}
