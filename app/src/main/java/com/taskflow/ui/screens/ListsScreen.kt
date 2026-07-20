package com.taskflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskflow.data.entity.TaskList
import com.taskflow.ui.viewmodel.ListViewModel

// Small fixed palette for v1 — matches the monochrome-plus-accent design philosophy.
// Swap for a full color picker later if needed.
private val listColorPalette = listOf(
    "#DE3163", // Cherry Red accent
    "#4A4A4A", // Matte grey
    "#2D6E5E", // Muted teal
    "#8A6D3B", // Muted amber
    "#3B5488"  // Muted blue
)

@Composable
fun ListsScreen(
    viewModel: ListViewModel,
    onOpenList: (TaskList) -> Unit
) {
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    var newListName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(listColorPalette.first()) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("New list")
            OutlinedTextField(
                value = newListName,
                onValueChange = { newListName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("List name") }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listColorPalette.forEach { hex ->
                    ColorSwatch(
                        hex = hex,
                        onClick = { selectedColor = hex }
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.addList(newListName, selectedColor)
                    newListName = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create list")
            }

            Text("Your lists (${lists.size}):")

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(lists, key = { it.id }) { list ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenList(list) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ColorSwatch(hex = list.colorHex, onClick = {})
                            Text(list.name)
                        }
                        IconButton(onClick = { viewModel.deleteList(list) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete list")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(hex: String, onClick: () -> Unit) {
    val color = try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: IllegalArgumentException) {
        Color.Gray
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
    )
}