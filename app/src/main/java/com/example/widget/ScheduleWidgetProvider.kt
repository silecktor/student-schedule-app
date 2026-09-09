package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun triggerUpdate(context: Context) {
            val intent = Intent(context, ScheduleWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val widgetManager = AppWidgetManager.getInstance(context)
                val ids = widgetManager.getAppWidgetIds(ComponentName(context, ScheduleWidgetProvider::class.java))
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_schedule_layout)

            // Click opens main app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val latestWeek = db.weekScheduleDao().getLatestWeek()
                    if (latestWeek == null) {
                        views.setTextViewText(R.id.widget_header_label, "РАСПИСАНИЕ")
                        views.setTextViewText(R.id.widget_subject_name, "Нет добавленных недель")
                        views.setTextViewText(R.id.widget_time_countdown, "")
                        views.setTextViewText(R.id.widget_classroom, "Откройте приложение для настройки")
                        views.setTextViewText(R.id.widget_teacher, "")
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                        return@launch
                    }

                    val cal = Calendar.getInstance()
                    val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> 1
                        Calendar.TUESDAY -> 2
                        Calendar.WEDNESDAY -> 3
                        Calendar.THURSDAY -> 4
                        Calendar.FRIDAY -> 5
                        Calendar.SATURDAY -> 6
                        else -> 7
                    }

                    val lessons = db.lessonDao().getLessonsForWeek(latestWeek.id).filter { it.dayOfWeek == dayOfWeek }
                    val allBells = db.bellScheduleDao().getAllBells()
                    val daySpecificBells = allBells.filter { it.dayOfWeek == dayOfWeek }
                    val effectiveBells = if (daySpecificBells.isNotEmpty()) daySpecificBells else allBells.filter { it.dayOfWeek == null }
                    val bells = effectiveBells.associateBy { it.lessonNumber }
                    val subjects = db.subjectDao().getAllSubjects().associateBy { it.id }

                    val nowTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val currentStr = nowTimeFormat.format(cal.time)
                    val currentMinutes = parseTimeToMinutes(currentStr)

                    // Find current or next upcoming lesson
                    var nextLesson = lessons.sortedBy { it.lessonNumber }.firstOrNull { lesson ->
                        val bell = bells[lesson.lessonNumber]
                        if (bell != null) {
                            val endMin = parseTimeToMinutes(bell.endTime)
                            endMin >= currentMinutes
                        } else false
                    }

                    if (nextLesson == null) {
                        views.setTextViewText(R.id.widget_header_label, "УЧЁБА НА СЕГОДНЯ ЗАВЕРШЕНА")
                        views.setTextViewText(R.id.widget_subject_name, "Пар больше нет 🎉")
                        views.setTextViewText(R.id.widget_time_countdown, "Отдыхайте")
                        views.setTextViewText(R.id.widget_time_range, "Завтра новый день")
                        views.setTextViewText(R.id.widget_lesson_type, "Свободно")
                        views.setTextViewText(R.id.widget_classroom, "")
                        views.setTextViewText(R.id.widget_teacher, "")
                    } else {
                        val bell = bells[nextLesson.lessonNumber]
                        val subject = subjects[nextLesson.subjectId]

                        val startMin = bell?.let { parseTimeToMinutes(it.startTime) } ?: 0
                        val endMin = bell?.let { parseTimeToMinutes(it.endTime) } ?: 0

                        val isCurrent = currentMinutes in startMin..endMin
                        val countdownText = if (isCurrent) {
                            val remain = endMin - currentMinutes
                            "идёт прямо сейчас (ост. $remain мин)"
                        } else {
                            val before = startMin - currentMinutes
                            if (before > 0) "через $before мин" else "скоро"
                        }

                        val header = if (isCurrent) "ТЕКУЩАЯ ПАРА" else "СЛЕДУЮЩАЯ ПАРА"
                        val typeRu = when (nextLesson.lessonType) {
                            "LECTURE" -> "Лекция"
                            "PRACTICE" -> "Практика"
                            "SEMINAR" -> "Семинар"
                            "LAB" -> "Лаб. работа"
                            else -> "Пара"
                        }

                        val roomStr = if (nextLesson.roomNumber.isNotBlank()) "Ауд. ${nextLesson.roomNumber}, корп. ${nextLesson.building}, эт. ${nextLesson.floor}" else "Аудитория не указана"

                        views.setTextViewText(R.id.widget_header_label, header)
                        views.setTextViewText(R.id.widget_subject_name, subject?.name ?: "Занятие")
                        views.setTextViewText(R.id.widget_time_countdown, countdownText)
                        views.setTextViewText(R.id.widget_time_range, "${bell?.startTime ?: ""} – ${bell?.endTime ?: ""}")
                        views.setTextViewText(R.id.widget_lesson_type, typeRu)
                        views.setTextViewText(R.id.widget_classroom, roomStr)
                        views.setTextViewText(R.id.widget_teacher, subject?.teacher ?: "")
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    // Fallback
                    views.setTextViewText(R.id.widget_header_label, "РАСПИСАНИЕ")
                    views.setTextViewText(R.id.widget_subject_name, "Расписание студента")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        private fun parseTimeToMinutes(timeStr: String): Int {
            return try {
                val parts = timeStr.split(":")
                val h = parts[0].toInt()
                val m = parts[1].toInt()
                h * 60 + m
            } catch (e: Exception) {
                0
            }
        }
    }
}
