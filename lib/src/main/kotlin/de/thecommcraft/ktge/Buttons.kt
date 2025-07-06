package de.thecommcraft.ktge
import org.openrndr.MouseEvent
import org.openrndr.MouseButton
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import kotlin.properties.Delegates

class ButtonEvent {
    val down = Event<Unit>("down")
    val up = Event<Unit>("up")
}
abstract class BoxButton(val size: Vector2): Sprite() {
    var blocked by Delegates.observable(false) { _, old, new ->
        if (old == new) return@observable
        if (new && down) {
            down = false
        }
    }
    val hovering: Boolean
        get() {
            val d = (app.mouse.position - position)
            return (d.x in 0.0..size.x) && (d.y in 0.0..size.y)
        }
    var down: Boolean = false
        private set
    val event by lazy { ButtonEvent() }
    fun manualTrigger() {
        triggerDown()
        triggerUp()
    }
    private inline fun triggerDown() {
        down = true
        event.down.trigger(Unit)
    }
    private inline fun triggerUp() {
        down = false
        event.up.trigger(Unit)
    }
    abstract fun initButton(): Unit
    override fun initSprite() {
        on(app.mouse.buttonDown) {
            if (hovering && !blocked && clickPredicate(it)) {
                triggerDown()
            }
        }
        on(app.mouse.buttonUp) {
            if (down && clickPredicate(it)) {
                triggerUp()
            }
        }
        initButton()
    }
    open fun clickPredicate(ev: MouseEvent): Boolean {
        return ev.button == MouseButton.LEFT
    }
}