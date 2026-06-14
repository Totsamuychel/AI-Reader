package com.bookmind.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bookmind.ui.assistant.AssistantScreen
import com.bookmind.ui.auth.AuthScreen
import com.bookmind.ui.library.LibraryScreen
import com.bookmind.ui.reader.ReaderScreen
import com.bookmind.ui.screens.BookStatsScreen
import com.bookmind.ui.screens.OnboardingScreen
import com.bookmind.ui.screens.SettingsScreen
import com.bookmind.ui.screens.StatsScreen
import com.bookmind.ui.settings.SettingsViewModel

object Routes {
    const val ONBOARDING = "onboarding"
    const val LIBRARY = "library"
    const val READER = "reader/{bookId}"
    const val ASSISTANT = "assistant/{bookId}/{chapterIndex}"
    const val SETTINGS = "settings"
    const val ACCOUNT = "account"
    const val STATS = "stats"
    const val STATS_BOOK = "stats/{bookId}"

    fun reader(bookId: String) = "reader/$bookId"
    fun assistant(bookId: String, chapterIndex: Int) = "assistant/$bookId/$chapterIndex"
    fun stats(bookId: String) = "stats/$bookId"
}

@Composable
fun BookMindNavHost(settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    val start = if (settings.onboardingComplete) Routes.LIBRARY else Routes.ONBOARDING

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinish = {
                settingsViewModel.setOnboardingComplete(true)
                navController.navigate(Routes.LIBRARY) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onOpenBook = { book -> navController.navigate(Routes.reader(book.id.raw)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenStats = { navController.navigate(Routes.STATS) }
            )
        }
        composable(
            Routes.READER,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { entry ->
            val bookId = entry.arguments?.getString("bookId").orEmpty()
            // The assistant now lives in a side panel inside the reader;
            // the standalone route below remains for direct navigation.
            ReaderScreen(
                bookId = bookId,
                onBack = { navController.popBackStack() },
                onOpenStats = { navController.navigate(Routes.stats(bookId)) }
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
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAccount = { navController.navigate(Routes.ACCOUNT) }
            )
        }
        composable(Routes.ACCOUNT) {
            AuthScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.STATS) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.STATS_BOOK,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { entry ->
            BookStatsScreen(
                bookId = entry.arguments?.getString("bookId").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
