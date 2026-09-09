package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjectsFlow(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    suspend fun getAllSubjects(): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subject: SubjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subjects: List<SubjectEntity>)

    @Update
    suspend fun update(subject: SubjectEntity)

    @Delete
    suspend fun delete(subject: SubjectEntity)

    @Query("DELETE FROM subjects")
    suspend fun clear()
}

@Dao
interface BellScheduleDao {
    @Query("SELECT * FROM bell_schedules ORDER BY lessonNumber ASC")
    fun getAllBellsFlow(): Flow<List<BellScheduleEntity>>

    @Query("SELECT * FROM bell_schedules ORDER BY lessonNumber ASC")
    suspend fun getAllBells(): List<BellScheduleEntity>

    @Query("SELECT * FROM bell_schedules WHERE dayOfWeek = :dayOfWeek OR (dayOfWeek IS NULL AND :dayOfWeek NOT IN (SELECT DISTINCT dayOfWeek FROM bell_schedules WHERE dayOfWeek IS NOT NULL)) ORDER BY lessonNumber ASC")
    fun getBellsForDayFlow(dayOfWeek: Int): Flow<List<BellScheduleEntity>>

    @Query("SELECT * FROM bell_schedules WHERE dayOfWeek = :dayOfWeek OR (dayOfWeek IS NULL AND :dayOfWeek NOT IN (SELECT DISTINCT dayOfWeek FROM bell_schedules WHERE dayOfWeek IS NOT NULL)) ORDER BY lessonNumber ASC")
    suspend fun getBellsForDay(dayOfWeek: Int): List<BellScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bell: BellScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bells: List<BellScheduleEntity>)

    @Update
    suspend fun update(bell: BellScheduleEntity)

    @Delete
    suspend fun delete(bell: BellScheduleEntity)

    @Query("DELETE FROM bell_schedules")
    suspend fun clear()
}

@Dao
interface WeekScheduleDao {
    @Query("SELECT * FROM week_schedules ORDER BY startDate DESC")
    fun getAllWeeksFlow(): Flow<List<WeekScheduleEntity>>

    @Query("SELECT * FROM week_schedules ORDER BY startDate DESC")
    suspend fun getAllWeeks(): List<WeekScheduleEntity>

    @Query("SELECT * FROM week_schedules WHERE id = :id LIMIT 1")
    suspend fun getWeekById(id: Long): WeekScheduleEntity?

    @Query("SELECT * FROM week_schedules ORDER BY startDate DESC LIMIT 1")
    suspend fun getLatestWeek(): WeekScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(week: WeekScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(weeks: List<WeekScheduleEntity>)

    @Update
    suspend fun update(week: WeekScheduleEntity)

    @Delete
    suspend fun delete(week: WeekScheduleEntity)

    @Query("DELETE FROM week_schedules")
    suspend fun clear()
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE weekScheduleId = :weekScheduleId ORDER BY dayOfWeek ASC, lessonNumber ASC")
    fun getLessonsForWeekFlow(weekScheduleId: Long): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE weekScheduleId = :weekScheduleId AND dayOfWeek = :dayOfWeek ORDER BY lessonNumber ASC")
    fun getLessonsForDayFlow(weekScheduleId: Long, dayOfWeek: Int): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE weekScheduleId = :weekScheduleId ORDER BY dayOfWeek ASC, lessonNumber ASC")
    suspend fun getLessonsForWeek(weekScheduleId: Long): List<LessonEntity>

    @Query("SELECT * FROM lessons ORDER BY id ASC")
    suspend fun getAllLessons(): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE id = :id LIMIT 1")
    suspend fun getLessonById(id: Long): LessonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lesson: LessonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lessons: List<LessonEntity>)

    @Update
    suspend fun update(lesson: LessonEntity)

    @Delete
    suspend fun delete(lesson: LessonEntity)

    @Query("DELETE FROM lessons WHERE weekScheduleId = :weekScheduleId")
    suspend fun deleteForWeek(weekScheduleId: Long)

    @Query("DELETE FROM lessons")
    suspend fun clear()
}

@Dao
interface HomeworkDao {
    @Query("SELECT * FROM homeworks ORDER BY deadline ASC")
    fun getAllHomeworksFlow(): Flow<List<HomeworkEntity>>

    @Query("SELECT * FROM homeworks ORDER BY deadline ASC")
    suspend fun getAllHomeworks(): List<HomeworkEntity>

    @Query("SELECT * FROM homeworks WHERE subjectId = :subjectId ORDER BY deadline ASC")
    fun getHomeworksForSubjectFlow(subjectId: Long): Flow<List<HomeworkEntity>>

    @Query("SELECT * FROM homeworks WHERE lessonId = :lessonId")
    suspend fun getHomeworksForLesson(lessonId: Long): List<HomeworkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(homework: HomeworkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(homeworks: List<HomeworkEntity>)

    @Update
    suspend fun update(homework: HomeworkEntity)

    @Delete
    suspend fun delete(homework: HomeworkEntity)

    @Query("DELETE FROM homeworks")
    suspend fun clear()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    suspend fun getAllNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun getNotesForSubjectFlow(subjectId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE lessonId = :lessonId")
    suspend fun getNotesForLesson(lessonId: Long): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("DELETE FROM notes")
    suspend fun clear()
}

@Dao
interface GradeDao {
    @Query("SELECT * FROM grades ORDER BY date DESC")
    fun getAllGradesFlow(): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades ORDER BY date DESC")
    suspend fun getAllGrades(): List<GradeEntity>

    @Query("SELECT * FROM grades WHERE subjectId = :subjectId ORDER BY date DESC")
    fun getGradesForSubjectFlow(subjectId: Long): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades WHERE lessonId = :lessonId")
    suspend fun getGradesForLesson(lessonId: Long): List<GradeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(grade: GradeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(grades: List<GradeEntity>)

    @Update
    suspend fun update(grade: GradeEntity)

    @Delete
    suspend fun delete(grade: GradeEntity)

    @Query("DELETE FROM grades")
    suspend fun clear()
}

@Dao
interface ExamSessionDao {
    @Query("SELECT * FROM exam_sessions ORDER BY examDate ASC, examTime ASC")
    fun getAllExamsFlow(): Flow<List<ExamSessionEntity>>

    @Query("SELECT * FROM exam_sessions ORDER BY examDate ASC, examTime ASC")
    suspend fun getAllExams(): List<ExamSessionEntity>

    @Query("SELECT * FROM exam_sessions WHERE id = :id LIMIT 1")
    suspend fun getExamById(id: Long): ExamSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exam: ExamSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exams: List<ExamSessionEntity>)

    @Update
    suspend fun update(exam: ExamSessionEntity)

    @Delete
    suspend fun delete(exam: ExamSessionEntity)

    @Query("DELETE FROM exam_sessions")
    suspend fun clear()
}
