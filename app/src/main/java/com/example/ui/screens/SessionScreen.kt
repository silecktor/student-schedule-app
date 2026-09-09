package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExamSessionEntity
import com.example.ui.dialogs.EditExamDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(viewModel: MainViewModel) {
    val exams by viewModel.allExams.collectAsState()
    val subjects by viewModel.allSubjects.collectAsState()
    val overallGpa by viewModel.overallGpa.collectAsState()
    val scholarshipThreshold by viewModel.preferences.scholarshipThreshold.collectAsState()

    var showAddExamDialog by remember { mutableStateOf(false) }
    var examToEdit by remember { mutableStateOf<ExamSessionEntity?>(null) }
    var showTargetCalculator by remember { mutableStateOf(false) }

    val subjectsMap = subjects.associateBy { it.id }

    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val now = System.currentTimeMillis()

    val sortedExams = exams.sortedBy { exam ->
        try {
            sdf.parse("${exam.examDate} ${exam.examTime}")?.time ?: Long.MAX_VALUE
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
    }

    val nextExam = sortedExams.firstOrNull { exam ->
        val time = try { sdf.parse("${exam.examDate} ${exam.examTime}")?.time ?: 0L } catch (e: Exception) { 0L }
        time >= now
    }

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
                        text = "Сессия и экзамены",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Зачёты, экзамены и подготовка",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (showTargetCalculator) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { showTargetCalculator = !showTargetCalculator },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Calculate,
                        contentDescription = "Калькулятор",
                        tint = if (showTargetCalculator) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExamDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить экзамен")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Next Exam Bento Hero Card
            if (nextExam != null) {
                item {
                    val sub = subjectsMap[nextExam.subjectId]
                    val examTime = try { sdf.parse("${nextExam.examDate} ${nextExam.examTime}")?.time ?: now } catch (e: Exception) { now }
                    val diff = (examTime - now).coerceAtLeast(0L)
                    val days = diff / (1000 * 60 * 60 * 24)
                    val hours = (diff / (1000 * 60 * 60)) % 24

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
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = BentoHeroOnLavender.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "БЛИЖАЙШИЙ ЭКЗАМЕН",
                                        color = BentoHeroOnLavender,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    "${nextExam.examDate} в ${nextExam.examTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoHeroOnLavender
                                )
                            }

                            Text(
                                text = sub?.name ?: "Экзамен",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = BentoHeroOnLavender,
                                fontSize = 22.sp
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("⏳", fontSize = 16.sp)
                                Text(
                                    text = if (days > 0) "Осталось: $days д $hours ч" else "Осталось: $hours ч",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoHeroOnLavender
                                )
                            }
                        }
                    }
                }
            }

            // Scholarship / Target Calculator Bento Card
            if (showTargetCalculator) {
                item {
                    BentoTargetGpaCalculatorCard(currentGpa = overallGpa, targetGpa = scholarshipThreshold)
                }
            }

            item {
                Text(
                    text = "Список зачётов и экзаменов (${exams.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (exams.isEmpty()) {
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🎓", fontSize = 40.sp)
                            Text("Сессия пока не заполнена", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Нажмите «+», чтобы внести экзамены, зачёты и курсовые", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(sortedExams) { exam ->
                    val sub = subjectsMap[exam.subjectId]
                    val color = try {
                        Color(android.graphics.Color.parseColor(sub?.colorHex ?: "#D0BCFF"))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    val typeRu = when (exam.examType) {
                        "EXAM" -> "Экзамен"
                        "CREDIT" -> "Зачёт"
                        "COURSEWORK" -> "Курсовая"
                        "DIPLOMA" -> "Диплом"
                        else -> "Контроль"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .clickable { examToEdit = exam }
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                                    Text(sub?.name ?: "Предмет", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Text(typeRu, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${exam.examDate} в ${exam.examTime}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                if (exam.ticketNumber.isNotBlank()) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = BentoHeroLavender.copy(alpha = 0.2f)) {
                                        Text("Билет №${exam.ticketNumber}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                }
                            }

                            // Readiness Progress Bar
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Готовность к сдаче", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${exam.readinessPercent}%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (exam.readinessPercent >= 70) EmeraldAccent else if (exam.readinessPercent >= 40) AmberAccent else RoseAccent
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { exam.readinessPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (exam.readinessPercent >= 70) EmeraldAccent else if (exam.readinessPercent >= 40) AmberAccent else RoseAccent,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }

                            if (exam.prepNotes.isNotBlank()) {
                                Text(
                                    text = "📝 ${exam.prepNotes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddExamDialog) {
        EditExamDialog(
            initialExam = null,
            subjects = subjects,
            onDismiss = { showAddExamDialog = false },
            onSave = {
                viewModel.saveExam(it)
                showAddExamDialog = false
            }
        )
    }

    if (examToEdit != null) {
        EditExamDialog(
            initialExam = examToEdit,
            subjects = subjects,
            onDismiss = { examToEdit = null },
            onSave = {
                viewModel.saveExam(it)
                examToEdit = null
            },
            onDelete = {
                viewModel.deleteExam(examToEdit!!)
                examToEdit = null
            }
        )
    }
}

@Composable
fun BentoTargetGpaCalculatorCard(currentGpa: Double, targetGpa: Double) {
    var upcomingCountText by remember { mutableStateOf("4") }
    val upcomingCount = upcomingCountText.toIntOrNull() ?: 4

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Калькулятор стипендии", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }

            Text(
                text = "Текущий балл: ${if (currentGpa > 0) String.format(Locale.getDefault(), "%.2f", currentGpa) else "—"} | Порог: $targetGpa",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = upcomingCountText,
                onValueChange = { upcomingCountText = it },
                label = { Text("Количество предстоящих экзаменов") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            val advice = if (currentGpa >= targetGpa) {
                "Отлично! Чтобы сохранить стипендию, сдавайте оставшиеся $upcomingCount экзамена на оценку не ниже 4 («хорошо»)."
            } else {
                "Для выхода на стипендию ($targetGpa) необходимо сдать все оставшиеся $upcomingCount экзамена на «отлично» (5)!"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            ) {
                Text(
                    text = "💡 $advice",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
