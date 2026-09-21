package com.example.habitwater.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun MainNavigation(selected: String, onHome: () -> Unit, onCalendar: () -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == "home",
            onClick = onHome,
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("今天") },
        )
        NavigationBarItem(
            selected = selected == "calendar",
            onClick = onCalendar,
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
            label = { Text("月曆") },
        )
    }
}
