package com.downly.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.downly.app.BuildConfig
import com.downly.app.engine.Engine
import com.downly.app.ui.MainViewModel
import com.downly.app.ui.components.Logo
import com.downly.app.ui.theme.DColors

@Composable
fun SettingsScreen(vm: MainViewModel, modifier: Modifier) {
    val context = LocalContext.current
    val updating by vm.updating.collectAsState()
    val engine by vm.engineState.collectAsState()
    var version by remember { mutableStateOf("—") }
    LaunchedEffect(engine, updating) { if (engine is Engine.State.Ready && !updating) version = Engine.version(context) }

    Column(
        modifier.verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall)

        Card(Icons.Rounded.Bolt, "Motor de descarga", "Versión $version") {
            Text(
                "Si un enlace deja de funcionar, actualiza el motor: las plataformas cambian a menudo.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = vm::updateEngine, enabled = !updating && engine is Engine.State.Ready, shape = RoundedCornerShape(12.dp)) {
                if (updating) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp)); Text("Actualizando…")
                } else {
                    Icon(Icons.Rounded.SystemUpdate, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Actualizar motor")
                }
            }
            if (engine is Engine.State.Error) {
                Spacer(Modifier.height(8.dp))
                Text((engine as Engine.State.Error).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(Icons.Rounded.Folder, "Dónde se guardan", null) {
            Text("Videos: Películas/Downly  ·  Música: Música/Downly", style = MaterialTheme.typography.bodyMedium)
            Text("Aparecen en tu galería y en tu reproductor de música.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Card(Icons.Rounded.Share, "Truco", null) {
            Text("Desde cualquier app toca Compartir → Downly y el enlace se analiza solo.", style = MaterialTheme.typography.bodyMedium)
        }

        Surface(shape = RoundedCornerShape(18.dp), color = DColors.Violet.copy(alpha = .15f), border = BorderStroke(1.dp, DColors.Violet.copy(alpha = .4f))) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PhoneIphone, null, tint = DColors.Violet)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Downly para iOS", style = MaterialTheme.typography.titleMedium)
                    Text("Disponible pronto", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Card(Icons.Rounded.Gavel, "Uso responsable", null) {
            Text(
                "Descarga solo contenido propio, de dominio público o para el que tengas permiso del autor. " +
                    "Respeta los derechos de autor y los términos de cada plataforma.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Logo(22.dp); Spacer(Modifier.width(8.dp))
            Text("Downly ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Card(icon: ImageVector, title: String, subtitle: String?, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
