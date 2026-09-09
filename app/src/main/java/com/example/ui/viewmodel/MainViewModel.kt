package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.preferences.AppPreferences
import com.example.data.repository.DatabaseBackupSnapshot
import com.example.data.repository.ScheduleRepository
import com.example.export.ExportImportManager
import com.example.network.WeatherInfo
import com.example.network.WeatherRepository
import com.example.notifications.AlarmScheduler
import com.example.notifications.NotificationHelper
import com.example.widget.ScheduleWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = ScheduleRepository(application)
    val preferences = AppPreferences(application)

    // Current Tab Index: 0=Сегодня, 1=Неделя, 2=Звонки, 3=Задания, 4=Сессия, 5=Настройки
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Current Day of Week (1..7, Monday=1)
    private val _selectedDayOfWeek = MutableStateFlow(getCurrentDayOfWeek())
    val selectedDayOfWeek: StateFlow<Int> = _selectedDayOfWeek.asStateFlow()

    // Selected Week for browsing
    private val _selectedWeekId = MutableStateFlow<Long?>(null)
    val selectedWeekId: StateFlow<Long?> = _selectedWeekId.asStateFlow()

    // All entities flows
    val allWeeks: StateFlow<List<WeekScheduleEntity>> = repository.allWeeks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubjects: StateFlow<List<SubjectEntity>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBells: StateFlow<List<BellScheduleEntity>> = repository.allBells
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHomeworks: StateFlow<List<HomeworkEntity>> = repository.allHomeworks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGrades: StateFlow<List<GradeEntity>> = repository.allGrades
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExams: StateFlow<List<ExamSessionEntity>> = repository.allExams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active week: explicitly selected or latest available
    val currentWeek: StateFlow<WeekScheduleEntity?> = combine(allWeeks, _selectedWeekId) { weeks, selId ->
        if (selId != null) {
            weeks.find { it.id == selId } ?: weeks.firstOrNull()
        } else {
            weeks.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Lessons with full details for currently selected day
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todayLessons: StateFlow<List<LessonWithDetails>> = combine(currentWeek, _selectedDayOfWeek) { week, day ->
        Pair(week, day)
    }.flatMapLatest { (week, day) ->
        if (week != null) {
            repository.getDetailedLessonsForDay(week.id, day)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Free windows for today
    val todayFreeWindows: StateFlow<List<FreeWindowItem>> = todayLessons.map { lessons ->
        repository.calculateFreeWindows(lessons)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Global Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<SearchResultItem>> = _searchResults.asStateFlow()

    // Weather State
    private val _weatherState = MutableStateFlow<WeatherInfo?>(null)
    val weatherState: StateFlow<WeatherInfo?> = _weatherState.asStateFlow()

    private val _weatherLoading = MutableStateFlow(false)
    val weatherLoading: StateFlow<Boolean> = _weatherLoading.asStateFlow()

    init {
        // Fetch weather if enabled
        if (preferences.weatherEnabled.value) {
            refreshWeather()
        }
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun setSelectedDayOfWeek(day: Int) {
        _selectedDayOfWeek.value = day
    }

    fun setSelectedWeekId(weekId: Long?) {
        _selectedWeekId.value = weekId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
            } else {
                _searchResults.value = repository.searchAll(query)
            }
        }
    }

    fun refreshWeather() {
        viewModelScope.launch {
            _weatherLoading.value = true
            val city = preferences.weatherCity.value
            val res = WeatherRepository.getTomorrowWeather(city)
            res.onSuccess {
                _weatherState.value = it
            }
            _weatherLoading.value = false
        }
    }

    // Attendance stats by subject
    val attendanceStats: StateFlow<List<AttendanceSubjectStats>> = combine(
        allSubjects,
        allWeeks,
        preferences.expulsionThreshold
    ) { subjects, weeks, threshold ->
        val db = com.example.data.db.AppDatabase.getDatabase(getApplication())
        val allLessons = db.lessonDao().getAllLessons()

        subjects.map { subject ->
            val subLessons = allLessons.filter { it.subjectId == subject.id }
            val total = subLessons.size
            val attended = subLessons.count { it.attendance == "ATTENDED" }
            val absent = subLessons.count { it.attendance == "ABSENT" }
            val excused = subLessons.count { it.attendance == "EXCUSED" }

            val percentage = if (total > 0) (absent.toFloat() / total.toFloat()) * 100f else 0f
            val isRisk = percentage >= threshold && total > 0

            AttendanceSubjectStats(
                subject = subject,
                totalLessons = total,
                attendedCount = attended,
                absentCount = absent,
                excusedCount = excused,
                absencePercentage = percentage,
                isRiskOfExpulsion = isRisk
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Grades stats by subject
    val gradesStats: StateFlow<List<SubjectGradeStats>> = combine(
        allSubjects,
        allGrades
    ) { subjects, grades ->
        subjects.map { subject ->
            val subGrades = grades.filter { it.subjectId == subject.id }
            val fivePointGrades = subGrades.filter { it.gradeType == "FIVE_POINT" }
            val avg = if (fivePointGrades.isNotEmpty()) {
                fivePointGrades.map { it.numericValue }.average()
            } else 0.0

            val passCount = subGrades.count { it.gradeType == "PASS_FAIL" && it.isPassed }
            val failCount = subGrades.count { it.gradeType == "PASS_FAIL" && !it.isPassed }

            SubjectGradeStats(
                subject = subject,
                grades = subGrades,
                averageScore = avg,
                passCount = passCount,
                failCount = failCount
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overall GPA
    val overallGpa: StateFlow<Double> = allGrades.map { grades ->
        val numericGrades = grades.filter { it.gradeType == "FIVE_POINT" }
        if (numericGrades.isNotEmpty()) numericGrades.map { it.numericValue }.average() else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // DB Operations
    fun saveSubject(subject: SubjectEntity) = viewModelScope.launch {
        if (subject.id == 0L) repository.insertSubject(subject) else repository.updateSubject(subject)
        ScheduleWidgetProvider.triggerUpdate(getApplication())
    }

    fun deleteSubject(subject: SubjectEntity) = viewModelScope.launch {
        repository.deleteSubject(subject)
        ScheduleWidgetProvider.triggerUpdate(getApplication())
    }

    fun saveBell(bell: BellScheduleEntity) = viewModelScope.launch {
        if (bell.id == 0L) repository.insertBell(bell) else repository.updateBell(bell)
        ScheduleWidgetProvider.triggerUpdate(getApplication())
    }

    fun deleteBell(bell: BellScheduleEntity) = viewModelScope.launch {
        repository.deleteBell(bell)
        ScheduleWidgetProvider.triggerUpdate(getApplication())
    }

    fun resetBellsToDefaults() = viewModelScope.launch {
        repository.resetBellsToDefaultThreeTypes()
        ScheduleWidgetProvider.triggerUpdate(getApplication())
    }

    fun copyBells(sourceDay: Int?, targetDay: Int?) = viewModelScope.launch {
        repository.copyBellsFromTo(sourceDay, targetDay)
        ScheduleWidgetProvider.triggerUpdate(getApplication())
    }

    fun createWeek(title: String, startDate: String, endDate: String, copyFromWeekId: Long? = null) = viewModelScope.launch {
        val newWeekId = if (copyFromWeekId != null && copyFromWeekId > 0) {
            repository.cloneWeek(copyFromWeekId, title, startDate, endDate)
        } else {
            repository.insertWeek(WeekScheduleEntity(title = title, startDate = startDate, endDate = endDate))
        }
        _selectedWeekId.value = newWeekId
        ScheduleWidgetProvider.triggerUpdate(getApplication())
    }

    fun deleteWeek(week: WeekScheduleEntity) = viewModelScope.launch {
        repository.deleteWeek(week)
        _selectedWeekId.value = null
        ScheduleWidgetProvider.triggerUpdate(getApplication())
    }

    fun saveLesson(lesson: LessonEntity) = viewModelScope.launch {
        if (lesson.id == 0L) repository.insertLesson(lesson) else repository.updateLesson(lesson)
        ScheduleWidgetProvider.triggerUpdate(getApplication())
    }

    fun deleteLesson(lesson: LessonEntity) = viewModelScope.launch {
        repository.deleteLesson(lesson)
        ScheduleWidgetProvider.triggerUpdate(getApplication())
    }

    fun setAttendance(lessonId: Long, status: String) = viewModelScope.launch {
        repository.setLessonAttendance(lessonId, status)
    }

    fun saveHomework(hw: HomeworkEntity) = viewModelScope.launch {
        val id = if (hw.id == 0L) repository.insertHomework(hw) else {
            repository.updateHomework(hw)
            hw.id
        }
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val date = sdf.parse(hw.deadline)
            if (date != null) {
                AlarmScheduler.scheduleHwReminder(getApplication(), hw.text, date.time)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun deleteHomework(hw: HomeworkEntity) = viewModelScope.launch {
        repository.deleteHomework(hw)
    }

    fun toggleHomeworkStatus(hw: HomeworkEntity) = viewModelScope.launch {
        val nextStatus = when (hw.status) {
            "NOT_STARTED" -> "IN_PROGRESS"
            "IN_PROGRESS" -> "COMPLETED"
            else -> "NOT_STARTED"
        }
        repository.updateHomework(hw.copy(status = nextStatus))
    }

    fun saveNote(note: NoteEntity) = viewModelScope.launch {
        if (note.id == 0L) repository.insertNote(note) else repository.updateNote(note)
    }

    fun deleteNote(note: NoteEntity) = viewModelScope.launch {
        repository.deleteNote(note)
    }

    fun saveGrade(grade: GradeEntity) = viewModelScope.launch {
        if (grade.id == 0L) repository.insertGrade(grade) else repository.updateGrade(grade)
    }

    fun deleteGrade(grade: GradeEntity) = viewModelScope.launch {
        repository.deleteGrade(grade)
    }

    fun saveExam(exam: ExamSessionEntity) = viewModelScope.launch {
        if (exam.id == 0L) repository.insertExam(exam) else repository.updateExam(exam)
    }

    fun deleteExam(exam: ExamSessionEntity) = viewModelScope.launch {
        repository.deleteExam(exam)
    }

    fun importBackupJson(jsonString: String, replaceAll: Boolean, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = ExportImportManager.importFromJsonString(jsonString)
            res.fold(
                onSuccess = { snapshot ->
                    repository.restoreDatabaseSnapshot(snapshot, replaceAll)
                    ScheduleWidgetProvider.triggerUpdate(getApplication())
                    onResult(true, "База данных успешно восстановлена (${snapshot.lessons.size} занятий, ${snapshot.subjects.size} предметов)")
                },
                onFailure = { error ->
                    onResult(false, error.localizedMessage ?: "Ошибка импорта JSON")
                }
            )
        }
    }

    fun sendTestNotification() {
        NotificationHelper.showNotification(
            context = getApplication(),
            notificationId = 9999,
            channelId = NotificationHelper.CHANNEL_SCHEDULE,
            title = "Тестовое уведомление",
            message = "Система уведомлений расписания работает отлично! 🔔"
        )
    }

    private fun getCurrentDayOfWeek(): Int {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 7
        }
    }
}
