package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.preferences.AppPreferences
import com.example.data.repository.ScheduleRepository
import com.example.network.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val prefs = AppPreferences(context)
        val repository = ScheduleRepository(context)

        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                AlarmScheduler.rescheduleAll(context)
            }
            ACTION_PAIR_ALERT -> {
                if (prefs.notifyBeforePairEnd.value) {
                    val nextPairInfo = intent.getStringExtra("EXTRA_NEXT_PAIR_INFO")
                    if (nextPairInfo != null && nextPairInfo.isNotBlank()) {
                        NotificationHelper.showNotification(
                            context = context,
                            notificationId = 1001,
                            channelId = NotificationHelper.CHANNEL_SCHEDULE,
                            title = "Следующая пара через 5 минут",
                            message = nextPairInfo
                        )
                    } else {
                        NotificationHelper.showNotification(
                            context = context,
                            notificationId = 1001,
                            channelId = NotificationHelper.CHANNEL_SCHEDULE,
                            title = "Учебный день завершён",
                            message = "Пар больше нет, можно домой! 🎉"
                        )
                    }
                }
            }
            ACTION_MORNING_BRIEF -> {
                if (prefs.notifyMorningBrief.value) {
                    CoroutineScope(Dispatchers.IO).launch {
                        handleMorningBrief(context, repository)
                    }
                }
            }
            ACTION_HW_ALERT -> {
                if (prefs.notifyHwDeadline.value) {
                    val hwText = intent.getStringExtra("EXTRA_HW_TEXT") ?: "Домашнее задание"
                    val deadline = intent.getStringExtra("EXTRA_HW_DEADLINE") ?: ""
                    NotificationHelper.showNotification(
                        context = context,
                        notificationId = 2000 + (hwText.hashCode() % 1000),
                        channelId = NotificationHelper.CHANNEL_HOMEWORK,
                        title = "Скоро дедлайн ДЗ!",
                        message = "$hwText (срок: $deadline)"
                    )
                }
            }
            ACTION_WEATHER_ALERT -> {
                if (prefs.notifyEveningWeather.value && prefs.weatherEnabled.value) {
                    CoroutineScope(Dispatchers.IO).launch {
                        handleWeatherAlert(context, prefs)
                    }
                }
            }
            ACTION_BACKUP_ALERT -> {
                if (prefs.notifyBackupReminder.value) {
                    NotificationHelper.showNotification(
                        context = context,
                        notificationId = 4001,
                        channelId = NotificationHelper.CHANNEL_BACKUP,
                        title = "Резервная копия расписания",
                        message = "Сделайте бэкап расписания в Настройках, чтобы не потерять данные!"
                    )
                }
            }
        }
    }

    private suspend fun handleMorningBrief(context: Context, repository: ScheduleRepository) {
        val latestWeek = repository.getLatestWeek() ?: return
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

        val db = com.example.data.db.AppDatabase.getDatabase(context)
        val lessons = db.lessonDao().getLessonsForWeek(latestWeek.id).filter { it.dayOfWeek == dayOfWeek }
        if (lessons.isEmpty()) {
            NotificationHelper.showNotification(
                context = context,
                notificationId = 1002,
                channelId = NotificationHelper.CHANNEL_SCHEDULE,
                title = "Доброе утро! Сегодня выходной",
                message = "На сегодня занятий в расписании нет."
            )
            return
        }

        val subjects = db.subjectDao().getAllSubjects().associateBy { it.id }
        val bells = db.bellScheduleDao().getAllBells().filter { it.dayOfWeek == dayOfWeek || it.dayOfWeek == null }.associateBy { it.lessonNumber }

        val firstLesson = lessons.minByOrNull { it.lessonNumber }
        val firstBell = firstLesson?.let { bells[it.lessonNumber] }
        val startStr = firstBell?.startTime ?: ""

        val count = lessons.size
        val countStr = "$count ${if (count == 1) "пара" else if (count in 2..4) "пары" else "пар"}"

        val message = "Сегодня $countStr, первая в $startStr. Не забудьте студенческий!"
        NotificationHelper.showNotification(
            context = context,
            notificationId = 1002,
            channelId = NotificationHelper.CHANNEL_SCHEDULE,
            title = "Расписание на сегодня ($countStr)",
            message = message
        )
    }

    private suspend fun handleWeatherAlert(context: Context, prefs: AppPreferences) {
        val city = prefs.weatherCity.value
        val res = WeatherRepository.getTomorrowWeather(city)
        res.onSuccess { info ->
            val advice = if (info.isRainExpected) "Завтра дождь (${info.rainProb}%), возьми зонт! ☔" else "Завтра ${info.description.lowercase()}, до +${info.tempMax.toInt()}°C 🌤️"
            NotificationHelper.showNotification(
                context = context,
                notificationId = 3001,
                channelId = NotificationHelper.CHANNEL_WEATHER,
                title = "Погода на завтра: $city",
                message = advice
            )
        }
    }

    companion object {
        const val ACTION_PAIR_ALERT = "com.example.ACTION_PAIR_ALERT"
        const val ACTION_MORNING_BRIEF = "com.example.ACTION_MORNING_BRIEF"
        const val ACTION_HW_ALERT = "com.example.ACTION_HW_ALERT"
        const val ACTION_WEATHER_ALERT = "com.example.ACTION_WEATHER_ALERT"
        const val ACTION_BACKUP_ALERT = "com.example.ACTION_BACKUP_ALERT"
    }
}
