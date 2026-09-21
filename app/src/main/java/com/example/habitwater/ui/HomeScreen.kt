package com.example.habitwater.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import androidx.core.content.ContextCompat
import com.example.habitwater.data.EVERY_DAY_MASK
import com.example.habitwater.data.Habit
import com.example.habitwater.data.isScheduledOn
import java.util.Calendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: MainUiState,
    viewModel: MainViewModel,
    onCalendar: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    var editedHabit by remember { mutableStateOf<Habit?>(null) }
    var addingHabit by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.refreshToday()
        if (!granted) scope.launch {
            snackbar.showSnackbar("提醒已儲存，但通知權限尚未開啟")
        }
    }
    val todayHabits = state.habits.filter { it.isScheduledOn(state.today) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Column {
                        Text("今天", fontWeight = FontWeight.Bold)
                        Text("每一小步，都算數", style = MaterialTheme.typography.labelMedium)
                    }
                },
                actions = {
                    IconButton(onClick = { addingHabit = true }) {
                        Icon(Icons.Default.Add, contentDescription = "新增習慣")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "設定")
                    }
                },
            )
        },
        bottomBar = {
            Box(Modifier.navigationBarsPadding()) {
                MainNavigation("home", onHome = {}, onCalendar = onCalendar)
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            item {
                WaterProgress(state.waterTotal, state.settings.waterGoalMl)
            }
            item {
                Text("快速加水", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(250 to "玻璃杯", 500 to "保溫瓶", 600 to "寶特瓶").forEach { (amount, name) ->
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp),
                            onClick = {
                                viewModel.addWater(amount) { id ->
                                    scope.launch {
                                        val result = snackbar.showSnackbar("已記錄 ${amount}ml", "復原")
                                        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                            viewModel.removeWater(id)
                                        }
                                    }
                                }
                            },
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${amount}ml", fontWeight = FontWeight.Bold)
                                Text(name, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("今日習慣", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${todayHabits.count { state.isCompleted(it.id) }} / ${todayHabits.size}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (todayHabits.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Text("今天沒有排定的習慣，好好休息吧！", Modifier.padding(20.dp))
                    }
                }
            } else {
                items(todayHabits, key = { it.id }) { habit ->
                    HabitRow(
                        habit = habit,
                        completed = state.isCompleted(habit.id),
                        onToggle = { viewModel.toggleHabit(habit) },
                        onEdit = { editedHabit = habit },
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (addingHabit || editedHabit != null) {
        HabitDialog(
            habit = editedHabit,
            onDismiss = { addingHabit = false; editedHabit = null },
            onSave = {
                viewModel.saveHabit(it)
                if (it.reminderHour != null) {
                    if (Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else if (!state.notificationStatus.habitAvailable) {
                        scope.launch { snackbar.showSnackbar("提醒已儲存，但通知已被系統封鎖") }
                    }
                }
                addingHabit = false
                editedHabit = null
            },
            onArchive = editedHabit?.let { habit ->
                { viewModel.archiveHabit(habit); editedHabit = null }
            },
        )
    }
}

@Composable
private fun WaterProgress(total: Int, goal: Int) {
    val target = (total.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val progress by animateFloatAsState(target, label = "water")
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(
                Modifier.width(92.dp).height(116.dp)
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp, topStart = 12.dp, topEnd = 12.dp))
                    .border(5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp, topStart = 12.dp, topEnd = 12.dp)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    Modifier.fillMaxWidth().height(108.dp * progress)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)),
                )
                Text("${(target * 100).toInt()}%", Modifier.align(Alignment.Center), fontWeight = FontWeight.Bold)
            }
            Column {
                Text("今日飲水", style = MaterialTheme.typography.labelLarge)
                Text("$total ml", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text("目標 $goal ml", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (total >= goal) Text("今日目標達成！", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HabitRow(habit: Habit, completed: Boolean, onToggle: () -> Unit, onEdit: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick = onToggle,
                colors = if (completed) {
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    )
                } else {
                    IconButtonDefaults.filledIconButtonColors()
                },
            ) {
                if (completed) Icon(Icons.Default.Check, contentDescription = "已完成")
                else Box(Modifier.size(12.dp).border(2.dp, MaterialTheme.colorScheme.onPrimary, CircleShape))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(if (completed) "今天已完成" else "點一下完成打卡", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "編輯") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HabitDialog(
    habit: Habit?,
    onDismiss: () -> Unit,
    onSave: (Habit) -> Unit,
    onArchive: (() -> Unit)?,
) {
    var name by remember(habit) { mutableStateOf(habit?.name.orEmpty()) }
    var daysMask by remember(habit) { mutableIntStateOf(habit?.daysMask ?: EVERY_DAY_MASK) }
    var reminder by remember(habit) { mutableStateOf(habit?.reminderHour != null) }
    var hour by remember(habit) { mutableIntStateOf(habit?.reminderHour ?: 8) }
    var minute by remember(habit) { mutableIntStateOf(habit?.reminderMinute ?: 0) }
    var showTimePicker by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (habit == null) "新增習慣" else "編輯習慣") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it.take(24) },
                    label = { Text("習慣名稱") },
                    singleLine = true,
                )
                Text("每週執行日", fontWeight = FontWeight.Bold)
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    maxItemsInEachRow = 4,
                ) {
                    dayLabels.forEachIndexed { index, label ->
                        val bit = 1 shl index
                        FilterChip(
                            selected = daysMask and bit != 0,
                            onClick = { daysMask = daysMask xor bit },
                            label = { Text(label, fontWeight = if (daysMask and bit != 0) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (daysMask and bit != 0) ({ Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }) else null,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("提醒", Modifier.weight(1f))
                    Switch(checked = reminder, onCheckedChange = { reminder = it })
                }
                if (reminder) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showTimePicker = true },
                    ) {
                        Text("提醒時間 ${formatTime(context, hour, minute)}")
                    }
                }
                if (onArchive != null) {
                    TextButton(onClick = { confirmArchive = true }) { Text("封存習慣") }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && daysMask != 0,
                onClick = {
                    onSave(
                        (habit ?: Habit(name = name.trim())).copy(
                            name = name.trim(),
                            daysMask = daysMask,
                            reminderHour = if (reminder) hour else null,
                            reminderMinute = if (reminder) minute else null,
                        ),
                    )
                },
            ) { Text("儲存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("選擇提醒時間") },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        hour = pickerState.hour
                        minute = pickerState.minute
                        showTimePicker = false
                    },
                ) { Text("確定") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("取消") } },
        )
    }

    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text("封存這個習慣？") },
            text = { Text("封存後將從今日清單移除並停止提醒，歷史紀錄會保留，可稍後恢復。") },
            confirmButton = { TextButton(onClick = { confirmArchive = false; onArchive?.invoke() }) { Text("封存") } },
            dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("取消") } },
        )
    }
}

private fun formatTime(context: android.content.Context, hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return DateFormat.getTimeFormat(context).format(calendar.time)
}
