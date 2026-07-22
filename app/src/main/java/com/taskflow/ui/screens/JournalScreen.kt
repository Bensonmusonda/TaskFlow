package com.taskflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskflow.data.entity.Task
import com.taskflow.ui.viewmodel.JournalViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val completedAtFormatter = DateTimeFormatter
    .ofPattern("MMM d, yyyy · h:mm a")
    .withZone(ZoneId.systemDefault())

@Composable
fun JournalScreen(viewModel: JournalViewModel) {
    val entries by viewModel.completedTasks.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tagsCsv by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Retrospective entry", style = MaterialTheme.typography.titleMedium)
        Text(
            "Log something you already did — it's saved as complete immediately.",
            style = MaterialTheme.typography.bodySmall
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("What did you do?") }
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Details (optional)") }
        )
        OutlinedTextField(
            value = tagsCsv,
            onValueChange = { tagsCsv = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Growth domains, comma separated (e.g. Coding, 3DModeling)") }
        )

        Button(
            onClick = {
                viewModel.addRetrospectiveEntry(
                    title = title,
                    description = description.ifBlank { null },
                    tagNamesCsv = tagsCsv
                )
                title = ""
                description = ""
                tagsCsv = ""
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank()
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
            Text("Log entry")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("History (${entries.size}):", style = MaterialTheme.typography.titleMedium)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(entries, key = { it.id }) { entry ->
                JournalEntryRow(entry)
            }
        }
    }
}

@Composable
private fun JournalEntryRow(entry: Task) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(entry.title, style = MaterialTheme.typography.bodyLarge)
            val completedLabel = entry.completedAt?.let {
                completedAtFormatter.format(Instant.ofEpochMilli(it))
            } ?: "—"
            Text(completedLabel, style = MaterialTheme.typography.bodySmall)
        }
    }
}