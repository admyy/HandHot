package com.handhot.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.handhot.app.ui.detail.WebViewScreen
import com.handhot.app.ui.main.MainScreen
import com.handhot.app.ui.main.MainViewModel
import com.handhot.app.ui.settings.SettingsScreen
import com.handhot.app.ui.settings.SettingsViewModel
import com.handhot.app.ui.source.AddSourceScreen
import com.handhot.app.ui.source.SourceViewModel

object Routes {
    const val MAIN = "main"
    const val ADD_SOURCE = "add_source"
    const val EDIT_SOURCE = "edit_source/{sourceId}"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{itemId}"
    const val ABOUT = "about"

    fun editSource(id: Long) = "edit_source/$id"
    fun detail(id: Long) = "detail/$id"
}

@Composable
fun HandHotNavHost(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel
) {
    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(
                viewModel = mainViewModel,
                onAddSource = { navController.navigate(Routes.ADD_SOURCE) },
                onEditSource = { id -> navController.navigate(Routes.editSource(id)) },
                onOpenDetail = { id -> navController.navigate(Routes.detail(id)) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onAbout = { navController.navigate(Routes.ABOUT) },
                onRefreshSource = { source -> mainViewModel.refreshSource(source) }
            )
        }

        composable(Routes.ADD_SOURCE) {
            AddSourceScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EDIT_SOURCE,
            arguments = listOf(navArgument("sourceId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sourceId = backStackEntry.arguments?.getLong("sourceId") ?: return@composable
            AddSourceScreen(
                sourceId = sourceId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@composable
            WebViewScreen(
                itemId = itemId,
                mainViewModel = mainViewModel,
                settingsViewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
