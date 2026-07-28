package com.example.edu_tester.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tests")
data class TestEntity(
    @PrimaryKey val id: String,
    val title: String,
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val testId: String,
    val text: String,
    val optionsJson: String,
    val correctOptionIndex: Int,
    val sortOrder: Int,
)

@Entity(tableName = "attempts")
data class AttemptEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val firstName: String,
    val lastName: String,
    val answersJson: String,
    val score: Int,
    val maxScore: Int,
    val submittedAt: Long,
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val testId: String,
    val testTitle: String,
    val port: Int,
    val startedAt: Long,
    val endedAt: Long?,
    val isActive: Boolean,
)
