package com.downly.app.engine

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.downly.app.DownlyApp
import com.downly.app.MainActivity
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DownloadWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    companion object {
        const val KEY_ID = "id"
    }

    private val id: String get() = inputData.getString(KEY_ID).orEmpty()
    private val notifId get() = id.hashCode()

    override suspend fun getForegroundInfo(): ForegroundInfo = foreground(0f, "Preparando…")

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val item = DownloadRepository.get(id) ?: return@withContext Result.failure()
        if (!Engine.awaitReady()) {
            DownloadRepository.update(id) { it.copy(status = DownloadStatus.FAILED, error = "Motor no disponible") }
            return@withContext Result.failure()
        }
        runCatching { setForeground(foreground(0f, item.title)) }
        DownloadRepository.update(id) { it.copy(status = DownloadStatus.RUNNING, progress = 0f, error = null) }

        val workDir = File(applicationContext.cacheDir, "dl/$id").apply { deleteRecursively(); mkdirs() }
        try {
            val req = YoutubeDLRequest(item.url).apply {
                addOption("--no-playlist")
                addOption("--no-mtime")
                addOption("--no-warnings")
                addOption("--newline")
                addOption("--socket-timeout", "30")
                addOption("--retries", "5")
                addOption("--fragment-retries", "5")
                addOption("-o", "${workDir.absolutePath}/%(title).70B [%(id)s].%(ext)s")
                addOption("-f", item.formatSelector)
                if (item.kind == MediaKind.AUDIO) {
                    addOption("-x")
                    addOption("--audio-format", "mp3")
                    addOption("--audio-quality", "${item.audioBitrate.coerceAtLeast(64)}K")
                } else {
                    addOption("--merge-output-format", "mp4")
                    addOption("--remux-video", "mp4")
                }
            }

            var lastUi = 0L
            YoutubeDL.getInstance().execute(req, id) { progress, eta, _ ->
                val now = System.currentTimeMillis()
                if (now - lastUi > 400) {
                    lastUi = now
                    val p = (progress / 100f).coerceIn(0f, 1f)
                    val status = if (progress >= 100f) DownloadStatus.PROCESSING else DownloadStatus.RUNNING
                    DownloadRepository.update(id, persistNow = false) {
                        it.copy(progress = p, etaSec = eta, status = status)
                    }
                    notify(p, item.title)
                }
            }

            DownloadRepository.update(id, persistNow = false) { it.copy(status = DownloadStatus.PROCESSING, progress = 1f) }
            val output = workDir.listFiles()
                ?.filter { it.isFile && !it.name.endsWith(".part") && !it.name.endsWith(".ytdl") && it.length() > 0 }
                ?.maxByOrNull { it.length() }
                ?: error("No se generó ningún archivo")

            val saved = MediaSaver.save(applicationContext, output, item.kind)
            DownloadRepository.update(id) {
                it.copy(status = DownloadStatus.DONE, progress = 1f, contentUri = saved.uri.toString(), displayPath = saved.displayPath)
            }
            notifyDone(item.title, ok = true)
            Result.success()
        } catch (c: CancellationException) {
            runCatching { YoutubeDL.getInstance().destroyProcessById(id) }
            DownloadRepository.update(id) { it.copy(status = DownloadStatus.CANCELLED) }
            throw c
        } catch (t: Throwable) {
            val current = DownloadRepository.get(id)
            if (current?.status != DownloadStatus.CANCELLED) {
                DownloadRepository.update(id) { it.copy(status = DownloadStatus.FAILED, error = friendlyError(t)) }
                notifyDone(item.title, ok = false)
            }
            Result.failure()
        } finally {
            workDir.deleteRecursively()
        }
    }

    // ------------------------------------------------------------ notificaciones

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        applicationContext, 0,
        Intent(applicationContext, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_DOWNLOADS, true)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun buildProgress(p: Float, title: String) =
        NotificationCompat.Builder(applicationContext, DownlyApp.CHANNEL_DOWNLOADS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(if (p >= 1f) "Procesando…" else "${(p * 100).toInt()}%")
            .setProgress(100, (p * 100).toInt(), p <= 0f)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent())
            .build()

    private fun foreground(p: Float, title: String): ForegroundInfo {
        val n = buildProgress(p, title)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ForegroundInfo(notifId, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else ForegroundInfo(notifId, n)
    }

    private fun notify(p: Float, title: String) {
        runCatching { NotificationManagerCompat.from(applicationContext).notify(notifId, buildProgress(p, title)) }
    }

    private fun notifyDone(title: String, ok: Boolean) {
        val n = NotificationCompat.Builder(applicationContext, DownlyApp.CHANNEL_DOWNLOADS)
            .setSmallIcon(if (ok) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
            .setContentTitle(if (ok) "Descarga completada" else "La descarga falló")
            .setContentText(title)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .build()
        runCatching { NotificationManagerCompat.from(applicationContext).notify(notifId + 1, n) }
    }

    private fun friendlyError(t: Throwable): String {
        val msg = t.message.orEmpty()
        return when {
            msg.contains("login", true) || msg.contains("private", true) -> "Contenido privado o requiere iniciar sesión"
            msg.contains("Unsupported URL", true) -> "Enlace no compatible"
            msg.contains("HTTP Error 403", true) -> "Acceso denegado por el sitio. Actualiza el motor en Ajustes"
            msg.contains("Unable to resolve", true) || msg.contains("network", true) -> "Sin conexión a internet"
            else -> msg.lines().lastOrNull { it.contains("ERROR") }?.substringAfter("ERROR:")?.trim()?.take(160)
                ?: "Error desconocido"
        }
    }
}
