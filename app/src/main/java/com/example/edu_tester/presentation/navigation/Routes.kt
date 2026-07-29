package com.example.edu_tester.presentation.navigation

object Routes {
    const val TEST_LIST = "tests"
    const val HISTORY = "history"
    const val TEST_EDITOR = "tests/{testId}/edit"
    const val SESSION = "tests/{testId}/session"
    const val RESULTS = "sessions/{sessionId}/results"

    fun testEditor(testId: String) = "tests/$testId/edit"
    fun session(testId: String) = "tests/$testId/session"
    fun results(sessionId: String) = "sessions/$sessionId/results"
}
