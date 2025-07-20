package de.thecommcraft.ktge

import org.openrndr.events.Event
import kotlin.properties.Delegates

open class StateHandler<T>(startState: T) {
    companion object {
        open class StateEvent<T> {
            open val change = Event<Pair<T, T>>("change")
        }
    }
    open val event = StateEvent<T>()
    open var state: T by Delegates.observable(startState) { _, t, t2 ->
        if (t != t2) event.change.trigger(t to t2)
    }
    open var lastState: T = startState
    open val changed
        get() = state != lastState
    open fun transitionComplete() {
        lastState = state
    }
}