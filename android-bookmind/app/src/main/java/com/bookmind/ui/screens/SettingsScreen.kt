package com.bookmind.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookmind.settings.AiModel
import com.bookmind.settings.AnswerLanguage
import com.bookmind.settings.PageAnimation
import com.bookmind.settings.ScrollDirection
import com.bookmind.ui.components.ThemeSelector
import com.bookmind.ui.settings.SettingsViewModel
import com.bookmind.ui.theme.ReaderFont
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAccount: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val modelStatus by viewModel.modelStatus.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportBackup) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::importBackup) }

    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeSyncMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Account & subscription
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAccount)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Аккаунт и подписка",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Вход, Premium, облачная синхронизация",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }

            // 5.1 Appearance
            ExpandableSection(title = "Appearance", initiallyExpanded = true) {
                Text("Theme", style = MaterialTheme.typography.titleSmall)
                ThemeSelector(
                    selected = settings.theme,
                    onSelect = viewModel::setTheme,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                SwitchRow("Dynamic color (Material You)", settings.dynamicColor, viewModel::setDynamicColor)
                SwitchRow("E-Ink mode (no animations)", settings.eInkMode, viewModel::setEInk)

                Text("Reading font", style = MaterialTheme.typography.titleSmall)
                FlowChips {
                    ReaderFont.entries.forEach { font ->
                        FilterChip(
                            selected = settings.readerFont == font,
                            onClick = { viewModel.setFont(font) },
                            label = { Text(font.displayName) }
                        )
                    }
                }

                SliderRow(
                    label = "Font size",
                    value = settings.fontSizeSp,
                    valueRange = 10f..36f,
                    valueLabel = "${settings.fontSizeSp.roundToInt()} sp",
                    onChange = viewModel::setFontSize
                )
                SliderRow(
                    label = "Line spacing",
                    value = settings.lineSpacing,
                    valueRange = 1.0f..3.0f,
                    valueLabel = String.format("%.1f", settings.lineSpacing),
                    onChange = viewModel::setLineSpacing
                )
            }

            // 5.2 Reading
            ExpandableSection(title = "Reading") {
                Text("Page animation", style = MaterialTheme.typography.titleSmall)
                Column(modifier = Modifier.selectableGroup()) {
                    PageAnimation.entries.forEach { anim ->
                        RadioRow(
                            label = anim.displayName,
                            selected = settings.pageAnimation == anim,
                            onSelect = { viewModel.setPageAnimation(anim) }
                        )
                    }
                }
                Text("Scroll direction", style = MaterialTheme.typography.titleSmall)
                Column(modifier = Modifier.selectableGroup()) {
                    ScrollDirection.entries.forEach { dir ->
                        RadioRow(
                            label = dir.name.lowercase().replaceFirstChar { it.uppercase() },
                            selected = settings.scrollDirection == dir,
                            onSelect = { viewModel.setScrollDirection(dir) }
                        )
                    }
                }
            }

            // 5.3 AI Assistant
            ExpandableSection(title = "AI Assistant") {
                Text("Model", style = MaterialTheme.typography.titleSmall)
                FlowChips {
                    AiModel.entries.forEach { model ->
                        FilterChip(
                            selected = settings.aiModel == model,
                            onClick = { viewModel.setAiModel(model) },
                            label = { Text(model.displayName) }
                        )
                    }
                }

                if (settings.aiModel.needsApiKey) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = viewModel::setApiKey,
                        label = { Text("${settings.aiModel.displayName} API key") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            if (apiKey.isNotBlank()) {
                                Icon(Icons.Default.Check, contentDescription = "Key stored", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Stored encrypted on this device (EncryptedSharedPreferences).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (settings.aiModel.needsUrl) {
                    OutlinedTextField(
                        value = settings.ollamaUrl,
                        onValueChange = viewModel::setOllamaUrl,
                        label = { Text("Ollama server URL") },
                        placeholder = { Text("http://10.0.2.2:11434") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Answer language", style = MaterialTheme.typography.titleSmall)
                FlowChips {
                    AnswerLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = settings.answerLanguage == lang,
                            onClick = { viewModel.setAnswerLanguage(lang) },
                            label = { Text(lang.displayName) }
                        )
                    }
                }

                SwitchRow(
                    "Auto-analyze selected text",
                    settings.autoAnalyzeSelection,
                    viewModel::setAutoAnalyze
                )
                SwitchRow("Keep chat history", settings.keepChatHistory, viewModel::setKeepHistory)
                Text(
                    "Today the on-device Gemma model answers spoiler-free based on how far " +
                        "you've read. Other providers store credentials here for upcoming routing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OnDeviceModelRow(
                    status = modelStatus,
                    onDownload = viewModel::downloadModel
                )
            }

            // 5.4 Library / backup
            ExpandableSection(title = "Library & backup") {
                Text(
                    "Export your reading progress and highlights to a JSON file, or restore them on another device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { exportLauncher.launch("bookmind-backup.json") },
                        modifier = Modifier.weight(1f)
                    ) { Text("Export") }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Import") }
                }
            }
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) { content() }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) { content() }
}

@Composable
private fun OnDeviceModelRow(
    status: com.bookmind.ui.settings.ModelStatus,
    onDownload: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("On-device model (Gemma)", style = MaterialTheme.typography.titleSmall)
                Text(
                    when {
                        status.present -> "Установлена · работает офлайн"
                        status.downloading -> status.message ?: "Загрузка…"
                        status.message != null -> status.message!!
                        else -> "Не загружена · ~1.5 ГБ"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (status.present) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            } else {
                Button(onClick = onDownload, enabled = !status.downloading) { Text("Загрузить") }
            }
        }
        if (status.downloading) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { status.fraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row {
            Text(label, modifier = Modifier.weight(1f))
            Text(valueLabel, style = MaterialTheme.typography.labelLarge)
        }
        Slider(value = value, onValueChange = onChange, valueRange = valueRange)
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label)
    }
}
