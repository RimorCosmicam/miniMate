package com.minimate

import com.minimate.touchpad.model.AbstractShaderTheme
import com.minimate.touchpad.model.BackgroundAnimation
import com.minimate.touchpad.model.abstractSubthemes
import com.minimate.touchpad.model.colorwaysFor
import com.minimate.touchpad.model.subthemesFor
import com.minimate.ui.shader.abstractShaderSourceFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeCatalogTest {
    @Test fun productExposesExactlyFiveWorldsAndFiftySourcedScenes() {
        assertEquals(listOf("Space", "Abstract", "Tech", "Arcade", "Beach"), AbstractShaderTheme.values().map { it.label })
        assertEquals(50, abstractSubthemes.size)
        AbstractShaderTheme.values().forEach { assertEquals(it.label, 10, subthemesFor(it).size) }
        assertTrue(abstractSubthemes.all { it.sourceId.isNotBlank() && it.sourceUrl.startsWith("https://") })
        assertTrue(abstractSubthemes.all { it.sourceLicense == "MIT" })
    }

    @Test fun everySceneHasTailoredColorwaysAndCustomColor() {
        abstractSubthemes.forEach { scene ->
            assertTrue(scene.label, scene.colorways.size in 3..8)
            assertEquals(scene.colorways.size + 1, colorwaysFor(scene.theme, scene.index).size)
            assertEquals(scene.colorways.size, scene.colorways.map { it.label }.toSet().size)
            assertEquals(scene.colorways.size, scene.colorways.map { it.stops }.toSet().size)
            assertTrue(scene.colorways.all { it.stops.size == 4 })
            assertTrue(scene.supportsCustomColors)
        }
    }

    @Test fun visualIdentitiesAndAssetSourcesAreNotDuplicatedInsideImageWorlds() {
        assertEquals(50, abstractSubthemes.map { it.label }.toSet().size)
        assertEquals(50, abstractSubthemes.map { it.compositionFamily }.toSet().size)
        listOf(AbstractShaderTheme.COSMIC, AbstractShaderTheme.ARCADE, AbstractShaderTheme.OCEANIC).forEach { theme ->
            assertEquals(10, subthemesFor(theme).map { it.sourceId }.toSet().size)
        }
    }

    @Test fun rejectedCharacterWorldAndOldSceneNamesCannotLeakBack() {
        val text = abstractSubthemes.joinToString(" ") { "${it.theme} ${it.label} ${it.sceneDescription}" }.lowercase()
        assertFalse("miku" in text)
        assertFalse("hatsune" in text)
        val retired=setOf("Nebula Swirl","Meteor Shower","Tide Wash","Terminal Cursor","Disco Ball Glint","Oil Slick")
        assertFalse(abstractSubthemes.any { it.label in retired })
    }

    @Test fun shaderUsesCodeScenesPersistentTouchesAndRealGlyphAtlas() {
        val source=abstractShaderSourceFor(AbstractShaderTheme.ARCADE)
        assertTrue("uniform shader glyphAtlas" in source)
        assertFalse("uniform shader sceneTexture" in source)
        assertTrue("for(int i=0;i<8;i++)" in source)
        assertFalse("imageWorld" in source)
        assertTrue("spaceScene" in source)
        assertTrue("arcadeScene" in source)
        assertTrue("beachScene" in source)
        assertTrue("techField" in source)
        assertTrue("abstractField" in source)
        assertFalse("miku" in source.lowercase())
    }

    @Test fun motionModesProgressFromStillToHyper() {
        val speeds=BackgroundAnimation.values().map { it.speed }
        assertEquals(0f,speeds.first(),0f)
        assertTrue(speeds.zipWithNext().all { (a,b)->b>a })
    }
}
