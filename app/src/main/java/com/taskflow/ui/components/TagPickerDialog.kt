package com.taskflow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.taskflow.data.entity.Tag

/**
 * Shows every existing tag as a checkbox reflecting whether it's attached to this task,
 * plus a small inline field to create a brand-new tag and attach it immediately.
 *
 * This dialog doesn't own any data itself — the caller (a screen backed by a ViewModel
 * that already knows about tags) supplies the current state and reacts to callbacks,
 * so it can be reused from Inbox, List Detail, or anywhere else a task lives.
 */
@Composable
fun TagPickerDialog(
    allTags: List<Tag>,
    attachedTagIds: Set<Long>,
    onToggleTag: (Tag) -> Unit,
    onCreateAndAttachTag: (name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newTagName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tags") },
        text = {
            Column {
                if (allTags.isEmpty()) {
                    Text("No tags yet — create one below.")
                } else {
                    allTags.forEach { tag ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = tag.id in attachedTagIds,
                                onCheckedChange = { onToggleTag(tag) }
                            )
                            Text("#${tag.name}")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("New tag") }
                    )
                    Button(onClick = {
                        if (newTagName.isNotBlank()) {
                            onCreateAndAttachTag(newTagName)
                            newTagName = ""
                        }
                    }) {
                        Text("Add")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}