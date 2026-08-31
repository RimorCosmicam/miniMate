package com.minimate

import com.minimate.touchpad.model.TouchpadSettings
import com.minimate.ui.components.CommandContext
import com.minimate.ui.components.MenuBranch
import com.minimate.ui.components.MenuNode
import com.minimate.ui.components.buildCommandMenu
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The command bar draws three columns and the third is the setting itself.
 *
 * A branch reaching the third column therefore has nowhere to open into: it lists, it highlights,
 * and nothing happens. That is exactly how the scene picker failed — Scene, Choose, a family, and
 * then a fourth column that does not exist — and it looked like a menu that simply did not work.
 */
class CommandMenuTest {

    private fun menu(context: CommandContext): List<MenuNode> = buildCommandMenu(
        context = context,
        settings = TouchpadSettings(),
        onChange = {},
        onOpenSceneStudio = {},
        onOpenKeyboardStudio = {},
        onOpenEdgeStudio = {},
        onOpenPillEditor = {},
        onEditPanels = {},
        onPairNewDevice = {},
        onRefreshDevices = {},
        onDisconnect = {},
        pairedSummary = "Not connected"
    )

    @Test
    fun noBranchOpensIntoAColumnTheBarDoesNotDraw() {
        CommandContext.entries.forEach { context ->
            menu(context).forEach { first ->
                (first as? MenuBranch)?.children?.forEach { second ->
                    (second as? MenuBranch)?.children?.forEach { third ->
                        assertTrue(
                            "${first.label} > ${second.label} > ${third.label} is a branch in the " +
                                "third column, which has nothing to open into",
                            third !is MenuBranch
                        )
                    }
                }
            }
        }
    }

    /** Every page offers something, or the bar opens onto an empty column. */
    @Test
    fun everyPageHasItsOwnMenuAndNoneAreEmpty() {
        CommandContext.entries.forEach { context ->
            val root = menu(context)
            assertTrue(context.name, root.isNotEmpty())
            root.forEach { node ->
                if (node is MenuBranch) assertTrue("${context.name}/${node.label}", node.children.isNotEmpty())
            }
        }
    }

    /** Every scene is reachable from the bar, not only the ones a family happened to expose. */
    @Test
    fun everySceneIsOneTapAway() {
        val scene = menu(CommandContext.TRACKPAD).filterIsInstance<MenuBranch>().first { it.label == "Scene" }
        val choose = scene.children.filterIsInstance<MenuBranch>().first { it.label == "Choose" }
        assertTrue(choose.children.size == com.minimate.touchpad.model.shaderScenes.size)
    }
}
