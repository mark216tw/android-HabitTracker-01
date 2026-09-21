package com.example.habitwater.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.habitwater.data.isScheduledOn
import com.example.habitwater.data.isCompletionEditable
import com.example.habitwater.data.Habit
import com.example.habitwater.data.HabitStats
import com.example.habitwater.data.calculateHabitStats
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: MainUiState,
    onHome: (() -> Unit)?,
    onSettings: (() -> Unit)?,
    onToggleCompletion: (Long, LocalDate) -> Unit,
    habits: List<Habit> = state.habits,
    initialHabitId: Long? = null,
    readOnly: Boolean = false,
    title: String = "習慣月曆",
    onBack: (() -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    val initialHabit = habits.firstOrNull { it.id == initialHabitId } ?: habits.firstOrNull()
    val initialDate = initialHabit?.archivedOn?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: state.today
    var month by remember(initialHabitId, readOnly) { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedId by remember(initialHabitId, readOnly) { mutableLongStateOf(initialHabit?.id ?: -1L) }
    var menuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(habits) {
        if (habits.none { it.id == selectedId }) selectedId = habits.firstOrNull()?.id ?: -1L
    }
    val selected = habits.firstOrNull { it.id == selectedId }
    val completedDates = state.completions.filter { it.habitId == selectedId }.map { it.date }.toSet()
    val statsAsOf = selected?.archivedOn?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: state.today

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                    }
                },
                actions = {
                    if (onSettings != null) IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "設定") }
                },
            )
        },
        bottomBar = {
            if (onHome != null) Box(Modifier.navigationBarsPadding()) {
                MainNavigation("calendar", onHome = onHome, onCalendar = {})
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box {
                Card(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("查看習慣", style = MaterialTheme.typography.labelMedium)
                            Text(selected?.name ?: "尚無習慣", fontWeight = FontWeight.Bold)
                        }
                        Text("切換", color = MaterialTheme.colorScheme.primary)
                    }
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    habits.forEach { habit ->
                        DropdownMenuItem(
                            text = { Text(habit.name) },
                            onClick = { selectedId = habit.id; menuOpen = false },
                            leadingIcon = if (habit.id == selectedId) ({ Icon(Icons.Default.Check, null) }) else null,
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Default.ArrowBackIosNew, "上個月") }
                Text("${month.year} 年 ${month.monthValue} 月", Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold)
                IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, "下個月") }
            }
            if (selected != null) {
                HabitStatsSummary(calculateHabitStats(selected, state.completions, statsAsOf, state.archivePeriods))
                CalendarGrid(
                    month = month,
                    completedDates = completedDates,
                    habit = selected,
                    today = state.today,
                    readOnly = readOnly,
                    onToggleCompletion = onToggleCompletion,
                )
                val completedThisMonth = (1..month.lengthOfMonth()).count { month.atDay(it).toString() in completedDates }
                Text("此月份完成 $completedThisMonth 天", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                if (!readOnly) Text("可點選今天起最近 7 天補登或取消紀錄", style = MaterialTheme.typography.bodySmall)
            }
            footer?.invoke()
            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 12.dp))
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    completedDates: Set<String>,
    habit: Habit,
    today: LocalDate,
    readOnly: Boolean,
    onToggleCompletion: (Long, LocalDate) -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        listOf("一", "二", "三", "四", "五", "六", "日").forEach {
            Text(it, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelMedium)
        }
    }
    val leading = month.atDay(1).dayOfWeek.value - 1
    val cellCount = ((leading + month.lengthOfMonth() + 6) / 7) * 7
    (0 until cellCount).chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            week.forEach { index ->
                val day = index - leading + 1
                if (day !in 1..month.lengthOfMonth()) {
                    Box(Modifier.weight(1f).aspectRatio(1f))
                } else {
                    val date = month.atDay(day)
                    val editable = !readOnly && isCompletionEditable(date, today)
                    DayCell(
                        modifier = Modifier.weight(1f),
                        date = date,
                        completed = date.toString() in completedDates,
                        scheduled = habit.isScheduledOn(date),
                        today = today,
                        onToggle = if (editable) ({ onToggleCompletion(habit.id, date) }) else null,
                    )
                }
            }
        }
    }
}

@Composable
fun HabitStatsSummary(stats: HabitStats) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatItem("最近 7 天", stats.completedLast7Days, Modifier.weight(1f))
                StatItem("目前連續", stats.currentStreak, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatItem("最佳連續", stats.bestStreak, Modifier.weight(1f))
                StatItem("本月完成", stats.completedThisMonth, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value 天", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DayCell(
    modifier: Modifier = Modifier,
    date: LocalDate,
    completed: Boolean,
    scheduled: Boolean,
    today: LocalDate,
    onToggle: (() -> Unit)?,
) {
    Box(
        modifier.aspectRatio(1f).padding(4.dp).then(
            if (onToggle != null) Modifier.clickable(onClick = onToggle) else Modifier,
        ),
        contentAlignment = Alignment.Center,
    ) {
        val background = when {
            completed -> MaterialTheme.colorScheme.primary
            date == today -> MaterialTheme.colorScheme.primaryContainer
            else -> androidx.compose.ui.graphics.Color.Transparent
        }
        Box(
            Modifier.fillMaxSize().clip(CircleShape).background(background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = when {
                    completed -> MaterialTheme.colorScheme.onPrimary
                    scheduled -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                },
                fontWeight = if (completed) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}
