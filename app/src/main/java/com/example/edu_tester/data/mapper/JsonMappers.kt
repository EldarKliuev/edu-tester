package com.example.edu_tester.data.mapper

import com.example.edu_tester.data.local.entity.AttemptEntity
import com.example.edu_tester.data.local.entity.QuestionEntity
import com.example.edu_tester.data.local.entity.SessionEntity
import com.example.edu_tester.data.local.entity.TestEntity
import com.example.edu_tester.domain.model.Answer
import com.example.edu_tester.domain.model.Question
import com.example.edu_tester.domain.model.StudentAttempt
import com.example.edu_tester.domain.model.Test
import com.example.edu_tester.domain.model.TestSession
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class JsonMappers(private val gson: Gson) {
    private val stringListType = object : TypeToken<List<String>>() {}.type
    private val answerListType = object : TypeToken<List<Answer>>() {}.type

    fun optionsToJson(options: List<String>): String = gson.toJson(options)

    fun optionsFromJson(json: String): List<String> =
        gson.fromJson(json, stringListType) ?: emptyList()

    fun answersToJson(answers: List<Answer>): String = gson.toJson(answers)

    fun answersFromJson(json: String): List<Answer> =
        gson.fromJson(json, answerListType) ?: emptyList()

    fun toDomain(test: TestEntity, questions: List<QuestionEntity>): Test =
        Test(
            id = test.id,
            title = test.title,
            questions = questions.map { toDomain(it) },
        )

    fun toDomain(question: QuestionEntity): Question =
        Question(
            id = question.id,
            text = question.text,
            options = optionsFromJson(question.optionsJson),
            correctOptionIndex = question.correctOptionIndex,
        )

    fun toEntity(test: Test): TestEntity =
        TestEntity(id = test.id, title = test.title)

    fun toEntities(test: Test): List<QuestionEntity> =
        test.questions.mapIndexed { index, question ->
            QuestionEntity(
                id = question.id,
                testId = test.id,
                text = question.text,
                optionsJson = optionsToJson(question.options),
                correctOptionIndex = question.correctOptionIndex,
                sortOrder = index,
            )
        }

    fun toDomain(attempt: AttemptEntity): StudentAttempt =
        StudentAttempt(
            id = attempt.id,
            sessionId = attempt.sessionId,
            firstName = attempt.firstName,
            lastName = attempt.lastName,
            answers = answersFromJson(attempt.answersJson),
            score = attempt.score,
            maxScore = attempt.maxScore,
            submittedAt = attempt.submittedAt,
        )

    fun toEntity(attempt: StudentAttempt): AttemptEntity =
        AttemptEntity(
            id = attempt.id,
            sessionId = attempt.sessionId,
            firstName = attempt.firstName,
            lastName = attempt.lastName,
            answersJson = answersToJson(attempt.answers),
            score = attempt.score,
            maxScore = attempt.maxScore,
            submittedAt = attempt.submittedAt,
        )

    fun toDomain(session: SessionEntity): TestSession =
        TestSession(
            id = session.id,
            testId = session.testId,
            testTitle = session.testTitle,
            port = session.port,
            startedAt = session.startedAt,
            endedAt = session.endedAt,
            isActive = session.isActive,
        )

    fun toEntity(session: TestSession): SessionEntity =
        SessionEntity(
            id = session.id,
            testId = session.testId,
            testTitle = session.testTitle,
            port = session.port,
            startedAt = session.startedAt,
            endedAt = session.endedAt,
            isActive = session.isActive,
        )
}
