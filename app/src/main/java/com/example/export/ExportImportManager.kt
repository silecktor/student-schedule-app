package com.example.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.LessonWithDetails
import com.example.data.model.WeekScheduleEntity
import com.example.data.repository.DatabaseBackupSnapshot
import com.example.data.repository.ScheduleRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportImportManager {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val snapshotAdapter = moshi.adapter(DatabaseBackupSnapshot::class.java).indent("  ")

    // Export database snapshot to pretty JSON string
    fun exportToJsonString(snapshot: DatabaseBackupSnapshot): String {
        return snapshotAdapter.toJson(snapshot)
    }

    // Import from JSON string
    fun importFromJsonString(json: String): Result<DatabaseBackupSnapshot> {
        return try {
            val snapshot = snapshotAdapter.fromJson(json)
            if (snapshot != null && snapshot.schemaVersion >= 1) {
                Result.success(snapshot)
            } else {
                Result.failure(Exception("Неверный формат или неподдерживаемая версия схемы (schemaVersion)"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка разбора JSON: ${e.localizedMessage}"))
        }
    }

    // Import and restore database from JSON string
    suspend fun importDatabaseBackup(
        context: Context,
        repository: ScheduleRepository,
        json: String,
        replaceAll: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = importFromJsonString(json)
            if (result.isSuccess) {
                val snapshot = result.getOrThrow()
                repository.restoreDatabaseSnapshot(snapshot, replaceAll)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // Share JSON file via System Share Intent
    suspend fun shareDatabaseBackup(context: Context, repository: ScheduleRepository): Intent = withContext(Dispatchers.IO) {
        val snapshot = repository.getFullDatabaseSnapshot()
        val jsonString = exportToJsonString(snapshot)

        val file = File(context.cacheDir, "student_schedule_backup.json")
        FileOutputStream(file).use {
            it.write(jsonString.toByteArray(Charsets.UTF_8))
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Резервная копия расписания (JSON)")
            putExtra(Intent.EXTRA_TEXT, "Файл резервной копии расписания студента")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // Export Week Schedule as .ics (iCalendar) file
    suspend fun exportWeekToIcs(
        context: Context,
        week: WeekScheduleEntity,
        lessons: List<LessonWithDetails>
    ): Intent = withContext(Dispatchers.IO) {
        val icsBuilder = StringBuilder()
        icsBuilder.appendLine("BEGIN:VCALENDAR")
        icsBuilder.appendLine("VERSION:2.0")
        icsBuilder.appendLine("PRODID:-//Student Schedule App//RU")
        icsBuilder.appendLine("CALSCALE:GREGORIAN")
        icsBuilder.appendLine("X-WR-CALNAME:${week.title}")

        val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val icsTimeSdf = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault())

        val weekStartDate = try { dateSdf.parse(week.startDate) } catch (e: Exception) { Date() } ?: Date()

        for (item in lessons) {
            val bell = item.bell ?: continue
            val subjectName = item.subject?.name ?: "Занятие"
            val teacher = item.subject?.teacher ?: ""
            val classroom = "Корпус: ${item.lesson.building}, Этаж: ${item.lesson.floor}, Ауд: ${item.lesson.roomNumber}"

            // Calculate actual date for this day of week (dayOfWeek: 1..7)
            val cal = Calendar.getInstance().apply {
                time = weekStartDate
                add(Calendar.DAY_OF_YEAR, item.lesson.dayOfWeek - 1)
            }
            val startParts = bell.startTime.split(":")
            val endParts = bell.endTime.split(":")

            val startHour = startParts.getOrNull(0)?.toIntOrNull() ?: 8
            val startMin = startParts.getOrNull(1)?.toIntOrNull() ?: 30
            val endHour = endParts.getOrNull(0)?.toIntOrNull() ?: 10
            val endMin = endParts.getOrNull(1)?.toIntOrNull() ?: 0

            cal.set(Calendar.HOUR_OF_DAY, startHour)
            cal.set(Calendar.MINUTE, startMin)
            cal.set(Calendar.SECOND, 0)
            val dtStart = icsTimeSdf.format(cal.time)

            cal.set(Calendar.HOUR_OF_DAY, endHour)
            cal.set(Calendar.MINUTE, endMin)
            val dtEnd = icsTimeSdf.format(cal.time)

            val typeRu = when (item.lesson.lessonType) {
                "LECTURE" -> "Лекция"
                "PRACTICE" -> "Практика"
                "SEMINAR" -> "Семинар"
                "LAB" -> "Лабораторная"
                else -> "Пара"
            }

            icsBuilder.appendLine("BEGIN:VEVENT")
            icsBuilder.appendLine("UID:lesson_${item.lesson.id}_${System.currentTimeMillis()}@schedule.app")
            icsBuilder.appendLine("SUMMARY:[$typeRu] $subjectName")
            icsBuilder.appendLine("LOCATION:$classroom")
            icsBuilder.appendLine("DESCRIPTION:Преподаватель: $teacher\\nПримечание: ${item.lesson.notes}")
            icsBuilder.appendLine("DTSTART:$dtStart")
            icsBuilder.appendLine("DTEND:$dtEnd")
            icsBuilder.appendLine("STATUS:CONFIRMED")
            icsBuilder.appendLine("END:VEVENT")
        }

        icsBuilder.appendLine("END:VCALENDAR")

        val file = File(context.cacheDir, "schedule_${week.id}.ics")
        FileOutputStream(file).use {
            it.write(icsBuilder.toString().toByteArray(Charsets.UTF_8))
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Расписание на неделю (${week.title})")
            putExtra(Intent.EXTRA_TEXT, "Экспорт расписания в формате iCalendar (.ics)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // Format text representation of a day's schedule for instant sharing in group chats
    fun formatDayScheduleText(dayName: String, dateStr: String, lessons: List<LessonWithDetails>): String {
        val sb = StringBuilder()
        sb.appendLine("📅 Расписание на $dayName ($dateStr):")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")

        if (lessons.isEmpty()) {
            sb.appendLine("🎉 Занятий нет — выходной!")
            return sb.toString()
        }

        lessons.sortedBy { it.lesson.lessonNumber }.forEach { item ->
            val bell = item.bell
            val timeStr = if (bell != null) "${bell.startTime}–${bell.endTime}" else "Пара №${item.lesson.lessonNumber}"
            val typeBadge = when (item.lesson.lessonType) {
                "LECTURE" -> "[Лекция]"
                "PRACTICE" -> "[Практика]"
                "SEMINAR" -> "[Семинар]"
                "LAB" -> "[Лаб.]"
                else -> ""
            }
            val subject = item.subject?.name ?: "Предмет"
            val room = if (item.lesson.roomNumber.isNotBlank()) "ауд. ${item.lesson.roomNumber}" else ""
            val building = if (item.lesson.building.isNotBlank()) "(${item.lesson.building})" else ""
            val teacher = if (item.subject?.teacher?.isNotBlank() == true) " • ${item.subject.teacher}" else ""

            sb.appendLine("⏰ $timeStr | $typeBadge $subject")
            sb.appendLine("📍 $room $building$teacher")
            if (item.lesson.notes.isNotBlank()) {
                sb.appendLine("   📝 ${item.lesson.notes}")
            }
            sb.appendLine()
        }

        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("Сгенерировано в приложении «Расписание студента»")
        return sb.toString()
    }
}
