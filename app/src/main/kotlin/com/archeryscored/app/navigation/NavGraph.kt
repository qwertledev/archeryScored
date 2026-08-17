package com.archeryscored.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.archeryscored.app.ui.addend.AddEndScreen
import com.archeryscored.app.ui.capture.CaptureEndScreen
import com.archeryscored.app.ui.home.HomeScreen
import com.archeryscored.app.ui.newsession.NewSessionScreen
import com.archeryscored.app.ui.review.ReviewEndScreen
import com.archeryscored.app.ui.session.SessionScreen

@Composable
fun ArcheryNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNewSession = { navController.navigate(Routes.NEW_SESSION) },
                onOpenSession = { sessionId -> navController.navigate(Routes.session(sessionId)) }
            )
        }
        composable(Routes.NEW_SESSION) {
            NewSessionScreen(
                onSessionCreated = { sessionId ->
                    navController.navigate(Routes.session(sessionId)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }
        composable(
            route = Routes.SESSION,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) {
            SessionScreen(
                onAddEnd = { sessionId -> navController.navigate(Routes.addEnd(sessionId)) }
            )
        }
        composable(
            route = Routes.ADD_END,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) {
            AddEndScreen(
                onTakePicture = { sessionId -> navController.navigate(Routes.capture(sessionId)) },
                onEndCaptured = { sessionId, endId ->
                    navController.navigate(Routes.review(sessionId, endId))
                },
                onEndEntrySaved = { sessionId ->
                    navController.navigate(Routes.session(sessionId)) {
                        popUpTo(Routes.SESSION) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.CAPTURE,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) {
            CaptureEndScreen(
                onEndCaptured = { sessionId, endId ->
                    navController.navigate(Routes.review(sessionId, endId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.REVIEW,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.LongType },
                navArgument("endId") { type = NavType.LongType }
            )
        ) {
            ReviewEndScreen(
                onDone = { sessionId ->
                    navController.navigate(Routes.session(sessionId)) {
                        popUpTo(Routes.SESSION) { inclusive = true }
                    }
                }
            )
        }
    }
}
