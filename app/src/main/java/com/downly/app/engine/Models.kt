package com.downly.app.engine

import org.json.JSONObject

enum class MediaKind { VIDEO, AUDIO }

/** Una opción de descarga concreta (p. ej. "1080p MP4" o "MP3 320 kbps"). */
data class DownloadOption(
    val id: String,
    val kind: MediaKind,
    val title: String,          // "1080p"
    val subtitle: String,       // "Full HD · MP4"
    val formatSelector: String, // selector -f de yt-dlp
    val audioBitrate: Int = 0,  // solo MP3
    val sizeBytes: Long = 0,
    val premium: Boolean = false, // se desbloquea con anuncio de recompensa
    val badge: String? = null,    // "HD", "4K"...
)

data class MediaInfo(
    val url: String,
    val webpageUrl: String,
    val title: String,
    val uploader: String?,
    val thumbnail: String?,
    val durationSec: Int,
    val platform: Platform,
    val videoOptions: List<DownloadOption>,
    val audioOptions: List<DownloadOption>,
)

enum class DownloadStatus { QUEUED, RUNNING, PROCESSING, DONE, FAILED, CANCELLED }

data class DownloadItem(
    val id: String,
    val url: String,
    val title: String,
    val thumbnail: String?,
    val platform: Platform,
    val kind: MediaKind,
    val optionTitle: String,
    val formatSelector: String,
    val audioBitrate: Int,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f,
    val etaSec: Long = -1,
    val contentUri: String? = null,
    val displayPath: String? = null,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isActive get() = status == DownloadStatus.QUEUED || status == DownloadStatus.RUNNING || status == DownloadStatus.PROCESSING

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("url", url); put("title", title); put("thumbnail", thumbnail ?: "")
        put("platform", platform.name); put("kind", kind.name); put("optionTitle", optionTitle)
        put("formatSelector", formatSelector); put("audioBitrate", audioBitrate)
        put("status", status.name); put("progress", progress.toDouble()); put("contentUri", contentUri ?: "")
        put("displayPath", displayPath ?: ""); put("error", error ?: ""); put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(o: JSONObject): DownloadItem? = runCatching {
            DownloadItem(
                id = o.getString("id"),
                url = o.getString("url"),
                title = o.optString("title"),
                thumbnail = o.optString("thumbnail").ifBlank { null },
                platform = runCatching { Platform.valueOf(o.optString("platform")) }.getOrDefault(Platform.OTHER),
                kind = runCatching { MediaKind.valueOf(o.optString("kind")) }.getOrDefault(MediaKind.VIDEO),
                optionTitle = o.optString("optionTitle"),
                formatSelector = o.optString("formatSelector"),
                audioBitrate = o.optInt("audioBitrate"),
                status = runCatching { DownloadStatus.valueOf(o.optString("status")) }.getOrDefault(DownloadStatus.FAILED),
                progress = o.optDouble("progress", 0.0).toFloat(),
                contentUri = o.optString("contentUri").ifBlank { null },
                displayPath = o.optString("displayPath").ifBlank { null },
                error = o.optString("error").ifBlank { null },
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }.getOrNull()
    }
}

fun Long.toReadableSize(): String {
    if (this <= 0) return ""
    val kb = this / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> "%.2f GB".format(gb)
        mb >= 1 -> "%.1f MB".format(mb)
        else -> "%.0f KB".format(kb)
    }
}

fun Int.toDuration(): String {
    if (this <= 0) return ""
    val h = this / 3600; val m = (this % 3600) / 60; val s = this % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
