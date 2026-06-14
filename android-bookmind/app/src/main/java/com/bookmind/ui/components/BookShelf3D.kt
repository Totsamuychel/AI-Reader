package com.bookmind.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.bookmind.core.model.Book

/** A book paired with its reading progress, for shelf rendering. */
data class ShelfBook(val book: Book, val progress: Float)

/**
 * Horizontal 3D-ish book shelf. Covers are drawn with a slight perspective tilt
 * (via [graphicsLayer] rotationY) and sit on a wooden plank; selecting a book
 * "pulls it forward" (scale + lift). Pure Compose — no OpenGL — per the plan.
 */
@Composable
fun BookShelf3D(
    books: List<ShelfBook>,
    onOpenBook: (Book) -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (Book) -> Unit = {}
) {
    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x11000000), Color(0x33000000))
                )
            )
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            contentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            itemsIndexed(books, key = { _, it -> it.book.id.raw }) { index, shelfBook ->
                Book3DItem(
                    shelfBook = shelfBook,
                    index = index,
                    onTap = { onOpenBook(shelfBook.book) },
                    onLongPress = { onLongPress(shelfBook.book) }
                )
            }
        }
        WoodenPlank()
    }
}

@Composable
private fun Book3DItem(
    shelfBook: ShelfBook,
    index: Int,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    // Staggered "drop in" on first composition.
    val drop = remember(shelfBook.book.id.raw) { Animatable(1f) }
    LaunchedEffect(shelfBook.book.id.raw) {
        drop.animateTo(0f, animationSpec = tween(durationMillis = 350, delayMillis = index * 50))
    }

    Box(
        modifier = Modifier.graphicsLayer {
            rotationY = -14f
            // A little depth: tilted books cast their lift via translas well.
            translationY = drop.value * 120f
            alpha = 1f - drop.value
            transformOrigin = TransformOrigin(0f, 1f)
            cameraDistance = 12f * density
        }
    ) {
        BookCoverCard(
            book = shelfBook.book,
            progress = shelfBook.progress,
            onTap = onTap,
            onLongPress = onLongPress
        )
    }
}

@Composable
private fun WoodenPlank() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        // Top edge catches the light.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color(0xFFB08968))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF8D6E63), Color(0xFF5D4037), Color(0xFF3E2723))
                    )
                )
        )
    }
}
