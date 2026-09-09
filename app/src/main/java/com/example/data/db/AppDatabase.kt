package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        SubjectEntity::class,
        BellScheduleEntity::class,
        WeekScheduleEntity::class,
        LessonEntity::class,
        HomeworkEntity::class,
        NoteEntity::class,
        GradeEntity::class,
        ExamSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun bellScheduleDao(): BellScheduleDao
    abstract fun weekScheduleDao(): WeekScheduleDao
    abstract fun lessonDao(): LessonDao
    abstract fun homeworkDao(): HomeworkDao
    abstract fun noteDao(): NoteDao
    abstract fun gradeDao(): GradeDao
    abstract fun examSessionDao(): ExamSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_schedule_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
