package com.minimate

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.minimate.touchpad.model.ThemeFilter
import com.minimate.ui.shader.filterShaderSource
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A filter can compile and still put up a black screen — sampling out of bounds, a vignette that
 * saturates, a uniform left at its default. Compiling proves nothing about what comes out, so
 * this renders each filter over a known image offscreen and looks at the result.
 */
@RunWith(AndroidJUnit4::class)
class ThemeFilterRenderTest {

    private val size = 96

    @Test
    fun noFilterRendersABlackScreen() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        val report = ThemeFilter.entries.map { filter -> filter to brightnessThrough(filter) }
        val dark = report.filter { (_, lit) -> lit < 0.02f }
        check(dark.isEmpty()) {
            "these filters render black over a lit source:\n" +
                report.joinToString("\n") { (f, lit) -> "  ${f.name} = %.4f".format(lit) }
        }
    }

    /**
     * The same check with the resolution never supplied.
     *
     * This is the actual shape of the bug that blacked out CRT, kaleidoscope, fisheye and mirror
     * prism: the uniform was being written to the shader after its RenderEffect already existed,
     * so it never arrived, and every filter that divides by it collapsed. A filter must degrade to
     * an unfiltered frame when it does not know how big it is, never to black.
     */
    @Test
    fun noFilterRendersBlackBeforeItsSizeArrives() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        val report = ThemeFilter.entries.map { it to brightnessThrough(it, withResolution = false) }
        val dark = report.filter { (_, lit) -> lit < 0.02f }
        check(dark.isEmpty()) {
            "these filters go black when the layer size has not reached them yet:\n" +
                report.joinToString("\n") { (f, lit) -> "  ${f.name} = %.4f".format(lit) }
        }
    }

    /** Mean luminance of a mid-grey, clearly-lit test image put through the filter. */
    private fun brightnessThrough(filter: ThemeFilter, withResolution: Boolean = true): Float {
        val shader = RuntimeShader(filterShaderSource(filter)).apply {
            setFloatUniform("uTime", 3.25f)
            if (withResolution) setFloatUniform("uResolution", size.toFloat(), size.toFloat())
        }
        val reader = ImageReader.newInstance(
            size, size, PixelFormat.RGBA_8888, 2,
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
        )
        val renderer = HardwareRenderer().apply { setSurface(reader.surface) }
        val node = RenderNode(filter.name).apply { setPosition(0, 0, size, size) }

        node.beginRecording().apply {
            // Deliberately not flat: a solid fill cannot tell a filter that samples the wrong
            // place apart from one that works.
            drawColor(android.graphics.Color.rgb(150, 150, 150))
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(230, 200, 120)
            }
            drawCircle(size * .5f, size * .5f, size * .3f, paint)
        }
        node.endRecording()
        node.setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "content"))
        renderer.setContentRoot(node)
        renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()

        val image = reader.acquireNextImage()
        val buffer = requireNotNull(image.hardwareBuffer)
        val bitmap = requireNotNull(
            Bitmap.wrapHardwareBuffer(buffer, ColorSpace.get(ColorSpace.Named.SRGB))
        ).copy(Bitmap.Config.ARGB_8888, false)!!

        var total = 0.0
        // The centre only. Several filters legitimately vignette their edges to black.
        val from = size / 4
        val until = size * 3 / 4
        for (y in from until until) for (x in from until until) {
            val c = bitmap.getPixel(x, y)
            total += (0.2126 * ((c shr 16) and 255) + 0.7152 * ((c shr 8) and 255) + 0.0722 * (c and 255)) / 255.0
        }
        val count = (until - from) * (until - from)

        bitmap.recycle()
        buffer.close()
        image.close()
        renderer.destroy()
        reader.close()
        node.discardDisplayList()
        return (total / count).toFloat()
    }
}
