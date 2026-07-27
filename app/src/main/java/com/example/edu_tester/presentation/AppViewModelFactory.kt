package com.example.edu_tester.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.edu_tester.di.AppContainer
import com.example.edu_tester.presentation.editor.TestEditorViewModel
import com.example.edu_tester.presentation.results.ResultsViewModel
import com.example.edu_tester.presentation.session.SessionViewModel
import com.example.edu_tester.presentation.tests.TestListViewModel

class AppViewModelFactory(
    private val container: AppContainer,
    private val testId: String? = null,
    private val sessionId: String? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TestListViewModel::class.java) ->
                TestListViewModel(
                    observeTests = container.observeTests,
                    createEmptyTest = container.createEmptyTest,
                    deleteTest = container.deleteTest,
                ) as T

            modelClass.isAssignableFrom(TestEditorViewModel::class.java) ->
                TestEditorViewModel(
                    testId = requireNotNull(testId),
                    getTest = container.getTest,
                    saveTest = container.saveTest,
                ) as T

            modelClass.isAssignableFrom(SessionViewModel::class.java) ->
                SessionViewModel(
                    testId = requireNotNull(testId),
                    getTest = container.getTest,
                    sessionController = container.sessionController,
                    activeSessionManager = container.activeSessionManager,
                ) as T

            modelClass.isAssignableFrom(ResultsViewModel::class.java) ->
                ResultsViewModel(
                    sessionId = requireNotNull(sessionId),
                    observeSessionResults = container.observeSessionResults,
                ) as T

            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
