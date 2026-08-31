package com.minimate

import android.graphics.RuntimeShader
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.minimate.touchpad.model.shaderScenes
import com.minimate.ui.shader.sceneShaderSource
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every scene must compile.
 *
 * AGSL is compiled by Skia at runtime, so a green build says nothing about whether a scene body
 * is valid — a typo in a shader ships as a black screen and is only found by opening that scene
 * on the device. This walks the whole catalog in one instrumented run instead, which is both
 * faster and far less intrusive than paging through the scene list by hand.
 */
@RunWith(AndroidJUnit4::class)
class SceneShaderCompileTest {

    @Test
    fun everySceneCompiles() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        val failures = shaderScenes.mapNotNull { scene ->
            runCatching { RuntimeShader(sceneShaderSource(scene)) }
                .exceptionOrNull()
                ?.let { "${scene.id}: ${it.message?.trim()}" }
        }
        check(failures.isEmpty()) {
            "${failures.size} of ${shaderScenes.size} scenes failed to compile:\n" +
                failures.joinToString("\n\n")
        }
    }

    /** The uniform names the renderer sets must exist, or setFloatUniform throws at draw time. */
    @Test
    fun everySceneAcceptsTheUniformsTheRendererSets() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        val failures = shaderScenes.mapNotNull { scene ->
            runCatching {
                RuntimeShader(sceneShaderSource(scene)).apply {
                    setFloatUniform("uResolution", 361f, 399f)
                    setFloatUniform("uTime", 1f)
                    setFloatUniform("uNow", 1f)
                    setFloatUniform("uTouchCount", 0f)
                    setFloatUniform("uTouchStrength", 1f)
                    setFloatUniform("uAberration", .6f)
                    setFloatUniform("uGrain", .3f)
                    scene.defaults.forEachIndexed { i, v -> setFloatUniform("uP$i", v) }
                }
            }.exceptionOrNull()?.let { "${scene.id}: ${it.message?.trim()}" }
        }
        check(failures.isEmpty()) { failures.joinToString("\n\n") }
    }
}
