package com.example.edu_tester.data.session

import com.example.edu_tester.domain.model.Answer
import com.example.edu_tester.domain.model.Question
import com.example.edu_tester.domain.model.Test
import com.example.edu_tester.domain.model.TestSession
import com.example.edu_tester.domain.usecase.SubmitStudentAttemptUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class RegisteredStudent(
    val token: String,
    val firstName: String,
    val lastName: String,
)

data class ActiveSessionState(
    val session: TestSession,
    val test: Test,
    val baseUrl: String,
)

/**
 * In-memory holder for the currently running classroom session.
 */
class ActiveSessionManager(
    private val submitStudentAttempt: SubmitStudentAttemptUseCase,
) {
    private val _state = MutableStateFlow<ActiveSessionState?>(null)
    val state: StateFlow<ActiveSessionState?> = _state.asStateFlow()

    private val students = ConcurrentHashMap<String, RegisteredStudent>()
    private val submittedTokens = ConcurrentHashMap.newKeySet<String>()

    fun isActive(): Boolean = _state.value != null

    fun current(): ActiveSessionState? = _state.value

    fun start(test: Test, port: Int, baseUrl: String): ActiveSessionState {
        students.clear()
        submittedTokens.clear()
        val session = TestSession(
            id = UUID.randomUUID().toString(),
            testId = test.id,
            port = port,
            startedAt = System.currentTimeMillis(),
            isActive = true,
        )
        val active = ActiveSessionState(session = session, test = test, baseUrl = baseUrl)
        _state.value = active
        return active
    }

    fun stop() {
        _state.value = null
        students.clear()
        submittedTokens.clear()
    }

    fun publicQuestions(): List<QuestionPublic> {
        val test = _state.value?.test ?: return emptyList()
        return test.questions.map { it.toPublic() }
    }

    fun register(firstName: String, lastName: String): RegisteredStudent {
        require(isActive()) { "Session is not active" }
        val token = UUID.randomUUID().toString()
        val student = RegisteredStudent(
            token = token,
            firstName = firstName.trim(),
            lastName = lastName.trim(),
        )
        students[token] = student
        return student
    }

    fun findStudent(token: String): RegisteredStudent? = students[token]

    suspend fun submit(token: String, answers: List<Answer>): SubmitStudentAttemptUseCase.Result {
        val active = _state.value ?: error("Session is not active")
        val student = students[token] ?: error("Unknown student token")
        if (!submittedTokens.add(token)) {
            // Already submitted with this token — return existing if possible
            val result = submitStudentAttempt(
                sessionId = active.session.id,
                firstName = student.firstName,
                lastName = student.lastName,
                questions = active.test.questions,
                answers = answers,
            )
            return result.copy(alreadySubmitted = true)
        }
        return submitStudentAttempt(
            sessionId = active.session.id,
            firstName = student.firstName,
            lastName = student.lastName,
            questions = active.test.questions,
            answers = answers,
        )
    }
}

data class QuestionPublic(
    val id: String,
    val text: String,
    val options: List<String>,
)

private fun Question.toPublic() = QuestionPublic(
    id = id,
    text = text,
    options = options,
)
