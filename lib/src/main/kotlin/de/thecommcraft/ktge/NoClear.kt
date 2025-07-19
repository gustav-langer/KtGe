package de.thecommcraft.ktge

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Matrix44

class NoClear(val multisample: BufferMultisample = BufferMultisample.Disabled) : KtgeExtension {
    override var enabled: Boolean = true
    private var renderTarget: RenderTarget? = null
    private var resolvedColorBuffer: ColorBuffer? = null

    /**
     * code-block to draw an optional custom backdrop
     */
    var backdrop: (() -> Unit)? = null

    override fun beforeDraw(drawer: Drawer, app: KtgeApp) {
        if (app.width > 0 && app.height > 0) {    // only if the window is not minimised
            if (renderTarget == null || renderTarget?.width != app.width || renderTarget?.height != app.height) {
                renderTarget?.let {
                    it.colorBuffer(0).destroy()
                    it.detachColorAttachments()
                    it.destroy()
                }

                renderTarget = renderTarget(app.width, app.height, app.window.contentScale, multisample) {
                    colorBuffer()
                    depthBuffer()
                }

                if (multisample != BufferMultisample.Disabled) {
                    resolvedColorBuffer?.destroy()
                    resolvedColorBuffer = colorBuffer(app.width, app.height, app.window.contentScale)
                }

                renderTarget?.let {
                    drawer.withTarget(it) {
                        clear(app.backgroundColor ?: ColorRGBa.TRANSPARENT)
                        backdrop?.invoke() // draw custom backdrop
                    }
                }
            }
        }
        renderTarget?.bind()
    }

    override fun afterDraw(drawer: Drawer, app: KtgeApp) {
        renderTarget?.unbind()

        renderTarget?.let {
            drawer.isolated {
                drawer.ortho()
                drawer.view = Matrix44.IDENTITY
                drawer.model = Matrix44.IDENTITY
                if (multisample != BufferMultisample.Disabled) {
                    it.colorBuffer(0).copyTo(resolvedColorBuffer!!)
                    drawer.image(resolvedColorBuffer!!)
                } else {
                    drawer.image(it.colorBuffer(0))
                }
            }
        }
    }
}