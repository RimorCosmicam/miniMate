package com.minimate

import android.graphics.RuntimeShader
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.minimate.touchpad.model.ThemeFilter
import com.minimate.ui.shader.filterShaderSource
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A filter that fails to compile falls back to a decorative overlay, which looks like the filter
 * doing nothing or doing something wrong rather than like an error. Nothing surfaces it except
 * selecting each of the seventeen by hand, so it gets checked here instead.
 */
@RunWith(AndroidJUnit4::class)
class ThemeFilterCompileTest {

    @Test
    fun everyFilterCompiles() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        val failures = ThemeFilter.entries.mapNotNull { filter ->
            runCatching { RuntimeShader(filterShaderSource(filter)) }
                .exceptionOrNull()?.let { "${filter.name}: ${it.message?.trim()}" }
        }
        check(failures.isEmpty()) {
            "${failures.size} of ${ThemeFilter.entries.size} filters failed to compile:\n" +
                failures.joinToString("\n\n")
        }
    }
}
