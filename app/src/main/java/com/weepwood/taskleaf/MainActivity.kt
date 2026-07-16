package com.weepwood.taskleaf

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

private enum class TaskFilter(val label: String) { INBOX("收件箱"), TODAY("今天"), COMPLETED("已完成") }
private enum class Priority(val label: String) { NONE("无"), LOW("低"), MEDIUM("中"), HIGH("高") }
private data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val note: String = "",
    val dueDate: String? = null,
    val priority: Priority = Priority.NONE,
    val completed: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TaskLeafApp(applicationContext) }
    }
}

@Composable
private fun TaskLeafApp(context: Context) {
    var darkMode by remember { mutableStateOf(false) }
    val colors = if (darkMode) darkColorScheme(primary = Color(0xFF8ACF9A)) else lightColorScheme(primary = Color(0xFF3BA55D))
    MaterialTheme(colorScheme = colors) {
        var tasks by remember { mutableStateOf(loadTasks(context)) }
        var filter by remember { mutableStateOf(TaskFilter.INBOX) }
        var query by remember { mutableStateOf("") }
        var showAdd by remember { mutableStateOf(false) }

        fun persist(next: List<TaskItem>) {
            tasks = next
            saveTasks(context, next)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(filter.label, fontWeight = FontWeight.SemiBold) },
                    actions = {
                        IconButton(onClick = { darkMode = !darkMode }) {
                            Icon(if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode, null)
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    TaskFilter.entries.forEach { item ->
                        val icon = when (item) {
                            TaskFilter.INBOX -> Icons.Default.Inbox
                            TaskFilter.TODAY -> Icons.Default.Today
                            TaskFilter.COMPLETED -> Icons.Default.CheckCircle
                        }
                        NavigationBarItem(
                            selected = filter == item,
                            onClick = { filter = item },
                            icon = { Icon(icon, null) },
                            label = { Text(item.label) }
                        )
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "新增任务") }
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("搜索任务") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(18.dp)
                )

                val today = LocalDate.now().toString()
                val shown = tasks.filter {
                    val inSection = when (filter) {
                        TaskFilter.INBOX -> !it.completed
                        TaskFilter.TODAY -> !it.completed && it.dueDate == today
                        TaskFilter.COMPLETED -> it.completed
                    }
                    inSection && (query.isBlank() || it.title.contains(query, true) || it.note.contains(query, true))
                }

                if (shown.isEmpty()) EmptyState(filter)
                else LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(shown, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggle = { persist(tasks.map { if (it.id == task.id) it.copy(completed = !it.completed) else it }) },
                            onDelete = { persist(tasks.filterNot { it.id == task.id }) }
                        )
                    }
                }
            }
        }

        if (showAdd) AddTaskDialog(
            onDismiss = { showAdd = false },
            onAdd = { persist(listOf(it) + tasks); showAdd = false }
        )
    }
}

@Composable
private fun TaskCard(task: TaskItem, onToggle: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = task.completed, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f).padding(start = 6.dp)) {
                Text(
                    task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null
                )
                if (task.note.isNotBlank()) Text(task.note, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    task.dueDate?.let {
                        AssistChip(onClick = {}, label = { Text(formatDate(it)) }, leadingIcon = { Icon(Icons.Default.Event, null, Modifier.size(16.dp)) })
                    }
                    if (task.priority != Priority.NONE) {
                        AssistChip(onClick = {}, label = { Text(task.priority.label) }, leadingIcon = { Icon(Icons.Default.Flag, null, Modifier.size(16.dp)) })
                    }
                }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "删除") }
        }
    }
}

@Composable
private fun EmptyState(filter: TaskFilter) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(72.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircleOutline, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(if (filter == TaskFilter.COMPLETED) "还没有已完成任务" else "当前没有任务", fontWeight = FontWeight.Medium)
            Text("点击右下角按钮添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (TaskItem) -> Unit) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dueToday by remember { mutableStateOf(true) }
    var priority by remember { mutableStateOf(Priority.NONE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增任务") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("任务名称") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("备注") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(dueToday, { dueToday = it })
                    Text("安排到今天")
                }
                Text("优先级", fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Priority.entries.forEach {
                        FilterChip(selected = priority == it, onClick = { priority = it }, label = { Text(it.label) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                onAdd(TaskItem(title = title.trim(), note = note.trim(), dueDate = if (dueToday) LocalDate.now().toString() else null, priority = priority))
            }) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun formatDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("M月d日"))
}.getOrDefault(value)

private fun saveTasks(context: Context, tasks: List<TaskItem>) {
    val array = JSONArray()
    tasks.forEach { task ->
        array.put(JSONObject().apply {
            put("id", task.id); put("title", task.title); put("note", task.note)
            put("dueDate", task.dueDate); put("priority", task.priority.name); put("completed", task.completed)
        })
    }
    context.getSharedPreferences("taskleaf", Context.MODE_PRIVATE).edit().putString("tasks", array.toString()).apply()
}

private fun loadTasks(context: Context): List<TaskItem> {
    val raw = context.getSharedPreferences("taskleaf", Context.MODE_PRIVATE).getString("tasks", null)
        ?: return listOf(
            TaskItem(title = "欢迎使用 TaskLeaf", note = "点击复选框完成任务", dueDate = LocalDate.now().toString(), priority = Priority.MEDIUM),
            TaskItem(title = "规划本周重点", note = "保持任务简洁、可执行", priority = Priority.HIGH)
        )
    return runCatching {
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            TaskItem(
                id = o.getString("id"), title = o.getString("title"), note = o.optString("note"),
                dueDate = if (o.isNull("dueDate")) null else o.optString("dueDate").ifBlank { null },
                priority = Priority.valueOf(o.optString("priority", "NONE")), completed = o.optBoolean("completed")
            )
        }
    }.getOrDefault(emptyList())
}
