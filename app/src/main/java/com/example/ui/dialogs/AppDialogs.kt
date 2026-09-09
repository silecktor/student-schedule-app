package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.theme.SubjectColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EditLessonDialog(
    initialLesson: LessonEntity?,
    weekId: Long,
    defaultDayOfWeek: Int,
    subjects: List<SubjectEntity>,
    bells: List<BellScheduleEntity>,
    onDismiss: () -> Unit,
    onSave: (LessonEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var selectedSubjectId by remember { mutableStateOf(initialLesson?.subjectId ?: subjects.firstOrNull()?.id ?: 0L) }
    var selectedDay by remember { mutableIntStateOf(initialLesson?.dayOfWeek ?: defaultDayOfWeek) }
    var selectedLessonNumber by remember { mutableIntStateOf(initialLesson?.lessonNumber ?: 1) }
    var building by remember { mutableStateOf(initialLesson?.building ?: "Главный корпус") }
    var floorText by remember { mutableStateOf((initialLesson?.floor ?: 1).toString()) }
    var roomNumber by remember { mutableStateOf(initialLesson?.roomNumber ?: "") }
    var lessonType by remember { mutableStateOf(initialLesson?.lessonType ?: "LECTURE") }
    var mapSchemeNote by remember { mutableStateOf(initialLesson?.mapSchemeNote ?: "") }
    var notes by remember { mutableStateOf(initialLesson?.notes ?: "") }
    var attendance by remember { mutableStateOf(initialLesson?.attendance ?: "UNSET") }

    val daysOfWeek = listOf(
        1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт", 5 to "Пт", 6 to "Сб", 7 to "Вс"
    )

    val lessonTypes = listOf(
        "LECTURE" to "Лекция",
        "PRACTICE" to "Практика",
        "SEMINAR" to "Семинар",
        "LAB" to "Лабораторная"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialLesson == null) "Добавить занятие" else "Редактировать занятие",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (initialLesson != null && onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Subject Selection
                Text("Предмет", style = MaterialTheme.typography.labelLarge)
                if (subjects.isEmpty()) {
                    Text("Сначала добавьте предметы в Настройках", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else {
                    var expandedSubject by remember { mutableStateOf(false) }
                    val currentSubject = subjects.find { it.id == selectedSubjectId } ?: subjects.first()
                    OutlinedCard(
                        onClick = { expandedSubject = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(try { Color(android.graphics.Color.parseColor(currentSubject.colorHex)) } catch (e: Exception) { Color.Gray })
                                )
                                Text(currentSubject.name, fontWeight = FontWeight.Medium)
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = expandedSubject,
                            onDismissRequest = { expandedSubject = false }
                        ) {
                            subjects.forEach { subj ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(try { Color(android.graphics.Color.parseColor(subj.colorHex)) } catch (e: Exception) { Color.Gray })
                                            )
                                            Text(subj.name)
                                        }
                                    },
                                    onClick = {
                                        selectedSubjectId = subj.id
                                        expandedSubject = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Day of Week
                Text("День недели", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysOfWeek.forEach { (day, label) ->
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = { Text(label) }
                        )
                    }
                }

                // Lesson Number
                Text("Номер пары", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..6).forEach { num ->
                        FilterChip(
                            selected = selectedLessonNumber == num,
                            onClick = { selectedLessonNumber = num },
                            label = { Text("№$num") }
                        )
                    }
                }

                val daySpecificBells = bells.filter { it.dayOfWeek == selectedDay }
                val effectiveBells = if (daySpecificBells.isNotEmpty()) daySpecificBells else bells.filter { it.dayOfWeek == null }
                val matchedBell = effectiveBells.find { it.lessonNumber == selectedLessonNumber }
                if (matchedBell != null) {
                    val scheduleTypeName = when {
                        selectedDay == 1 && daySpecificBells.isNotEmpty() -> "Понедельник"
                        selectedDay == 6 && daySpecificBells.isNotEmpty() -> "Суббота"
                        else -> "Вт – Пт (основное)"
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Время пары: ${matchedBell.startTime} — ${matchedBell.endTime} ($scheduleTypeName)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Lesson Type
                Text("Тип занятия", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(lessonTypes) { (type, label) ->
                        FilterChip(
                            selected = lessonType == type,
                            onClick = { lessonType = type },
                            label = { Text(label) }
                        )
                    }
                }

                // Structured Classroom (Building, Floor, Room)
                Text("Аудитория (структурированная)", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = roomNumber,
                        onValueChange = { roomNumber = it },
                        label = { Text("Кабинет") },
                        placeholder = { Text("304") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = floorText,
                        onValueChange = { floorText = it },
                        label = { Text("Этаж") },
                        placeholder = { Text("3") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.7f)
                    )
                }

                OutlinedTextField(
                    value = building,
                    onValueChange = { building = it },
                    label = { Text("Корпус") },
                    placeholder = { Text("Главный / ИТ / Физкорпус") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Scheme note ("как пройти")
                OutlinedTextField(
                    value = mapSchemeNote,
                    onValueChange = { mapSchemeNote = it },
                    label = { Text("Как пройти (схема/навигация)") },
                    placeholder = { Text("Например: 3 этаж, налево от лифта, крыло Б") },
                    leadingIcon = { Icon(Icons.Default.Navigation, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Note
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Примечание к занятию") },
                    placeholder = { Text("Например: принести калькулятор") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedSubjectId > 0) {
                                onSave(
                                    LessonEntity(
                                        id = initialLesson?.id ?: 0L,
                                        weekScheduleId = weekId,
                                        dayOfWeek = selectedDay,
                                        lessonNumber = selectedLessonNumber,
                                        subjectId = selectedSubjectId,
                                        building = building.trim(),
                                        floor = floorText.toIntOrNull() ?: 1,
                                        roomNumber = roomNumber.trim(),
                                        mapSchemeNote = mapSchemeNote.ifBlank { null },
                                        lessonType = lessonType,
                                        notes = notes.trim(),
                                        attendance = attendance
                                    )
                                )
                            }
                        },
                        enabled = selectedSubjectId > 0
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
fun EditSubjectDialog(
    initialSubject: SubjectEntity?,
    onDismiss: () -> Unit,
    onSave: (SubjectEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialSubject?.name ?: "") }
    var teacher by remember { mutableStateOf(initialSubject?.teacher ?: "") }
    var selectedColor by remember { mutableStateOf(initialSubject?.colorHex ?: SubjectColors.first()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialSubject == null) "Новый предмет" else "Редактировать предмет",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (initialSubject != null && onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название предмета*") },
                    placeholder = { Text("Например: Математический анализ") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("ФИО преподавателя") },
                    placeholder = { Text("Иванов Иван Иванович") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Цвет-метка", style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(SubjectColors) { colorHex ->
                        val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Color.Gray }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == colorHex) 3.dp else 1.dp,
                                    color = if (selectedColor == colorHex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == colorHex) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    SubjectEntity(
                                        id = initialSubject?.id ?: 0L,
                                        name = name.trim(),
                                        teacher = teacher.trim(),
                                        colorHex = selectedColor
                                    )
                                )
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
fun EditBellDialog(
    initialBell: BellScheduleEntity?,
    defaultDayOfWeek: Int?,
    onDismiss: () -> Unit,
    onSave: (BellScheduleEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var lessonNumber by remember { mutableIntStateOf(initialBell?.lessonNumber ?: 1) }
    var startTime by remember { mutableStateOf(initialBell?.startTime ?: "08:30") }
    var endTime by remember { mutableStateOf(initialBell?.endTime ?: "10:00") }
    var dayOfWeek by remember { mutableStateOf(initialBell?.dayOfWeek ?: defaultDayOfWeek) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialBell == null) "Добавить звонок" else "Редактировать звонок",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (initialBell != null && onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Text("Номер пары", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..6).forEach { num ->
                        FilterChip(
                            selected = lessonNumber == num,
                            onClick = { lessonNumber = num },
                            label = { Text("№$num") }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Начало (ЧЧ:ММ)") },
                        placeholder = { Text("08:30") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("Конец (ЧЧ:ММ)") },
                        placeholder = { Text("10:00") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Вид расписания звонков", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = dayOfWeek == 1,
                        onClick = { dayOfWeek = 1 },
                        label = { Text("Понедельник") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = dayOfWeek == null,
                        onClick = { dayOfWeek = null },
                        label = { Text("Вт – Пт") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = dayOfWeek == 6,
                        onClick = { dayOfWeek = 6 },
                        label = { Text("Суббота") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = when (dayOfWeek) {
                        1 -> "📌 Понедельник: особое время пар, классные часы или линейка."
                        6 -> "⏱️ Суббота: сокращенные пары (80 мин) и ускоренный день."
                        else -> "📅 Вторник – Пятница: основная академическая сетка пар."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (startTime.isNotBlank() && endTime.isNotBlank()) {
                                onSave(
                                    BellScheduleEntity(
                                        id = initialBell?.id ?: 0L,
                                        lessonNumber = lessonNumber,
                                        startTime = startTime.trim(),
                                        endTime = endTime.trim(),
                                        dayOfWeek = dayOfWeek
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
fun CreateWeekDialog(
    existingWeeks: List<WeekScheduleEntity>,
    onDismiss: () -> Unit,
    onCreate: (title: String, startDate: String, endDate: String, copyFromWeekId: Long?) -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    if (existingWeeks.isNotEmpty()) {
        cal.add(Calendar.WEEK_OF_YEAR, 1)
    }
    val defaultStart = sdf.format(cal.time)
    cal.add(Calendar.DAY_OF_WEEK, 6)
    val defaultEnd = sdf.format(cal.time)

    var title by remember { mutableStateOf("Учебная неделя ${existingWeeks.size + 1}") }
    var startDate by remember { mutableStateOf(defaultStart) }
    var endDate by remember { mutableStateOf(defaultEnd) }
    var copyFromWeekId by remember { mutableStateOf<Long?>(existingWeeks.firstOrNull()?.id) }
    var copyTemplate by remember { mutableStateOf(existingWeeks.isNotEmpty()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Создать новую неделю",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название недели") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Дата начала") },
                        placeholder = { Text("ГГГГ-ММ-ДД") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("Дата конца") },
                        placeholder = { Text("ГГГГ-ММ-ДД") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (existingWeeks.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = copyTemplate,
                            onCheckedChange = { copyTemplate = it }
                        )
                        Text("Скопировать пары из прошлой недели как шаблон", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (copyTemplate) {
                        var expanded by remember { mutableStateOf(false) }
                        val selectedWeek = existingWeeks.find { it.id == copyFromWeekId } ?: existingWeeks.first()

                        OutlinedCard(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Шаблон: ${selectedWeek.title} (${selectedWeek.startDate})", style = MaterialTheme.typography.bodyMedium)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                existingWeeks.forEach { w ->
                                    DropdownMenuItem(
                                        text = { Text("${w.title} (${w.startDate})") },
                                        onClick = {
                                            copyFromWeekId = w.id
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && startDate.isNotBlank() && endDate.isNotBlank()) {
                                onCreate(
                                    title.trim(),
                                    startDate.trim(),
                                    endDate.trim(),
                                    if (copyTemplate) copyFromWeekId else null
                                )
                            }
                        }
                    ) {
                        Text("Создать")
                    }
                }
            }
        }
    }
}

@Composable
fun EditHomeworkDialog(
    initialHomework: HomeworkEntity?,
    subjects: List<SubjectEntity>,
    preselectedSubjectId: Long?,
    preselectedLessonId: Long?,
    onDismiss: () -> Unit,
    onSave: (HomeworkEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var subjectId by remember { mutableStateOf(initialHomework?.subjectId ?: preselectedSubjectId ?: subjects.firstOrNull()?.id ?: 0L) }
    var text by remember { mutableStateOf(initialHomework?.text ?: "") }

    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_MONTH, 3)
    val defaultDeadline = SimpleDateFormat("yyyy-MM-dd 23:59", Locale.getDefault()).format(cal.time)
    var deadline by remember { mutableStateOf(initialHomework?.deadline ?: defaultDeadline) }
    var status by remember { mutableStateOf(initialHomework?.status ?: "NOT_STARTED") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialHomework == null) "Новое задание" else "Редактировать ДЗ",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (initialHomework != null && onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Subject picker
                Text("Предмет", style = MaterialTheme.typography.labelLarge)
                var expandedSubject by remember { mutableStateOf(false) }
                val currentSubject = subjects.find { it.id == subjectId } ?: subjects.firstOrNull()
                OutlinedCard(onClick = { expandedSubject = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(currentSubject?.name ?: "Выберите предмет")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = expandedSubject, onDismissRequest = { expandedSubject = false }) {
                        subjects.forEach { subj ->
                            DropdownMenuItem(text = { Text(subj.name) }, onClick = { subjectId = subj.id; expandedSubject = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Описание задания*") },
                    placeholder = { Text("Например: лабораторная работа №3, вариант 5") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Дедлайн (ГГГГ-ММ-ДД ЧЧ:ММ)") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Статус", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = status == "NOT_STARTED", onClick = { status = "NOT_STARTED" }, label = { Text("Не начато") })
                    FilterChip(selected = status == "IN_PROGRESS", onClick = { status = "IN_PROGRESS" }, label = { Text("В процессе") })
                    FilterChip(selected = status == "COMPLETED", onClick = { status = "COMPLETED" }, label = { Text("Готово") })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (text.isNotBlank() && subjectId > 0) {
                                onSave(
                                    HomeworkEntity(
                                        id = initialHomework?.id ?: 0L,
                                        subjectId = subjectId,
                                        lessonId = initialHomework?.lessonId ?: preselectedLessonId,
                                        text = text.trim(),
                                        deadline = deadline.trim(),
                                        status = status
                                    )
                                )
                            }
                        },
                        enabled = text.isNotBlank() && subjectId > 0
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
fun EditNoteDialog(
    initialNote: NoteEntity?,
    subjects: List<SubjectEntity>,
    preselectedSubjectId: Long?,
    preselectedLessonId: Long?,
    onDismiss: () -> Unit,
    onSave: (NoteEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var subjectId by remember { mutableStateOf(initialNote?.subjectId ?: preselectedSubjectId) }
    var text by remember { mutableStateOf(initialNote?.text ?: "") }
    var selectedTags by remember { mutableStateOf(initialNote?.tagsList?.toSet() ?: emptySet()) }
    var customTagInput by remember { mutableStateOf("") }

    val commonTags = listOf("экзамен", "важно", "лектор акцентировал", "формула", "доклад", "литература")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialNote == null) "Новая заметка" else "Редактировать заметку",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (initialNote != null && onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Optional Subject
                Text("Предмет (опционально)", style = MaterialTheme.typography.labelLarge)
                var expandedSubject by remember { mutableStateOf(false) }
                val currentSub = subjects.find { it.id == subjectId }
                OutlinedCard(onClick = { expandedSubject = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(currentSub?.name ?: "Общая заметка")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = expandedSubject, onDismissRequest = { expandedSubject = false }) {
                        DropdownMenuItem(text = { Text("Общая заметка (без предмета)") }, onClick = { subjectId = null; expandedSubject = false })
                        subjects.forEach { subj ->
                            DropdownMenuItem(text = { Text(subj.name) }, onClick = { subjectId = subj.id; expandedSubject = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Текст заметки*") },
                    placeholder = { Text("Запишите важное напоминание или мысль с лекции...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Теги (множественный выбор)", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(commonTags) { tag ->
                        FilterChip(
                            selected = selectedTags.contains(tag),
                            onClick = {
                                selectedTags = if (selectedTags.contains(tag)) selectedTags - tag else selectedTags + tag
                            },
                            label = { Text("#$tag") }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customTagInput,
                        onValueChange = { customTagInput = it },
                        label = { Text("Свой тег") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (customTagInput.isNotBlank()) {
                                selectedTags = selectedTags + customTagInput.trim()
                                customTagInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить тег")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                onSave(
                                    NoteEntity(
                                        id = initialNote?.id ?: 0L,
                                        subjectId = subjectId,
                                        lessonId = initialNote?.lessonId ?: preselectedLessonId,
                                        text = text.trim(),
                                        tagsCsv = selectedTags.joinToString(",")
                                    )
                                )
                            }
                        },
                        enabled = text.isNotBlank()
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
fun EditGradeDialog(
    initialGrade: GradeEntity?,
    subjects: List<SubjectEntity>,
    preselectedSubjectId: Long?,
    preselectedLessonId: Long?,
    onDismiss: () -> Unit,
    onSave: (GradeEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var subjectId by remember { mutableStateOf(initialGrade?.subjectId ?: preselectedSubjectId ?: subjects.firstOrNull()?.id ?: 0L) }
    var gradeType by remember { mutableStateOf(initialGrade?.gradeType ?: "FIVE_POINT") }
    var numericScore by remember { mutableDoubleStateOf(initialGrade?.numericValue ?: 5.0) }
    var isPassed by remember { mutableStateOf(initialGrade?.isPassed ?: true) }
    var category by remember { mutableStateOf(initialGrade?.category ?: "REGULAR") }

    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var dateStr by remember { mutableStateOf(initialGrade?.date ?: todayStr) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialGrade == null) "Выставить оценку" else "Редактировать оценку",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (initialGrade != null && onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Subject picker
                Text("Предмет", style = MaterialTheme.typography.labelLarge)
                var expandedSubject by remember { mutableStateOf(false) }
                val currentSubject = subjects.find { it.id == subjectId } ?: subjects.firstOrNull()
                OutlinedCard(onClick = { expandedSubject = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(currentSubject?.name ?: "Выберите предмет")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = expandedSubject, onDismissRequest = { expandedSubject = false }) {
                        subjects.forEach { subj ->
                            DropdownMenuItem(text = { Text(subj.name) }, onClick = { subjectId = subj.id; expandedSubject = false })
                        }
                    }
                }

                Text("Тип шкалы", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = gradeType == "FIVE_POINT", onClick = { gradeType = "FIVE_POINT" }, label = { Text("5-балльная (2-5)") })
                    FilterChip(selected = gradeType == "PASS_FAIL", onClick = { gradeType = "PASS_FAIL" }, label = { Text("Зачёт / Незачёт") })
                }

                if (gradeType == "FIVE_POINT") {
                    Text("Балл", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(5.0, 4.0, 3.0, 2.0).forEach { score ->
                            FilterChip(
                                selected = numericScore == score,
                                onClick = { numericScore = score },
                                label = { Text(score.toInt().toString(), fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                } else {
                    Text("Результат", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = isPassed, onClick = { isPassed = true }, label = { Text("Зачёт ✓") })
                        FilterChip(selected = !isPassed, onClick = { isPassed = false }, label = { Text("Незачёт ✗") })
                    }
                }

                Text("Категория", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        listOf(
                            "REGULAR" to "Текущая",
                            "TEST" to "Контрольная",
                            "CREDIT" to "Зачёт",
                            "EXAM" to "Экзамен"
                        )
                    ) { (cat, label) ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(label) })
                    }
                }

                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("Дата (ГГГГ-ММ-ДД)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (subjectId > 0) {
                                onSave(
                                    GradeEntity(
                                        id = initialGrade?.id ?: 0L,
                                        subjectId = subjectId,
                                        lessonId = initialGrade?.lessonId ?: preselectedLessonId,
                                        gradeType = gradeType,
                                        numericValue = numericScore,
                                        isPassed = isPassed,
                                        date = dateStr.trim(),
                                        category = category
                                    )
                                )
                            }
                        },
                        enabled = subjectId > 0
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
fun EditExamDialog(
    initialExam: ExamSessionEntity?,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (ExamSessionEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var subjectId by remember { mutableStateOf(initialExam?.subjectId ?: subjects.firstOrNull()?.id ?: 0L) }
    var examType by remember { mutableStateOf(initialExam?.examType ?: "EXAM") }
    var examDate by remember { mutableStateOf(initialExam?.examDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var examTime by remember { mutableStateOf(initialExam?.examTime ?: "09:00") }
    var ticketNumber by remember { mutableStateOf(initialExam?.ticketNumber ?: "") }
    var readiness by remember { mutableFloatStateOf((initialExam?.readinessPercent ?: 50).toFloat()) }
    var prepNotes by remember { mutableStateOf(initialExam?.prepNotes ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialExam == null) "Добавить в сессию" else "Редактировать экзамен",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (initialExam != null && onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Subject picker
                Text("Предмет", style = MaterialTheme.typography.labelLarge)
                var expandedSubject by remember { mutableStateOf(false) }
                val currentSubject = subjects.find { it.id == subjectId } ?: subjects.firstOrNull()
                OutlinedCard(onClick = { expandedSubject = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(currentSubject?.name ?: "Выберите предмет")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = expandedSubject, onDismissRequest = { expandedSubject = false }) {
                        subjects.forEach { subj ->
                            DropdownMenuItem(text = { Text(subj.name) }, onClick = { subjectId = subj.id; expandedSubject = false })
                        }
                    }
                }

                Text("Тип контроля", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        listOf(
                            "EXAM" to "Экзамен",
                            "CREDIT" to "Зачёт",
                            "COURSEWORK" to "Курсовая",
                            "DIPLOMA" to "Диплом"
                        )
                    ) { (type, label) ->
                        FilterChip(selected = examType == type, onClick = { examType = type }, label = { Text(label) })
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = examDate,
                        onValueChange = { examDate = it },
                        label = { Text("Дата (ГГГГ-ММ-ДД)") },
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = examTime,
                        onValueChange = { examTime = it },
                        label = { Text("Время") },
                        modifier = Modifier.weight(0.8f)
                    )
                }

                OutlinedTextField(
                    value = ticketNumber,
                    onValueChange = { ticketNumber = it },
                    label = { Text("Номер билета (если известен)") },
                    placeholder = { Text("Например: 14") },
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Готовность к сдаче", style = MaterialTheme.typography.labelLarge)
                        Text("${readiness.toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = readiness,
                        onValueChange = { readiness = it },
                        valueRange = 0f..100f,
                        steps = 20
                    )
                }

                OutlinedTextField(
                    value = prepNotes,
                    onValueChange = { prepNotes = it },
                    label = { Text("Заметки по подготовке") },
                    placeholder = { Text("Список вопросов, шпаргалки, формулы...") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (subjectId > 0 && examDate.isNotBlank()) {
                                onSave(
                                    ExamSessionEntity(
                                        id = initialExam?.id ?: 0L,
                                        subjectId = subjectId,
                                        examDate = examDate.trim(),
                                        examTime = examTime.trim(),
                                        examType = examType,
                                        ticketNumber = ticketNumber.trim(),
                                        readinessPercent = readiness.toInt(),
                                        prepNotes = prepNotes.trim()
                                    )
                                )
                            }
                        },
                        enabled = subjectId > 0 && examDate.isNotBlank()
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
fun BuildingMapDialog(
    lesson: LessonEntity,
    subjectName: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Как пройти: $subjectName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Кабинет: ${lesson.roomNumber}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text("Корпус: ${lesson.building}", style = MaterialTheme.typography.bodyMedium)
                        Text("Этаж: ${lesson.floor}", style = MaterialTheme.typography.bodyMedium)

                        if (!lesson.mapSchemeNote.isNullOrBlank()) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("Маршрут:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                            Text(lesson.mapSchemeNote, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Понятно")
                }
            }
        }
    }
}
