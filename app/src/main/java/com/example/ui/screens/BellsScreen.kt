package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BellScheduleEntity
import com.example.ui.dialogs.EditBellDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BellsScreen(viewModel: MainViewModel) {
    val bells by viewModel.allBells.collectAsState()
    // 3 Types: 1 = Monday, null = Tuesday-Friday, 6 = Saturday
    var selectedFilterDay by remember { mutableStateOf<Int?>(1) } // Default to Monday or Tue-Fri
    var bellToEdit by remember { mutableStateOf<BellScheduleEntity?>(null) }
    var showAddBellDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val mondayBells = bells.filter { it.dayOfWeek == 1 }.sortedBy { it.lessonNumber }
    val tueFriBells = bells.filter { it.dayOfWeek == null }.sortedBy { it.lessonNumber }
    val satBells = bells.filter { it.dayOfWeek == 6 }.sortedBy { it.lessonNumber }

    val filteredBells = when (selectedFilterDay) {
        1 -> mondayBells
        6 -> satBells
        else -> tueFriBells
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
                        text = "Расписание звонков",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "3 вида сеток: Пн, Вт–Пт, Суббота",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Reset to default 3 bell types
                IconButton(
                    onClick = { showResetConfirmDialog = true },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = "Сбросить к 3 типовым сеткам",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddBellDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить звонок")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Bento 3-Way Day Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Понедельник
                val isMonday = selectedFilterDay == 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isMonday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            if (isMonday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedFilterDay = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Понедельник",
                            fontSize = 12.sp,
                            fontWeight = if (isMonday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isMonday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        if (mondayBells.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isMonday) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${mondayBells.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    color = if (isMonday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 2. Вторник – Пятница
                val isTueFri = selectedFilterDay == null
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isTueFri) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            if (isTueFri) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedFilterDay = null }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Вт – Пт",
                            fontSize = 12.sp,
                            fontWeight = if (isTueFri) FontWeight.Bold else FontWeight.Medium,
                            color = if (isTueFri) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        if (tueFriBells.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isTueFri) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${tueFriBells.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    color = if (isTueFri) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 3. Суббота
                val isSat = selectedFilterDay == 6
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            if (isSat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedFilterDay = 6 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Суббота",
                            fontSize = 12.sp,
                            fontWeight = if (isSat) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSat) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        if (satBells.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSat) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${satBells.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    color = if (isSat) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Bento Context Info Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val (iconEmoji, title, subtitle) = when (selectedFilterDay) {
                        1 -> Triple(
                            "🗓️",
                            "Сетка звонков понедельника",
                            "Учитывает кураторские часы, линейку или смещённое начало занятий."
                        )
                        6 -> Triple(
                            "⏱️",
                            "Субботняя сетка звонков",
                            "Сокращённые пары (80 минут) и короткие перемены для ускоренного дня."
                        )
                        else -> Triple(
                            "📅",
                            "Основная сетка звонков (Вт – Пт)",
                            "Стандартное академическое расписание для будних учебных дней."
                        )
                    }

                    Text(text = iconEmoji, fontSize = 22.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Bells List or Empty State
            if (filteredBells.isEmpty()) {
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
                        Text("⏰", fontSize = 44.sp)
                        Text(
                            text = when (selectedFilterDay) {
                                1 -> "Нет звонков для понедельника"
                                6 -> "Нет звонков для субботы"
                                else -> "Нет звонков для вторника–пятницы"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Вы можете добавить звонки вручную или скопировать готовую сетку.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (selectedFilterDay != null && tueFriBells.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.copyBells(sourceDay = null, targetDay = selectedFilterDay)
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Скопировать из Вт–Пт")
                                }
                            }

                            Button(
                                onClick = { showAddBellDialog = true },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Добавить пару")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filteredBells.forEachIndexed { index, bell ->
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                    .clickable { bellToEdit = bell }
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // Number badge
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    when (bell.dayOfWeek) {
                                                        1 -> BentoHeroLavender
                                                        6 -> EmeraldAccent.copy(alpha = 0.25f)
                                                        else -> MaterialTheme.colorScheme.primaryContainer
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${bell.lessonNumber}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = when (bell.dayOfWeek) {
                                                    1 -> BentoHeroOnLavender
                                                    6 -> EmeraldAccent
                                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                                }
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "${bell.startTime} — ${bell.endTime}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            val dur = calculateDurationMinutes(bell.startTime, bell.endTime)
                                            Text(
                                                text = "Длительность: $dur мин",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = when (bell.dayOfWeek) {
                                                1 -> "Пн"
                                                6 -> "Сб"
                                                else -> "Вт – Пт"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Break indicator before next bell
                        val nextBell = filteredBells.getOrNull(index + 1)
                        if (nextBell != null) {
                            val breakMin = calculateBreakMinutes(bell.endTime, nextBell.startTime)
                            if (breakMin > 0) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Notifications,
                                                contentDescription = null,
                                                tint = if (breakMin >= 30) AmberAccent else EmeraldAccent,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = if (breakMin >= 30) "Большая перемена (обед): $breakMin мин" else "Перемена: $breakMin мин",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (breakMin >= 30) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant
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

    if (showAddBellDialog) {
        EditBellDialog(
            initialBell = null,
            defaultDayOfWeek = selectedFilterDay,
            onDismiss = { showAddBellDialog = false },
            onSave = {
                viewModel.saveBell(it)
                showAddBellDialog = false
            }
        )
    }

    if (bellToEdit != null) {
        EditBellDialog(
            initialBell = bellToEdit,
            defaultDayOfWeek = bellToEdit!!.dayOfWeek,
            onDismiss = { bellToEdit = null },
            onSave = {
                viewModel.saveBell(it)
                bellToEdit = null
            },
            onDelete = {
                viewModel.deleteBell(bellToEdit!!)
                bellToEdit = null
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Сбросить расписание звонков?") },
            text = {
                Text("Будут установлены 3 стандартные сетки звонков:\n\n1. Понедельник (с 09:00, смещённый график)\n2. Вторник – Пятница (с 08:30, 90 мин пары)\n3. Суббота (с 09:00, сокращённые пары по 80 мин)")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetBellsToDefaults()
                        showResetConfirmDialog = false
                    }
                ) {
                    Text("Восстановить 3 вида")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Отмена")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

private fun calculateDurationMinutes(start: String, end: String): Int {
    return try {
        val sParts = start.split(":")
        val eParts = end.split(":")
        val sMin = sParts[0].toInt() * 60 + sParts[1].toInt()
        val eMin = eParts[0].toInt() * 60 + eParts[1].toInt()
        eMin - sMin
    } catch (e: Exception) {
        90
    }
}

private fun calculateBreakMinutes(prevEnd: String, nextStart: String): Int {
    return try {
        val sParts = prevEnd.split(":")
        val eParts = nextStart.split(":")
        val sMin = sParts[0].toInt() * 60 + sParts[1].toInt()
        val eMin = eParts[0].toInt() * 60 + eParts[1].toInt()
        eMin - sMin
    } catch (e: Exception) {
        0
    }
}
