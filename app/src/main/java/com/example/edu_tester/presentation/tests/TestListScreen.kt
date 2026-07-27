package com.example.edu_tester.presentation.tests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.edu_tester.domain.model.Test

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestListScreen(
    viewModel: TestListViewModel,
    onOpenEditor: (String) -> Unit,
    onStartSession: (String) -> Unit,
) {
    val tests by viewModel.tests.collectAsStateWithLifecycle()
    val testPendingDelete = remember { mutableStateOf<Test?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TestListViewModel.Event.OpenEditor -> onOpenEditor(event.testId)
            }
        }
    }

    testPendingDelete.value?.let { test ->
        AlertDialog(
            onDismissRequest = { testPendingDelete.value = null },
            title = { Text("Удаление теста") },
            text = { Text("Вы уверены что хотите удалить ${test.title}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTest(test.id)
                        testPendingDelete.value = null
                    },
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { testPendingDelete.value = null }) {
                    Text("Отмена")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Мои тесты") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::createTest) {
                Icon(Icons.Default.Add, contentDescription = "Создать тест")
            }
        },
    ) { padding ->
        if (tests.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Пока нет тестов", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Нажмите +, чтобы создать первый тест",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(tests, key = { it.id }) { test ->
                    TestCard(
                        test = test,
                        onClick = { onOpenEditor(test.id) },
                        onStart = { onStartSession(test.id) },
                        onDelete = { testPendingDelete.value = test },
                    )
                }
            }
        }
    }
}

@Composable
private fun TestCard(
    test: Test,
    onClick: () -> Unit,
    onStart: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(test.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Вопросов: ${test.questions.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onStart, enabled = test.questions.isNotEmpty()) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Запустить сессию")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить")
            }
        }
    }
}
