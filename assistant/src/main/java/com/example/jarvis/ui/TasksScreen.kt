package com.example.jarvis.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.jarvis.data.MemoryItem
import com.example.jarvis.data.ReminderItem
import com.example.jarvis.data.TimerItem
import com.example.jarvis.data.TodoItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TasksScreen(
    todos: List<TodoItem>,
    memories: List<MemoryItem>,
    timers: List<TimerItem>,
    reminders: List<ReminderItem>,
    onAddTodo: (String) -> Unit,
    onToggleTodo: (TodoItem) -> Unit,
    onDeleteTodo: (TodoItem) -> Unit,
    onAddMemory: (String) -> Unit,
    onDeleteMemory: (MemoryItem) -> Unit,
    onDeleteTimer: (TimerItem) -> Unit,
    onCompleteReminder: (ReminderItem) -> Unit,
    onDeleteReminder: (ReminderItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var todoInput by remember { mutableStateOf("") }
    var memoryInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "todo-header") {
            Text(
                text = "TAREAS",
                style = MaterialTheme.typography.labelMedium,
                color = JarvisColors.GreenDim,
            )
        }
        item(key = "todo-input") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = todoInput,
                    onValueChange = { todoInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Nueva tarea…", color = JarvisColors.Muted) },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = JarvisColors.Green),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisColors.Green,
                        unfocusedBorderColor = JarvisColors.Outline,
                        focusedTextColor = JarvisColors.Green,
                        unfocusedTextColor = JarvisColors.Green,
                        cursorColor = JarvisColors.Green,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                    singleLine = true,
                )
                IconButton(onClick = {
                    val clean = todoInput.trim()
                    if (clean.isNotEmpty()) {
                        onAddTodo(clean)
                        todoInput = ""
                    }
                }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Añadir tarea", tint = JarvisColors.Green)
                }
            }
        }
        if (todos.isEmpty()) {
            item(key = "todo-empty") {
                Text(
                    text = "Sin tareas. Puede decir «añade tarea comprar leche».",
                    style = MaterialTheme.typography.bodySmall,
                    color = JarvisColors.Muted,
                )
            }
        } else {
            items(items = todos, key = { "todo-${it.id}" }) { todo ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = todo.done,
                        onCheckedChange = { onToggleTodo(todo) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = JarvisColors.Green,
                            uncheckedColor = JarvisColors.Outline,
                            checkmarkColor = Color.Black,
                        ),
                    )
                    Text(
                        text = todo.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (todo.done) JarvisColors.Muted else JarvisColors.Green,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onDeleteTodo(todo) }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Borrar tarea",
                            tint = JarvisColors.Outline,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        item(key = "timer-header") {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "TEMPORIZADORES",
                style = MaterialTheme.typography.labelMedium,
                color = JarvisColors.GreenDim,
            )
        }
        if (timers.isEmpty()) {
            item(key = "timer-empty") {
                Text(
                    text = "Ningún temporizador activo. Diga «pon un temporizador de 5 minutos».",
                    style = MaterialTheme.typography.bodySmall,
                    color = JarvisColors.Muted,
                )
            }
        } else {
            items(items = timers, key = { "timer-${it.id}" }) { timer ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (timer.fired) Icons.Rounded.Check else Icons.Rounded.Add,
                        contentDescription = null,
                        tint = if (timer.fired) JarvisColors.Green else JarvisColors.Amber,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${timer.label} · ${timer.durationSeconds / 60} min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (timer.fired) JarvisColors.Muted else JarvisColors.Green,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onDeleteTimer(timer) }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Borrar temporizador",
                            tint = JarvisColors.Outline,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        item(key = "reminder-header") {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "RECORDATORIOS",
                style = MaterialTheme.typography.labelMedium,
                color = JarvisColors.GreenDim,
            )
        }
        if (reminders.isEmpty()) {
            item(key = "reminder-empty") {
                Text(
                    text = "Ningún recordatorio. Diga «recuérdame comprar pan a las 6 de la tarde».",
                    style = MaterialTheme.typography.bodySmall,
                    color = JarvisColors.Muted,
                )
            }
        } else {
            items(items = reminders, key = { "rem-${it.id}" }) { reminder ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (reminder.done) Icons.Rounded.Check else Icons.Rounded.Alarm,
                        contentDescription = null,
                        tint = if (reminder.done) JarvisColors.Muted else JarvisColors.Amber,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reminder.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (reminder.done) JarvisColors.Muted else JarvisColors.Green,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = formatReminderMoment(reminder.triggerAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = JarvisColors.Muted,
                        )
                    }
                    if (!reminder.done) {
                        IconButton(onClick = { onCompleteReminder(reminder) }) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "Marcar como hecho",
                                tint = JarvisColors.Green,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    IconButton(onClick = { onDeleteReminder(reminder) }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Borrar recordatorio",
                            tint = JarvisColors.Outline,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        item(key = "memory-header") {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "MEMORIAS",
                style = MaterialTheme.typography.labelMedium,
                color = JarvisColors.GreenDim,
            )
        }
        item(key = "memory-input") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = memoryInput,
                    onValueChange = { memoryInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Memorizar…", color = JarvisColors.Muted) },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = JarvisColors.Green),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisColors.Green,
                        unfocusedBorderColor = JarvisColors.Outline,
                        focusedTextColor = JarvisColors.Green,
                        unfocusedTextColor = JarvisColors.Green,
                        cursorColor = JarvisColors.Green,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                    singleLine = true,
                )
                IconButton(onClick = {
                    val clean = memoryInput.trim()
                    if (clean.isNotEmpty()) {
                        onAddMemory(clean)
                        memoryInput = ""
                    }
                }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Memorizar", tint = JarvisColors.Green)
                }
            }
        }
        if (memories.isEmpty()) {
            item(key = "memory-empty") {
                Text(
                    text = "Sin memorias guardadas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = JarvisColors.Muted,
                )
            }
        } else {
            items(items = memories, key = { "mem-${it.id}" }) { memory ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = memory.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = JarvisColors.Green,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onDeleteMemory(memory) }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Borrar memoria",
                            tint = JarvisColors.Outline,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Fecha y hora del recordatorio en formato corto: "14 nov · 18:00". */
private fun formatReminderMoment(triggerAt: Long): String {
    val spanish = Locale.forLanguageTag("es")
    val formatter = DateTimeFormatter.ofPattern("d MMM · HH:mm", spanish)
    return Instant.ofEpochMilli(triggerAt)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(formatter)
}
