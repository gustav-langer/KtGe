package de.thecommcraft.ktge

import org.openrndr.events.Event

open class EventListener<E>(open val event: Event<E>, open val eventListener: (E) -> Unit) : OwnedResource {
    open fun unlisten() {
        event.cancel(eventListener)
    }
    open fun listen() {
        event.listen(eventListener)
    }
    override fun cleanUp() {
        this.unlisten()
    }
}

class OneTimeEventListener<E>(event: Event<E>, private val parent: Sprite, eventListener: (E) -> Unit) : EventListener<E>(event, eventListener) {
    override val eventListener: (E) -> Unit = {
        parent.removeOwnedResource(parent)
        eventListener(it)
    }
}
