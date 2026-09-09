package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val teacher: String = "",
    val colorHex: String = "#6366F1" // Default Indigo
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "bell_schedules")
data class BellScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lessonNumber: Int,
    val startTime: String, // "08:30"
    val endTime: String,   // "10:00"
    val dayOfWeek: Int? = null // null for standard daily grid, 6 for Saturday, etc.
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "week_schedules")
data class WeekScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startDate: String, // "2026-09-01"
    val endDate: String,   // "2026-09-07"
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekScheduleId: Long,
    val dayOfWeek: Int, // 1 (Mon) .. 7 (Sun)
    val lessonNumber: Int,
    val subjectId: Long,
    val building: String = "",
    val floor: Int = 1,
    val roomNumber: String = "",
    val mapSchemeNote: String? = null,
    val lessonType: String = "LECTURE", // LECTURE, PRACTICE, SEMINAR, LAB
    val notes: String = "",
    val attachmentUri: String? = null,
    val attendance: String = "UNSET" // UNSET, ATTENDED, ABSENT, EXCUSED
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "homeworks")
data class HomeworkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val lessonId: Long? = null,
    val text: String,
    val deadline: String, // "2026-09-05 23:59"
    val status: String = "NOT_STARTED" // NOT_STARTED, IN_PROGRESS, COMPLETED
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long? = null,
    val lessonId: Long? = null,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val tagsCsv: String = "" // e.g. "экзамен,важно"
) {
    val tagsList: List<String>
        get() = if (tagsCsv.isBlank()) emptyList() else tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

@JsonClass(generateAdapter = true)
@Entity(tableName = "grades")
data class GradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val lessonId: Long? = null,
    val gradeType: String = "FIVE_POINT", // FIVE_POINT (2..5), PASS_FAIL
    val numericValue: Double = 5.0,
    val isPassed: Boolean = true,
    val date: String, // "2026-09-02"
    val category: String = "REGULAR" // REGULAR, TEST, CREDIT, EXAM
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "exam_sessions")
data class ExamSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val examDate: String, // "2026-09-20"
    val examTime: String = "09:00",
    val examType: String = "EXAM", // CREDIT, EXAM, COURSEWORK, DIPLOMA
    val ticketNumber: String = "",
    val readinessPercent: Int = 50, // 0..100
    val prepNotes: String = ""
)
