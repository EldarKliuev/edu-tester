package com.example.edu_tester.presentation.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.edu_tester.domain.model.Question

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestEditorScreen(
    viewModel: TestEditorViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактор теста") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.notFound -> {
                Text(
                    "Тест не найден",
                    modifier = Modifier.padding(padding).padding(24.dp),
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::onTitleChange,
                        label = { Text("Название теста") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    state.questions.forEachIndexed { index, question ->
                        QuestionEditorCard(
                            index = index,
                            question = question,
                            onTextChange = { viewModel.updateQuestionText(question.id, it) },
                            onOptionChange = { optIndex, text ->
                                viewModel.updateOption(question.id, optIndex, text)
                            },
                            onAddOption = { viewModel.addOption(question.id) },
                            onRemoveOption = { optIndex ->
                                viewModel.removeOption(question.id, optIndex)
                            },
                            onCorrectSelected = { viewModel.setCorrectOption(question.id, it) },
                            onDelete = { viewModel.removeQuestion(question.id) },
                        )
                    }

                    OutlinedButton(
                        onClick = viewModel::addQuestion,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text("Добавить вопрос")
                    }

                    state.message?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary)
                    }

                    Button(
                        onClick = { viewModel.save(onSaved) },
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (state.isSaving) "Сохранение…" else "Сохранить")
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun QuestionEditorCard(
    index: Int,
    question: Question,
    onTextChange: (String) -> Unit,
    onOptionChange: (Int, String) -> Unit,
    onAddOption: () -> Unit,
    onRemoveOption: (Int) -> Unit,
    onCorrectSelected: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Вопрос ${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить вопрос")
                }
            }
            OutlinedTextField(
                value = question.text,
                onValueChange = onTextChange,
                label = { Text("Текст вопроса") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Варианты (отметьте правильный)", style = MaterialTheme.typography.labelLarge)
            question.options.forEachIndexed { optionIndex, option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = question.correctOptionIndex == optionIndex,
                            onClick = { onCorrectSelected(optionIndex) },
                            role = Role.RadioButton,
                        ),
                ) {
                    RadioButton(
                        selected = question.correctOptionIndex == optionIndex,
                        onClick = { onCorrectSelected(optionIndex) },
                    )
                    OutlinedTextField(
                        value = option,
                        onValueChange = { onOptionChange(optionIndex, it) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Вариант ${optionIndex + 1}") },
                        singleLine = true,
                    )
                    if (question.options.size > 2) {
                        IconButton(onClick = { onRemoveOption(optionIndex) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить вариант")
                        }
                    }
                }
            }
            if (question.options.size < 4) {
                OutlinedButton(onClick = onAddOption) {
                    Text("Добавить вариант")
                }
            }
        }
    }
}
