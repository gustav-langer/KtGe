package de.thecommcraft.ktge
import org.openrndr.MouseEvent
import org.openrndr.MouseButton
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import javax.swing.Box
import kotlin.properties.Delegates

class ButtonEvent {
    val down = Event<Unit>("down")
    val up = Event<Unit>("up")
    val select = Event<Unit>("select")
    val unselect = Event<Unit>("unselect")
}
abstract class BoxButton(val size: Vector2): Sprite() {
    var blocked by Delegates.observable(false) { _, old, new ->
        if (old == new) return@observable
        if (new && down) {
            down = false
        }
    }
    var selected: Boolean = false
        private set
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
        on(event.select) {
            selected = true
        }
        on(event.unselect) {
            selected = false
        }
        initButton()
    }
    open fun clickPredicate(ev: MouseEvent): Boolean {
        return ev.button == MouseButton.LEFT
    }
}

open class ButtonList<T: BoxButton>(val buildFun: BuildFun<ButtonList<T>>) : Sprite() {
    protected open val buttons: MutableList<T> = mutableListOf()
    protected open var buttonIdx: Int = 0

    var blocked: Boolean
        get() = buttons.all { it.blocked }
        set(value) { buttons.forEach { it.blocked = value } }

    override fun initSprite() {
        buildFun()
        assert(buttons.size > 0)
        selectButton()
        initButtonList()
    }

    open fun initButtonList() {}

    fun button(button: T) {
        buttons.add(button)
        createSprite(button)
    }

    protected fun unselectButton() {
        buttons[buttonIdx].event.unselect.trigger(Unit)
    }

    protected fun selectButton() {
        buttons[buttonIdx].event.select.trigger(Unit)
    }

    fun nextButton() {
        unselectButton()
        buttonIdx = (buttonIdx + 1).mod(buttons.size)
        selectButton()
    }

    fun previousButton() {
        unselectButton()
        buttonIdx = (buttonIdx - 1).mod(buttons.size)
        selectButton()
    }

    fun triggerButton() {
        buttons[buttonIdx].manualTrigger()
    }
}
