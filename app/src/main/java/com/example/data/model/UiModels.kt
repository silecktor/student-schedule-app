package com.example.data.model

data class LessonWithDetails(
    val lesson: LessonEntity,
    val subject: SubjectEntity?,
    val bell: BellScheduleEntity?,
    val homeworks: List<HomeworkEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val grades: List<GradeEntity> = emptyList()
)

data class FreeWindowItem(
    val afterLessonNumber: Int,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Long
)

data class AttendanceSubjectStats(
    val subject: SubjectEntity,
    val totalLessons: Int,
    val attendedCount: Int,
    val absentCount: Int,
    val excusedCount: Int,
    val absencePercentage: Float, // 0..100
    val isRiskOfExpulsion: Boolean
)

data class SubjectGradeStats(
    val subject: SubjectEntity,
    val grades: List<GradeEntity>,
    val averageScore: Double,
    val passCount: Int,
    val failCount: Int
)

data class SearchResultItem(
    val type: SearchCategory,
    val title: String,
    val subtitle: String,
    val dateOrTime: String,
    val extraInfo: String = "",
    val entityId: Long
)

enum class SearchCategory(val titleRu: String) {
    SUBJECT("Предметы"),
    LESSON("Занятия"),
    HOMEWORK("Домашние задания"),
    NOTE("Заметки"),
    GRADE("Оценки"),
    EXAM("Экзамены / Зачёты")
}
