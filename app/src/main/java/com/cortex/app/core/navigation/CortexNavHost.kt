package com.cortex.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cortex.app.feature.home.HomeScreen
import com.cortex.app.feature.lesson.LessonScreen
import com.cortex.app.feature.library.LibraryScreen
import com.cortex.app.feature.progress.ProgressScreen
import com.cortex.app.feature.review.ReviewScreen

@Composable
fun CortexNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = CortexRoute.Home,
    ) {
        composable<CortexRoute.Home> {
            HomeScreen(
                onBeginSession = { lessonId -> navController.navigate(CortexRoute.Lesson(lessonId)) },
                onContinueLesson = { lessonId -> navController.navigate(CortexRoute.Lesson(lessonId)) },
                onOpenLibrary = { navController.navigate(CortexRoute.Library) },
                onOpenProgress = { navController.navigate(CortexRoute.Progress) },
            )
        }
        composable<CortexRoute.Lesson> {
            LessonScreen(onBack = { navController.popBackStack() })
        }
        composable<CortexRoute.Library> {
            LibraryScreen(onBack = { navController.popBackStack() })
        }
        composable<CortexRoute.Review> {
            ReviewScreen(onBack = { navController.popBackStack() })
        }
        composable<CortexRoute.Progress> {
            ProgressScreen(onBack = { navController.popBackStack() })
        }
    }
}
