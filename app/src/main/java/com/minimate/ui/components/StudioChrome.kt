package com.minimate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun StudioPanel(
    title: String,
    subtitle: String,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier.padding(start = 14.dp, top = 18.dp)
            .fillMaxWidth(.72f)
            .shadow(24.dp, RoundedCornerShape(22.dp), spotColor = Color.Black)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Color(0xED1B1C1E), Color(0xF40B0C0E))))
            .border(1.dp, Color.White.copy(.22f), RoundedCornerShape(22.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(start = 2.dp)) {
                Text(title.uppercase(), color = Color.White.copy(.55f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(subtitle, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StudioAction(false, onCancel) { Icon(Icons.Default.Close, "Cancel", tint = Color.White, modifier = Modifier.size(15.dp)) }
            Spacer(Modifier.width(5.dp))
            StudioAction(true, onDone) { Icon(Icons.Default.Check, "Done", tint = Color(0xFF111214), modifier = Modifier.size(15.dp)) }
        }
        content()
    }
}

@Composable
internal fun StudioChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color.White else Color.White.copy(.06f))
            .border(1.dp, if (selected) Color.White.copy(.72f) else Color.White.copy(.14f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (selected) Color(0xFF111214) else Color.White.copy(.78f), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun StudioAction(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
        Modifier.size(29.dp).clip(CircleShape)
            .background(if (selected) Color.White else Color.White.copy(.07f))
            .border(1.dp, Color.White.copy(if (selected) .7f else .16f), CircleShape)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}
