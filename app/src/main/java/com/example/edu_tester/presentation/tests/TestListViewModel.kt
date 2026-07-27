package com.example.edu_tester.presentation.tests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edu_tester.domain.model.Test
import com.example.edu_tester.domain.usecase.CreateEmptyTestUseCase
import com.example.edu_tester.domain.usecase.DeleteTestUseCase
import com.example.edu_tester.domain.usecase.ObserveTestsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TestListViewModel(
    observeTests: ObserveTestsUseCase,
    private val createEmptyTest: CreateEmptyTestUseCase,
    private val deleteTest: DeleteTestUseCase,
) : ViewModel() {

    val tests: StateFlow<List<Test>> = observeTests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    fun createTest() {
        viewModelScope.launch {
            val test = createEmptyTest()
            _events.emit(Event.OpenEditor(test.id))
        }
    }

    fun deleteTest(testId: String) {
        viewModelScope.launch {
            deleteTest.invoke(testId)
        }
    }

    sealed interface Event {
        data class OpenEditor(val testId: String) : Event
    }
}
