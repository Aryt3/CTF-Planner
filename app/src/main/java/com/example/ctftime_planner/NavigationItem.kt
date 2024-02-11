package com.example.ctftime_planner

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavigationItem(val route:String, val label:String, val icons:ImageVector) {

    object Home : NavigationItem(route = "Home", label = "Home", Icons.Default.Home)
    object NowRunning : NavigationItem(route = "nowRunning", label = "Running", Icons.Default.KeyboardArrowUp)
    object Upcoming : NavigationItem(route = "upcoming", label = "Upcoming", Icons.Default.KeyboardArrowDown)
    object Calendar : NavigationItem(route = "calendar", label = "Calendar", Icons.Default.DateRange)

}
