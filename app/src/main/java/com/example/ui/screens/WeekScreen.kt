package com.example.ui.screens

import android.content.Intent
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
import com.example.data.model.BellScheduleEntity
import com.example.data.model.LessonEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.WeekScheduleEntity
import com.example.export.ExportImportManager
import com.example.ui.dialogs.CreateWeekDialog
import com.example.ui.dialogs.EditLessonDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allWeeks by viewModel.allWeeks.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()
    val subjects by viewModel.allSubjects.collectAsState()
    val bells by viewModel.allBells.collectAsState()

    var isGridView by remember { mutableStateOf(false) }
    var showCreateWeekDialog by remember { mutableStateOf(false) }
    var lessonToEdit by remember { mutableStateOf<LessonEntity?>(null) }
    var addLessonForDay by remember { mutableStateOf<Int?>(null) }

    val db = remember { com.example.data.db.AppDatabase.getDatabase(context) }
    var weekLessons by remember { mutableStateOf<List<LessonEntity>>(emptyList()) }

    LaunchedEffect(currentWeek) {
        if (currentWeek != null) {
            db.lessonDao().getLessonsForWeekFlow(currentWeek!!.id).collect {
                weekLessons = it
            }
        } else {
            weekLessons = emptyList()
        }
    }

    val daysOfWeek = listOf(
        1 to "Понедельник",
        2 to "Вторник",
        3 to "Среда",
        4 to "Четверг",
        5 to "Пятница",
        6 to "Суббота",
        7 to "Воскресенье"
    )

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Расписание недели",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = currentWeek?.title ?: "Выбор недели",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Switch view icon button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .clickable { isGridView = !isGridView },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                            contentDescription = "Вид",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Export .ics
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .clickable {
                                if (currentWeek != null) {
                                    coroutineScope.launch {
                                        val subjectsMap = subjects.associateBy { it.id }
                                        val detailed = weekLessons.map { l ->
                                            val daySpecificBells = bells.filter { it.dayOfWeek == l.dayOfWeek }
                                            val effectiveBells = if (daySpecificBells.isNotEmpty()) daySpecificBells else bells.filter { it.dayOfWeek == null }
                                            val matchedBell = effectiveBells.find { it.lessonNumber == l.lessonNumber } ?: bells.find { it.lessonNumber == l.lessonNumber }
                                            com.example.data.model.LessonWithDetails(
                                                lesson = l,
                                                subject = subjectsMap[l.subjectId],
                                                bell = matchedBell,
                                                homeworks = emptyList(),
                                                notes = emptyList(),
                                                grades = emptyList()
                                            )
                                        }
                                        val intent = ExportImportManager.exportWeekToIcs(context, currentWeek!!, detailed)
                                        context.startActivity(Intent.createChooser(intent, "Экспорт в iCalendar"))
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Экспорт",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Add week button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { showCreateWeekDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Создать неделю",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Bento Week Selector Pills
            if (allWeeks.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allWeeks) { week ->
                        val isSelected = currentWeek?.id == week.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.setSelectedWeekId(week.id) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                                }
                                Text(
                                    text = week.title,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            if (currentWeek == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("📅", fontSize = 48.sp)
                        Text("Нет созданных недель", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { showCreateWeekDialog = true },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Создать учебную неделю")
                        }
                    }
                }
            } else {
                if (isGridView) {
                    // Bento Grid / Horizontal Day Columns
                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(daysOfWeek) { (dayNum, dayName) ->
                            val dayLessons = weekLessons.filter { it.dayOfWeek == dayNum }.sortedBy { it.lessonNumber }
                            BentoDayGridColumn(
                                dayName = dayName,
                                dayNumber = dayNum,
                                lessons = dayLessons,
                                subjects = subjects,
                                bells = bells,
                                onAddLesson = { addLessonForDay = dayNum },
                                onEditLesson = { lessonToEdit = it }
                            )
                        }
                    }
                } else {
                    // Bento List View (Day Cards)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(daysOfWeek) { (dayNum, dayName) ->
                            val dayLessons = weekLessons.filter { it.dayOfWeek == dayNum }.sortedBy { it.lessonNumber }
                            BentoDaySectionCard(
                                dayName = dayName,
                                dayNumber = dayNum,
                                lessons = dayLessons,
                                subjects = subjects,
                                bells = bells,
                                onAddLesson = { addLessonForDay = dayNum },
                                onEditLesson = { lessonToEdit = it }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateWeekDialog) {
        CreateWeekDialog(
            existingWeeks = allWeeks,
            onDismiss = { showCreateWeekDialog = false },
            onCreate = { title, start, end, copyFromId ->
                viewModel.createWeek(title, start, end, copyFromId)
                showCreateWeekDialog = false
            }
        )
    }

    if (addLessonForDay != null && currentWeek != null) {
        EditLessonDialog(
            initialLesson = null,
            weekId = currentWeek!!.id,
            defaultDayOfWeek = addLessonForDay!!,
            subjects = subjects,
            bells = bells,
            onDismiss = { addLessonForDay = null },
            onSave = {
                viewModel.saveLesson(it)
                addLessonForDay = null
            }
        )
    }

    if (lessonToEdit != null && currentWeek != null) {
        EditLessonDialog(
            initialLesson = lessonToEdit,
            weekId = currentWeek!!.id,
            defaultDayOfWeek = lessonToEdit!!.dayOfWeek,
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
}

@Composable
fun BentoDaySectionCard(
    dayName: String,
    dayNumber: Int,
    lessons: List<LessonEntity>,
    subjects: List<SubjectEntity>,
    bells: List<BellScheduleEntity>,
    onAddLesson: () -> Unit,
    onEditLesson: (LessonEntity) -> Unit
) {
    val subjectsMap = subjects.associateBy { it.id }
    val daySpecificBells = bells.filter { it.dayOfWeek == dayNumber }
    val effectiveBells = if (daySpecificBells.isNotEmpty()) daySpecificBells else bells.filter { it.dayOfWeek == null }
    val bellsMap = effectiveBells.associateBy { it.lessonNumber }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BentoHeroLavender.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${lessons.size} ${if (lessons.size == 1) "пара" else "пар"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onAddLesson,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.AddCircleOutline,
                        contentDescription = "Добавить пару",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (lessons.isEmpty()) {
                Text(
                    text = "Занятий нет — свободный день",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    lessons.forEach { lesson ->
                        val sub = subjectsMap[lesson.subjectId]
                        val bell = bellsMap[lesson.lessonNumber]
                        val color = try {
                            Color(android.graphics.Color.parseColor(sub?.colorHex ?: "#D0BCFF"))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                .clickable { onEditLesson(lesson) }
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Text(
                                    text = "№${lesson.lessonNumber}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (bell != null) {
                                    Text(
                                        text = "${bell.startTime}–${bell.endTime}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sub?.name ?: "Предмет",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (lesson.roomNumber.isNotBlank()) {
                                        Text(
                                            text = "Ауд. ${lesson.roomNumber} (${lesson.building})",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BentoDayGridColumn(
    dayName: String,
    dayNumber: Int,
    lessons: List<LessonEntity>,
    subjects: List<SubjectEntity>,
    bells: List<BellScheduleEntity>,
    onAddLesson: () -> Unit,
    onEditLesson: (LessonEntity) -> Unit
) {
    val subjectsMap = subjects.associateBy { it.id }
    val daySpecificBells = bells.filter { it.dayOfWeek == dayNumber }
    val effectiveBells = if (daySpecificBells.isNotEmpty()) daySpecificBells else bells.filter { it.dayOfWeek == null }
    val bellsMap = effectiveBells.associateBy { it.lessonNumber }

    Box(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onAddLesson, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить", tint = MaterialTheme.colorScheme.primary)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            if (lessons.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Выходной", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(lessons) { lesson ->
                        val sub = subjectsMap[lesson.subjectId]
                        val bell = bellsMap[lesson.lessonNumber]
                        val color = try {
                            Color(android.graphics.Color.parseColor(sub?.colorHex ?: "#D0BCFF"))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .clickable { onEditLesson(lesson) }
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                                        Text("№${lesson.lessonNumber}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    if (bell != null) {
                                        Text("${bell.startTime}–${bell.endTime}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text(
                                    text = sub?.name ?: "Предмет",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (lesson.roomNumber.isNotBlank()) {
                                    Text(
                                        text = "Ауд. ${lesson.roomNumber} (${lesson.building})",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
