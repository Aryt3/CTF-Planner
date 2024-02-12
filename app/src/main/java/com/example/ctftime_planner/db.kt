package com.example.ctftime_planner

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val weight: Float,
    @ColumnInfo(name = "start_date")
    val start: String,
    @ColumnInfo(name = "end_date")
    val end: String,
    val eventId: String
)

@RequiresApi(Build.VERSION_CODES.O)
@TypeConverter
fun fromDate(date: LocalDate): Long {
    return date.toEpochDay()
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events")
    suspend fun getAllEvents(): List<EventEntity>

    @Query("SELECT * FROM events WHERE eventId = :id")
    suspend fun getEventById(id: String): EventEntity?

    @Query("SELECT * FROM events WHERE start_date = :dateInMillis OR end_date >= :dateInMillis")
    suspend fun getEventsForDate(dateInMillis: Long): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE eventId = :id")
    suspend fun deleteEventById(id: String)

    @Delete
    suspend fun deleteEvent(event: EventEntity)
}

@Database(entities = [EventEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app-database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}