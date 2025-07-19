package de.thecommcraft.ktge

import org.openrndr.draw.*

interface OwnedRenderTarget : RenderTarget, OwnedResource {
    companion object {
        fun of(
            width: Int,
            height: Int,
            contentScale: Double = 1.0,
            multisample: BufferMultisample = BufferMultisample.Disabled,
            session: Session? = Session.active,
            builder: RenderTargetBuilder.() -> Unit
        ): OwnedRenderTarget = object :
            OwnedRenderTarget,
            RenderTarget by renderTarget(
                width,
                height,
                contentScale,
                multisample,
                session,
                builder
            ) {
            override fun cleanUp() {
                this.colorBuffer(0).destroy()
                this.detachColorAttachments()
                this.destroy()
            }
        }
    }
}
