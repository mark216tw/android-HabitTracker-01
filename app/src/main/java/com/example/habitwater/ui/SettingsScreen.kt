package com.example.habitwater.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.habitwater.data.ThemeMode
import com.example.habitwater.ui.theme.colorFromHue
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: MainUiState, viewModel: MainViewModel, onBack: () -> Unit, onArchivedHabits: () -> Unit) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshToday()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("設定", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SettingSection("顯示模式") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ThemeMode.SYSTEM to "系統",
                        ThemeMode.LIGHT to "淺色",
                        ThemeMode.DARK to "深色",
                    ).forEach { (mode, label) ->
                        val selected = state.settings.themeMode == mode
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = selected,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (selected) ({ Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }) else null,
                        )
                    }
                }
            }
            SettingSection("主題色彩") {
                val presets = listOf(205f, 165f, 45f, 15f, 285f, 335f)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    presets.forEach { hue ->
                        val selected = circularHueDistance(state.settings.hue, hue) < 1f
                        Box(
                            Modifier.size(44.dp).background(colorFromHue(hue), CircleShape)
                                .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            IconButton(onClick = { viewModel.setHue(hue) }) {
                                if (selected) Icon(Icons.Default.Check, "已選取", tint = Color.White)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).background(colorFromHue(state.settings.hue), CircleShape))
                    Spacer(Modifier.width(10.dp))
                    HueSlider(state.settings.hue, viewModel::setHue, Modifier.weight(1f))
                }
            }
            SettingSection("每日飲水目標") {
                Text("${state.settings.waterGoalMl} ml", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Slider(
                    value = state.settings.waterGoalMl.toFloat(),
                    onValueChange = { viewModel.setWaterGoal((it / 250).roundToInt() * 250) },
                    valueRange = 1000f..4000f,
                    steps = 11,
                )
            }
            SettingSection("飲水提醒") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("每 2 小時提醒", fontWeight = FontWeight.Bold)
                        Text("系統可能依省電狀態延後通知", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = state.settings.waterReminders,
                        onCheckedChange = { enabled ->
                            viewModel.setWaterReminders(enabled)
                            if (enabled && Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                    )
                }
                Text("開始 ${state.settings.wakeHour}:00", fontWeight = FontWeight.Bold)
                Slider(
                    value = state.settings.wakeHour.toFloat(),
                    onValueChange = { viewModel.setWakeHour(it.roundToInt().coerceAtMost(state.settings.sleepHour - 1)) },
                    valueRange = 5f..12f,
                    steps = 6,
                )
                Text("結束 ${state.settings.sleepHour}:00", fontWeight = FontWeight.Bold)
                Slider(
                    value = state.settings.sleepHour.toFloat(),
                    onValueChange = { viewModel.setSleepHour(it.roundToInt().coerceAtLeast(state.settings.wakeHour + 1)) },
                    valueRange = 18f..24f,
                    steps = 5,
                )
            }
            SettingSection("通知狀態") {
                val status = state.notificationStatus
                val habitReminderConfigured = state.habits.any { it.reminderHour != null }
                val blocked = state.settings.waterReminders && !status.waterAvailable ||
                    habitReminderConfigured && !status.habitAvailable
                Text(if (status.appEnabled && status.permissionGranted) "App 通知已開啟" else "App 通知未開啟")
                Text(if (status.waterChannelEnabled) "飲水通知頻道已開啟" else "飲水通知頻道已關閉")
                Text(if (status.habitChannelEnabled) "習慣通知頻道已開啟" else "習慣通知頻道已關閉")
                if (blocked) {
                    Text(
                        "提醒設定會保留，但遭系統封鎖的通知無法送達。",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    },
                ) { Text("前往系統通知設定") }
            }
            SettingSection("習慣管理") {
                Text("已封存 ${state.archivedHabits.size} 個習慣", style = MaterialTheme.typography.bodySmall)
                Button(onClick = onArchivedHabits) { Text("查看已封存習慣") }
            }
            Text("習慣追蹤單機版  1.0.0-prerelease", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun HueSlider(hue: Float, onHueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    var preview by remember(hue) { mutableFloatStateOf(hue) }
    Box(modifier.height(44.dp), contentAlignment = Alignment.Center) {
        val colors = remember { (0..12).map { colorFromHue(it * 30f) } }
        Canvas(Modifier.fillMaxWidth().height(12.dp).padding(horizontal = 10.dp)) {
            drawLine(
                brush = Brush.horizontalGradient(colors),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = size.height,
                cap = StrokeCap.Round,
            )
        }
        Slider(
            value = preview,
            onValueChange = { preview = it; onHueChange(it) },
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(
                thumbColor = colorFromHue(preview),
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
            ),
        )
    }
}

private fun circularHueDistance(a: Float, b: Float): Float {
    val distance = kotlin.math.abs(a - b) % 360f
    return minOf(distance, 360f - distance)
}
