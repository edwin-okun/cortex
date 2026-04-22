package com.cortex.app.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cortex.app.core.ui.theme.CortexColors
import com.cortex.app.feature.home.HomeScreen
import com.cortex.app.feature.lesson.LessonScreen
import com.cortex.app.feature.library.LibraryScreen
import com.cortex.app.feature.progress.ProgressScreen
import com.cortex.app.feature.review.ReviewScreen
import com.cortex.app.feature.session.SessionScreen
import com.cortex.app.feature.session.SessionViewModel
import org.koin.androidx.compose.koinViewModel

private data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: CortexRoute,
)

private val navItems = listOf(
    NavItem("Home", Icons.Filled.Home, CortexRoute.Home),
    NavItem("Review", Icons.Filled.Repeat, CortexRoute.Review),
    NavItem("Library", Icons.Filled.MenuBook, CortexRoute.Library),
    NavItem("Progress", Icons.Filled.BarChart, CortexRoute.Progress),
)

private fun NavHostController.navigateToTab(route: CortexRoute) {
    navigate(route) {
        popUpTo(CortexRoute.Home) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun CortexNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val isOnLesson = currentDest?.hasRoute<CortexRoute.Lesson>() == true
    val isOnSession = currentDest?.hasRoute<CortexRoute.Session>() == true

    Scaffold(
        bottomBar = {
            if (!isOnLesson && !isOnSession) {
                NavigationBar(
                    containerColor = CortexColors.PaperRaised,
                ) {
                    navItems.forEach { item ->
                        val selected = currentDest?.hasRoute(item.route::class) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CortexColors.Paper,
                                indicatorColor = CortexColors.Accent,
                                unselectedIconColor = CortexColors.Muted,
                                selectedTextColor = CortexColors.Accent,
                                unselectedTextColor = CortexColors.Muted,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = CortexRoute.Home,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<CortexRoute.Home> {
                HomeScreen(
                    onBeginSession = { navController.navigate(CortexRoute.Session) },
                    onContinueLesson = { lessonId -> navController.navigate(CortexRoute.Lesson(lessonId)) },
                    onOpenLibrary = { navController.navigateToTab(CortexRoute.Library) },
                    onOpenProgress = { navController.navigateToTab(CortexRoute.Progress) },
                )
            }
            composable<CortexRoute.Session> { backStackEntry ->
                val viewModel: SessionViewModel = koinViewModel()

                // Receive lesson-done result set by LessonScreen before popping back.
                val lessonDoneFlow = backStackEntry.savedStateHandle
                    .getStateFlow("lesson_done", false)
                val lessonDone by lessonDoneFlow.collectAsStateWithLifecycle()
                LaunchedEffect(lessonDone) {
                    if (lessonDone) {
                        backStackEntry.savedStateHandle["lesson_done"] = false
                        viewModel.onLessonReturned()
                    }
                }

                SessionScreen(
                    onNavigateToLesson = { lessonId ->
                        navController.navigate(CortexRoute.Lesson(lessonId))
                    },
                    onSessionDone = {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigateToTab(CortexRoute.Home)
                        }
                    },
                    viewModel = viewModel,
                )
            }
            composable<CortexRoute.Lesson> {
                LessonScreen(
                    onBack = { navController.popBackStack() },
                    onLessonCompleted = {
                        // Signal back to SessionScreen if it's the previous destination.
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("lesson_done", true)
                    },
                )
            }
            composable<CortexRoute.Library> {
                LibraryScreen(onBack = { navController.popBackStack() })
            }
            composable<CortexRoute.Review> {
                ReviewScreen(onBack = { navController.navigateToTab(CortexRoute.Home) })
            }
            composable<CortexRoute.Progress> {
                ProgressScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
