package com.bookmind.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import com.bookmind.core.model.Book
import kotlin.math.absoluteValue

/**
 * A generated book cover. Since EPUB/TXT imports don't carry artwork, the cover
 * is a deterministic gradient derived from the title (so a given book always
 * looks the same), with a darker spine on the left and a reading-progress bar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCoverCard(
    book: Book,
    progress: Float,
    modifier: Modifier = Modifier,
    width: Dp = 120.dp,
    onTap: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    val (top, bottom) = coverColors(book.title)
    val height = width * 1.5f

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shadow(8.dp, RoundedCornerShape(10.dp), clip = false)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(top, bottom)))
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
    ) {
        // Spine
        Box(
            modifier = Modifier
                .fillMaxSize()
                .width(10.dp)
                .background(bottom.darken(0.3f))
        )
        // Diagonal gloss highlight for a glossy-cover feel.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        0f to Color.White.copy(alpha = 0.18f),
                        0.35f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.12f)
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, top = 14.dp, end = 10.dp, bottom = 10.dp)
        ) {
            Text(
                book.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            book.author?.let {
                Text(
                    it,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Box(modifier = Modifier.weight(1f))
            Text(
                book.format.raw.uppercase(),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
        if (progress > 0f) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

/** Two palette stops chosen deterministically from the title. */
private fun coverColors(title: String): Pair<Color, Color> {
    val palettes = listOf(
        Color(0xFF5C6BC0) to Color(0xFF303F9F),
        Color(0xFF26A69A) to Color(0xFF00695C),
        Color(0xFFEF5350) to Color(0xFFC62828),
        Color(0xFFAB47BC) to Color(0xFF6A1B9A),
        Color(0xFFFFA726) to Color(0xFFEF6C00),
        Color(0xFF66BB6A) to Color(0xFF2E7D32),
        Color(0xFF42A5F5) to Color(0xFF1565C0),
        Color(0xFF8D6E63) to Color(0xFF4E342E)
    )
    val idx = (title.hashCode().absoluteValue) % palettes.size
    return palettes[idx]
}

private fun Color.darken(amount: Float): Color =
    Color(
        red = (red * (1 - amount)).coerceIn(0f, 1f),
        green = (green * (1 - amount)).coerceIn(0f, 1f),
        blue = (blue * (1 - amount)).coerceIn(0f, 1f),
        alpha = alpha
    )
