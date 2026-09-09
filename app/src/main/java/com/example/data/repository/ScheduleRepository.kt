package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ScheduleRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val subjectDao = db.subjectDao()
    private val bellDao = db.bellScheduleDao()
    private val weekDao = db.weekScheduleDao()
    private val lessonDao = db.lessonDao()
    private val homeworkDao = db.homeworkDao()
    private val noteDao = db.noteDao()
    private val gradeDao = db.gradeDao()
    private val examDao = db.examSessionDao()

    // Flows
    val allSubjects: Flow<List<SubjectEntity>> = subjectDao.getAllSubjectsFlow()
    val allBells: Flow<List<BellScheduleEntity>> = bellDao.getAllBellsFlow()
    val allWeeks: Flow<List<WeekScheduleEntity>> = weekDao.getAllWeeksFlow()
    val allHomeworks: Flow<List<HomeworkEntity>> = homeworkDao.getAllHomeworksFlow()
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotesFlow()
    val allGrades: Flow<List<GradeEntity>> = gradeDao.getAllGradesFlow()
    val allExams: Flow<List<ExamSessionEntity>> = examDao.getAllExamsFlow()

    // Bell Schedule by Day
    fun getBellsForDay(dayOfWeek: Int): Flow<List<BellScheduleEntity>> =
        bellDao.getBellsForDayFlow(dayOfWeek)

    // Lessons for a Week
    fun getLessonsForWeek(weekId: Long): Flow<List<LessonEntity>> =
        lessonDao.getLessonsForWeekFlow(weekId)

    // Lessons with full details (Subject, Bell, Homeworks, Notes, Grades) for a day
    fun getDetailedLessonsForDay(weekId: Long, dayOfWeek: Int): Flow<List<LessonWithDetails>> {
        val baseFlow = combine(
            lessonDao.getLessonsForDayFlow(weekId, dayOfWeek),
            subjectDao.getAllSubjectsFlow(),
            bellDao.getAllBellsFlow()
        ) { lessons, subjects, bells ->
            Triple(lessons, subjects, bells)
        }

        val extraFlow = combine(
            homeworkDao.getAllHomeworksFlow(),
            noteDao.getAllNotesFlow(),
            gradeDao.getAllGradesFlow()
        ) { homeworks, notes, grades ->
            Triple(homeworks, notes, grades)
        }

        return combine(baseFlow, extraFlow) { (lessons, subjects, bells), (homeworks, notes, grades) ->
            val subjectsMap = subjects.associateBy { it.id }
            val daySpecificBells = bells.filter { it.dayOfWeek == dayOfWeek }
            val effectiveBells = if (daySpecificBells.isNotEmpty()) daySpecificBells else bells.filter { it.dayOfWeek == null }
            val bellsMap = effectiveBells.associateBy { it.lessonNumber }

            lessons.sortedBy { it.lessonNumber }.map { lesson ->
                LessonWithDetails(
                    lesson = lesson,
                    subject = subjectsMap[lesson.subjectId],
                    bell = bellsMap[lesson.lessonNumber] ?: bells.firstOrNull { it.lessonNumber == lesson.lessonNumber },
                    homeworks = homeworks.filter { it.lessonId == lesson.id },
                    notes = notes.filter { it.lessonId == lesson.id },
                    grades = grades.filter { it.lessonId == lesson.id }
                )
            }
        }
    }

    // Free windows ("Окна") calculation between consecutive lessons
    fun calculateFreeWindows(lessons: List<LessonWithDetails>): List<FreeWindowItem> {
        val windows = mutableListOf<FreeWindowItem>()
        if (lessons.size < 2) return windows

        val sorted = lessons.filter { it.bell != null }.sortedBy { it.lesson.lessonNumber }
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        for (i in 0 until sorted.size - 1) {
            val current = sorted[i]
            val next = sorted[i + 1]
            val currentBell = current.bell ?: continue
            val nextBell = next.bell ?: continue

            try {
                val currentEnd = timeFormat.parse(currentBell.endTime) ?: continue
                val nextStart = timeFormat.parse(nextBell.startTime) ?: continue

                val diffMinutes = (nextStart.time - currentEnd.time) / (60 * 1000)
                if (diffMinutes >= 15) { // If break is 15 minutes or more (free window)
                    windows.add(
                        FreeWindowItem(
                            afterLessonNumber = current.lesson.lessonNumber,
                            startTime = currentBell.endTime,
                            endTime = nextBell.startTime,
                            durationMinutes = diffMinutes
                        )
                    )
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
        return windows
    }

    // CRUD Subjects
    suspend fun insertSubject(subject: SubjectEntity): Long = withContext(Dispatchers.IO) { subjectDao.insert(subject) }
    suspend fun updateSubject(subject: SubjectEntity) = withContext(Dispatchers.IO) { subjectDao.update(subject) }
    suspend fun deleteSubject(subject: SubjectEntity) = withContext(Dispatchers.IO) { subjectDao.delete(subject) }

    // CRUD Bells
    suspend fun insertBell(bell: BellScheduleEntity): Long = withContext(Dispatchers.IO) { bellDao.insert(bell) }
    suspend fun updateBell(bell: BellScheduleEntity) = withContext(Dispatchers.IO) { bellDao.update(bell) }
    suspend fun deleteBell(bell: BellScheduleEntity) = withContext(Dispatchers.IO) { bellDao.delete(bell) }

    suspend fun resetBellsToDefaultThreeTypes() = withContext(Dispatchers.IO) {
        val currentBells = bellDao.getAllBells()
        currentBells.forEach { bellDao.delete(it) }

        // 1. Понедельник (смещенный график / классные часы)
        bellDao.insert(BellScheduleEntity(lessonNumber = 1, startTime = "09:00", endTime = "10:30", dayOfWeek = 1))
        bellDao.insert(BellScheduleEntity(lessonNumber = 2, startTime = "10:45", endTime = "12:15", dayOfWeek = 1))
        bellDao.insert(BellScheduleEntity(lessonNumber = 3, startTime = "13:00", endTime = "14:30", dayOfWeek = 1))
        bellDao.insert(BellScheduleEntity(lessonNumber = 4, startTime = "14:45", endTime = "16:15", dayOfWeek = 1))
        bellDao.insert(BellScheduleEntity(lessonNumber = 5, startTime = "16:30", endTime = "18:00", dayOfWeek = 1))
        bellDao.insert(BellScheduleEntity(lessonNumber = 6, startTime = "18:15", endTime = "19:45", dayOfWeek = 1))

        // 2. Вторник – Пятница (основная академическая сетка пар)
        bellDao.insert(BellScheduleEntity(lessonNumber = 1, startTime = "08:30", endTime = "10:00", dayOfWeek = null))
        bellDao.insert(BellScheduleEntity(lessonNumber = 2, startTime = "10:15", endTime = "11:45", dayOfWeek = null))
        bellDao.insert(BellScheduleEntity(lessonNumber = 3, startTime = "12:30", endTime = "14:00", dayOfWeek = null))
        bellDao.insert(BellScheduleEntity(lessonNumber = 4, startTime = "14:15", endTime = "15:45", dayOfWeek = null))
        bellDao.insert(BellScheduleEntity(lessonNumber = 5, startTime = "16:00", endTime = "17:30", dayOfWeek = null))
        bellDao.insert(BellScheduleEntity(lessonNumber = 6, startTime = "17:45", endTime = "19:15", dayOfWeek = null))

        // 3. Суббота (сокращенные пары и перемены)
        bellDao.insert(BellScheduleEntity(lessonNumber = 1, startTime = "09:00", endTime = "10:20", dayOfWeek = 6))
        bellDao.insert(BellScheduleEntity(lessonNumber = 2, startTime = "10:30", endTime = "11:50", dayOfWeek = 6))
        bellDao.insert(BellScheduleEntity(lessonNumber = 3, startTime = "12:10", endTime = "13:30", dayOfWeek = 6))
        bellDao.insert(BellScheduleEntity(lessonNumber = 4, startTime = "13:40", endTime = "15:00", dayOfWeek = 6))
    }

    suspend fun copyBellsFromTo(sourceDay: Int?, targetDay: Int?) = withContext(Dispatchers.IO) {
        val allBells = bellDao.getAllBells()
        val sourceBells = allBells.filter { it.dayOfWeek == sourceDay }
        if (sourceBells.isEmpty()) return@withContext

        val targetExisting = allBells.filter { it.dayOfWeek == targetDay }
        targetExisting.forEach { bellDao.delete(it) }

        sourceBells.forEach { b ->
            bellDao.insert(
                BellScheduleEntity(
                    lessonNumber = b.lessonNumber,
                    startTime = b.startTime,
                    endTime = b.endTime,
                    dayOfWeek = targetDay
                )
            )
        }
    }

    // CRUD Weeks
    suspend fun insertWeek(week: WeekScheduleEntity): Long = withContext(Dispatchers.IO) { weekDao.insert(week) }
    suspend fun updateWeek(week: WeekScheduleEntity) = withContext(Dispatchers.IO) { weekDao.update(week) }
    suspend fun deleteWeek(week: WeekScheduleEntity) = withContext(Dispatchers.IO) {
        lessonDao.deleteForWeek(week.id)
        weekDao.delete(week)
    }
    suspend fun getLatestWeek(): WeekScheduleEntity? = withContext(Dispatchers.IO) { weekDao.getLatestWeek() }
    suspend fun getWeekById(id: Long): WeekScheduleEntity? = withContext(Dispatchers.IO) { weekDao.getWeekById(id) }

    // Clone week schedule as a template for a new week
    suspend fun cloneWeek(sourceWeekId: Long, newTitle: String, newStartDate: String, newEndDate: String): Long = withContext(Dispatchers.IO) {
        val newWeekId = weekDao.insert(
            WeekScheduleEntity(
                title = newTitle,
                startDate = newStartDate,
                endDate = newEndDate
            )
        )
        val sourceLessons = lessonDao.getLessonsForWeek(sourceWeekId)
        val clonedLessons = sourceLessons.map { lesson ->
            lesson.copy(
                id = 0,
                weekScheduleId = newWeekId,
                attendance = "UNSET",
                notes = "",
                attachmentUri = null
            )
        }
        lessonDao.insertAll(clonedLessons)
        newWeekId
    }

    // CRUD Lessons
    suspend fun insertLesson(lesson: LessonEntity): Long = withContext(Dispatchers.IO) { lessonDao.insert(lesson) }
    suspend fun updateLesson(lesson: LessonEntity) = withContext(Dispatchers.IO) { lessonDao.update(lesson) }
    suspend fun deleteLesson(lesson: LessonEntity) = withContext(Dispatchers.IO) { lessonDao.delete(lesson) }

    // Attendance toggle on Lesson
    suspend fun setLessonAttendance(lessonId: Long, attendanceStatus: String) = withContext(Dispatchers.IO) {
        val lesson = lessonDao.getLessonById(lessonId)
        if (lesson != null) {
            lessonDao.update(lesson.copy(attendance = attendanceStatus))
        }
    }

    // CRUD Homework
    suspend fun insertHomework(hw: HomeworkEntity): Long = withContext(Dispatchers.IO) { homeworkDao.insert(hw) }
    suspend fun updateHomework(hw: HomeworkEntity) = withContext(Dispatchers.IO) { homeworkDao.update(hw) }
    suspend fun deleteHomework(hw: HomeworkEntity) = withContext(Dispatchers.IO) { homeworkDao.delete(hw) }

    // CRUD Notes
    suspend fun insertNote(note: NoteEntity): Long = withContext(Dispatchers.IO) { noteDao.insert(note) }
    suspend fun updateNote(note: NoteEntity) = withContext(Dispatchers.IO) { noteDao.update(note) }
    suspend fun deleteNote(note: NoteEntity) = withContext(Dispatchers.IO) { noteDao.delete(note) }

    // CRUD Grades
    suspend fun insertGrade(grade: GradeEntity): Long = withContext(Dispatchers.IO) { gradeDao.insert(grade) }
    suspend fun updateGrade(grade: GradeEntity) = withContext(Dispatchers.IO) { gradeDao.update(grade) }
    suspend fun deleteGrade(grade: GradeEntity) = withContext(Dispatchers.IO) { gradeDao.delete(grade) }

    // CRUD Exams
    suspend fun insertExam(exam: ExamSessionEntity): Long = withContext(Dispatchers.IO) { examDao.insert(exam) }
    suspend fun updateExam(exam: ExamSessionEntity) = withContext(Dispatchers.IO) { examDao.update(exam) }
    suspend fun deleteExam(exam: ExamSessionEntity) = withContext(Dispatchers.IO) { examDao.delete(exam) }

    // Unified Global Search
    suspend fun searchAll(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.isBlank()) return@withContext emptyList()

        val results = mutableListOf<SearchResultItem>()

        val subjects = subjectDao.getAllSubjects()
        val subjectsMap = subjects.associateBy { it.id }

        // Search Subjects
        subjects.filter { it.name.lowercase().contains(q) || it.teacher.lowercase().contains(q) }
            .forEach {
                results.add(
                    SearchResultItem(
                        type = SearchCategory.SUBJECT,
                        title = it.name,
                        subtitle = if (it.teacher.isNotBlank()) "Преподаватель: ${it.teacher}" else "Предмет",
                        dateOrTime = "",
                        entityId = it.id
                    )
                )
            }

        // Search Homeworks
        homeworkDao.getAllHomeworks().filter { it.text.lowercase().contains(q) }
            .forEach {
                val subjectName = subjectsMap[it.subjectId]?.name ?: "Предмет"
                results.add(
                    SearchResultItem(
                        type = SearchCategory.HOMEWORK,
                        title = it.text,
                        subtitle = "$subjectName • ${when(it.status) { "COMPLETED" -> "Выполнено"; "IN_PROGRESS" -> "В процессе"; else -> "Не начато"}}",
                        dateOrTime = "Дедлайн: ${it.deadline}",
                        entityId = it.id
                    )
                )
            }

        // Search Notes
        noteDao.getAllNotes().filter { it.text.lowercase().contains(q) || it.tagsCsv.lowercase().contains(q) }
            .forEach {
                val subjectName = it.subjectId?.let { id -> subjectsMap[id]?.name } ?: "Общая заметка"
                val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                results.add(
                    SearchResultItem(
                        type = SearchCategory.NOTE,
                        title = it.text,
                        subtitle = "$subjectName ${if (it.tagsCsv.isNotBlank()) "• Теги: ${it.tagsCsv}" else ""}",
                        dateOrTime = sdf.format(Date(it.createdAt)),
                        entityId = it.id
                    )
                )
            }

        // Search Exams
        examDao.getAllExams().filter {
            val subName = subjectsMap[it.subjectId]?.name ?: ""
            subName.lowercase().contains(q) || it.prepNotes.lowercase().contains(q) || it.ticketNumber.lowercase().contains(q)
        }.forEach {
            val subName = subjectsMap[it.subjectId]?.name ?: "Экзамен"
            results.add(
                SearchResultItem(
                    type = SearchCategory.EXAM,
                    title = "$subName (${when(it.examType) { "CREDIT" -> "Зачёт"; "COURSEWORK" -> "Курсовая"; "DIPLOMA" -> "Диплом"; else -> "Экзамен"}})",
                    subtitle = "Готовность: ${it.readinessPercent}% ${if (it.ticketNumber.isNotBlank()) "• Билет №${it.ticketNumber}" else ""}",
                    dateOrTime = "${it.examDate} ${it.examTime}",
                    entityId = it.id
                )
            )
        }

        // Search Lessons (by classroom or notes)
        lessonDao.getAllLessons().filter {
            it.building.lowercase().contains(q) || it.roomNumber.lowercase().contains(q) || it.notes.lowercase().contains(q)
        }.forEach {
            val subName = subjectsMap[it.subjectId]?.name ?: "Пара"
            results.add(
                SearchResultItem(
                    type = SearchCategory.LESSON,
                    title = "$subName (Пара №${it.lessonNumber})",
                    subtitle = "Ауд. ${it.roomNumber}, корп. ${it.building}, эт. ${it.floor} ${if (it.notes.isNotBlank()) "• ${it.notes}" else ""}",
                    dateOrTime = "День недели: ${getDayName(it.dayOfWeek)}",
                    entityId = it.id
                )
            )
        }

        results
    }

    private fun getDayName(day: Int): String {
        return when (day) {
            1 -> "Понедельник"
            2 -> "Вторник"
            3 -> "Среда"
            4 -> "Четверг"
            5 -> "Пятница"
            6 -> "Суббота"
            else -> "Воскресенье"
        }
    }

    // Pre-populate Starter Sample Data if DB is empty
    suspend fun populateInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingWeeks = weekDao.getAllWeeks()
        val existingBells = bellDao.getAllBells()

        // If app already has data but lacks Monday schedule, auto-seed Monday bells
        if (existingBells.isNotEmpty() && existingBells.none { it.dayOfWeek == 1 }) {
            bellDao.insert(BellScheduleEntity(lessonNumber = 1, startTime = "09:00", endTime = "10:30", dayOfWeek = 1))
            bellDao.insert(BellScheduleEntity(lessonNumber = 2, startTime = "10:45", endTime = "12:15", dayOfWeek = 1))
            bellDao.insert(BellScheduleEntity(lessonNumber = 3, startTime = "13:00", endTime = "14:30", dayOfWeek = 1))
            bellDao.insert(BellScheduleEntity(lessonNumber = 4, startTime = "14:45", endTime = "16:15", dayOfWeek = 1))
            bellDao.insert(BellScheduleEntity(lessonNumber = 5, startTime = "16:30", endTime = "18:00", dayOfWeek = 1))
            bellDao.insert(BellScheduleEntity(lessonNumber = 6, startTime = "18:15", endTime = "19:45", dayOfWeek = 1))
        }

        if (existingWeeks.isNotEmpty()) return@withContext

        // 1. Subjects
        val mathId = subjectDao.insert(SubjectEntity(name = "Высшая математика", teacher = "Иванов Иван Иванович", colorHex = "#6366F1"))
        val csId = subjectDao.insert(SubjectEntity(name = "Программирование (Kotlin/Android)", teacher = "Смирнов Алексей Петрович", colorHex = "#10B981"))
        val physicsId = subjectDao.insert(SubjectEntity(name = "Теоретическая физика", teacher = "Кузнецова Елена Сергеевна", colorHex = "#F59E0B"))
        val engId = subjectDao.insert(SubjectEntity(name = "Иностранный язык", teacher = "Brown Sarah", colorHex = "#EC4899"))
        val dbId = subjectDao.insert(SubjectEntity(name = "Базы данных и SQL", teacher = "Федоров Дмитрий Викторович", colorHex = "#3B82F6"))

        // 2. Bell Schedule (3 Types: Monday, Tuesday-Friday, Saturday)
        // Type 1: Понедельник (смещенный график / кураторский час)
        bellDao.insert(BellScheduleEntity(lessonNumber = 1, startTime = "09:00", endTime = "10:30", dayOfWeek = 1))
        bellDao.insert(BellScheduleEntity(lessonNumber = 2, startTime = "10:45", endTime = "12:15", dayOfWeek = 1))
        bellDao.insert(BellScheduleEntity(lessonNumber = 3, startTime = "13:00", endTime = "14:30", dayOfWeek = 1))
        bellDao.insert(BellScheduleEntity(lessonNumber = 4, startTime = "14:45", endTime = "16:15", dayOfWeek = 1))
        bellDao.insert(BellScheduleEntity(lessonNumber = 5, startTime = "16:30", endTime = "18:00", dayOfWeek = 1))
        bellDao.insert(BellScheduleEntity(lessonNumber = 6, startTime = "18:15", endTime = "19:45", dayOfWeek = 1))

        // Type 2: Вторник – Пятница (основная академическая сетка)
        bellDao.insert(BellScheduleEntity(lessonNumber = 1, startTime = "08:30", endTime = "10:00", dayOfWeek = null))
        bellDao.insert(BellScheduleEntity(lessonNumber = 2, startTime = "10:15", endTime = "11:45", dayOfWeek = null))
        bellDao.insert(BellScheduleEntity(lessonNumber = 3, startTime = "12:30", endTime = "14:00", dayOfWeek = null))
        bellDao.insert(BellScheduleEntity(lessonNumber = 4, startTime = "14:15", endTime = "15:45", dayOfWeek = null))
        bellDao.insert(BellScheduleEntity(lessonNumber = 5, startTime = "16:00", endTime = "17:30", dayOfWeek = null))
        bellDao.insert(BellScheduleEntity(lessonNumber = 6, startTime = "17:45", endTime = "19:15", dayOfWeek = null))

        // Type 3: Суббота (сокращенные пары и перемены)
        bellDao.insert(BellScheduleEntity(lessonNumber = 1, startTime = "09:00", endTime = "10:20", dayOfWeek = 6))
        bellDao.insert(BellScheduleEntity(lessonNumber = 2, startTime = "10:30", endTime = "11:50", dayOfWeek = 6))
        bellDao.insert(BellScheduleEntity(lessonNumber = 3, startTime = "12:10", endTime = "13:30", dayOfWeek = 6))
        bellDao.insert(BellScheduleEntity(lessonNumber = 4, startTime = "13:40", endTime = "15:00", dayOfWeek = 6))

        // 3. Current Week
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_WEEK, 6)
        val endDate = sdf.format(cal.time)

        val weekId = weekDao.insert(
            WeekScheduleEntity(
                title = "Учебная неделя 1",
                startDate = startDate,
                endDate = endDate
            )
        )

        // 4. Lessons for the week
        // Mon: Math (1), Programming (2), Window, Physics (4)
        val l1 = lessonDao.insert(LessonEntity(weekScheduleId = weekId, dayOfWeek = 1, lessonNumber = 1, subjectId = mathId, building = "Главный корпус", floor = 3, roomNumber = "312", lessonType = "LECTURE", mapSchemeNote = "Главный вход -> Центральная лестница -> 3 этаж направо"))
        val l2 = lessonDao.insert(LessonEntity(weekScheduleId = weekId, dayOfWeek = 1, lessonNumber = 2, subjectId = csId, building = "ИТ-корпус", floor = 4, roomNumber = "408", lessonType = "PRACTICE", mapSchemeNote = "ИТ-корпус -> Лифт -> 4 этаж налево"))
        val l3 = lessonDao.insert(LessonEntity(weekScheduleId = weekId, dayOfWeek = 1, lessonNumber = 4, subjectId = physicsId, building = "Физкорпус", floor = 2, roomNumber = "204", lessonType = "SEMINAR"))

        // Tue: English (1), DB (2), Programming (3)
        lessonDao.insert(LessonEntity(weekScheduleId = weekId, dayOfWeek = 2, lessonNumber = 1, subjectId = engId, building = "Главный корпус", floor = 2, roomNumber = "215", lessonType = "SEMINAR"))
        lessonDao.insert(LessonEntity(weekScheduleId = weekId, dayOfWeek = 2, lessonNumber = 2, subjectId = dbId, building = "ИТ-корпус", floor = 3, roomNumber = "302", lessonType = "LAB"))
        lessonDao.insert(LessonEntity(weekScheduleId = weekId, dayOfWeek = 2, lessonNumber = 3, subjectId = csId, building = "ИТ-корпус", floor = 4, roomNumber = "408", lessonType = "PRACTICE"))

        // Wed: Math (2), DB (3)
        lessonDao.insert(LessonEntity(weekScheduleId = weekId, dayOfWeek = 3, lessonNumber = 2, subjectId = mathId, building = "Главный корпус", floor = 3, roomNumber = "312", lessonType = "PRACTICE"))
        lessonDao.insert(LessonEntity(weekScheduleId = weekId, dayOfWeek = 3, lessonNumber = 3, subjectId = dbId, building = "ИТ-корпус", floor = 3, roomNumber = "302", lessonType = "LECTURE"))

        // Thu: Physics (1), English (2), Programming (4)
        lessonDao.insert(LessonEntity(weekScheduleId = weekId, dayOfWeek = 4, lessonNumber = 1, subjectId = physicsId, building = "Физкорпус", floor = 2, roomNumber = "204", lessonType = "LECTURE"))
        lessonDao.insert(LessonEntity(weekScheduleId = weekId, dayOfWeek = 4, lessonNumber = 2, subjectId = engId, building = "Главный корпус", floor = 2, roomNumber = "215", lessonType = "SEMINAR"))
        lessonDao.insert(LessonEntity(weekScheduleId = weekId, dayOfWeek = 4, lessonNumber = 4, subjectId = csId, building = "ИТ-корпус", floor = 4, roomNumber = "408", lessonType = "LAB"))

        // Fri: Math (2), Physics (3)
        lessonDao.insert(LessonEntity(weekScheduleId = weekId, dayOfWeek = 5, lessonNumber = 2, subjectId = mathId, building = "Главный корпус", floor = 3, roomNumber = "312", lessonType = "SEMINAR"))
        lessonDao.insert(LessonEntity(weekScheduleId = weekId, dayOfWeek = 5, lessonNumber = 3, subjectId = physicsId, building = "Физкорпус", floor = 2, roomNumber = "204", lessonType = "LAB"))

        // 5. Sample Homework
        val deadlineCal = Calendar.getInstance()
        deadlineCal.add(Calendar.DAY_OF_MONTH, 3)
        val deadlineStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(deadlineCal.time)
        homeworkDao.insert(HomeworkEntity(subjectId = mathId, lessonId = l1, text = "Решить задачи №45-52 из сборника Демидовича", deadline = deadlineStr, status = "IN_PROGRESS"))
        homeworkDao.insert(HomeworkEntity(subjectId = csId, lessonId = l2, text = "Разработать Room DAO и ViewModel для проекта", deadline = deadlineStr, status = "NOT_STARTED"))

        // 6. Sample Notes
        noteDao.insert(NoteEntity(subjectId = mathId, lessonId = l1, text = "Лектор подчеркнул, что теорема Коши будет в 100% билетов на экзамене!", tagsCsv = "экзамен,важно,лектор акцентировал"))
        noteDao.insert(NoteEntity(subjectId = csId, lessonId = l2, text = "Использовать StateFlow и collectAsStateWithLifecycle для Compose UI", tagsCsv = "важно,код"))

        // 7. Sample Grades
        gradeDao.insert(GradeEntity(subjectId = mathId, lessonId = l1, gradeType = "FIVE_POINT", numericValue = 5.0, date = startDate, category = "TEST"))
        gradeDao.insert(GradeEntity(subjectId = csId, lessonId = l2, gradeType = "FIVE_POINT", numericValue = 5.0, date = startDate, category = "REGULAR"))
        gradeDao.insert(GradeEntity(subjectId = engId, gradeType = "PASS_FAIL", isPassed = true, date = startDate, category = "CREDIT"))

        // 8. Sample Exam Session
        val examCal = Calendar.getInstance()
        examCal.add(Calendar.DAY_OF_MONTH, 14)
        val examDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(examCal.time)
        examDao.insert(ExamSessionEntity(subjectId = csId, examDate = examDateStr, examTime = "09:00", examType = "EXAM", ticketNumber = "7", readinessPercent = 75, prepNotes = "Повторить архитектуру Jetpack Compose и корутины"))
        examCal.add(Calendar.DAY_OF_MONTH, 5)
        examDao.insert(ExamSessionEntity(subjectId = mathId, examDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(examCal.time), examTime = "10:00", examType = "EXAM", ticketNumber = "12", readinessPercent = 50, prepNotes = "Интегралы и ряды Тейлора"))
        examCal.add(Calendar.DAY_OF_MONTH, 4)
        examDao.insert(ExamSessionEntity(subjectId = engId, examDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(examCal.time), examTime = "11:30", examType = "CREDIT", ticketNumber = "", readinessPercent = 90, prepNotes = "Презентация научного проекта на английском"))
    }

    // Full Database Export / Import Models
    suspend fun getFullDatabaseSnapshot(): DatabaseBackupSnapshot = withContext(Dispatchers.IO) {
        DatabaseBackupSnapshot(
            schemaVersion = 1,
            exportTimestamp = System.currentTimeMillis(),
            subjects = subjectDao.getAllSubjects(),
            bellSchedules = bellDao.getAllBells(),
            weekSchedules = weekDao.getAllWeeks(),
            lessons = lessonDao.getAllLessons(),
            homeworks = homeworkDao.getAllHomeworks(),
            notes = noteDao.getAllNotes(),
            grades = gradeDao.getAllGrades(),
            examSessions = examDao.getAllExams()
        )
    }

    suspend fun restoreDatabaseSnapshot(snapshot: DatabaseBackupSnapshot, replaceAll: Boolean) = withContext(Dispatchers.IO) {
        if (replaceAll) {
            subjectDao.clear()
            bellDao.clear()
            weekDao.clear()
            lessonDao.clear()
            homeworkDao.clear()
            noteDao.clear()
            gradeDao.clear()
            examDao.clear()
        }
        subjectDao.insertAll(snapshot.subjects)
        bellDao.insertAll(snapshot.bellSchedules)
        weekDao.insertAll(snapshot.weekSchedules)
        lessonDao.insertAll(snapshot.lessons)
        homeworkDao.insertAll(snapshot.homeworks)
        noteDao.insertAll(snapshot.notes)
        gradeDao.insertAll(snapshot.grades)
        examDao.insertAll(snapshot.examSessions)
    }
}

@JsonClass(generateAdapter = true)
data class DatabaseBackupSnapshot(
    val schemaVersion: Int = 1,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val subjects: List<SubjectEntity> = emptyList(),
    val bellSchedules: List<BellScheduleEntity> = emptyList(),
    val weekSchedules: List<WeekScheduleEntity> = emptyList(),
    val lessons: List<LessonEntity> = emptyList(),
    val homeworks: List<HomeworkEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val grades: List<GradeEntity> = emptyList(),
    val examSessions: List<ExamSessionEntity> = emptyList()
)
