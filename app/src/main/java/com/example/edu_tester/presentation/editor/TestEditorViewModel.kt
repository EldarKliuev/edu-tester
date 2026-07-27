package com.example.edu_tester.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edu_tester.domain.model.Question
import com.example.edu_tester.domain.model.Test
import com.example.edu_tester.domain.usecase.DeleteTestUseCase
import com.example.edu_tester.domain.usecase.GetTestUseCase
import com.example.edu_tester.domain.usecase.SaveTestUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class TestEditorUiState(
    val title: String = "",
    val questions: List<Question> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val message: String? = null,
    val notFound: Boolean = false,
)

class TestEditorViewModel(
    private val testId: String,
    private val getTest: GetTestUseCase,
    private val saveTest: SaveTestUseCase,
    private val deleteTest: DeleteTestUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TestEditorUiState())
    val uiState: StateFlow<TestEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val test = getTest(testId)
            if (test == null) {
                _uiState.update { it.copy(isLoading = false, notFound = true) }
            } else {
                _uiState.update {
                    it.copy(
                        title = test.title,
                        questions = test.questions,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, message = null) }
    }

    fun addQuestion() {
        _uiState.update { state ->
            val question = Question(
                id = UUID.randomUUID().toString(),
                text = "",
                options = listOf("", ""),
                correctOptionIndex = 0,
            )
            state.copy(questions = state.questions + question, message = null)
        }
    }

    fun updateQuestionText(questionId: String, text: String) {
        mutateQuestion(questionId) { it.copy(text = text) }
    }

    fun updateOption(questionId: String, optionIndex: Int, text: String) {
        mutateQuestion(questionId) { question ->
            val options = question.options.toMutableList()
            if (optionIndex in options.indices) {
                options[optionIndex] = text
            }
            question.copy(options = options)
        }
    }

    fun addOption(questionId: String) {
        mutateQuestion(questionId) { question ->
            if (question.options.size >= 4) question
            else question.copy(options = question.options + "")
        }
    }

    fun removeOption(questionId: String, optionIndex: Int) {
        mutateQuestion(questionId) { question ->
            if (question.options.size <= 2) return@mutateQuestion question
            val options = question.options.toMutableList().also { it.removeAt(optionIndex) }
            val correct = when {
                question.correctOptionIndex == optionIndex -> 0
                question.correctOptionIndex > optionIndex -> question.correctOptionIndex - 1
                else -> question.correctOptionIndex
            }.coerceIn(0, options.lastIndex)
            question.copy(options = options, correctOptionIndex = correct)
        }
    }

    fun setCorrectOption(questionId: String, optionIndex: Int) {
        mutateQuestion(questionId) { it.copy(correctOptionIndex = optionIndex) }
    }

    fun removeQuestion(questionId: String) {
        _uiState.update { state ->
            state.copy(questions = state.questions.filterNot { it.id == questionId }, message = null)
        }
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val title = state.title.trim()
        if (title.isBlank()) {
            _uiState.update { it.copy(message = "Введите название теста") }
            return
        }
        if (state.questions.isEmpty()) {
            _uiState.update { it.copy(message = "Добавьте хотя бы один вопрос") }
            return
        }
        for ((index, question) in state.questions.withIndex()) {
            if (question.text.isBlank()) {
                _uiState.update { it.copy(message = "Вопрос ${index + 1}: введите текст") }
                return
            }
            if (question.options.any { it.isBlank() }) {
                _uiState.update { it.copy(message = "Вопрос ${index + 1}: заполните все варианты") }
                return
            }
            if (question.correctOptionIndex !in question.options.indices) {
                _uiState.update { it.copy(message = "Вопрос ${index + 1}: выберите правильный ответ") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            saveTest(
                Test(
                    id = testId,
                    title = title,
                    questions = state.questions,
                )
            )
            _uiState.update { it.copy(isSaving = false, message = "Сохранено") }
            onSaved()
        }
    }

    fun leave(onLeft: () -> Unit) {
        viewModelScope.launch {
            val existing = getTest(testId)
            if (existing != null && existing.questions.isEmpty()) {
                deleteTest(testId)
            }
            onLeft()
        }
    }

    private fun mutateQuestion(questionId: String, transform: (Question) -> Question) {
        _uiState.update { state ->
            state.copy(
                questions = state.questions.map { if (it.id == questionId) transform(it) else it },
                message = null,
            )
        }
    }
}
