package com.minimate.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimate.ui.theme.Mont

/** The menu is set entirely in Mont Black. */
private val MenuWeight = FontWeight.Black

/** Nodes of the command tree. Depth is capped at three by construction: the third level is a leaf. */
sealed interface MenuNode {
    val label: String
}

data class MenuBranch(override val label: String, val children: List<MenuNode>) : MenuNode
data class MenuAction(override val label: String, val onInvoke: () -> Unit) : MenuNode
data class MenuToggle(override val label: String, val value: Boolean, val onChange: (Boolean) -> Unit) : MenuNode
data class MenuChoice(
    override val label: String,
    val options: List<String>,
    val selected: Int,
    val onSelect: (Int) -> Unit
) : MenuNode

data class MenuSlider(
    override val label: String,
    val value: Float,
    val range: ClosedFloatingPointRange<Float>,
    val display: String,
    val onChange: (Float) -> Unit
) : MenuNode

data class MenuInfo(override val label: String, val body: String) : MenuNode

/**
 * The command bar.
 *
 * Drops from the top edge and never takes more than a third of the display, so whatever is being
 * adjusted stays visible underneath — on a cover screen a full-height sheet means adjusting a
 * scene you cannot see. Columns fill left to right as choices are made, each separated by a hairline,
 * and the third column is always the setting itself rather than another list.
 */
@Composable
fun CommandBar(
    root: List<MenuNode>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)
    var firstIndex by remember { mutableStateOf(-1) }
    var secondIndex by remember { mutableStateOf(-1) }

    val first = root.getOrNull(firstIndex)
    val secondList = (first as? MenuBranch)?.children.orEmpty()
    val second = secondList.getOrNull(secondIndex)
    val thirdList = (second as? MenuBranch)?.children.orEmpty()

    Box(modifier.fillMaxSize()) {
        // Tapping away closes, so the bar never traps the screen.
        Box(Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onDismiss() })

        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(.33f)
                // Just short of opaque, so the scene reads faintly behind the bar.
                .background(Color.Black.copy(alpha = .95f))
                .padding(start = 22.dp, top = 26.dp, end = 14.dp, bottom = 10.dp)
        ) {
            MenuColumn(
                nodes = root + MenuAction("Close") { onDismiss() },
                selectedIndex = firstIndex,
                onSelect = { index ->
                    firstIndex = if (firstIndex == index) -1 else index
                    secondIndex = -1
                },
                modifier = Modifier.weight(1f)
            )

            if (first is MenuBranch) {
                MenuColumn(
                    nodes = secondList,
                    selectedIndex = secondIndex,
                    onSelect = { index -> secondIndex = if (secondIndex == index) -1 else index },
                    modifier = Modifier.weight(1f)
                )
            }

            if (second is MenuBranch) {
                MenuColumn(
                    nodes = thirdList,
                    selectedIndex = -1,
                    onSelect = { },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MenuColumn(
    nodes: List<MenuNode>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        nodes.forEachIndexed { index, node ->
            when (node) {
                is MenuBranch -> MenuRow(node.label, index == selectedIndex) { onSelect(index) }
                is MenuAction -> MenuRow(node.label, false) { node.onInvoke() }
                is MenuToggle -> MenuRow(node.label, node.value, trailing = if (node.value) "ON" else "OFF") {
                    node.onChange(!node.value)
                }
                is MenuChoice -> MenuRow(
                    node.label,
                    false,
                    trailing = node.options.getOrNull(node.selected).orEmpty()
                ) {
                    node.onSelect((node.selected + 1) % node.options.size.coerceAtLeast(1))
                }
                is MenuSlider -> {
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 2.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Label(node.label, Modifier.weight(1f))
                            Label(node.display, dim = true)
                        }
                        Slider(
                            value = node.value.coerceIn(node.range.start, node.range.endInclusive),
                            onValueChange = node.onChange,
                            valueRange = node.range,
                            modifier = Modifier.fillMaxWidth().height(20.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(.22f)
                            )
                        )
                    }
                }
                is MenuInfo -> Column(Modifier.fillMaxWidth().padding(vertical = 5.dp, horizontal = 2.dp)) {
                    Label(node.label)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        node.body,
                        color = Color.White.copy(.62f),
                        fontFamily = Mont,
                        fontWeight = MenuWeight,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * A row states its own state through brightness alone. Chevrons and highlight fills were doing
 * work the type already does: the selected item is simply the bright one, and the column that
 * appeared beside it is the evidence that it opened.
 */
@Composable
private fun MenuRow(label: String, active: Boolean, trailing: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Label(label, Modifier.weight(1f), dim = !active)
        trailing?.let { Label(it, dim = true) }
    }
}

@Composable
private fun Label(text: String, modifier: Modifier = Modifier, dim: Boolean = false) {
    Text(
        text,
        modifier = modifier,
        color = if (dim) Color.White.copy(.58f) else Color.White,
        fontFamily = Mont,
        fontWeight = MenuWeight,
        fontSize = 15.sp,
        maxLines = 1
    )
}
