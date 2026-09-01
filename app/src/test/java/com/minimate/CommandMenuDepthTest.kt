package com.minimate

import com.minimate.touchpad.model.TouchpadSettings
import com.minimate.ui.components.CommandContext
import com.minimate.ui.components.MenuBranch
import com.minimate.ui.components.MenuNode
import com.minimate.ui.components.buildCommandMenu
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bar draws three columns and the third holds the setting itself, so a branch nested any
 * deeper is unreachable: its parent lists fine and tapping it does nothing at all, which is
 * exactly how the scene picker came to show every scene in the catalog and change none of them.
 * Cheap to check, and invisible until someone tries the one path that is too deep.
 */
class CommandMenuDepthTest {

    private fun menu(context: CommandContext) = buildCommandMenu(
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

    private fun tooDeep(nodes: List<MenuNode>, depth: Int = 1, trail: String = ""): List<String> =
        nodes.flatMap { node ->
            val path = if (trail.isEmpty()) node.label else "$trail > ${node.label}"
            when {
                node !is MenuBranch -> emptyList()
                depth >= 3 -> listOf(path)
                else -> tooDeep(node.children, depth + 1, path)
            }
        }

    @Test
    fun noMenuPathIsDeeperThanTheBarCanShow() {
        CommandContext.entries.forEach { context ->
            val unreachable = tooDeep(menu(context))
            assertTrue(
                "$context has branches the bar cannot open: $unreachable",
                unreachable.isEmpty()
            )
        }
    }

    @Test
    fun everyContextOffersSomething() {
        CommandContext.entries.forEach { context ->
            assertTrue(context.name, menu(context).isNotEmpty())
        }
    }
}
