package com.downly.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.downly.app.MainActivity
import com.downly.app.ads.AdManager
import com.downly.app.ads.MediumRectangleAd
import com.downly.app.engine.*
import com.downly.app.ui.AnalyzeState
import com.downly.app.ui.MainViewModel
import com.downly.app.ui.components.GradientButton
import com.downly.app.ui.components.Logo
import com.downly.app.ui.components.PlatformDot

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(vm: MainViewModel, activity: MainActivity, onNeedPermissions: () -> Unit, modifier: Modifier) {
    val url by vm.url.collectAsState()
    val state by vm.analyze.collectAsState()
    val engine by vm.engineState.collectAsState()
    val clipboard = LocalClipboardManager.current
    val focus = LocalFocusManager.current

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ---- cabecera
        Row(verticalAlignment = Alignment.CenterVertically) {
            Logo(40.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Downly", style = MaterialTheme.typography.headlineLarge)
                Text("Videos y música, sin marca de agua", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ---- entrada de enlace
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = vm::setUrl,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Pega aquí el enlace del video") },
                    leadingIcon = { Icon(Icons.Rounded.Link, null) },
                    trailingIcon = {
                        if (url.isNotEmpty()) IconButton(onClick = vm::reset) { Icon(Icons.Rounded.Close, "Borrar") }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { focus.clearFocus(); vm.analyzeNow() }),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            val text = clipboard.getText()?.text
                            val link = Platform.extractUrl(text)
                            if (link != null) { vm.setUrl(link); focus.clearFocus(); vm.analyzeNow() }
                            else vm.toast("No hay ningún enlace en el portapapeles")
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.ContentPaste, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp)); Text("Pegar")
                    }
                    GradientButton(
                        text = if (state is AnalyzeState.Loading) "Analizando…" else "Buscar",
                        icon = Icons.Rounded.Search,
                        enabled = url.isNotBlank() && state !is AnalyzeState.Loading,
                        modifier = Modifier.weight(1.4f),
                    ) { focus.clearFocus(); vm.analyzeNow() }
                }
            }
        }

        if (engine is Engine.State.Loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Preparando el motor de descarga…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        AnimatedContent(targetState = state, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "analyze") { s ->
            when (s) {
                AnalyzeState.Idle -> IdleContent()
                AnalyzeState.Loading -> LoadingCard()
                is AnalyzeState.Error -> ErrorCard(s.message, onRetry = vm::analyzeNow)
                is AnalyzeState.Result -> ResultCard(
                    info = s.info,
                    onDownload = { opt ->
                        onNeedPermissions()
                        vm.download(s.info, opt)
                        AdManager.onDownloadStarted(activity)
                    },
                    onUnlock = { opt ->
                        AdManager.showRewarded(activity) { earned, shown ->
                            if (earned) {
                                vm.toast(if (shown) "¡Desbloqueado! Calidad máxima por 30 min" else "Calidad máxima desbloqueada")
                                onNeedPermissions()
                                vm.download(s.info, opt)
                            } else vm.toast("Mira el anuncio completo para desbloquear")
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdleContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Compatible con", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Platform.featured.forEach { p ->
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        PlatformDot(p); Spacer(Modifier.width(8.dp))
                        Text(p.label, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface) {
                Text("+1000 sitios", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Cómo usar Downly", style = MaterialTheme.typography.titleMedium)
                Step(1, "En TikTok, Instagram, YouTube… toca Compartir → Copiar enlace (o Compartir → Downly).")
                Step(2, "Pégalo aquí y toca Buscar.")
                Step(3, "Elige la calidad o MP3 y listo: se guarda en tu galería o en tu música.")
            }
        }

        MediumRectangleAd()
    }
}

@Composable
private fun Step(n: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Text("$n", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LoadingCard() {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(14.dp))
            Text("Buscando las mejores calidades…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .4f))) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(10.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
            TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) { Text("Reintentar") }
        }
    }
}

@Composable
private fun ResultCard(info: MediaInfo, onDownload: (DownloadOption) -> Unit, onUnlock: (DownloadOption) -> Unit) {
    var audioTab by remember(info) { mutableStateOf(false) }
    val unlockedUntil by AdManager.unlockedUntil.collectAsState()
    val unlocked = AdManager.isPremiumUnlocked() || unlockedUntil > System.currentTimeMillis()

    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
                AsyncImage(
                    model = info.thumbnail, contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                )
                Row(Modifier.padding(10.dp).align(Alignment.TopStart)) {
                    Surface(shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = .6f)) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            PlatformDot(info.platform); Spacer(Modifier.width(6.dp))
                            Text(info.platform.label, style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
                if (info.durationSec > 0) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Color.Black.copy(alpha = .7f), modifier = Modifier.padding(10.dp).align(Alignment.BottomEnd)) {
                        Text(info.durationSec.toDuration(), Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(info.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                info.uploader?.let { Text("@$it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }

                // Selector Video / MP3
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp),
                ) {
                    SegTab("Video", Icons.Rounded.Movie, !audioTab, Modifier.weight(1f)) { audioTab = false }
                    SegTab("Audio MP3", Icons.Rounded.MusicNote, audioTab, Modifier.weight(1f)) { audioTab = true }
                }

                val options = if (audioTab) info.audioOptions else info.videoOptions
                options.forEach { opt ->
                    OptionRow(opt, locked = opt.premium && !unlocked) {
                        if (opt.premium && !unlocked) onUnlock(opt) else onDownload(opt)
                    }
                }

                if (!unlocked && options.any { it.premium }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.OndemandVideo, null, Modifier.size(16.dp), tint = com.downly.app.ui.theme.DColors.Gold)
                        Spacer(Modifier.width(6.dp))
                        Text("Mira un anuncio corto para desbloquear Full HD, 4K y MP3 320 kbps durante 30 min.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SegTab(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(18.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.titleSmall, color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OptionRow(opt: DownloadOption, locked: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(opt.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                opt.badge?.let {
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(5.dp), color = if (opt.premium) com.downly.app.ui.theme.DColors.Gold else MaterialTheme.colorScheme.secondary) {
                        Text(it, Modifier.padding(horizontal = 5.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall, color = Color.Black)
                    }
                }
            }
            val size = opt.sizeBytes.toReadableSize()
            Text(listOf(opt.subtitle, if (size.isNotEmpty()) "≈ $size" else null).filterNotNull().joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (locked) {
            Surface(shape = RoundedCornerShape(50), color = com.downly.app.ui.theme.DColors.Gold.copy(alpha = .15f)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PlayCircle, null, Modifier.size(16.dp), tint = com.downly.app.ui.theme.DColors.Gold)
                    Spacer(Modifier.width(4.dp))
                    Text("Desbloquear", style = MaterialTheme.typography.labelSmall, color = com.downly.app.ui.theme.DColors.Gold)
                }
            }
        } else {
            Box(Modifier.size(38.dp).clip(CircleShape).background(com.downly.app.ui.theme.DColors.Brand), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Download, "Descargar", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}
