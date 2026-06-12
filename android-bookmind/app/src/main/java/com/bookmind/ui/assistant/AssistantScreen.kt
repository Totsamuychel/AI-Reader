package com.bookmind.ui.assistant

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    bookId: String,
    chapterIndex: Int,
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(bookId, chapterIndex) { viewModel.configure(bookId, chapterIndex) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.bookTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Chapter ${uiState.currentChapter + 1} • Spoiler-Safe",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )
        }
    ) { padding ->
        AssistantPanel(
            uiState = uiState,
            onInputChange = viewModel::onInputChange,
            onSend = viewModel::sendMessage,
            onModeChange = viewModel::setMode,
            onWebToggle = viewModel::setWebEnabled,
            modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}
