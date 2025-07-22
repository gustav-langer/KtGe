package de.thecommcraft.ktge

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.MagnifyingFilter

/**
 * All methods modify the object and return it afterward by default unless specified otherwise.
 */
open class EditableImage internal constructor(private var colorBuffer: ColorBuffer) {
    var finalized: Boolean = false
        internal set
    fun scaleBy(scale: Int, scaleType: MagnifyingFilter = MagnifyingFilter.NEAREST) = apply {
        val oldBuf = colorBuffer
        colorBuffer = oldBuf.scaledBy(scale, scaleType)
        oldBuf.destroy()
    }

    fun scaleTo(width: Int, height: Int, scaleType: MagnifyingFilter = MagnifyingFilter.NEAREST) = apply {
        val oldBuf = colorBuffer
        colorBuffer = oldBuf.scaledTo(width, height, scaleType)
        oldBuf.destroy()
    }

    /**
     * This creates a copy of this and returns it.
     * The underlying [ColorBuffer] is also copied and so changes to the current object will not be reflected in the copy.
     */
    open fun copy() = EditableImage(colorBuffer.scaledBy(1))

    open fun complete(): OwnedImage {
        check(!finalized) { "EditableImage can only be completed once. If you need the behavior of making multiple OwnedImages, use copy() to create a deep copy of this object." }
        finalized = true
        return OwnedImage.Companion.OwnedImageImplementation(colorBuffer)
    }

    /**
     * This creates a copy of this and completes it.
     * Usage not recommended as it is unclear what exactly it does.
     */
    @Deprecated("Unclear usage", replaceWith = ReplaceWith(".copy().finalize()"))
    open fun makeOwnedImage(): OwnedImage {
        return copy().complete()
    }
}