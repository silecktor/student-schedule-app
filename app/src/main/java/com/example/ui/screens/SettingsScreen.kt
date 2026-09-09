package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.SearchCategory
import com.example.data.model.SubjectEntity
import com.example.export.ExportImportManager
import com.example.notifications.AlarmScheduler
import com.example.ui.dialogs.EditSubjectDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val subjects by viewModel.allSubjects.collectAsState()
    val themeMode by viewModel.preferences.themeMode.collectAsState()
    val expulsionThreshold by viewModel.preferences.expulsionThreshold.collectAsState()
    val scholarshipThreshold by viewModel.preferences.scholarshipThreshold.collectAsState()
    val weatherCity by viewModel.preferences.weatherCity.collectAsState()
    val weatherEnabled by viewModel.preferences.weatherEnabled.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val weatherLoading by viewModel.weatherLoading.collectAsState()

    val notifyPairEnd by viewModel.preferences.notifyBeforePairEnd.collectAsState()
    val notifyMorningBrief by viewModel.preferences.notifyMorningBrief.collectAsState()
    val morningBriefTime by viewModel.preferences.morningBriefTime.collectAsState()
    val notifyHw by viewModel.preferences.notifyHwDeadline.collectAsState()
    val notifyWeather by viewModel.preferences.notifyEveningWeather.collectAsState()
    val notifyBackup by viewModel.preferences.notifyBackupReminder.collectAsState()

    // Search state
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    // Dialogs
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var subjectToEdit by remember { mutableStateOf<SubjectEntity?>(null) }
    var showImportJsonDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Настройки и данные",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Параметры, предметы, поиск и бэкап",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Global Search Bento Card
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Поиск предметов, преподавателей, аудиторий...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистить")
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )
            }

            // Search Results Display
            if (searchQuery.isNotBlank()) {
                item {
                    Text(
                        text = "Результаты поиска (${searchResults.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (searchResults.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Ничего не найдено",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(searchResults) { result ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BentoHeroLavender.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = when (result.type) {
                                            SearchCategory.SUBJECT -> "Предмет"
                                            SearchCategory.LESSON -> "Занятие"
                                            SearchCategory.HOMEWORK -> "ДЗ"
                                            SearchCategory.NOTE -> "Заметка"
                                            SearchCategory.GRADE -> "Оценка"
                                            SearchCategory.EXAM -> "Экзамен"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Column {
                                    Text(result.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(result.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Section 1: Subject Management
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Предметы (${subjects.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Button(
                        onClick = { showAddSubjectDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Добавить", fontSize = 12.sp)
                    }
                }
            }

            items(subjects) { subject ->
                val color = try {
                    Color(android.graphics.Color.parseColor(subject.colorHex))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.primary
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                        .clickable { subjectToEdit = subject }
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(subject.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            if (subject.teacher.isNotBlank()) {
                                Text(subject.teacher, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.Default.Edit, contentDescription = "Редактировать", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Section 2: Appearance & Theme
            item {
                Text("Внешний вид и тема", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Режим оформления", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column {
                            listOf(
                                "SYSTEM" to "Системная тема Bento",
                                "LIGHT" to "Светлая Bento (Off-White)",
                                "DARK" to "Тёмная Bento (Deep Charcoal)",
                                "AUTO_TIME" to "Автоматически по времени (20:00–07:00)"
                            ).forEach { (modeKey, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.preferences.setThemeMode(modeKey) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = themeMode == modeKey,
                                        onClick = { viewModel.preferences.setThemeMode(modeKey) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Weather Forecast Integration
            item {
                Text("Погода перед учёбой (Open-Meteo)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Показывать погоду на завтра", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Предупреждает о дожде и напоминает зонт", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = weatherEnabled,
                                onCheckedChange = {
                                    viewModel.preferences.setWeatherEnabled(it)
                                    if (it) viewModel.refreshWeather()
                                }
                            )
                        }

                        if (weatherEnabled) {
                            var cityInput by remember { mutableStateOf(weatherCity) }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = cityInput,
                                    onValueChange = { cityInput = it },
                                    label = { Text("Город для прогноза") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        viewModel.preferences.setWeatherCity(cityInput.trim())
                                        viewModel.refreshWeather()
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("ОК")
                                }
                            }

                            if (weatherLoading) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)))
                            } else if (weatherState != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(
                                            imageVector = if (weatherState!!.isRainExpected) Icons.Default.Umbrella else Icons.Default.WbSunny,
                                            contentDescription = null,
                                            tint = if (weatherState!!.isRainExpected) MaterialTheme.colorScheme.primary else Color(0xFFF59E0B)
                                        )
                                        Column {
                                            Text("Завтра в г. ${weatherState!!.cityName}: ${weatherState!!.description}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                            Text("Температура: +${weatherState!!.tempMin.toInt()}°C...+${weatherState!!.tempMax.toInt()}°C • Осадки: ${weatherState!!.rainProb}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Notifications and Alarms
            item {
                Text("Уведомления и будильники", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        NotificationSettingRow(
                            title = "За 5 минут до конца пары",
                            subtitle = "Уведомляет о следующей аудитории и предмете",
                            isChecked = notifyPairEnd,
                            onCheckedChange = { viewModel.preferences.setNotifyBeforePairEnd(it) }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        NotificationSettingRow(
                            title = "Утренняя сводка расписания",
                            subtitle = "Сводка пар на сегодня в $morningBriefTime",
                            isChecked = notifyMorningBrief,
                            onCheckedChange = {
                                viewModel.preferences.setNotifyMorningBrief(it)
                                AlarmScheduler.scheduleMorningBrief(context, morningBriefTime, it)
                            }
                        )

                        if (notifyMorningBrief) {
                            var timeInput by remember { mutableStateOf(morningBriefTime) }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = timeInput,
                                    onValueChange = { timeInput = it },
                                    label = { Text("Время утренней сводки") },
                                    placeholder = { Text("07:30") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        viewModel.preferences.setMorningBriefTime(timeInput.trim())
                                        AlarmScheduler.scheduleMorningBrief(context, timeInput.trim(), true)
                                        Toast.makeText(context, "Время сохранено", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Сохранить")
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        NotificationSettingRow(
                            title = "Напоминания о дедлайнах ДЗ",
                            subtitle = "За 2 часа до окончания срока сдачи",
                            isChecked = notifyHw,
                            onCheckedChange = { viewModel.preferences.setNotifyHwDeadline(it) }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        NotificationSettingRow(
                            title = "Вечерний прогноз погоды",
                            subtitle = "Совет в 20:00 взять зонт, если завтра дождь",
                            isChecked = notifyWeather,
                            onCheckedChange = {
                                viewModel.preferences.setNotifyEveningWeather(it)
                                AlarmScheduler.scheduleEveningWeather(context, it)
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        NotificationSettingRow(
                            title = "Напоминание о бэкапе",
                            subtitle = "Раз в неделю предлагает сохранить копию расписания",
                            isChecked = notifyBackup,
                            onCheckedChange = {
                                viewModel.preferences.setNotifyBackupReminder(it)
                                AlarmScheduler.scheduleWeeklyBackup(context, it)
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Button(
                            onClick = {
                                viewModel.sendTestNotification()
                                Toast.makeText(context, "Тестовое уведомление отправлено", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Проверить уведомление")
                        }
                    }
                }
            }

            // Section 5: Academic Thresholds (Пороги)
            item {
                Text("Академические пороги", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        var expulsionInput by remember { mutableStateOf(expulsionThreshold.toString()) }
                        OutlinedTextField(
                            value = expulsionInput,
                            onValueChange = {
                                expulsionInput = it
                                it.toIntOrNull()?.let { num -> viewModel.preferences.setExpulsionThreshold(num) }
                            },
                            label = { Text("Порог риска недопуска/отчисления (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        var scholarshipInput by remember { mutableStateOf(scholarshipThreshold.toString()) }
                        OutlinedTextField(
                            value = scholarshipInput,
                            onValueChange = {
                                scholarshipInput = it
                                it.toDoubleOrNull()?.let { num -> viewModel.preferences.setScholarshipThreshold(num) }
                            },
                            label = { Text("Минимальный балл для стипендии") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Section 6: Backup & Export (JSON & ICS)
            item {
                Text("Резервное копирование и экспорт", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val shareIntent = ExportImportManager.shareDatabaseBackup(context, viewModel.repository)
                                    context.startActivity(Intent.createChooser(shareIntent, "Экспорт резервной копии"))
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Экспорт базы данных (JSON)")
                        }

                        OutlinedButton(
                            onClick = { showImportJsonDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Импорт расписания из JSON")
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddSubjectDialog) {
        EditSubjectDialog(
            initialSubject = null,
            onDismiss = { showAddSubjectDialog = false },
            onSave = {
                viewModel.saveSubject(it)
                showAddSubjectDialog = false
            }
        )
    }

    if (subjectToEdit != null) {
        EditSubjectDialog(
            initialSubject = subjectToEdit,
            onDismiss = { subjectToEdit = null },
            onSave = {
                viewModel.saveSubject(it)
                subjectToEdit = null
            },
            onDelete = {
                viewModel.deleteSubject(subjectToEdit!!)
                subjectToEdit = null
            }
        )
    }

    if (showImportJsonDialog) {
        ImportJsonDialog(
            onDismiss = { showImportJsonDialog = false },
            onImport = { jsonString ->
                coroutineScope.launch {
                    val success = ExportImportManager.importDatabaseBackup(context, viewModel.repository, jsonString)
                    if (success) {
                        Toast.makeText(context, "Расписание успешно импортировано!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Ошибка импорта JSON файла", Toast.LENGTH_LONG).show()
                    }
                    showImportJsonDialog = false
                }
            }
        )
    }
}

@Composable
fun NotificationSettingRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = isChecked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ImportJsonDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Импорт расписания из JSON", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Вставьте содержимое резервной копии JSON:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    placeholder = { Text("{\"subjects\":[...], \"weeks\":[...]}") },
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onImport(jsonText.trim()) },
                        enabled = jsonText.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Импортировать")
                    }
                }
            }
        }
    }
}
