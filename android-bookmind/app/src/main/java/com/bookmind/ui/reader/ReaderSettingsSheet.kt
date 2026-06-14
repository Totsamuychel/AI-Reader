package com.bookmind.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bookmind.settings.AppSettings
import com.bookmind.settings.PageAnimation
import com.bookmind.settings.ReaderBackground
import com.bookmind.settings.ScrollDirection
import com.bookmind.ui.theme.ReaderFont
import kotlin.math.roundToInt

/**
 * Reader-local appearance controls, shown as a bottom sheet from the reader's
 * overflow menu: font family + size, page background preset, page-turn animation,
 * scroll direction, and the warm "night" tint. Everything persists immediately via
 * the supplied setters (DataStore-backed).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReaderSettingsSheet(
    settings: AppSettings,
    onFontChange: (ReaderFont) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onBackgroundChange: (ReaderBackground) -> Unit,
    onAnimationChange: (PageAnimation) -> Unit,
    onScrollDirectionChange: (ScrollDirection) -> Unit,
    onWarmthChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        SectionTitle("Шрифт")
        ReaderFont.entries.forEach { font ->
            RadioRow(
                label = font.displayName,
                selected = settings.readerFont == font,
                onClick = { onFontChange(font) }
            )
        }
        LabeledSlider(
            label = "Размер: ${settings.fontSizeSp.roundToInt()} sp",
            value = settings.fontSizeSp,
            valueRange = 12f..28f,
            steps = 15,
            onValueChange = onFontSizeChange
        )
        LabeledSlider(
            label = "Межстрочный: ${"%.1f".format(settings.lineSpacing)}×",
            value = settings.lineSpacing,
            valueRange = 1.0f..2.2f,
            steps = 11,
            onValueChange = onLineSpacingChange
        )

        Divider()
        SectionTitle("Фон страницы")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReaderBackground.entries.forEach { bg ->
                BackgroundChip(
                    background = bg,
                    selected = settings.readerBackground == bg,
                    onClick = { onBackgroundChange(bg) }
                )
            }
        }

        Divider()
        SectionTitle("Анимация перелистывания")
        PageAnimation.entries.forEach { anim ->
            RadioRow(
                label = anim.displayName,
                selected = settings.pageAnimation == anim,
                onClick = { onAnimationChange(anim) }
            )
        }
        Divider()
        SectionTitle("Режим прокрутки")
        RadioRow(
            label = "Постранично (свайп)",
            selected = settings.scrollDirection == ScrollDirection.HORIZONTAL,
            onClick = { onScrollDirectionChange(ScrollDirection.HORIZONTAL) }
        )
        RadioRow(
            label = "Непрерывная прокрутка",
            selected = settings.scrollDirection == ScrollDirection.VERTICAL,
            onClick = { onScrollDirectionChange(ScrollDirection.VERTICAL) }
        )

        Divider()
        SectionTitle("Ночной режим (теплота)")
        LabeledSlider(
            label = if (settings.warmth <= 0f) "Выключено"
            else "Теплота: ${(settings.warmth * 100).roundToInt()}%",
            value = settings.warmth,
            valueRange = 0f..1f,
            steps = 9,
            onValueChange = onWarmthChange
        )
        Text(
            "Тёплый фильтр снижает синий свет при чтении в темноте.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
    )
}

@Composable
private fun Divider() = HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackgroundChip(
    background: ReaderBackground,
    selected: Boolean,
    onClick: () -> Unit
) {
    val swatch = background.bgArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant
    val swatchFg = background.fgArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(background.displayName) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(swatch)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = swatchFg,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    )
}
