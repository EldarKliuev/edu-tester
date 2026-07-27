package com.example.edu_tester.domain.model

data class Question(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctOptionIndex: Int,
)

data class Test(
    val id: String,
    val title: String,
    val questions: List<Question> = emptyList(),
)

data class TestSession(
    val id: String,
    val testId: String,
    val port: Int,
    val startedAt: Long,
    val isActive: Boolean,
)

data class Answer(
    val questionId: String,
    val optionIndex: Int,
)

data class StudentAttempt(
    val id: String,
    val sessionId: String,
    val firstName: String,
    val lastName: String,
    val answers: List<Answer>,
    val score: Int,
    val maxScore: Int,
    val submittedAt: Long,
)
