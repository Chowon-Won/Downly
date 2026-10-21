package com.downly.app.engine

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import java.util.UUID

/** Lista de descargas (en curso + historial), persistida en SharedPreferences. */
object DownloadRepository {
    private const val PREFS = "downly_downloads"
    private const val KEY = "items"

    private lateinit var appContext: Context
    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items: StateFlow<List<DownloadItem>> = _items

    fun init(context: Context) {
        appContext = context.applicationContext
        val raw = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        val loaded = (0 until arr.length()).mapNotNull { DownloadItem.fromJson(arr.getJSONObject(it)) }
            // Si la app se cerró a mitad, las marcamos como fallidas (WorkManager las reanudará si sigue vivo)
            .map { if (it.isActive) it.copy(status = DownloadStatus.FAILED, error = "Interrumpida") else it }
        _items.value = loaded.sortedByDescending { it.createdAt }
    }

    fun get(id: String) = _items.value.firstOrNull { it.id == id }

    fun enqueue(info: MediaInfo, option: DownloadOption): DownloadItem {
        val item = DownloadItem(
            id = UUID.randomUUID().toString(),
            url = info.webpageUrl.ifBlank { info.url },
            title = info.title,
            thumbnail = info.thumbnail,
            platform = info.platform,
            kind = option.kind,
            optionTitle = option.title,
            formatSelector = option.formatSelector,
            audioBitrate = option.audioBitrate,
        )
        _items.update { listOf(item) + it }
        persist()
        start(item.id)
        return item
    }

    fun retry(id: String) {
        update(id) { it.copy(status = DownloadStatus.QUEUED, progress = 0f, error = null) }
        start(id)
    }

    private fun start(id: String) {
        val req = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(DownloadWorker.KEY_ID to id))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("download")
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(id, ExistingWorkPolicy.REPLACE, req)
    }

    fun cancel(id: String) {
        runCatching { YoutubeDL.getInstance().destroyProcessById(id) }
        WorkManager.getInstance(appContext).cancelUniqueWork(id)
        update(id) { it.copy(status = DownloadStatus.CANCELLED) }
    }

    fun remove(id: String, deleteFile: Boolean) {
        val item = get(id) ?: return
        if (item.isActive) cancel(id)
        if (deleteFile) item.contentUri?.let { uri ->
            runCatching { appContext.contentResolver.delete(Uri.parse(uri), null, null) }
        }
        _items.update { list -> list.filterNot { it.id == id } }
        persist()
    }

    fun clearFinished() {
        _items.update { list -> list.filter { it.isActive } }
        persist()
    }

    fun update(id: String, persistNow: Boolean = true, block: (DownloadItem) -> DownloadItem) {
        _items.update { list -> list.map { if (it.id == id) block(it) else it } }
        if (persistNow) persist()
    }

    @Synchronized
    private fun persist() {
        val arr = JSONArray()
        _items.value.take(300).forEach { arr.put(it.toJson()) }
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }
}
