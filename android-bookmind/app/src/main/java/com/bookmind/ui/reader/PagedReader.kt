package com.bookmind.ui.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookmind.settings.AppSettings
import com.bookmind.settings.PageAnimation
import com.bookmind.settings.ScrollDirection
import kotlin.math.abs

/**
 * Paginated chapter reader. Splits the chapter into screen-sized pages and flips
 * between them honoring the user's [PageAnimation] and [ScrollDirection]. Each
 * page is a read-only [BasicTextField] so passage selection still works; flipping
 * pages clears the selection.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagedReader(
    text: String,
    settings: AppSettings,
    contentColor: Color,
    pagerState: PagerState,
    onPageCountChange: (Int) -> Unit,
    onSelectionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textStyle = TextStyle(
        fontFamily = settings.readerFont.fontFamily,
        fontSize = settings.fontSizeSp.sp,
        lineHeight = (settings.fontSizeSp * settings.lineSpacing).sp,
        color = contentColor
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Heuristic page size from the viewport and font metrics.
        val widthSp = with(density) { (maxWidth - 40.dp).toPx() / settings.fontSizeSp.sp.toPx() }
        val heightSp = with(density) { (maxHeight - 24.dp).toPx() / settings.fontSizeSp.sp.toPx() }
        val charsPerLine = (widthSp / 0.5f).toInt().coerceAtLeast(20)
        val lines = (heightSp / settings.lineSpacing).toInt().coerceAtLeast(8)
        val charsPerPage = (charsPerLine * lines * 0.92f).toInt().coerceAtLeast(200)

        val pages = remember(text, charsPerPage) { paginate(text, charsPerPage) }

        // Keep the hoisted pager's page count in sync with the pagination.
        LaunchedEffect(pages.size) { onPageCountChange(pages.size) }

        // Clear the highlight whenever the page changes.
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { onSelectionChange("") }
        }

        val quoteColor = androidx.compose.material3.MaterialTheme.colorScheme.primary

        val pageContent: @Composable (Int) -> Unit = { index ->
            PageSurface(
                pageText = pages.getOrElse(index) { "" },
                textStyle = textStyle,
                quoteColor = quoteColor,
                modifier = Modifier
                    .pageTransform(settings.pageAnimation, pagerState, index)
                    .curlShading(settings.pageAnimation, pagerState, index)
            )
        }

        if (settings.scrollDirection == ScrollDirection.VERTICAL) {
            VerticalPager(state = pagerState) { page -> pageContent(page) }
        } else {
            HorizontalPager(state = pagerState) { page -> pageContent(page) }
        }
    }
}

@Composable
private fun PageSurface(
    pageText: String,
    textStyle: TextStyle,
    quoteColor: Color,
    modifier: Modifier = Modifier
) {
    // Plain (selectable) Text rather than a text field, so a horizontal swipe is
    // delivered to the HorizontalPager instead of being captured for selection;
    // long-press still selects/copies inside the SelectionContainer.
    val annotated = remember(pageText, quoteColor) { buildReaderAnnotatedString(pageText, quoteColor) }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        SelectionContainer {
            Text(
                text = annotated,
                style = textStyle,
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}

/** Applies the chosen page-turn transform based on the page's scroll offset. */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.pageTransform(
    animation: PageAnimation,
    pagerState: PagerState,
    page: Int
): Modifier = this.graphicsLayer {
    val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
    when (animation) {
        PageAnimation.NONE, PageAnimation.SLIDE -> Unit
        PageAnimation.FADE -> alpha = 1f - abs(offset).coerceIn(0f, 1f)
        PageAnimation.CURL -> {
            // Lightweight curl: rotate around the binding edge with perspective.
            cameraDistance = 14f * density
            transformOrigin = TransformOrigin(if (offset > 0) 0f else 1f, 0.5f)
            rotationY = offset * 30f
            alpha = 1f - (abs(offset) * 0.4f).coerceIn(0f, 1f)
        }
    }
}

/**
 * For the CURL animation, paints a soft gradient shadow sweeping across the page
 * as it turns, so the rotateY fold reads as a real page lifting off the spine.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.curlShading(
    animation: PageAnimation,
    pagerState: PagerState,
    page: Int
): Modifier {
    if (animation != PageAnimation.CURL) return this
    return this.drawWithContent {
        drawContent()
        val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        val intensity = abs(offset).coerceIn(0f, 1f)
        if (intensity <= 0.01f) return@drawWithContent
        val fromLeft = offset > 0f
        val shadow = Brush.horizontalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.0f),
                Color.Black.copy(alpha = 0.35f * intensity)
            ),
            startX = if (fromLeft) size.width else 0f,
            endX = if (fromLeft) 0f else size.width
        )
        drawRect(brush = shadow, size = size)
    }
}

/**
 * Splits [text] into pages of roughly [charsPerPage] characters, breaking on
 * paragraph or word boundaries so words aren't cut in half.
 */
internal fun paginate(text: String, charsPerPage: Int): List<String> {
    if (text.isBlank()) return listOf("")
    // Paragraphs are single-newline separated (see normalizeChapterText); tolerate
    // any run of blank lines too.
    val paragraphs = text.split(Regex("\n+"))
    val pages = mutableListOf<String>()
    val current = StringBuilder()

    fun flush() {
        if (current.isNotBlank()) pages.add(current.toString().trim())
        current.clear()
    }

    for (para in paragraphs) {
        if (current.length + para.length + 1 <= charsPerPage) {
            if (current.isNotEmpty()) current.append("\n")
            current.append(para)
        } else if (para.length <= charsPerPage) {
            flush()
            current.append(para)
        } else {
            // Paragraph longer than a page: break it on word boundaries.
            flush()
            val words = para.split(' ')
            for (w in words) {
                if (current.length + w.length + 1 > charsPerPage) flush()
                if (current.isNotEmpty()) current.append(' ')
                current.append(w)
            }
        }
    }
    flush()
    return pages.ifEmpty { listOf(text) }
}
