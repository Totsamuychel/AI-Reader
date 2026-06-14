package com.bookmind.core

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.bookmind.core.model.Book
import com.bookmind.core.model.BookFormat
import com.bookmind.core.model.BookID
import com.bookmind.core.parser.ReaderError
import com.bookmind.persistence.BookStoring
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * = iOS `BookImportService`. Copies a picked file into private storage and
 * registers a [Book] record. Parsing/ingestion happen separately.
 */
class BookImportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookStore: BookStoring
) {
    suspend fun importBook(uri: Uri): Book = withContext(Dispatchers.IO) {
        val displayName = queryDisplayName(uri) ?: "book-${System.currentTimeMillis()}"
        val format = formatFor(displayName)

        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val id = UUID.randomUUID().toString()
        val dest = File(booksDir, "$id-${displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")}")

        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: throw ReaderError.FileNotReadable(uri.toString())

        val book = Book(
            id = BookID(id),
            title = displayName.substringBeforeLast('.'),
            author = null,
            format = format,
            fileUri = Uri.fromFile(dest).toString()
        )
        bookStore.save(book)
        book
    }

    private fun formatFor(name: String): BookFormat =
        BookFormat.fromExtension(name.substringAfterLast('.', ""))

    private fun queryDisplayName(uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }
}
