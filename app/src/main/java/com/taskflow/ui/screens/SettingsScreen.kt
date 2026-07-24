package com.taskflow.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskflow.ui.viewmodel.TagViewModel

/**
 * Placeholder — Tags management is the only "setting" that exists right now. As more
 * app-level preferences get added (theme, notification defaults, etc.) they land here
 * as additional sections above/below Tags rather than needing a new tab.
 */
@Composable
fun SettingsScreen(tagViewModel: TagViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
        Text(
            "Settings",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
        )
        TagsScreen(viewModel = tagViewModel)
    }
}