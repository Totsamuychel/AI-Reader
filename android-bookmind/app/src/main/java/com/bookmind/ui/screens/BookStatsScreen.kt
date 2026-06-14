package com.bookmind.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookmind.ui.stats.BookStatsViewModel
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookStatsScreen(
    bookId: String,
    onBack: () -> Unit,
    viewModel: BookStatsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(bookId) { viewModel.load(bookId) }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Статистика", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                state.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )

            ProgressRing(progress = state.progressFraction)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCell("Сессий", state.sessions.toString(), Modifier.weight(1f))
                StatCell("Минут", state.totalMinutes.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCell("Слов прочитано", state.wordsRead.toString(), Modifier.weight(1f))
                StatCell("Слов/мин", state.wordsPerMinute.toString(), Modifier.weight(1f))
            }
            StatCell(
                "Последнее чтение",
                state.lastReadAt?.let {
                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it))
                } ?: "—",
                Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ProgressRing(progress: Float) {
    val ringColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2f
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke.width, size.height - stroke.width)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
        }
        Text(
            "${(progress * 100).roundToInt()}%",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
