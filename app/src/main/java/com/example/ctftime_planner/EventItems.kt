package com.example.ctftime_planner

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
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
