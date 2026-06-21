package com.webscraper.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.webscraper.ui.screens.*
import com.webscraper.viewmodel.CreateTaskViewModel

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
    val createTaskViewModel: CreateTaskViewModel = viewModel()

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
                        createTaskViewModel.taskName = name
                        createTaskViewModel.taskUrl = url
                        navController.navigate("selector")
                    },
                    viewModel = createTaskViewModel
                )
            }

            composable("selector") {
                VisualSelectorScreen(
                    viewModel = createTaskViewModel,
                    onBack = { navController.popBackStack() },
                    onRulesCreated = {
                        navController.popBackStack()
                        navController.popBackStack()
                    }
                )
            }

            composable("task/{taskId}") { entry ->
                val taskId = entry.arguments?.getString("taskId") ?: return@composable
                TaskDetailScreen(
                    taskId = taskId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
