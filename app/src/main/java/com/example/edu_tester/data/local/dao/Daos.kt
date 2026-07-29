package com.example.edu_tester.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.example.edu_tester.data.local.entity.AttemptEntity
import com.example.edu_tester.data.local.entity.QuestionEntity
import com.example.edu_tester.data.local.entity.SessionEntity
import com.example.edu_tester.data.local.entity.TestEntity
import kotlinx.coroutines.flow.Flow

data class TestWithQuestions(
    @Embedded val test: TestEntity,
    @Relation(parentColumn = "id", entityColumn = "testId")
    val questions: List<QuestionEntity>,
)

data class SessionWithAttemptCount(
    @Embedded val session: SessionEntity,
    val attemptCount: Int,
)

@Dao
interface TestDao {
    @Transaction
    @Query("SELECT * FROM tests ORDER BY title COLLATE NOCASE ASC")
    fun observeTestsWithQuestions(): Flow<List<TestWithQuestions>>

    @Transaction
    @Query("SELECT * FROM tests WHERE id = :testId")
    suspend fun getTestWithQuestions(testId: String): TestWithQuestions?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTest(test: TestEntity)

    @Query("DELETE FROM tests WHERE id = :testId")
    suspend fun deleteTest(testId: String)

    @Query("SELECT * FROM questions WHERE testId = :testId ORDER BY sortOrder ASC")
    suspend fun getQuestions(testId: String): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuestions(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions WHERE testId = :testId")
    suspend fun deleteQuestionsForTest(testId: String)

    @Transaction
    suspend fun saveTestWithQuestions(test: TestEntity, questions: List<QuestionEntity>) {
        upsertTest(test)
        deleteQuestionsForTest(test.id)
        if (questions.isNotEmpty()) {
            upsertQuestions(questions)
        }
    }

    @Transaction
    suspend fun deleteTestCascade(testId: String) {
        deleteQuestionsForTest(testId)
        deleteTest(testId)
    }
}

@Dao
interface AttemptDao {
    @Query("SELECT * FROM attempts WHERE sessionId = :sessionId ORDER BY submittedAt ASC")
    fun observeAttempts(sessionId: String): Flow<List<AttemptEntity>>

    @Query("SELECT * FROM attempts WHERE sessionId = :sessionId ORDER BY submittedAt ASC")
    suspend fun getAttempts(sessionId: String): List<AttemptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: AttemptEntity)

    @Query(
        """
        SELECT COUNT(*) FROM attempts
        WHERE sessionId = :sessionId
          AND LOWER(firstName) = LOWER(:firstName)
          AND LOWER(lastName) = LOWER(:lastName)
        """
    )
    suspend fun countByName(sessionId: String, firstName: String, lastName: String): Int
}

@Dao
interface SessionDao {
    @Query(
        """
        SELECT s.*,
               (SELECT COUNT(*) FROM attempts a WHERE a.sessionId = s.id) AS attemptCount
        FROM sessions s
        ORDER BY s.startedAt DESC
        """
    )
    fun observeSessionsWithAttemptCount(): Flow<List<SessionWithAttemptCount>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: SessionEntity)

    @Query(
        """
        UPDATE sessions
        SET isActive = 0, endedAt = :endedAt
        WHERE id = :sessionId
        """
    )
    suspend fun endSession(sessionId: String, endedAt: Long)
}
