package com.example.edu_tester.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.edu_tester.EduTesterApp
import com.example.edu_tester.presentation.AppViewModelFactory
import com.example.edu_tester.presentation.editor.TestEditorScreen
import com.example.edu_tester.presentation.editor.TestEditorViewModel
import com.example.edu_tester.presentation.results.ResultsScreen
import com.example.edu_tester.presentation.results.ResultsViewModel
import com.example.edu_tester.presentation.session.SessionScreen
import com.example.edu_tester.presentation.session.SessionViewModel
import com.example.edu_tester.presentation.tests.TestListScreen
import com.example.edu_tester.presentation.tests.TestListViewModel

@Composable
fun EduTesterNavHost() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as EduTesterApp
    val container = app.container

    NavHost(
        navController = navController,
        startDestination = Routes.TEST_LIST,
    ) {
        composable(Routes.TEST_LIST) {
            val vm: TestListViewModel = viewModel(
                factory = AppViewModelFactory(container),
            )
            TestListScreen(
                viewModel = vm,
                onOpenEditor = { testId -> navController.navigate(Routes.testEditor(testId)) },
                onStartSession = { testId -> navController.navigate(Routes.session(testId)) },
            )
        }

        composable(
            route = Routes.TEST_EDITOR,
            arguments = listOf(navArgument("testId") { type = NavType.StringType }),
        ) { entry ->
            val testId = entry.arguments?.getString("testId").orEmpty()
            val vm: TestEditorViewModel = viewModel(
                factory = AppViewModelFactory(container, testId = testId),
            )
            TestEditorScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.SESSION,
            arguments = listOf(navArgument("testId") { type = NavType.StringType }),
        ) { entry ->
            val testId = entry.arguments?.getString("testId").orEmpty()
            val vm: SessionViewModel = viewModel(
                factory = AppViewModelFactory(container, testId = testId),
            )
            SessionScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenResults = { sessionId ->
                    navController.navigate(Routes.results(sessionId))
                },
            )
        }

        composable(
            route = Routes.RESULTS,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { entry ->
            val sessionId = entry.arguments?.getString("sessionId").orEmpty()
            val vm: ResultsViewModel = viewModel(
                factory = AppViewModelFactory(container, sessionId = sessionId),
            )
            ResultsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
