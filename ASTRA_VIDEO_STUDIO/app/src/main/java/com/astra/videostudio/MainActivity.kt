package com.astra.videostudio

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF0B0D16)
private val Panel = Color(0xFF171B2B)
private val Panel2 = Color(0xFF20263A)
private val Violet = Color(0xFF9B85FF)
private val Muted = Color(0xFF9AA3BD)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AstraTheme { StudioScreen() } }
    }
}

@Composable
private fun AstraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Violet, background = Ink, surface = Panel,
            onBackground = Color.White, onSurface = Color.White
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioScreen() {
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var frameDuration by remember { mutableFloatStateOf(0.10f) }
    var selectedTab by remember { mutableStateOf("Монтаж") }
    var showInfo by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("Добавь изображения — можно выбрать сразу 20–40 кадров.") }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { picked ->
        if (picked.isNotEmpty()) {
            images = images + picked
            notice = "Добавлено кадров: ${picked.size}. Всего: ${images.size}."
        }
    }

    val totalSeconds = images.size * frameDuration
    val timeLabel = "%.2f сек".format(totalSeconds)

    Scaffold(
        containerColor = Ink,
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                listOf("Монтаж", "Эффекты", "Экспорт").forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                when (tab) {
                                    "Эффекты" -> Icons.Rounded.AutoAwesome
                                    "Экспорт" -> Icons.Rounded.Movie
                                    else -> Icons.Rounded.Tune
                                }, contentDescription = null
                            )
                        },
                        label = { Text(tab) }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ASTRA", color = Violet, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text("VIDEO STUDIO", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Твоя мобильная монтажная", color = Muted, fontSize = 13.sp)
                    }
                    Surface(shape = CircleShape, color = Panel2) {
                        IconButton(onClick = { showInfo = true }) {
                            Icon(Icons.Rounded.Info, contentDescription = "О приложении", tint = Violet)
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Panel),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ПРОЕКТ", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Spacer(Modifier.weight(1f))
                            Text("${images.size} кадров", color = Violet, fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            Modifier.fillMaxWidth().height(178.dp)
                                .background(Brush.linearGradient(listOf(Color(0xFF29294B), Color(0xFF171D32))), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Movie, contentDescription = null, tint = Violet, modifier = Modifier.size(42.dp))
                                Text(if (images.isEmpty()) "Здесь будет предпросмотр" else "Последовательность готова к настройке", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(if (images.isEmpty()) "Сначала добавь фото или кадры" else "$timeLabel · ${images.size} изображений", color = Muted, fontSize = 12.sp)
                            }
                        }
                        Button(
                            onClick = { picker.launch(arrayOf("image/*")) },
                            modifier = Modifier.fillMaxWidth().height(58.dp),
                            shape = RoundedCornerShape(17.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Violet)
                        ) {
                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                            Spacer(Modifier.width(9.dp))
                            Text("Добавить изображения", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("Можно выбрать много файлов за один раз. Порядок пока соответствует выбору в системной галерее.", color = Muted, fontSize = 12.sp)
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Длительность кадра", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text("Одинаковое время для всех изображений", color = Muted, fontSize = 12.sp)
                            }
                            Text("%.2f с".format(frameDuration), color = Violet, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Slider(
                            value = frameDuration,
                            onValueChange = { frameDuration = it },
                            valueRange = 0.05f..2.0f,
                            steps = 38,
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0.1f, 0.2f, 0.5f, 1.0f).forEach { v ->
                                FilterChip(
                                    selected = frameDuration == v,
                                    onClick = { frameDuration = v },
                                    label = { Text("%.1f с".format(v), fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                        HorizontalDivider(color = Panel2)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Итоговая длительность", color = Muted, modifier = Modifier.weight(1f))
                            Text(timeLabel, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("Например: 40 кадров × 0,1 секунды = 4 секунды видео.", color = Muted, fontSize = 12.sp)
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("ИНСТРУМЕНТЫ", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Всё важное — по отдельным разделам", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ToolTile("Кадры", "Порядок и длительность", Icons.Rounded.Collections) { selectedTab = "Монтаж" }
                    ToolTile("Плавность", "RIFE и Optical Flow", Icons.Rounded.Waves) { notice = "Раздел плавности будет подключён после интеграции нативных движков." }
                    ToolTile("Цвет", "Свет и настроение", Icons.Rounded.Palette) { notice = "Цветокоррекция запланирована для следующего этапа." }
                    ToolTile("Звук", "Музыка и громкость", Icons.Rounded.GraphicEq) { notice = "Аудиомонтаж запланирован для следующего этапа." }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lightbulb, tint = Violet, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Подсказка Лиры", fontWeight = FontWeight.Bold)
                        }
                        Text(notice, color = Muted, fontSize = 13.sp)
                    }
                }
            }

            if (images.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ДОБАВЛЕННЫЕ КАДРЫ", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { images = emptyList() }) { Text("Очистить") }
                    }
                }
                itemsIndexed(images) { index, uri ->
                    Row(
                        Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = Panel2, shape = RoundedCornerShape(10.dp)) {
                            Text("${index + 1}", modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp), color = Violet, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Кадр ${index + 1}", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(uri.lastPathSegment ?: "Изображение", color = Muted, fontSize = 11.sp, maxLines = 1)
                        }
                        IconButton(onClick = { images = images.toMutableList().also { it.removeAt(index) } }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Удалить кадр", tint = Muted)
                        }
                    }
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Понятно") } },
            title = { Text("ASTRA VIDEO STUDIO") },
            text = { Text("Редактор создаётся поэтапно. Сейчас это стартовый интерфейс и импорт изображений. Экспорт FFmpeg, RIFE Vulkan для Mali и Optical Flow подключаются отдельными проверяемыми этапами — приложение не будет притворяться, что функции уже работают.") },
            containerColor = Panel
        )
    }
}

@Composable
private fun ToolTile(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Panel,
        shape = RoundedCornerShape(17.dp),
        modifier = Modifier.width(158.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(25.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}
