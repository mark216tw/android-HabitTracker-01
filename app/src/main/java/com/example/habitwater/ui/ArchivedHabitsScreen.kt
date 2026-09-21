package com.example.habitwater.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.habitwater.data.calculateHabitStats
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedHabitsScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onOpenHabit: (Long) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("已封存習慣", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.archivedHabits.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Text("目前沒有已封存的習慣。", Modifier.padding(20.dp))
                    }
                }
            } else {
                items(state.archivedHabits, key = { it.id }) { habit ->
                    val archivedOn = habit.archivedOn?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: state.today
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("封存於 $archivedOn", style = MaterialTheme.typography.bodySmall)
                            HabitStatsSummary(calculateHabitStats(habit, state.completions, archivedOn, state.archivePeriods))
                            TextButton(onClick = { onOpenHabit(habit.id) }) { Text("查看紀錄與管理") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchivedHabitActions(onRestore: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("習慣管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(modifier = Modifier.fillMaxWidth(), onClick = onRestore) { Text("恢復習慣") }
            TextButton(onClick = { confirmDelete = true }) {
                Text("永久刪除", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("永久刪除這個習慣？") },
            text = { Text("所有歷史打卡紀錄都會一併刪除，且無法復原。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("永久刪除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}
