package com.example.edu_tester.domain.repository

import com.example.edu_tester.domain.model.SessionHistoryItem
import com.example.edu_tester.domain.model.StudentAttempt
import com.example.edu_tester.domain.model.Test
import com.example.edu_tester.domain.model.TestSession
import kotlinx.coroutines.flow.Flow

interface TestRepository {
    fun observeTests(): Flow<List<Test>>
    suspend fun getTest(testId: String): Test?
    suspend fun saveTest(test: Test)
    suspend fun deleteTest(testId: String)
}

interface AttemptRepository {
    fun observeAttempts(sessionId: String): Flow<List<StudentAttempt>>
    suspend fun getAttempts(sessionId: String): List<StudentAttempt>
    suspend fun saveAttempt(attempt: StudentAttempt)
    suspend fun hasSubmitted(sessionId: String, firstName: String, lastName: String): Boolean
}

interface SessionRepository {
    fun observeSessionHistory(): Flow<List<SessionHistoryItem>>
    suspend fun getSession(sessionId: String): TestSession?
    suspend fun saveSession(session: TestSession)
    suspend fun endSession(sessionId: String, endedAt: Long)
}
