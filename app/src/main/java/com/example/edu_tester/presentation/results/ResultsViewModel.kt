package com.example.edu_tester.presentation.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edu_tester.domain.model.StudentAttempt
import com.example.edu_tester.domain.usecase.ObserveSessionResultsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultsViewModel(
    sessionId: String,
    observeSessionResults: ObserveSessionResultsUseCase,
) : ViewModel() {

    val attempts: StateFlow<List<StudentAttempt>> = observeSessionResults(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun formatTime(millis: Long): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(millis))
    }
}
