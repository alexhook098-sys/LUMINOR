package com.astra.videostudio

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

private val Ink = Color(0xFF0B0D16)
private val Panel = Color(0xFF171B2B)
private val Panel2 = Color(0xFF22283D)
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
    val context = LocalContext.current
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var frameDuration by remember { mutableFloatStateOf(0.10f) }
    var inEditor by remember { mutableStateOf(false) }
    var selectedFrame by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var selectedTool by remember { mutableStateOf("Монтаж") }
    var notice by remember { mutableStateOf("Добавь изображения — можно выбрать сразу много кадров.") }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { picked ->
        if (picked.isNotEmpty()) {
            picked.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            images = (images + picked).distinct()
            selectedFrame = 0
            notice = "Добавлено кадров: \${picked.size}. Всего: \${images.size}."
        }
    }

    val totalSeconds = images.size * frameDuration
    val timeLabel = "%.2f сек".format(totalSeconds)

    LaunchedEffect(isPlaying, images.size, frameDuration) {
        while (isPlaying && images.isNotEmpty()) {
            delay((frameDuration * 1000f).toLong().coerceAtLeast(50L))
            selectedFrame = (selectedFrame + 1) % images.size
        }
    }

    if (!inEditor) {
        ImportScreen(
            images = images,
            frameDuration = frameDuration,
            totalLabel = timeLabel,
            notice = notice,
            onAdd = { picker.launch(arrayOf("image/*")) },
            onDuration = { frameDuration = it },
            onRemove = { index -> images = images.toMutableList().also { it.removeAt(index) } },
            onClear = { images = emptyList() },
            onContinue = { selectedFrame = 0; inEditor = true }
        )
    } else {
        EditorScreen(
            images = images,
            selectedFrame = selectedFrame,
            isPlaying = isPlaying,
            duration = frameDuration,
            totalLabel = timeLabel,
            selectedTool = selectedTool,
            onBack = { isPlaying = false; inEditor = false },
            onAdd = { picker.launch(arrayOf("image/*")) },
            onSelectFrame = { selectedFrame = it; isPlaying = false },
            onPlay = { isPlaying = !isPlaying },
            onTool = { selectedTool = it },
            onRemove = {
                images = images.toMutableList().also { if (selectedFrame in it.indices) it.removeAt(selectedFrame) }
                selectedFrame = (selectedFrame - 1).coerceAtLeast(0)
                isPlaying = false
            },
            onMove = { delta ->
                val target = selectedFrame + delta
                if (target in images.indices) {
                    images = images.toMutableList().also { list ->
                        val item = list.removeAt(selectedFrame)
                        list.add(target, item)
                    }
                    selectedFrame = target
                }
            }
        )
    }
}

@Composable
private fun ImportScreen(
    images: List<Uri>, frameDuration: Float, totalLabel: String, notice: String,
    onAdd: () -> Unit, onDuration: (Float) -> Unit, onRemove: (Int) -> Unit,
    onClear: () -> Unit, onContinue: () -> Unit
) {
    Scaffold(containerColor = Ink) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ASTRA", color = Violet, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Text("VIDEO STUDIO", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Создай свой видеоряд", color = Muted, fontSize = 13.sp)
                }
                Text("\${images.size} кадров", color = Violet, fontWeight = FontWeight.Bold)
            }

            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(23.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("ПРОЕКТ", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    if (images.isEmpty()) {
                        Box(
                            Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp)).background(Panel2),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.Movie, null, tint = Violet, modifier = Modifier.size(42.dp))
                                Text("Добавь изображения", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Они появятся на монтажной ленте", color = Muted, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Последовательность готова", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(totalLabel, color = Violet, fontWeight = FontWeight.Bold)
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            itemsIndexed(images) { index, uri ->
                                Box {
                                    ImageThumb(uri, Modifier.size(width = 72.dp, height = 92.dp))
                                    Surface(
                                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                                        color = Ink.copy(alpha = 0.8f), shape = CircleShape
                                    ) { Text("\${index + 1}", modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = Color.White, fontSize = 10.sp) }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = onAdd, modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Violet)
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (images.isEmpty()) "Добавить изображения" else "Добавить ещё кадры", fontWeight = FontWeight.Bold)
                    }
                    Text(notice, color = Muted, fontSize = 12.sp)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Длительность кадра", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("Одинаковая для всех изображений", color = Muted, fontSize = 12.sp)
                        }
                        Text("%.2f с".format(frameDuration), color = Violet, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Slider(value = frameDuration, onValueChange = onDuration, valueRange = 0.05f..2f, steps = 38)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.1f, 0.2f, 0.5f, 1f).forEach { value ->
                            FilterChip(
                                selected = frameDuration == value, onClick = { onDuration(value) },
                                label = { Text("%.1f c".format(value)) }
                            )
                        }
                    }
                    HorizontalDivider(color = Panel2)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Итоговая длительность", color = Muted, modifier = Modifier.weight(1f))
                        Text(totalLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    }
                }
            }

            if (images.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("КАДРЫ: \${images.size}", color = Muted, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClear) { Text("Очистить") }
                }
                Button(
                    onClick = onContinue, modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Violet)
                ) {
                    Text("Продолжить в редактор", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Rounded.ArrowForward, null)
                }
                Text("Кадры можно будет переставлять и удалять уже на монтажной ленте.", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EditorScreen(
    images: List<Uri>, selectedFrame: Int, isPlaying: Boolean, duration: Float,
    totalLabel: String, selectedTool: String, onBack: () -> Unit, onAdd: () -> Unit,
    onSelectFrame: (Int) -> Unit, onPlay: () -> Unit, onTool: (String) -> Unit,
    onRemove: () -> Unit, onMove: (Int) -> Unit
) {
    val current = images.getOrNull(selectedFrame)
    Scaffold(
        containerColor = Ink,
        topBar = {
            Row(
                Modifier.fillMaxWidth().background(Panel).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text("МОНТАЖ", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("\${images.size} кадров · $totalLabel", color = Muted, fontSize = 11.sp)
                }
                TextButton(onClick = { onTool("Экспорт") }) { Text("Экспорт", color = Violet, fontWeight = FontWeight.Bold) }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                listOf("Монтаж", "Звук", "Текст", "Эффекты").forEach { tool ->
                    NavigationBarItem(
                        selected = selectedTool == tool, onClick = { onTool(tool) },
                        icon = {
                            Icon(when (tool) {
                                "Звук" -> Icons.Rounded.MusicNote
                                "Текст" -> Icons.Rounded.TextFields
                                "Эффекты" -> Icons.Rounded.AutoAwesome
                                else -> Icons.Rounded.Tune
                            }, null)
                        },
                        label = { Text(tool, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.fillMaxWidth().weight(1f).padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(18.dp)).background(Color(0xFF05060A)),
                contentAlignment = Alignment.Center
            ) {
                if (current != null) {
                    ImageThumb(current, Modifier.fillMaxSize(), ContentScale.Fit)
                } else {
                    Text("Добавь кадры, чтобы начать монтаж", color = Muted)
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "КАДР \${if (images.isEmpty()) 0 else selectedFrame + 1} / \${images.size}",
                    color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
                Text("%.2f c · $totalLabel".format(duration), color = Muted, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onSelectFrame((selectedFrame - 1).coerceAtLeast(0)) }, enabled = selectedFrame > 0) {
                        Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White)
                    }
                    FilledIconButton(onClick = onPlay, enabled = images.isNotEmpty(), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Violet)) {
                        Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Ink)
                    }
                    IconButton(onClick = { onSelectFrame((selectedFrame + 1).coerceAtMost(images.lastIndex)) }, enabled = selectedFrame < images.lastIndex) {
                        Icon(Icons.Rounded.SkipNext, null, tint = Color.White)
                    }
                }
            }

            Card(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ВРЕМЕННАЯ ШКАЛА", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), letterSpacing = 1.5.sp)
                        TextButton(onClick = onAdd) { Icon(Icons.Rounded.Add, null); Text("Кадры") }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        itemsIndexed(images) { index, uri ->
                            Column(
                                Modifier.width(76.dp).clip(RoundedCornerShape(10.dp))
                                    .background(if (index == selectedFrame) Panel2 else Ink)
                                    .clickable { onSelectFrame(index) }.padding(4.dp)
                            ) {
                                Box {
                                    ImageThumb(uri, Modifier.fillMaxWidth().height(76.dp))
                                    Text(
                                        "\${index + 1}", color = Color.White, fontSize = 10.sp,
                                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                                            .background(Ink.copy(alpha = 0.75f), CircleShape).padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Text("%.1fс".format(duration), color = if (index == selectedFrame) Violet else Muted, fontSize = 10.sp)
                            }
                        }
                    }
                    if (images.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Выбран кадр \${selectedFrame + 1}", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onMove(-1) }, enabled = selectedFrame > 0) { Icon(Icons.Rounded.ArrowBack, "Переместить влево", tint = Muted) }
                            IconButton(onClick = { onMove(1) }, enabled = selectedFrame < images.lastIndex) { Icon(Icons.Rounded.ArrowForward, "Переместить вправо", tint = Muted) }
                            IconButton(onClick = onRemove) { Icon(Icons.Rounded.Delete, "Удалить кадр", tint = Color(0xFFFF8B9A)) }
                        }
                    }
                }
            }

            ToolPanel(selectedTool)
        }
    }
}

@Composable
private fun ToolPanel(tool: String) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            when (tool) {
                "Звук" -> {
                    Text("ЗВУК", color = Violet, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Добавление аудиодорожки — следующий этап разработки.", color = Muted, fontSize = 12.sp)
                }
                "Текст" -> {
                    Text("ТЕКСТ", color = Violet, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Титры и надписи появятся в следующем обновлении.", color = Muted, fontSize = 12.sp)
                }
                "Эффекты" -> {
                    Text("ЭФФЕКТЫ", color = Violet, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Переходы, цвет и плавность подключим отдельными инструментами.", color = Muted, fontSize = 12.sp)
                }
                "Экспорт" -> {
                    Text("ЭКСПОРТ MP4", color = Violet, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Подготовка MP4-экспорта ещё не завершена. Кнопка пока показывает статус, а не создаёт пустой файл.", color = Muted, fontSize = 12.sp)
                }
                else -> {
                    Text("МОНТАЖ", color = Violet, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Выбери кадр на ленте. Его можно переместить, удалить или просмотреть.", color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ImageThumb(uri: Uri, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Crop) {
    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(9.dp)),
        factory = { ctx ->
            ImageView(ctx).apply {
                this.scaleType = if (scale == ContentScale.Fit) ImageView.ScaleType.FIT_CENTER else ImageView.ScaleType.CENTER_CROP
                adjustViewBounds = true
                setBackgroundColor(android.graphics.Color.rgb(24, 28, 43))
            }
        },
        update = { view ->
            view.scaleType = if (scale == ContentScale.Fit) ImageView.ScaleType.FIT_CENTER else ImageView.ScaleType.CENTER_CROP
            runCatching { view.setImageURI(null); view.setImageURI(uri) }
        }
    )
}
