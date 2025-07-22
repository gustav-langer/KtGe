package de.thecommcraft.ktge

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.MagnifyingFilter

interface OwnedImage : OwnedResource {
    val buf: ColorBuffer
    override fun cleanUp() {
        buf.destroy()
    }
    companion object {
        @JvmInline
        internal value class OwnedImageImplementation(override val buf: ColorBuffer) : OwnedImage
    }
}
