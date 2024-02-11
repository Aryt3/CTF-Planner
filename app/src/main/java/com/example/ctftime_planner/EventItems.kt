package com.example.ctftime_planner

import java.time.Duration

data class EventItems(
    val title: String,
    val weight: String,
    val format: String,
    val start: String,
    val end: String,
    val duration: Int,
    val location: String,
    val id: String
)