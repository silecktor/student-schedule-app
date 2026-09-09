package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object NotificationHelper {
    const val CHANNEL_SCHEDULE = "schedule_channel"
    const val CHANNEL_HOMEWORK = "homework_channel"
    const val CHANNEL_WEATHER = "weather_channel"
    const val CHANNEL_BACKUP = "backup_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val scheduleChannel = NotificationChannel(
                CHANNEL_SCHEDULE,
                "Расписание и пары",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о следующей паре и утренняя сводка дня"
                enableVibration(true)
            }

            val homeworkChannel = NotificationChannel(
                CHANNEL_HOMEWORK,
                "Домашние задания",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания о приближающихся дедлайнах ДЗ"
                enableVibration(true)
            }

            val weatherChannel = NotificationChannel(
                CHANNEL_WEATHER,
                "Погода перед учёбой",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Вечерний прогноз погоды и совет взять зонт"
            }

            val backupChannel = NotificationChannel(
                CHANNEL_BACKUP,
                "Резервные копии",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Напоминания о сохранении бэкапа расписания"
            }

            notificationManager.createNotificationChannels(
                listOf(scheduleChannel, homeworkChannel, weatherChannel, backupChannel)
            )
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        channelId: String,
        title: String,
        message: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}
