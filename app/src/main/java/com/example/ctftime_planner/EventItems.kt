package com.example.ctftime_planner

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

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

data class Event(
    val title: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val location: String
)

