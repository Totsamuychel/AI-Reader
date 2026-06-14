package com.bookmind.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookmind.core.model.Book
import com.bookmind.core.model.Shelf
import com.bookmind.ui.components.BookCoverCard
import com.bookmind.ui.components.BookShelf3D
import com.bookmind.ui.components.ShelfBook

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenBook: (Book) -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    var contextBook by remember { mutableStateOf<Book?>(null) }
    var renamingBook by remember { mutableStateOf<Book?>(null) }
    var showCreateShelf by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::importAndIngest) }

    // Cover image picker; persists read access so Coil can load it across restarts.
    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val book = contextBook
        if (uri != null && book != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.updateCover(book.id, uri.toString())
        }
        contextBook = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchOpen) {
                        OutlinedTextField(
                            value = uiState.query,
                            onValueChange = viewModel::onQueryChange,
                            placeholder = { Text("Search title or author") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("BookMind")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        searchOpen = !searchOpen
                        if (!searchOpen) viewModel.onQueryChange("")
                    }) { Icon(Icons.Default.Search, contentDescription = "Search") }
                    ViewModeButton(uiState.viewMode, viewModel::setViewMode)
                    IconButton(onClick = { sortOpen = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Default.MenuBook, contentDescription = "Reading stats")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                picker.launch(
                    arrayOf(
                        "application/epub+zip",
                        "application/pdf",
                        "application/x-fictionbook+xml",
                        "text/xml",
                        "text/plain",
                        "application/octet-stream" // many .fb2 files report this
                    )
                )
            }) { Icon(Icons.Default.Add, contentDescription = "Add book") }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isImporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            uiState.importStatus?.takeIf { uiState.isImporting }?.let {
                Text(it, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }

            ShelfChips(
                shelves = uiState.shelves,
                selectedShelfId = uiState.selectedShelfId,
                onSelect = viewModel::selectShelf,
                onCreate = { showCreateShelf = true }
            )

            val books = uiState.visibleBooks
            if (books.isEmpty()) {
                EmptyLibrary(hasQuery = uiState.query.isNotBlank())
            } else {
                Crossfade(targetState = uiState.viewMode, label = "libraryViewMode") { mode ->
                    when (mode) {
                        LibraryViewMode.SHELF -> ShelfView(books, onOpenBook) { contextBook = it }
                        LibraryViewMode.GRID -> GridView(books, onOpenBook) { contextBook = it }
                        LibraryViewMode.LIST -> ListView(books, onOpenBook) { contextBook = it }
                    }
                }
            }
        }
    }

    contextBook?.let { book ->
        BookContextSheet(
            book = book,
            shelves = uiState.shelves,
            onDismiss = { contextBook = null },
            onMoveToShelf = { shelfId ->
                viewModel.moveBookToShelf(book.id, shelfId)
                contextBook = null
            },
            onChangeCover = { coverPicker.launch(arrayOf("image/*")) },
            onPickPreset = { index ->
                viewModel.updateCover(book.id, com.bookmind.ui.components.CoverPresets.id(index))
            },
            onRemoveCover = {
                viewModel.updateCover(book.id, null)
                contextBook = null
            },
            onRename = {
                renamingBook = book
                contextBook = null
            },
            onDelete = {
                viewModel.deleteBook(book.id)
                contextBook = null
            }
        )
    }

    renamingBook?.let { book ->
        RenameBookDialog(
            initial = book.title,
            onDismiss = { renamingBook = null },
            onConfirm = { newTitle ->
                viewModel.renameBook(book.id, newTitle)
                renamingBook = null
            }
        )
    }

    if (showCreateShelf) {
        CreateShelfDialog(
            onDismiss = { showCreateShelf = false },
            onCreate = { name, color ->
                viewModel.createShelf(name, color)
                showCreateShelf = false
            }
        )
    }

    if (sortOpen) {
        ModalBottomSheet(onDismissRequest = { sortOpen = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Sort by", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                BookSort.entries.forEach { option ->
                    Text(
                        option.displayName,
                        fontWeight = if (option == uiState.sort) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setSort(option)
                                sortOpen = false
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewModeButton(mode: LibraryViewMode, onChange: (LibraryViewMode) -> Unit) {
    val (icon, next, desc) = when (mode) {
        LibraryViewMode.SHELF -> Triple(Icons.Default.GridView, LibraryViewMode.GRID, "Grid view")
        LibraryViewMode.GRID -> Triple(Icons.AutoMirrored.Filled.ViewList, LibraryViewMode.LIST, "List view")
        LibraryViewMode.LIST -> Triple(Icons.Default.MenuBook, LibraryViewMode.SHELF, "Shelf view")
    }
    IconButton(onClick = { onChange(next) }) { Icon(icon, contentDescription = desc) }
}

@Composable
private fun ShelfView(books: List<ShelfBook>, onOpenBook: (Book) -> Unit, onLongPress: (Book) -> Unit) {
    val reading = books.filter { it.progress in 0.01f..0.98f }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)
    ) {
        item { HeroHeader(bookCount = books.size, readingCount = reading.size) }
        if (reading.isNotEmpty()) {
            item { ShelfBlock("Continue reading", reading, onOpenBook, onLongPress) }
        }
        item {
            ShelfBlock(
                if (reading.isEmpty()) "Your books" else "All books",
                books, onOpenBook, onLongPress
            )
        }
    }
}

@Composable
private fun ShelfBlock(
    title: String,
    books: List<ShelfBook>,
    onOpenBook: (Book) -> Unit,
    onLongPress: (Book) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        BookShelf3D(books = books, onOpenBook = onOpenBook, onLongPress = onLongPress)
    }
}

@Composable
private fun HeroHeader(bookCount: Int, readingCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)
        ) {
            Text(
                "Your library",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                "$bookCount books · $readingCount in progress",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun GridView(books: List<ShelfBook>, onOpenBook: (Book) -> Unit, onLongPress: (Book) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(books, key = { it.book.id.raw }) { sb ->
            BookCoverCard(
                book = sb.book,
                progress = sb.progress,
                onTap = { onOpenBook(sb.book) },
                onLongPress = { onLongPress(sb.book) }
            )
        }
    }
}

@Composable
private fun ListView(books: List<ShelfBook>, onOpenBook: (Book) -> Unit, onLongPress: (Book) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(books, key = { it.book.id.raw }) { sb ->
            BookRow(sb, onClick = { onOpenBook(sb.book) }, onLongPress = { onLongPress(sb.book) })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookRow(shelfBook: ShelfBook, onClick: () -> Unit, onLongPress: () -> Unit) {
    val book = shelfBook.book
    Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongPress)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCoverCard(book = book, progress = shelfBook.progress, width = 44.dp)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    book.author ?: book.format.raw.uppercase(),
                    style = MaterialTheme.typography.bodySmall
                )
                if (shelfBook.progress > 0f) {
                    LinearProgressIndicator(
                        progress = { shelfBook.progress },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShelfChips(
    shelves: List<Shelf>,
    selectedShelfId: String?,
    onSelect: (String?) -> Unit,
    onCreate: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedShelfId == null,
                onClick = { onSelect(null) },
                label = { Text("All") }
            )
        }
        items(shelves, key = { it.id }) { shelf ->
            FilterChip(
                selected = selectedShelfId == shelf.id,
                onClick = { onSelect(shelf.id) },
                label = { Text(shelf.name) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(shelf.colorRgb))
                    )
                }
            )
        }
        item {
            AssistChip(
                onClick = onCreate,
                label = { Text("New shelf") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookContextSheet(
    book: Book,
    shelves: List<Shelf>,
    onDismiss: () -> Unit,
    onMoveToShelf: (String?) -> Unit,
    onChangeCover: () -> Unit,
    onPickPreset: (Int) -> Unit,
    onRemoveCover: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "${book.format.raw.uppercase()}${book.author?.let { " · $it" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRename)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Переименовать")
            }

            Spacer(Modifier.height(4.dp))
            Text("Обложка — пресеты", style = MaterialTheme.typography.labelLarge)
            CoverPresetRow(book = book, onPickPreset = onPickPreset)

            Spacer(Modifier.height(12.dp))
            Text("Move to shelf", style = MaterialTheme.typography.labelLarge)
            ContextRow(label = "None (unfiled)", selected = book.shelfId == null) { onMoveToShelf(null) }
            shelves.forEach { shelf ->
                ContextRow(
                    label = shelf.name,
                    selected = book.shelfId == shelf.id,
                    color = Color(shelf.colorRgb)
                ) { onMoveToShelf(shelf.id) }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onChangeCover)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Своё изображение обложки")
            }
            if (book.coverUri != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRemoveCover)
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.HideImage, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Убрать обложку")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDelete)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.size(8.dp))
                Text("Delete book", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ContextRow(label: String, selected: Boolean, color: Color? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (color != null) {
            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(color))
        } else {
            Spacer(Modifier.size(14.dp))
        }
        Text(
            label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (selected) Icon(Icons.Default.Check, contentDescription = "Current shelf")
    }
}

@Composable
private fun CoverPresetRow(book: Book, onPickPreset: (Int) -> Unit) {
    val selectedIndex = book.coverUri
        ?.takeIf { it.startsWith(com.bookmind.ui.components.CoverPresets.PREFIX) }
        ?.removePrefix(com.bookmind.ui.components.CoverPresets.PREFIX)
        ?.toIntOrNull()
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(com.bookmind.ui.components.CoverPresets.palettes) { index, palette ->
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 56.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(palette.first, palette.second)
                        )
                    )
                    .then(
                        if (index == selectedIndex)
                            Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                        else Modifier
                    )
                    .clickable { onPickPreset(index) }
            )
        }
    }
}

@Composable
private fun RenameBookDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Переименовать книгу") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun CreateShelfDialog(onDismiss: () -> Unit, onCreate: (String, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(shelfColors.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New shelf") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Shelf name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("Color", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    shelfColors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .then(
                                    if (c == color)
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { color = c }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name, color) }, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private val shelfColors = listOf(
    0xFF5C6BC0, 0xFF26A69A, 0xFFEF5350, 0xFFAB47BC, 0xFFFFA726, 0xFF66BB6A
)

@Composable
private fun EmptyLibrary(hasQuery: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (hasQuery) "No books match your search" else "Your library is empty",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (hasQuery) "Try a different title or author."
                else "Tap + to import your first EPUB or TXT.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
