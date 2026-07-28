package com.example.edu_tester.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.edu_tester.EduTesterApp
import com.example.edu_tester.presentation.AppViewModelFactory
import com.example.edu_tester.presentation.editor.TestEditorScreen
import com.example.edu_tester.presentation.editor.TestEditorViewModel
import com.example.edu_tester.presentation.history.SessionHistoryScreen
import com.example.edu_tester.presentation.history.SessionHistoryViewModel
import com.example.edu_tester.presentation.results.ResultsScreen
import com.example.edu_tester.presentation.results.ResultsViewModel
import com.example.edu_tester.presentation.session.SessionScreen
import com.example.edu_tester.presentation.session.SessionViewModel
import com.example.edu_tester.presentation.tests.TestListScreen
import com.example.edu_tester.presentation.tests.TestListViewModel
import kotlinx.coroutines.launch

@Composable
fun EduTesterNavHost() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as EduTesterApp
    val container = app.container
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun openDrawer() {
        scope.launch { drawerState.open() }
    }

    fun navigateDrawerDestination(route: String) {
        scope.launch {
            drawerState.close()
            navController.navigate(route) {
                popUpTo(Routes.TEST_LIST) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute == Routes.TEST_LIST || currentRoute == Routes.HISTORY,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "EduTester",
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                )
                NavigationDrawerItem(
                    label = { Text("Мои тесты") },
                    selected = currentRoute == Routes.TEST_LIST,
                    onClick = { navigateDrawerDestination(Routes.TEST_LIST) },
                    icon = {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
                NavigationDrawerItem(
                    label = { Text("История") },
                    selected = currentRoute == Routes.HISTORY,
                    onClick = { navigateDrawerDestination(Routes.HISTORY) },
                    icon = {
                        Icon(Icons.Default.History, contentDescription = null)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }
        },
    ) {
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
                    onOpenDrawer = ::openDrawer,
                    onOpenEditor = { testId -> navController.navigate(Routes.testEditor(testId)) },
                    onStartSession = { testId -> navController.navigate(Routes.session(testId)) },
                )
            }

            composable(Routes.HISTORY) {
                val vm: SessionHistoryViewModel = viewModel(
                    factory = AppViewModelFactory(container),
                )
                SessionHistoryScreen(
                    viewModel = vm,
                    onOpenDrawer = ::openDrawer,
                    onOpenResults = { sessionId ->
                        navController.navigate(Routes.results(sessionId))
                    },
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
}
