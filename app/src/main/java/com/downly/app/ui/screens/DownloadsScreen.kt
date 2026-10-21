package com.downly.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.downly.app.engine.DownloadItem
import com.downly.app.engine.DownloadRepository
import com.downly.app.engine.DownloadStatus
import com.downly.app.engine.MediaKind
import com.downly.app.ui.MainViewModel
import com.downly.app.ui.components.PlatformDot
import com.downly.app.ui.theme.DColors

@Composable
fun DownloadsScreen(vm: MainViewModel, modifier: Modifier) {
    val items by vm.downloads.collectAsState()
    var filter by remember { mutableIntStateOf(0) } // 0 todo, 1 videos, 2 música
    var confirmDelete by remember { mutableStateOf<DownloadItem?>(null) }
    val context = LocalContext.current

    val shown = items.filter {
        when (filter) { 1 -> it.kind == MediaKind.VIDEO; 2 -> it.kind == MediaKind.AUDIO; else -> true }
    }

    Column(modifier.statusBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Descargas", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (items.any { !it.isActive }) {
                TextButton(onClick = { DownloadRepository.clearFinished() }) { Text("Limpiar historial") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todo", "Videos", "Música").forEachIndexed { i, label ->
                FilterChip(
                    selected = filter == i, onClick = { filter = i }, label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DColors.Coral.copy(alpha = .2f), selectedLabelColor = DColors.Text),
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (shown.isEmpty()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.CloudDownload, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text("Aún no tienes descargas", style = MaterialTheme.typography.titleMedium)
                Text("Pega un enlace en Inicio para empezar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(shown, key = { it.id }) { item ->
                    DownloadCard(
                        item,
                        onOpen = { open(context, item) },
                        onShare = { share(context, item) },
                        onCancel = { DownloadRepository.cancel(item.id) },
                        onRetry = { DownloadRepository.retry(item.id) },
                        onDelete = { confirmDelete = item },
                    )
                }
            }
        }
    }

    confirmDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Eliminar descarga") },
            text = { Text("¿Quitar \"${item.title.take(60)}\" de la lista?") },
            confirmButton = {
                TextButton(onClick = { DownloadRepository.remove(item.id, deleteFile = true); confirmDelete = null }) {
                    Text("Borrar también el archivo", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { DownloadRepository.remove(item.id, deleteFile = false); confirmDelete = null }) {
                    Text("Solo de la lista")
                }
            },
        )
    }
}

@Composable
private fun DownloadCard(
    item: DownloadItem,
    onOpen: () -> Unit, onShare: () -> Unit, onCancel: () -> Unit, onRetry: () -> Unit, onDelete: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, onClick = { if (item.status == DownloadStatus.DONE) onOpen() }) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 84.dp, height = 56.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black)) {
                    AsyncImage(item.thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Icon(
                        if (item.kind == MediaKind.AUDIO) Icons.Rounded.MusicNote else Icons.Rounded.PlayArrow, null,
                        tint = Color.White, modifier = Modifier.align(Alignment.Center).size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlatformDot(item.platform, 6.dp); Spacer(Modifier.width(6.dp))
                        Text("${item.platform.label} · ${item.optionTitle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                var menu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Más") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (item.status == DownloadStatus.DONE) {
                            DropdownMenuItem(text = { Text("Abrir") }, leadingIcon = { Icon(Icons.Rounded.OpenInNew, null) }, onClick = { menu = false; onOpen() })
                            DropdownMenuItem(text = { Text("Compartir") }, leadingIcon = { Icon(Icons.Rounded.Share, null) }, onClick = { menu = false; onShare() })
                        }
                        if (item.isActive) DropdownMenuItem(text = { Text("Cancelar") }, leadingIcon = { Icon(Icons.Rounded.Close, null) }, onClick = { menu = false; onCancel() })
                        if (item.status == DownloadStatus.FAILED || item.status == DownloadStatus.CANCELLED)
                            DropdownMenuItem(text = { Text("Reintentar") }, leadingIcon = { Icon(Icons.Rounded.Refresh, null) }, onClick = { menu = false; onRetry() })
                        DropdownMenuItem(text = { Text("Eliminar") }, leadingIcon = { Icon(Icons.Rounded.Delete, null) }, onClick = { menu = false; onDelete() })
                    }
                }
            }

            when (item.status) {
                DownloadStatus.QUEUED, DownloadStatus.RUNNING, DownloadStatus.PROCESSING -> {
                    Spacer(Modifier.height(10.dp))
                    if (item.status == DownloadStatus.RUNNING && item.progress > 0f) {
                        LinearProgressIndicator(
                            progress = { item.progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = DColors.Coral, trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    } else {
                        LinearProgressIndicator(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = DColors.Violet, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    val label = when (item.status) {
                        DownloadStatus.QUEUED -> "En cola…"
                        DownloadStatus.PROCESSING -> if (item.kind == MediaKind.AUDIO) "Convirtiendo a MP3…" else "Uniendo audio y video…"
                        else -> "${(item.progress * 100).toInt()}%" + if (item.etaSec > 0) " · quedan ${item.etaSec}s" else ""
                    }
                    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DownloadStatus.DONE -> {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, Modifier.size(16.dp), tint = DColors.Mint)
                        Spacer(Modifier.width(6.dp))
                        Text(item.displayPath ?: "Guardado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        TextButton(onClick = onShare) { Text("Compartir") }
                    }
                }
                DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ErrorOutline, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(6.dp))
                        Text(if (item.status == DownloadStatus.CANCELLED) "Cancelada" else item.error ?: "Error",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f), maxLines = 2)
                        TextButton(onClick = onRetry) { Text("Reintentar") }
                    }
                }
            }
        }
    }
}

private fun mime(item: DownloadItem) = if (item.kind == MediaKind.AUDIO) "audio/*" else "video/*"

private fun open(context: Context, item: DownloadItem) {
    val uri = item.contentUri?.let(Uri::parse) ?: return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime(item)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    } catch (_: ActivityNotFoundException) { }
}

private fun share(context: Context, item: DownloadItem) {
    val uri = item.contentUri?.let(Uri::parse) ?: return
    val send = Intent(Intent.ACTION_SEND).setType(mime(item)).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, "Compartir con"))
}
