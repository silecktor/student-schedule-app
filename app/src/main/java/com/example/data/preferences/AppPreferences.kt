package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("student_schedule_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _expulsionThreshold = MutableStateFlow(prefs.getInt(KEY_EXPULSION_THRESHOLD, 20))
    val expulsionThreshold: StateFlow<Int> = _expulsionThreshold.asStateFlow()

    private val _scholarshipThreshold = MutableStateFlow(prefs.getFloat(KEY_SCHOLARSHIP_THRESHOLD, 4.0f).toDouble())
    val scholarshipThreshold: StateFlow<Double> = _scholarshipThreshold.asStateFlow()

    private val _weatherCity = MutableStateFlow(prefs.getString(KEY_WEATHER_CITY, "Москва") ?: "Москва")
    val weatherCity: StateFlow<String> = _weatherCity.asStateFlow()

    private val _weatherEnabled = MutableStateFlow(prefs.getBoolean(KEY_WEATHER_ENABLED, true))
    val weatherEnabled: StateFlow<Boolean> = _weatherEnabled.asStateFlow()

    // Notification Toggles
    private val _notifyBeforePairEnd = MutableStateFlow(prefs.getBoolean(KEY_NOTIFY_PAIR_END, true))
    val notifyBeforePairEnd: StateFlow<Boolean> = _notifyBeforePairEnd.asStateFlow()

    private val _notifyMorningBrief = MutableStateFlow(prefs.getBoolean(KEY_NOTIFY_MORNING_BRIEF, true))
    val notifyMorningBrief: StateFlow<Boolean> = _notifyMorningBrief.asStateFlow()

    private val _morningBriefTime = MutableStateFlow(prefs.getString(KEY_MORNING_BRIEF_TIME, "07:30") ?: "07:30")
    val morningBriefTime: StateFlow<String> = _morningBriefTime.asStateFlow()

    private val _notifyLessonChange = MutableStateFlow(prefs.getBoolean(KEY_NOTIFY_LESSON_CHANGE, true))
    val notifyLessonChange: StateFlow<Boolean> = _notifyLessonChange.asStateFlow()

    private val _notifyHwDeadline = MutableStateFlow(prefs.getBoolean(KEY_NOTIFY_HW_DEADLINE, true))
    val notifyHwDeadline: StateFlow<Boolean> = _notifyHwDeadline.asStateFlow()

    private val _notifyEveningWeather = MutableStateFlow(prefs.getBoolean(KEY_NOTIFY_EVENING_WEATHER, true))
    val notifyEveningWeather: StateFlow<Boolean> = _notifyEveningWeather.asStateFlow()

    private val _notifyBackupReminder = MutableStateFlow(prefs.getBoolean(KEY_NOTIFY_BACKUP, true))
    val notifyBackupReminder: StateFlow<Boolean> = _notifyBackupReminder.asStateFlow()

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun setExpulsionThreshold(threshold: Int) {
        prefs.edit().putInt(KEY_EXPULSION_THRESHOLD, threshold).apply()
        _expulsionThreshold.value = threshold
    }

    fun setScholarshipThreshold(threshold: Double) {
        prefs.edit().putFloat(KEY_SCHOLARSHIP_THRESHOLD, threshold.toFloat()).apply()
        _scholarshipThreshold.value = threshold
    }

    fun setWeatherCity(city: String) {
        prefs.edit().putString(KEY_WEATHER_CITY, city).apply()
        _weatherCity.value = city
    }

    fun setWeatherEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WEATHER_ENABLED, enabled).apply()
        _weatherEnabled.value = enabled
    }

    fun setNotifyBeforePairEnd(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY_PAIR_END, enabled).apply()
        _notifyBeforePairEnd.value = enabled
    }

    fun setNotifyMorningBrief(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY_MORNING_BRIEF, enabled).apply()
        _notifyMorningBrief.value = enabled
    }

    fun setMorningBriefTime(time: String) {
        prefs.edit().putString(KEY_MORNING_BRIEF_TIME, time).apply()
        _morningBriefTime.value = time
    }

    fun setNotifyLessonChange(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY_LESSON_CHANGE, enabled).apply()
        _notifyLessonChange.value = enabled
    }

    fun setNotifyHwDeadline(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY_HW_DEADLINE, enabled).apply()
        _notifyHwDeadline.value = enabled
    }

    fun setNotifyEveningWeather(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY_EVENING_WEATHER, enabled).apply()
        _notifyEveningWeather.value = enabled
    }

    fun setNotifyBackupReminder(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY_BACKUP, enabled).apply()
        _notifyBackupReminder.value = enabled
    }

    // Helper: is dark theme active given current settings & time of day
    fun isDarkThemeActive(systemInDarkTheme: Boolean): Boolean {
        return when (_themeMode.value) {
            "LIGHT" -> false
            "DARK" -> true
            "AUTO_TIME" -> {
                // Auto dark between 20:00 and 07:00
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                hour >= 20 || hour < 7
            }
            else -> systemInDarkTheme // "SYSTEM"
        }
    }

    companion object {
        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_EXPULSION_THRESHOLD = "pref_expulsion_threshold"
        private const val KEY_SCHOLARSHIP_THRESHOLD = "pref_scholarship_threshold"
        private const val KEY_WEATHER_CITY = "pref_weather_city"
        private const val KEY_WEATHER_ENABLED = "pref_weather_enabled"
        private const val KEY_NOTIFY_PAIR_END = "pref_notify_pair_end"
        private const val KEY_NOTIFY_MORNING_BRIEF = "pref_notify_morning_brief"
        private const val KEY_MORNING_BRIEF_TIME = "pref_morning_brief_time"
        private const val KEY_NOTIFY_LESSON_CHANGE = "pref_notify_lesson_change"
        private const val KEY_NOTIFY_HW_DEADLINE = "pref_notify_hw_deadline"
        private const val KEY_NOTIFY_EVENING_WEATHER = "pref_notify_evening_weather"
        private const val KEY_NOTIFY_BACKUP = "pref_notify_backup"
    }
}
