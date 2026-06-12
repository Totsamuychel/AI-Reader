package com.bookmind.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bookmind.ui.assistant.AssistantScreen
import com.bookmind.ui.library.LibraryScreen
import com.bookmind.ui.reader.ReaderScreen

object Routes {
    const val LIBRARY = "library"
    const val READER = "reader/{bookId}"
    const val ASSISTANT = "assistant/{bookId}/{chapterIndex}"

    fun reader(bookId: String) = "reader/$bookId"
    fun assistant(bookId: String, chapterIndex: Int) = "assistant/$bookId/$chapterIndex"
}

@Composable
fun BookMindNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LIBRARY) {
        composable(Routes.LIBRARY) {
            LibraryScreen(onOpenBook = { book ->
                navController.navigate(Routes.reader(book.id.raw))
            })
        }
        composable(
            Routes.READER,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { entry ->
            val bookId = entry.arguments?.getString("bookId").orEmpty()
            ReaderScreen(
                bookId = bookId,
                onOpenAssistant = { id, chapterIndex ->
                    navController.navigate(Routes.assistant(id, chapterIndex))
                }
            )
        }
        composable(
            Routes.ASSISTANT,
            arguments = listOf(
                navArgument("bookId") { type = NavType.StringType },
                navArgument("chapterIndex") { type = NavType.IntType }
            )
        ) { entry ->
            AssistantScreen(
                bookId = entry.arguments?.getString("bookId").orEmpty(),
                chapterIndex = entry.arguments?.getInt("chapterIndex") ?: 0
            )
        }
    }
}
