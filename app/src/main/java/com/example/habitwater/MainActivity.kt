package com.example.habitwater

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.habitwater.ui.ArchivedHabitActions
import com.example.habitwater.ui.ArchivedHabitsScreen
import com.example.habitwater.ui.CalendarScreen
import com.example.habitwater.ui.HomeScreen
import com.example.habitwater.ui.MainViewModel
import com.example.habitwater.ui.SettingsScreen
import com.example.habitwater.ui.theme.HabitWaterTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            val lifecycleOwner = LocalLifecycleOwner.current
            LaunchedEffect(lifecycleOwner) {
                lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.onAppForeground()
                    while (true) {
                        delay(30_000)
                        viewModel.refreshToday()
                    }
                }
            }
            HabitWaterTheme(state.settings.themeMode, state.settings.hue, this) {
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            state = state,
                            viewModel = viewModel,
                            onCalendar = { navController.navigate("calendar") },
                            onSettings = { navController.navigate("settings") },
                        )
                    }
                    composable("calendar") {
                        CalendarScreen(
                            state = state,
                            onHome = { navController.popBackStack("home", inclusive = false) },
                            onSettings = { navController.navigate("settings") },
                            onToggleCompletion = viewModel::toggleHabit,
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            state = state,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onArchivedHabits = { navController.navigate("archived") },
                        )
                    }
                    composable("archived") {
                        ArchivedHabitsScreen(
                            state = state,
                            onBack = { navController.popBackStack() },
                            onOpenHabit = { navController.navigate("archived/$it") },
                        )
                    }
                    composable(
                        route = "archived/{habitId}",
                        arguments = listOf(navArgument("habitId") { type = NavType.LongType }),
                    ) { entry ->
                        val habitId = entry.arguments?.getLong("habitId") ?: return@composable
                        val habit = state.archivedHabits.firstOrNull { it.id == habitId }
                        if (habit != null) {
                            CalendarScreen(
                                state = state,
                                onHome = null,
                                onSettings = null,
                                onToggleCompletion = { _, _ -> },
                                habits = listOf(habit),
                                initialHabitId = habitId,
                                readOnly = true,
                                title = "封存紀錄",
                                onBack = { navController.popBackStack() },
                                footer = {
                                    ArchivedHabitActions(
                                        onRestore = {
                                            viewModel.restoreHabit(habit)
                                            navController.popBackStack("archived", inclusive = false)
                                        },
                                        onDelete = {
                                            viewModel.deleteArchivedHabit(habit)
                                            navController.popBackStack("archived", inclusive = false)
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
