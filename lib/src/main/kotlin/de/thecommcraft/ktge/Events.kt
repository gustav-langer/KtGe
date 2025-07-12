package de.thecommcraft.ktge

import org.openrndr.events.Event

class EventListener<E>(val event: Event<E>, val eventListener: (E) -> Unit) {
    fun unlisten() {
        event.cancel(eventListener)
    }
    fun listen() {
        event.listen(eventListener)
    }
    fun listenOnce() {
        event.listenOnce(eventListener)
    }
}
