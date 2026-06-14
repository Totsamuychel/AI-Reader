package com.bookmind.ui.assistant

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookmind.safety.AnswerMode

/**
 * Chat surface shared by the full-screen assistant route and the reader's
 * right-hand side panel.
 */
@Composable
fun AssistantPanel(
    uiState: AssistantUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onModeChange: (AnswerMode) -> Unit,
    onWebToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    selectedText: String? = null,
    onQuickAction: ((String) -> Unit)? = null,
    onClearSelection: (() -> Unit)? = null,
    onClearHistory: (() -> Unit)? = null
) {
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.streamingText) {
        val count = uiState.messages.size + if (uiState.isGenerating) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (onClose != null || onClearHistory != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Book Assistant",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                if (onClearHistory != null && uiState.messages.isNotEmpty()) {
                    IconButton(onClick = onClearHistory) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Очистить историю")
                    }
                }
                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close assistant")
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnswerMode.entries.forEach { mode ->
                FilterChip(
                    selected = uiState.mode == mode,
                    onClick = { onModeChange(mode) },
                    label = { Text(mode.raw.replaceFirstChar { it.uppercase() }) }
                )
            }
            FilterChip(
                selected = uiState.webEnabled,
                onClick = { onWebToggle(!uiState.webEnabled) },
                label = { Text("Web") },
                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) }
            )
        }

        if (onQuickAction != null) {
            SelectionAndQuickActions(
                selectedText = selectedText,
                onQuickAction = onQuickAction,
                onClearSelection = onClearSelection
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.messages) { message -> MessageBubble(message) }
            if (uiState.isGenerating) {
                item { StreamingBubble(uiState.streamingText) }
            }
        }

        MessageInput(
            value = uiState.inputText,
            onValueChange = onInputChange,
            onSend = onSend,
            enabled = !uiState.isGenerating
        )
    }
}

/**
 * Shows the passage the reader highlighted (if any) plus one-tap prompts. When
 * text is selected the actions operate on it ("Explain"/"Translate"/"Summarize");
 * otherwise they offer general reading-companion prompts.
 */
@Composable
private fun SelectionAndQuickActions(
    selectedText: String?,
    onQuickAction: (String) -> Unit,
    onClearSelection: (() -> Unit)?
) {
    val hasSelection = !selectedText.isNullOrBlank()
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        if (hasSelection) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "“${selectedText!!.trim().take(120)}”",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(vertical = 8.dp)
                    )
                    if (onClearSelection != null) {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    }
                }
            }
        }

        val actions: List<Pair<String, String>> = if (hasSelection) {
            val t = selectedText!!.trim().take(400)
            listOf(
                "Explain" to "Explain this passage: \"$t\"",
                "Translate" to "Translate this passage to Russian: \"$t\"",
                "Summarize" to "Summarize this passage briefly: \"$t\"",
                "Synonyms" to "Give synonyms for key words in: \"$t\""
            )
        } else {
            listOf(
                "Who is this character?" to "Who is the character most relevant to where I am now? Answer without spoilers.",
                "Chapter pace" to "How is the pacing of the current chapter so far?",
                "Fact-check" to "Fact-check the claims in the current chapter against what I've read.",
                "Recap" to "Give me a spoiler-free recap of the story so far."
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            actions.forEach { (label, prompt) ->
                if (hasSelection) {
                    AssistChip(onClick = { onQuickAction(prompt) }, label = { Text(label) })
                } else {
                    SuggestionChip(onClick = { onQuickAction(prompt) }, label = { Text(label) })
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
internal fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.fromUser) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (message.fromUser) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    }
    val shape = if (message.fromUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Card(colors = color, shape = shape, modifier = Modifier.widthIn(max = 320.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.spoilerFlagged) {
                    Text(
                        "⚠️ Rewritten to avoid spoilers",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(message.text)
            }
        }
    }
}

@Composable
internal fun StreamingBubble(text: String) {
    val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Card(
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            if (text.isEmpty()) {
                ThinkingIndicator(modifier = Modifier.padding(12.dp))
            } else {
                Text(text, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
private fun ThinkingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "thinking")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "dots"
    )
    val dots = ".".repeat(progress.toInt() + 1)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("Analyzing$dots", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Ask about the story so far…") },
            enabled = enabled
        )
        IconButton(onClick = onSend, enabled = enabled && value.isNotBlank()) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }
}
