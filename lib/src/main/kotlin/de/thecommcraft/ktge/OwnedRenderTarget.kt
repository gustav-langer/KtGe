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
                for (attachment in this.colorAttachments) {
                    when (attachment) {
                        is ColorBufferAttachment -> attachment.colorBuffer.destroy()
                        else -> error("unsupported attachment `$attachment` in ResizableRenderTarget")
                    }
                }
                this.depthBuffer?.destroy()
                this.detachColorAttachments()
                this.detachDepthBuffer()
                this.destroy()
            }
        }
    }
}
