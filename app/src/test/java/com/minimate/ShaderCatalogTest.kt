package com.minimate

import com.minimate.touchpad.model.ShaderFamily
import com.minimate.touchpad.model.shaderScenes
import com.minimate.ui.shader.sceneShaderSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What can be checked without a GPU.
 *
 * Whether a scene body compiles is Skia's business and is covered by the instrumented test, but
 * the mistakes that are easiest to make here are structural — a fifth control with no uniform to
 * put it in, a body reading a slot the scene never declared — and those are visible from the JVM.
 */
class ShaderCatalogTest {

    @Test fun sceneIdsAndLabelsAreUnique() {
        assertEquals(shaderScenes.size, shaderScenes.map { it.id }.toSet().size)
        assertEquals(shaderScenes.size, shaderScenes.map { it.label }.toSet().size)
    }

    /** The prelude declares uP0..uP3 and nothing more, so a fifth control would be silently lost. */
    @Test fun noSceneDeclaresMoreControlsThanThereAreUniformSlots() {
        shaderScenes.forEach { scene ->
            assertTrue("${scene.id} has ${scene.params.size} params", scene.params.size <= 4)
            assertEquals(scene.id, scene.params.size, scene.params.map { it.key }.toSet().size)
        }
    }

    /** Reading uP2 with two controls declared reads whatever the previous scene left there. */
    @Test fun noSceneReadsAParameterSlotItDidNotDeclare() {
        shaderScenes.forEach { scene ->
            for (slot in scene.params.size until 4) {
                assertTrue("${scene.id} reads uP$slot but declares ${scene.params.size} controls",
                    "uP$slot" !in scene.body)
            }
        }
    }

    @Test fun everyControlDefaultSitsInsideItsOwnRange() {
        shaderScenes.forEach { scene ->
            scene.params.forEach { param ->
                assertTrue("${scene.id}/${param.key}", param.min < param.max)
                assertTrue("${scene.id}/${param.key}", param.default in param.min..param.max)
            }
        }
    }

    @Test fun everyPaletteHasFourStops() {
        shaderScenes.forEach { scene ->
            assertTrue(scene.id, scene.palettes.isNotEmpty())
            scene.palettes.forEach { assertEquals("${scene.id}/${it.label}", 4, it.stops.size) }
        }
    }

    @Test fun assembledSourceHasExactlyOneEntryPointAndOneSceneBody() {
        shaderScenes.forEach { scene ->
            val source = sceneShaderSource(scene)
            assertEquals(scene.id, 1, Regex("half4\\s+main\\s*\\(").findAll(source).count())
            assertEquals(scene.id, 1, Regex("float3\\s+scene\\s*\\(").findAll(source).count())
        }
    }

    /**
     * `out`, `in` and `sample` are SkSL keywords; using one as a variable name fails to compile on
     * the device only, which is the slowest possible place to find out.
     */
    @Test fun noSceneUsesAReservedWordAsAnIdentifier() {
        val reserved = listOf("out", "in", "inout", "sample", "varying", "attribute", "discard")
        shaderScenes.forEach { scene ->
            reserved.forEach { word ->
                assertTrue("${scene.id} declares a variable named '$word'",
                    Regex("""\b(float|float2|float3|float4|half|half2|half3|half4|int|bool)\s+$word\b""")
                        .find(scene.body) == null)
            }
        }
    }

    /** Every family carries scenes; an empty one is a heading with nothing under it. */
    @Test fun everyFamilyHasScenes() {
        ShaderFamily.entries.forEach { family ->
            assertTrue(family.label, shaderScenes.any { it.family == family })
        }
    }
}
