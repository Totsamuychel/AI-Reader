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
 * Built-in cover styles. A cover is one of:
 *  - a user image      → `book.coverUri` is a content:// URI
 *  - a chosen preset   → `book.coverUri` is "preset:N" (gradient palette index)
 *  - nothing set       → a deterministic gradient derived from the title
 */
object CoverPresets {
    const val PREFIX = "preset:"

    /** Gradient palettes offered as cover presets (top → bottom stop). */
    val palettes: List<Pair<Color, Color>> = listOf(
        Color(0xFF5C6BC0) to Color(0xFF303F9F),
        Color(0xFF26A69A) to Color(0xFF00695C),
        Color(0xFFEF5350) to Color(0xFFC62828),
        Color(0xFFAB47BC) to Color(0xFF6A1B9A),
        Color(0xFFFFA726) to Color(0xFFEF6C00),
        Color(0xFF66BB6A) to Color(0xFF2E7D32),
        Color(0xFF42A5F5) to Color(0xFF1565C0),
        Color(0xFF8D6E63) to Color(0xFF4E342E),
        Color(0xFF26C6DA) to Color(0xFF00838F),
        Color(0xFFEC407A) to Color(0xFFAD1457),
        Color(0xFF7E57C2) to Color(0xFF4527A0),
        Color(0xFF455A64) to Color(0xFF263238)
    )

    fun id(index: Int): String = "$PREFIX$index"

    /** True when the cover is a real image rather than a preset/generated one. */
    fun isImage(coverUri: String?): Boolean =
        coverUri != null && !coverUri.startsWith(PREFIX)

    /** The gradient palette to paint for [book] (preset choice, else title hash). */
    fun paletteForBook(book: Book): Pair<Color, Color> {
        val uri = book.coverUri
        val presetIndex = if (uri != null && uri.startsWith(PREFIX)) {
            uri.removePrefix(PREFIX).toIntOrNull()
        } else null
        val index = (presetIndex ?: book.title.hashCode().absoluteValue) % palettes.size
        return palettes[index]
    }
}

/**
 * A book cover: a user image, a chosen preset gradient, or a title-derived
 * gradient — with a darker spine, glossy highlight, and reading-progress bar.
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
    val (top, bottom) = CoverPresets.paletteForBook(book)
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
        // User-picked cover image, if any, fills the card; the gradient below only
        // shows through while it loads.
        if (CoverPresets.isImage(book.coverUri)) {
            coil.compose.AsyncImage(
                model = book.coverUri,
                contentDescription = book.title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            ProgressBar(progress, Modifier.align(Alignment.BottomCenter))
            return@Box
        }
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
        ProgressBar(progress, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ProgressBar(progress: Float, modifier: Modifier = Modifier) {
    if (progress <= 0f) return
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp),
        color = Color.White,
        trackColor = Color.White.copy(alpha = 0.3f)
    )
}

private fun Color.darken(amount: Float): Color =
    Color(
        red = (red * (1 - amount)).coerceIn(0f, 1f),
        green = (green * (1 - amount)).coerceIn(0f, 1f),
        blue = (blue * (1 - amount)).coerceIn(0f, 1f),
        alpha = alpha
    )
