package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.export.ExportImportManager
import com.example.ui.dialogs.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentWeek by viewModel.currentWeek.collectAsState()
    val selectedDayOfWeek by viewModel.selectedDayOfWeek.collectAsState()
    val todayLessons by viewModel.todayLessons.collectAsState()
    val freeWindows by viewModel.todayFreeWindows.collectAsState()
    val subjects by viewModel.allSubjects.collectAsState()
    val bells by viewModel.allBells.collectAsState()
    val homeworks by viewModel.allHomeworks.collectAsState()
    val overallGpa by viewModel.overallGpa.collectAsState()
    val exams by viewModel.allExams.collectAsState()

    // Dialog states
    var showAddLessonDialog by remember { mutableStateOf(false) }
    var lessonToEdit by remember { mutableStateOf<LessonEntity?>(null) }
    var showMapDialogForLesson by remember { mutableStateOf<Pair<LessonEntity, String>?>(null) }
    var homeworkPreselectLesson by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var notePreselectLesson by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var gradePreselectLesson by remember { mutableStateOf<Pair<Long, Long>?>(null) }

    val daysOfWeek = listOf(
        1 to "Понедельник",
        2 to "Вторник",
        3 to "Среда",
        4 to "Четверг",
        5 to "Пятница",
        6 to "Суббота",
        7 to "Воскресенье"
    )
    val dayShortNames = listOf(1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт", 5 to "Пт", 6 to "Сб", 7 to "Вс")

    // Current time tracking for active lesson highlight
    var currentMinutesNow by remember { mutableIntStateOf(getCurrentMinutes()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentMinutesNow = getCurrentMinutes()
            kotlinx.coroutines.delay(30000) // 30s tick
        }
    }

    // Identify active and upcoming lessons
    val sortedLessons = todayLessons.sortedBy { it.lesson.lessonNumber }
    val activeLessonItem = sortedLessons.firstOrNull { item ->
        val bell = item.bell
        val startMin = bell?.let { parseTimeToMinutes(it.startTime) } ?: -1
        val endMin = bell?.let { parseTimeToMinutes(it.endTime) } ?: -1
        startMin >= 0 && endMin >= 0 && currentMinutesNow in startMin..endMin
    }
    val nextLessonItem = if (activeLessonItem == null) {
        sortedLessons.firstOrNull { item ->
            val bell = item.bell
            val startMin = bell?.let { parseTimeToMinutes(it.startTime) } ?: -1
            startMin > currentMinutesNow
        }
    } else {
        sortedLessons.firstOrNull { it.lesson.lessonNumber > activeLessonItem.lesson.lessonNumber }
    }

    val pendingHwCount = homeworks.count { it.status != "COMPLETED" }
    val todayFormattedDate = remember {
        val sdf = SimpleDateFormat("d MMMM", Locale("ru"))
        sdf.format(Date())
    }

    Scaffold(
        topBar = {
            // Bento Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = getDayFullTitle(selectedDayOfWeek),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "$todayFormattedDate • ${currentWeek?.title ?: "Учебная неделя"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Bento Action Button (Share Day)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable {
                            val dayTitle = getDayFullTitle(selectedDayOfWeek)
                            val shareText = ExportImportManager.formatDayScheduleText(
                                dayName = dayTitle,
                                dateStr = currentWeek?.startDate ?: "",
                                lessons = todayLessons
                            )
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                putExtra(Intent.EXTRA_SUBJECT, "Расписание на $dayTitle")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Поделиться расписанием"))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Поделиться",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddLessonDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить пару")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Bento Pill Day Selector
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dayShortNames) { (dayIdx, label) ->
                    val isSelected = selectedDayOfWeek == dayIdx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { viewModel.setSelectedDayOfWeek(dayIdx) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ================= BENTO GRID DASHBOARD =================
                // 1. Hero Bento Card (Active Lesson or Next Lesson)
                if (activeLessonItem != null) {
                    item {
                        BentoActiveHeroCard(
                            item = activeLessonItem,
                            currentMinutes = currentMinutesNow,
                            onMapClick = {
                                showMapDialogForLesson = Pair(activeLessonItem.lesson, activeLessonItem.subject?.name ?: "Занятие")
                            },
                            onAddNote = {
                                notePreselectLesson = Pair(activeLessonItem.lesson.subjectId, activeLessonItem.lesson.id)
                            },
                            onEdit = { lessonToEdit = activeLessonItem.lesson }
                        )
                    }
                } else if (nextLessonItem != null) {
                    item {
                        BentoNextLessonHeroCard(
                            item = nextLessonItem,
                            onMapClick = {
                                showMapDialogForLesson = Pair(nextLessonItem.lesson, nextLessonItem.subject?.name ?: "Занятие")
                            },
                            onEdit = { lessonToEdit = nextLessonItem.lesson }
                        )
                    }
                }

                // 2. Bento Metrics Row (HW Counter, GPA, Session Countdown)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Tile 1: Homeworks
                        BentoMetricTile(
                            modifier = Modifier.weight(1f),
                            emoji = "📝",
                            title = "ДЗ",
                            value = "$pendingHwCount",
                            subtitle = if (pendingHwCount > 0) "активно" else "готово",
                            onClick = { viewModel.setSelectedTab(3) }
                        )

                        // Tile 2: GPA
                        BentoMetricTile(
                            modifier = Modifier.weight(1f),
                            emoji = "📊",
                            title = "Средний",
                            value = if (overallGpa > 0) String.format(Locale.getDefault(), "%.1f", overallGpa) else "—",
                            subtitle = "успеваемость",
                            onClick = { viewModel.setSelectedTab(3) }
                        )

                        // Tile 3: Exams
                        val upcomingExamsCount = exams.size
                        BentoMetricTile(
                            modifier = Modifier.weight(1f),
                            emoji = "🎓",
                            title = "Сессия",
                            value = "$upcomingExamsCount",
                            subtitle = "испытаний",
                            onClick = { viewModel.setSelectedTab(4) }
                        )
                    }
                }

                // Section header for today's schedule
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Расписание дня (${todayLessons.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // Empty state if no lessons
                if (todayLessons.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("☀️", fontSize = 40.sp)
                                Text(
                                    text = "На этот день пар нет",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Отдыхайте или добавьте занятие кнопкой ниже",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { showAddLessonDialog = true },
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Добавить занятие")
                                }
                            }
                        }
                    }
                } else {
                    val windowsMap = freeWindows.associateBy { it.afterLessonNumber }

                    items(sortedLessons) { item ->
                        val bell = item.bell
                        val startMin = bell?.let { parseTimeToMinutes(it.startTime) } ?: 0
                        val endMin = bell?.let { parseTimeToMinutes(it.endTime) } ?: 0
                        val isCurrentActive = currentMinutesNow in startMin..endMin

                        BentoLessonCardItem(
                            item = item,
                            isActive = isCurrentActive,
                            currentMinutes = currentMinutesNow,
                            startMinutes = startMin,
                            endMinutes = endMin,
                            onEdit = { lessonToEdit = item.lesson },
                            onMapClick = {
                                showMapDialogForLesson = Pair(item.lesson, item.subject?.name ?: "Занятие")
                            },
                            onAttendanceChange = { newStatus ->
                                viewModel.setAttendance(item.lesson.id, newStatus)
                            },
                            onAddHomework = {
                                homeworkPreselectLesson = Pair(item.lesson.subjectId, item.lesson.id)
                            },
                            onAddNote = {
                                notePreselectLesson = Pair(item.lesson.subjectId, item.lesson.id)
                            },
                            onAddGrade = {
                                gradePreselectLesson = Pair(item.lesson.subjectId, item.lesson.id)
                            }
                        )

                        // Check if there is a free window after this lesson
                        val window = windowsMap[item.lesson.lessonNumber]
                        if (window != null) {
                            BentoFreeWindowCard(window = window)
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddLessonDialog && currentWeek != null) {
        EditLessonDialog(
            initialLesson = null,
            weekId = currentWeek!!.id,
            defaultDayOfWeek = selectedDayOfWeek,
            subjects = subjects,
            bells = bells,
            onDismiss = { showAddLessonDialog = false },
            onSave = {
                viewModel.saveLesson(it)
                showAddLessonDialog = false
            }
        )
    }

    if (lessonToEdit != null && currentWeek != null) {
        EditLessonDialog(
            initialLesson = lessonToEdit,
            weekId = currentWeek!!.id,
            defaultDayOfWeek = selectedDayOfWeek,
            subjects = subjects,
            bells = bells,
            onDismiss = { lessonToEdit = null },
            onSave = {
                viewModel.saveLesson(it)
                lessonToEdit = null
            },
            onDelete = {
                viewModel.deleteLesson(lessonToEdit!!)
                lessonToEdit = null
            }
        )
    }

    showMapDialogForLesson?.let { (lesson, subjectName) ->
        BuildingMapDialog(
            lesson = lesson,
            subjectName = subjectName,
            onDismiss = { showMapDialogForLesson = null }
        )
    }

    homeworkPreselectLesson?.let { (subId, lessonId) ->
        EditHomeworkDialog(
            initialHomework = null,
            subjects = subjects,
            preselectedSubjectId = subId,
            preselectedLessonId = lessonId,
            onDismiss = { homeworkPreselectLesson = null },
            onSave = {
                viewModel.saveHomework(it)
                homeworkPreselectLesson = null
            }
        )
    }

    notePreselectLesson?.let { (subId, lessonId) ->
        EditNoteDialog(
            initialNote = null,
            subjects = subjects,
            preselectedSubjectId = subId,
            preselectedLessonId = lessonId,
            onDismiss = { notePreselectLesson = null },
            onSave = {
                viewModel.saveNote(it)
                notePreselectLesson = null
            }
        )
    }

    gradePreselectLesson?.let { (subId, lessonId) ->
        EditGradeDialog(
            initialGrade = null,
            subjects = subjects,
            preselectedSubjectId = subId,
            preselectedLessonId = lessonId,
            onDismiss = { gradePreselectLesson = null },
            onSave = {
                viewModel.saveGrade(it)
                gradePreselectLesson = null
            }
        )
    }
}

/**
 * Bento Grid Hero Card for the currently ongoing active lesson (Lavender hero card)
 */
@Composable
fun BentoActiveHeroCard(
    item: LessonWithDetails,
    currentMinutes: Int,
    onMapClick: () -> Unit,
    onAddNote: () -> Unit,
    onEdit: () -> Unit
) {
    val subject = item.subject
    val bell = item.bell
    val lesson = item.lesson

    val startMin = bell?.let { parseTimeToMinutes(it.startTime) } ?: 0
    val endMin = bell?.let { parseTimeToMinutes(it.endTime) } ?: 0
    val progress = if (endMin > startMin) {
        ((currentMinutes - startMin).toFloat() / (endMin - startMin).toFloat()).coerceIn(0f, 1f)
    } else 0f
    val remainMinutes = (endMin - currentMinutes).coerceAtLeast(0)

    val typeRu = when (lesson.lessonType) {
        "LECTURE" -> "Лекция"
        "PRACTICE" -> "Практика"
        "SEMINAR" -> "Семинар"
        "LAB" -> "Лабораторная"
        else -> "Пара"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(BentoHeroLavender)
            .clickable { onEdit() }
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Top row: Pill badge & time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BentoHeroOnLavender.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "ИДЁТ СЕЙЧАС",
                            color = BentoHeroOnLavender,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = subject?.name ?: "Текущее занятие",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BentoHeroOnLavender,
                        fontSize = 22.sp
                    )
                    Text(
                        text = "$typeRu • ${subject?.teacher?.ifBlank { "Преподаватель" } ?: "Преподаватель"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BentoHeroOnLavender.copy(alpha = 0.8f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = bell?.startTime ?: "—",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = BentoHeroOnLavender
                    )
                    Text(
                        text = "— ${bell?.endTime ?: "—"}",
                        fontSize = 13.sp,
                        color = BentoHeroOnLavender.copy(alpha = 0.7f)
                    )
                }
            }

            // Progress bar and remain time
            Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Осталось $remainMinutes мин",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoHeroOnLavender
                    )
                    val roomText = if (lesson.roomNumber.isNotBlank()) {
                        "Ауд. ${lesson.roomNumber}${if (lesson.building.isNotBlank()) " (${lesson.building})" else ""}"
                    } else "Аудитория не указана"
                    Text(
                        text = roomText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoHeroOnLavender
                    )
                }

                // Custom Bento Progress Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(BentoHeroOnLavender.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress.coerceAtLeast(0.04f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(BentoHeroOnLavender)
                    )
                }
            }

            // Action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onMapClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BentoHeroOnLavender,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text("Схема корпуса", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onAddNote,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BentoHeroOnLavender
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BentoHeroOnLavender.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("Заметки", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Bento Grid Next Lesson Hero Card
 */
@Composable
fun BentoNextLessonHeroCard(
    item: LessonWithDetails,
    onMapClick: () -> Unit,
    onEdit: () -> Unit
) {
    val subject = item.subject
    val bell = item.bell
    val lesson = item.lesson

    val typeRu = when (lesson.lessonType) {
        "LECTURE" -> "Лекция"
        "PRACTICE" -> "Практика"
        "SEMINAR" -> "Семинар"
        "LAB" -> "Лабораторная"
        else -> "Пара"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(BentoDarkSurfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            .clickable { onEdit() }
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "СЛЕДУЮЩАЯ ПАРА",
                    color = BentoHeroLavender,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${bell?.startTime ?: "—"} – ${bell?.endTime ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = subject?.name ?: "Занятие",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$typeRu • ${subject?.teacher ?: "Преподаватель"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = if (lesson.roomNumber.isNotBlank()) "Ауд. ${lesson.roomNumber}" else "Ауд. —",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = BentoHeroLavender,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bento Metric Tile Card (for ДЗ, GPA, Exam stats)
 */
@Composable
fun BentoMetricTile(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    value: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Bento Lesson Card Item in regular schedule list
 */
@Composable
fun BentoLessonCardItem(
    item: LessonWithDetails,
    isActive: Boolean,
    currentMinutes: Int,
    startMinutes: Int,
    endMinutes: Int,
    onEdit: () -> Unit,
    onMapClick: () -> Unit,
    onAttendanceChange: (String) -> Unit,
    onAddHomework: () -> Unit,
    onAddNote: () -> Unit,
    onAddGrade: () -> Unit
) {
    val subject = item.subject
    val bell = item.bell
    val lesson = item.lesson

    val subjectColor = try {
        Color(android.graphics.Color.parseColor(subject?.colorHex ?: "#D0BCFF"))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val typeColor = when (lesson.lessonType) {
        "LECTURE" -> LectureColor
        "PRACTICE" -> PracticeColor
        "SEMINAR" -> SeminarColor
        "LAB" -> LabColor
        else -> MaterialTheme.colorScheme.primary
    }

    val typeRu = when (lesson.lessonType) {
        "LECTURE" -> "Лекция"
        "PRACTICE" -> "Практика"
        "SEMINAR" -> "Семинар"
        "LAB" -> "Лабораторная"
        else -> "Пара"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable { onEdit() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Dot + Number, Times, Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(subjectColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${lesson.lessonNumber}",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = if (bell != null) "${bell.startTime} – ${bell.endTime}" else "Пара №${lesson.lessonNumber}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = typeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = typeRu,
                        color = typeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Subject Name
            Text(
                text = subject?.name ?: "Предмет не выбран",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp
            )

            // Teacher Name
            if (!subject?.teacher.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = subject!!.teacher,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Room and Navigation Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (lesson.roomNumber.isNotBlank()) "Ауд. ${lesson.roomNumber}" else "Ауд. не указана",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (lesson.building.isNotBlank()) {
                    Text(
                        text = "${lesson.building}, эт. ${lesson.floor}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (lesson.building.isNotBlank() || !lesson.mapSchemeNote.isNullOrBlank()) {
                    FilledTonalButton(
                        onClick = onMapClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Как пройти", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Attached chips (HW, Notes, Grades count)
            if (item.homeworks.isNotEmpty() || item.notes.isNotEmpty() || item.grades.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (item.homeworks.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "📝 ДЗ: ${item.homeworks.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (item.notes.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "📌 Заметки: ${item.notes.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Divider & Bottom Actions: Attendance chips + Quick Add
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attendance Chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BentoAttendanceChip(
                        label = "Был",
                        isSelected = lesson.attendance == "ATTENDED",
                        color = EmeraldAccent,
                        onClick = { onAttendanceChange(if (lesson.attendance == "ATTENDED") "UNSET" else "ATTENDED") }
                    )
                    BentoAttendanceChip(
                        label = "Н",
                        isSelected = lesson.attendance == "ABSENT",
                        color = RoseAccent,
                        onClick = { onAttendanceChange(if (lesson.attendance == "ABSENT") "UNSET" else "ABSENT") }
                    )
                    BentoAttendanceChip(
                        label = "Ув.",
                        isSelected = lesson.attendance == "EXCUSED",
                        color = AmberAccent,
                        onClick = { onAttendanceChange(if (lesson.attendance == "EXCUSED") "UNSET" else "EXCUSED") }
                    )
                }

                // Quick Add Popups
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onAddHomework, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Assignment, contentDescription = "Добавить ДЗ", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onAddNote, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.EditNote, contentDescription = "Добавить заметку", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onAddGrade, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.StarBorder, contentDescription = "Поставить оценку", tint = AmberAccent, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BentoAttendanceChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp
        )
    }
}

/**
 * Bento Free Window Pill Banner (e.g. ☕ Окно • 45 мин • до 12:30)
 */
@Composable
fun BentoFreeWindowCard(window: FreeWindowItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("☕", fontSize = 18.sp)
                Text(
                    text = "Окно • ${formatMinutesDuration(window.durationMinutes)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "до ${window.endTime}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

private fun getCurrentMinutes(): Int {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
}

private fun formatMinutesDuration(minutes: Long): String {
    val hours = minutes / 60
    val rem = minutes % 60
    return if (hours > 0) {
        if (rem > 0) "$hours ч $rem мин" else "$hours ч"
    } else {
        "$rem мин"
    }
}

private fun getDayFullTitle(day: Int): String {
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
