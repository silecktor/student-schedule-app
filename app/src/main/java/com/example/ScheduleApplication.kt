package com.example

import android.app.Application
import com.example.data.repository.ScheduleRepository
import com.example.notifications.AlarmScheduler
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        AlarmScheduler.rescheduleAll(this)

        // Prepopulate sample initial data if database is brand new
        CoroutineScope(Dispatchers.IO).launch {
            val repository = ScheduleRepository(this@ScheduleApplication)
            repository.populateInitialDataIfEmpty()
        }
    }
}
