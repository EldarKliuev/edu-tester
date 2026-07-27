package com.example.edu_tester.data.repository

import com.example.edu_tester.data.local.dao.AttemptDao
import com.example.edu_tester.data.local.dao.TestDao
import com.example.edu_tester.data.mapper.JsonMappers
import com.example.edu_tester.domain.model.StudentAttempt
import com.example.edu_tester.domain.model.Test
import com.example.edu_tester.domain.repository.AttemptRepository
import com.example.edu_tester.domain.repository.TestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TestRepositoryImpl(
    private val testDao: TestDao,
    private val mappers: JsonMappers,
) : TestRepository {
    override fun observeTests(): Flow<List<Test>> =
        testDao.observeTestsWithQuestions().map { rows ->
            rows.map { row ->
                val ordered = row.questions.sortedBy { it.sortOrder }
                mappers.toDomain(row.test, ordered)
            }
        }

    override suspend fun getTest(testId: String): Test? {
        val row = testDao.getTestWithQuestions(testId) ?: return null
        val ordered = row.questions.sortedBy { it.sortOrder }
        return mappers.toDomain(row.test, ordered)
    }

    override suspend fun saveTest(test: Test) {
        testDao.saveTestWithQuestions(mappers.toEntity(test), mappers.toEntities(test))
    }

    override suspend fun deleteTest(testId: String) {
        testDao.deleteTestCascade(testId)
    }
}

class AttemptRepositoryImpl(
    private val attemptDao: AttemptDao,
    private val mappers: JsonMappers,
) : AttemptRepository {
    override fun observeAttempts(sessionId: String): Flow<List<StudentAttempt>> =
        attemptDao.observeAttempts(sessionId).map { list -> list.map(mappers::toDomain) }

    override suspend fun getAttempts(sessionId: String): List<StudentAttempt> =
        attemptDao.getAttempts(sessionId).map(mappers::toDomain)

    override suspend fun saveAttempt(attempt: StudentAttempt) {
        attemptDao.insertAttempt(mappers.toEntity(attempt))
    }

    override suspend fun hasSubmitted(sessionId: String, firstName: String, lastName: String): Boolean =
        attemptDao.countByName(sessionId, firstName, lastName) > 0
}
