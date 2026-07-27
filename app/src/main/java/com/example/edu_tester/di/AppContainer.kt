package com.example.edu_tester.di

import android.content.Context
import androidx.room.Room
import com.example.edu_tester.data.local.AppDatabase
import com.example.edu_tester.data.mapper.JsonMappers
import com.example.edu_tester.data.network.LanIpHelper
import com.example.edu_tester.data.repository.AttemptRepositoryImpl
import com.example.edu_tester.data.repository.TestRepositoryImpl
import com.example.edu_tester.data.server.SessionController
import com.example.edu_tester.data.session.ActiveSessionManager
import com.example.edu_tester.domain.repository.AttemptRepository
import com.example.edu_tester.domain.repository.TestRepository
import com.example.edu_tester.domain.usecase.CreateEmptyTestUseCase
import com.example.edu_tester.domain.usecase.DeleteTestUseCase
import com.example.edu_tester.domain.usecase.GetSessionResultsUseCase
import com.example.edu_tester.domain.usecase.GetTestUseCase
import com.example.edu_tester.domain.usecase.ObserveSessionResultsUseCase
import com.example.edu_tester.domain.usecase.ObserveTestsUseCase
import com.example.edu_tester.domain.usecase.SaveTestUseCase
import com.example.edu_tester.domain.usecase.ScoreAnswersUseCase
import com.example.edu_tester.domain.usecase.SubmitStudentAttemptUseCase
import com.google.gson.Gson

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val gson: Gson = Gson()
    val jsonMappers = JsonMappers(gson)

    val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "edu_tester.db",
    ).build()

    val testRepository: TestRepository =
        TestRepositoryImpl(database.testDao(), jsonMappers)

    val attemptRepository: AttemptRepository =
        AttemptRepositoryImpl(database.attemptDao(), jsonMappers)

    val scoreAnswers = ScoreAnswersUseCase()
    val submitStudentAttempt = SubmitStudentAttemptUseCase(attemptRepository, scoreAnswers)

    val activeSessionManager = ActiveSessionManager(submitStudentAttempt)
    val lanIpHelper = LanIpHelper(appContext)
    val sessionController = SessionController(appContext)

    val observeTests = ObserveTestsUseCase(testRepository)
    val getTest = GetTestUseCase(testRepository)
    val saveTest = SaveTestUseCase(testRepository)
    val deleteTest = DeleteTestUseCase(testRepository)
    val createEmptyTest = CreateEmptyTestUseCase(testRepository)
    val observeSessionResults = ObserveSessionResultsUseCase(attemptRepository)
    val getSessionResults = GetSessionResultsUseCase(attemptRepository)
}
