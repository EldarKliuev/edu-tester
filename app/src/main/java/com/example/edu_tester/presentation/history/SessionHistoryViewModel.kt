package com.example.edu_tester.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edu_tester.domain.model.SessionHistoryItem
import com.example.edu_tester.domain.usecase.ObserveSessionHistoryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionHistoryViewModel(
    observeSessionHistory: ObserveSessionHistoryUseCase,
) : ViewModel() {

    val items: StateFlow<List<SessionHistoryItem>> = observeSessionHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun formatDateTime(millis: Long): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(millis))
    }
}
