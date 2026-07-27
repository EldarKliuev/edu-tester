package com.example.edu_tester.presentation.session

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edu_tester.data.qr.QrCodeGenerator
import com.example.edu_tester.data.server.SessionController
import com.example.edu_tester.data.server.TestSessionService
import com.example.edu_tester.data.session.ActiveSessionManager
import com.example.edu_tester.domain.usecase.GetTestUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionUiState(
    val testTitle: String = "",
    val questionCount: Int = 0,
    val isLoading: Boolean = true,
    val isRunning: Boolean = false,
    val baseUrl: String? = null,
    val sessionId: String? = null,
    val qrBitmap: Bitmap? = null,
    val error: String? = null,
    val notFound: Boolean = false,
)

class SessionViewModel(
    private val testId: String,
    private val getTest: GetTestUseCase,
    private val sessionController: SessionController,
    private val activeSessionManager: ActiveSessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        sessionController.bind()
        viewModelScope.launch {
            val test = getTest(testId)
            if (test == null) {
                _uiState.update { it.copy(isLoading = false, notFound = true) }
                return@launch
            }
            val active = activeSessionManager.current()
            if (active != null && active.test.id == testId) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        testTitle = test.title,
                        questionCount = test.questions.size,
                        isRunning = true,
                        baseUrl = active.baseUrl,
                        sessionId = active.session.id,
                        qrBitmap = QrCodeGenerator.generate(active.baseUrl),
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        testTitle = test.title,
                        questionCount = test.questions.size,
                    )
                }
            }
        }
    }

    fun startSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val test = getTest(testId)
            if (test == null) {
                _uiState.update { it.copy(error = "Тест не найден") }
                return@launch
            }
            if (test.questions.isEmpty()) {
                _uiState.update { it.copy(error = "Добавьте вопросы перед запуском") }
                return@launch
            }

            sessionController.startForegroundShell()
            val service = sessionController.awaitService()
            if (service == null) {
                _uiState.update { it.copy(error = "Не удалось связаться с сервисом сессии") }
                return@launch
            }

            val result = service.startSession(test, TestSessionService.DEFAULT_PORT)
            result.fold(
                onSuccess = { active ->
                    _uiState.update {
                        it.copy(
                            isRunning = true,
                            baseUrl = active.baseUrl,
                            sessionId = active.session.id,
                            qrBitmap = QrCodeGenerator.generate(active.baseUrl),
                            error = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isRunning = false, error = error.message ?: "Ошибка запуска")
                    }
                },
            )
        }
    }

    fun stopSession() {
        viewModelScope.launch {
            val service = sessionController.awaitService()
            service?.stopSession()
            _uiState.update {
                it.copy(
                    isRunning = false,
                    baseUrl = null,
                    qrBitmap = null,
                    // keep sessionId so teacher can open results of last session
                )
            }
        }
    }

    override fun onCleared() {
        // Keep service running if session is active; only unbind.
        sessionController.unbind()
        super.onCleared()
    }
}
