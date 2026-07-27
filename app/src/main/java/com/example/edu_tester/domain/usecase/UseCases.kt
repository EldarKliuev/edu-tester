package com.example.edu_tester.domain.usecase

import com.example.edu_tester.domain.model.Answer
import com.example.edu_tester.domain.model.Question
import com.example.edu_tester.domain.model.StudentAttempt
import com.example.edu_tester.domain.model.Test
import com.example.edu_tester.domain.repository.AttemptRepository
import com.example.edu_tester.domain.repository.TestRepository
import java.util.UUID

class ObserveTestsUseCase(
    private val testRepository: TestRepository,
) {
    operator fun invoke() = testRepository.observeTests()
}

class GetTestUseCase(
    private val testRepository: TestRepository,
) {
    suspend operator fun invoke(testId: String) = testRepository.getTest(testId)
}

class SaveTestUseCase(
    private val testRepository: TestRepository,
) {
    suspend operator fun invoke(test: Test) = testRepository.saveTest(test)
}

class DeleteTestUseCase(
    private val testRepository: TestRepository,
) {
    suspend operator fun invoke(testId: String) = testRepository.deleteTest(testId)
}

class CreateEmptyTestUseCase(
    private val testRepository: TestRepository,
) {
    suspend operator fun invoke(title: String = "Новый тест"): Test {
        val test = Test(
            id = UUID.randomUUID().toString(),
            title = title,
            questions = emptyList(),
        )
        testRepository.saveTest(test)
        return test
    }
}

class ScoreAnswersUseCase {
    operator fun invoke(questions: List<Question>, answers: List<Answer>): Int {
        val byId = answers.associateBy { it.questionId }
        return questions.count { question ->
            byId[question.id]?.optionIndex == question.correctOptionIndex
        }
    }
}

class SubmitStudentAttemptUseCase(
    private val attemptRepository: AttemptRepository,
    private val scoreAnswers: ScoreAnswersUseCase,
) {
    data class Result(
        val attempt: StudentAttempt,
        val alreadySubmitted: Boolean = false,
    )

    suspend operator fun invoke(
        sessionId: String,
        firstName: String,
        lastName: String,
        questions: List<Question>,
        answers: List<Answer>,
    ): Result {
        val trimmedFirst = firstName.trim()
        val trimmedLast = lastName.trim()
        if (attemptRepository.hasSubmitted(sessionId, trimmedFirst, trimmedLast)) {
            val existing = attemptRepository.getAttempts(sessionId)
                .first { it.firstName.equals(trimmedFirst, true) && it.lastName.equals(trimmedLast, true) }
            return Result(attempt = existing, alreadySubmitted = true)
        }
        val score = scoreAnswers(questions, answers)
        val attempt = StudentAttempt(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            firstName = trimmedFirst,
            lastName = trimmedLast,
            answers = answers,
            score = score,
            maxScore = questions.size,
            submittedAt = System.currentTimeMillis(),
        )
        attemptRepository.saveAttempt(attempt)
        return Result(attempt = attempt)
    }
}

class ObserveSessionResultsUseCase(
    private val attemptRepository: AttemptRepository,
) {
    operator fun invoke(sessionId: String) = attemptRepository.observeAttempts(sessionId)
}

class GetSessionResultsUseCase(
    private val attemptRepository: AttemptRepository,
) {
    suspend operator fun invoke(sessionId: String) = attemptRepository.getAttempts(sessionId)
}
