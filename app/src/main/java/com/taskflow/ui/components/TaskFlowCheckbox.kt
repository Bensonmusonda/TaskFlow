package com.taskflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val TaskFlowAccent = Color(0xFFDE3163)

/**
 * Material3's built-in Checkbox can't be reshaped this way (fixed internal shape/stroke),
 * so this is a small custom control: a rounded-square box, no border, filled with the
 * accent color and a checkmark when checked. [uncheckedTint] lets a caller lighten the
 * unchecked state when this sits on a colored card (e.g. white-ish on the accent-bg card)
 * instead of the default subtle on-surface tint.
 */
@Composable
fun TaskFlowCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    uncheckedTint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (checked) TaskFlowAccent else uncheckedTint)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}