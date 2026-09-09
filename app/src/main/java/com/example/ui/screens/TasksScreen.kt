package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GradeEntity
import com.example.data.model.HomeworkEntity
import com.example.data.model.NoteEntity
import com.example.ui.dialogs.EditGradeDialog
import com.example.ui.dialogs.EditHomeworkDialog
import com.example.ui.dialogs.EditNoteDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: MainViewModel) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0=ДЗ, 1=Оценки, 2=Посещаемость, 3=Заметки

    val homeworks by viewModel.allHomeworks.collectAsState()
    val grades by viewModel.allGrades.collectAsState()
    val notes by viewModel.allNotes.collectAsState()
    val subjects by viewModel.allSubjects.collectAsState()
    val attendanceStats by viewModel.attendanceStats.collectAsState()
    val gradesStats by viewModel.gradesStats.collectAsState()
    val overallGpa by viewModel.overallGpa.collectAsState()
    val scholarshipThreshold by viewModel.preferences.scholarshipThreshold.collectAsState()
    val expulsionThreshold by viewModel.preferences.expulsionThreshold.collectAsState()

    // Dialog states
    var showAddHwDialog by remember { mutableStateOf(false) }
    var hwToEdit by remember { mutableStateOf<HomeworkEntity?>(null) }

    var showAddGradeDialog by remember { mutableStateOf(false) }
    var gradeToEdit by remember { mutableStateOf<GradeEntity?>(null) }

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<NoteEntity?>(null) }

    // Homework filter: 0=Все, 1=Активные, 2=Сделано
    var hwFilter by remember { mutableIntStateOf(1) }

    val subjectsMap = subjects.associateBy { it.id }

    val subTabs = listOf("ДЗ", "Оценки", "Посещаемость", "Заметки")

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Учёба и задания",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Контроль дедлайнов, успеваемости и пропусков",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedSubTab) {
                        0 -> showAddHwDialog = true
                        1 -> showAddGradeDialog = true
                        3 -> showAddNoteDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Bento Sub-Tabs Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subTabs.indices.toList()) { index ->
                    val isSelected = selectedSubTab == index
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
                            .clickable { selectedSubTab = index }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = subTabs[index],
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (selectedSubTab) {
                0 -> {
                    // ================= Homework Sub Tab =================
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Filter Pills Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val activeCount = homeworks.count { it.status != "COMPLETED" }
                            val doneCount = homeworks.count { it.status == "COMPLETED" }
                            val filterOptions = listOf(
                                0 to "Все (${homeworks.size})",
                                1 to "Активные ($activeCount)",
                                2 to "Готовые ($doneCount)"
                            )

                            filterOptions.forEach { (filterVal, label) ->
                                val isSel = hwFilter == filterVal
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { hwFilter = filterVal }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        val filteredHw = homeworks.filter {
                            when (hwFilter) {
                                1 -> it.status != "COMPLETED"
                                2 -> it.status == "COMPLETED"
                                else -> true
                            }
                        }.sortedBy { it.deadline }

                        if (filteredHw.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🎉", fontSize = 40.sp)
                                    Text(
                                        "Все задания выполнены!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Нажмите «+», чтобы добавить новое домашнее задание",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredHw) { hw ->
                                    val sub = subjectsMap[hw.subjectId]
                                    val isDone = hw.status == "COMPLETED"
                                    val color = try {
                                        Color(android.graphics.Color.parseColor(sub?.colorHex ?: "#D0BCFF"))
                                    } catch (e: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                if (isDone) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                            .clickable { hwToEdit = hw }
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.toggleHomeworkStatus(hw) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = "Статус",
                                                    tint = if (isDone) EmeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                                                    Text(sub?.name ?: "Предмет", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = color)
                                                }

                                                Text(
                                                    text = hw.text,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 15.sp,
                                                    textDecoration = if (isDone) TextDecoration.LineThrough else null,
                                                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )

                                                if (hw.deadline.isNotBlank()) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("Срок: ${hw.deadline}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = when (hw.status) {
                                                    "COMPLETED" -> EmeraldAccent.copy(alpha = 0.15f)
                                                    "IN_PROGRESS" -> AmberAccent.copy(alpha = 0.15f)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            ) {
                                                Text(
                                                    text = when (hw.status) {
                                                        "COMPLETED" -> "Сдано"
                                                        "IN_PROGRESS" -> "В работе"
                                                        else -> "Задано"
                                                    },
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (hw.status) {
                                                        "COMPLETED" -> EmeraldAccent
                                                        "IN_PROGRESS" -> AmberAccent
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    },
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // ================= Grades Sub Tab =================
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // GPA Bento Hero Card
                        item {
                            val isScholarship = overallGpa >= scholarshipThreshold && overallGpa > 0
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(BentoHeroLavender)
                                    .padding(20.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "СРЕДНИЙ БАЛЛ (GPA)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BentoHeroOnLavender.copy(alpha = 0.8f)
                                            )
                                            Text(
                                                text = if (overallGpa > 0) String.format(Locale.getDefault(), "%.2f", overallGpa) else "—",
                                                style = MaterialTheme.typography.headlineLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = BentoHeroOnLavender
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = BentoHeroOnLavender
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = if (isScholarship) "Стипендия ⭐" else "Порог: $scholarshipThreshold",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                "Оценки по предметам",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        items(gradesStats) { stat ->
                            val color = try {
                                Color(android.graphics.Color.parseColor(stat.subject.colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                                            Text(stat.subject.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        if (stat.averageScore > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = BentoHeroLavender.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "Средний: ${String.format(Locale.getDefault(), "%.2f", stat.averageScore)}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (stat.grades.isNotEmpty()) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            stat.grades.take(6).forEach { gr ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (gr.gradeType == "FIVE_POINT") {
                                                        if (gr.numericValue >= 4) EmeraldAccent.copy(alpha = 0.2f) else if (gr.numericValue == 3.0) AmberAccent.copy(alpha = 0.2f) else RoseAccent.copy(alpha = 0.2f)
                                                    } else {
                                                        if (gr.isPassed) EmeraldAccent.copy(alpha = 0.2f) else RoseAccent.copy(alpha = 0.2f)
                                                    },
                                                    modifier = Modifier.clickable { gradeToEdit = gr }
                                                ) {
                                                    Text(
                                                        text = if (gr.gradeType == "FIVE_POINT") "${gr.numericValue.toInt()}" else if (gr.isPassed) "Зач" else "Н/з",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Text("Оценок пока нет", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // ================= Attendance Sub Tab =================
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val atRiskList = attendanceStats.filter { it.isRiskOfExpulsion }
                        if (atRiskList.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(RoseAccent.copy(alpha = 0.15f))
                                        .border(1.dp, RoseAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = RoseAccent, modifier = Modifier.size(32.dp))
                                        Column {
                                            Text("Внимание! Риск недопуска/отчисления", fontWeight = FontWeight.Bold, color = RoseAccent, fontSize = 14.sp)
                                            Text(
                                                text = "Пропуски превысили $expulsionThreshold% по ${atRiskList.size} предметам.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                "Посещаемость по предметам",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        items(attendanceStats) { stat ->
                            val color = try {
                                Color(android.graphics.Color.parseColor(stat.subject.colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                                            Text(stat.subject.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }

                                        if (stat.isRiskOfExpulsion) {
                                            Surface(shape = RoundedCornerShape(6.dp), color = RoseAccent) {
                                                Text("РИСК", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }

                                    val absenceFraction = (stat.absencePercentage / 100f).coerceIn(0f, 1f)
                                    LinearProgressIndicator(
                                        progress = { absenceFraction },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                        color = if (stat.isRiskOfExpulsion) RoseAccent else MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Пропущено: ${String.format(Locale.getDefault(), "%.1f", stat.absencePercentage)}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (stat.isRiskOfExpulsion) RoseAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "Был: ${stat.attendedCount} | Н: ${stat.absentCount} | Ув: ${stat.excusedCount}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // ================= Notes Sub Tab =================
                    if (notes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("📌", fontSize = 40.sp)
                                Text("Нет сохранённых заметок", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Нажмите «+», чтобы создать заметку с тегами", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(notes) { note ->
                                val sub = subjectsMap[note.subjectId]
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                        .clickable { noteToEdit = note }
                                        .padding(14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (sub != null) {
                                            Text(sub.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        }

                                        Text(note.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)

                                        if (note.tagsList.isNotEmpty()) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                note.tagsList.forEach { tag ->
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant
                                                    ) {
                                                        Text(
                                                            "#$tag",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                }
            }
        }
    }

    // Dialogs
    if (showAddHwDialog) {
        EditHomeworkDialog(
            initialHomework = null,
            subjects = subjects,
            preselectedSubjectId = null,
            preselectedLessonId = null,
            onDismiss = { showAddHwDialog = false },
            onSave = {
                viewModel.saveHomework(it)
                showAddHwDialog = false
            }
        )
    }

    if (hwToEdit != null) {
        EditHomeworkDialog(
            initialHomework = hwToEdit,
            subjects = subjects,
            preselectedSubjectId = null,
            preselectedLessonId = null,
            onDismiss = { hwToEdit = null },
            onSave = {
                viewModel.saveHomework(it)
                hwToEdit = null
            },
            onDelete = {
                viewModel.deleteHomework(hwToEdit!!)
                hwToEdit = null
            }
        )
    }

    if (showAddGradeDialog) {
        EditGradeDialog(
            initialGrade = null,
            subjects = subjects,
            preselectedSubjectId = null,
            preselectedLessonId = null,
            onDismiss = { showAddGradeDialog = false },
            onSave = {
                viewModel.saveGrade(it)
                showAddGradeDialog = false
            }
        )
    }

    if (gradeToEdit != null) {
        EditGradeDialog(
            initialGrade = gradeToEdit,
            subjects = subjects,
            preselectedSubjectId = null,
            preselectedLessonId = null,
            onDismiss = { gradeToEdit = null },
            onSave = {
                viewModel.saveGrade(it)
                gradeToEdit = null
            },
            onDelete = {
                viewModel.deleteGrade(gradeToEdit!!)
                gradeToEdit = null
            }
        )
    }

    if (showAddNoteDialog) {
        EditNoteDialog(
            initialNote = null,
            subjects = subjects,
            preselectedSubjectId = null,
            preselectedLessonId = null,
            onDismiss = { showAddNoteDialog = false },
            onSave = {
                viewModel.saveNote(it)
                showAddNoteDialog = false
            }
        )
    }

    if (noteToEdit != null) {
        EditNoteDialog(
            initialNote = noteToEdit,
            subjects = subjects,
            preselectedSubjectId = null,
            preselectedLessonId = null,
            onDismiss = { noteToEdit = null },
            onSave = {
                viewModel.saveNote(it)
                noteToEdit = null
            },
            onDelete = {
                viewModel.deleteNote(noteToEdit!!)
                noteToEdit = null
            }
        )
    }
}
