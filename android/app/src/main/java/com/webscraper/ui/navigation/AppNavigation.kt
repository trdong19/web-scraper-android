package com.webscraper.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.webscraper.ui.screens.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Tasks : Screen("tasks", "任务", Icons.Default.List)
    data object Stats : Screen("stats", "统计", Icons.Default.BarChart)
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

val bottomNavItems = listOf(Screen.Tasks, Screen.Stats, Screen.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    // Show bottom bar only on main screens
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Tasks.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Tasks.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Tasks.route) {
                TaskListScreen(
                    onTaskClick = { taskId -> navController.navigate("task/$taskId") },
                    onCreateTask = { navController.navigate("create_task") }
                )
            }

            composable(Screen.Stats.route) {
                DataStatsScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable("create_task") {
                CreateTaskScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSelector = { url, name ->
                        navController.navigate("selector/${java.net.URLEncoder.encode(url, "UTF-8")}/${java.net.URLEncoder.encode(name, "UTF-8")}")
                    }
                )
            }

            composable(
                route = "task/{taskId}",
                arguments = listOf(navArgument("taskId") { type = NavType.StringType })
            ) { entry ->
                val taskId = entry.arguments?.getString("taskId") ?: return@composable
                TaskDetailScreen(
                    taskId = taskId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "selector/{url}/{name}",
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType }
                )
            ) { entry ->
                val url = java.net.URLDecoder.decode(entry.arguments?.getString("url") ?: "", "UTF-8")
                val name = java.net.URLDecoder.decode(entry.arguments?.getString("name") ?: "", "UTF-8")
                VisualSelectorScreen(
                    url = url,
                    taskName = name,
                    onBack = { navController.popBackStack() },
                    onRulesCreated = {
                        navController.popBackStack()
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
