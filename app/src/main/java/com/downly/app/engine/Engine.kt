package com.downly.app.engine

import android.content.Context
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Envoltorio sobre yt-dlp + ffmpeg. */
object Engine {
    private const val TAG = "DownlyEngine"

    sealed interface State {
        data object Loading : State
        data object Ready : State
        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state
    private val ready = CompletableDeferred<Boolean>()

    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        try {
            YoutubeDL.getInstance().init(context.applicationContext)
            FFmpeg.getInstance().init(context.applicationContext)
            _state.value = State.Ready
            ready.complete(true)
        } catch (t: Throwable) {
            Log.e(TAG, "init failed", t)
            _state.value = State.Error(t.message ?: "No se pudo iniciar el motor")
            ready.complete(false)
        }
    }

    suspend fun awaitReady(): Boolean = ready.await()

    fun version(context: Context): String =
        runCatching { YoutubeDL.getInstance().version(context.applicationContext) }.getOrNull() ?: "—"

    /** Actualiza yt-dlp (los sitios cambian a menudo; esto arregla la mayoría de fallos). */
    suspend fun update(context: Context): String = withContext(Dispatchers.IO) {
        val status = YoutubeDL.getInstance().updateYoutubeDL(context.applicationContext)
        status?.toString() ?: "OK"
    }

    // ---------------------------------------------------------------- análisis

    suspend fun analyze(url: String): MediaInfo = withContext(Dispatchers.IO) {
        check(awaitReady()) { "El motor de descarga no se pudo iniciar" }
        val req = YoutubeDLRequest(url).apply {
            addOption("--dump-single-json")
            addOption("--no-playlist")
            addOption("--no-warnings")
            addOption("--socket-timeout", "20")
        }
        val resp = YoutubeDL.getInstance().execute(req)
        val json = JSONObject(resp.out.trim().lines().last { it.startsWith("{") })
        parse(url, json)
    }

    private val noWatermark = Regex("watermark", RegexOption.IGNORE_CASE)

    private data class F(
        val id: String, val ext: String, val w: Int, val h: Int, val hasVideo: Boolean,
        val hasAudio: Boolean, val size: Long, val tbr: Double, val abr: Double, val note: String,
    ) {
        val shortSide get() = if (w > 0 && h > 0) minOf(w, h) else h
    }

    private fun parse(originalUrl: String, j: JSONObject): MediaInfo {
        // Si es un carrusel/lista, nos quedamos con el primer elemento con formatos
        val root = if (!j.has("formats") && j.optJSONArray("entries") != null) {
            j.getJSONArray("entries").optJSONObject(0) ?: j
        } else j

        val platform = Platform.detect(root.optString("webpage_url", originalUrl)).let {
            if (it == Platform.OTHER) Platform.detect(originalUrl) else it
        }

        val formats = (root.optJSONArray("formats") ?: JSONArray()).let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val vcodec = o.optString("vcodec", "none")
                val acodec = o.optString("acodec", "none")
                val h = o.optInt("height", 0)
                val note = o.optString("format_note", "") + " " + o.optString("format", "")
                F(
                    id = o.optString("format_id"),
                    ext = o.optString("ext"),
                    w = o.optInt("width", 0),
                    h = h,
                    hasVideo = vcodec != "none" && (h > 0 || vcodec.isNotBlank()),
                    hasAudio = acodec != "none" && acodec.isNotBlank(),
                    size = o.optLong("filesize", 0).takeIf { it > 0 } ?: o.optLong("filesize_approx", 0),
                    tbr = o.optDouble("tbr", 0.0).let { if (it.isNaN()) 0.0 else it },
                    abr = o.optDouble("abr", 0.0).let { if (it.isNaN()) 0.0 else it },
                    note = note,
                )
            }
        }.filter { it.id.isNotBlank() }.let { all ->
            // Sin marca de agua (TikTok): descartamos los formatos marcados si hay alternativa limpia
            val clean = all.filterNot { it.note.contains(noWatermark) }
            if (clean.any { it.hasVideo }) clean else all
        }

        val bestAudio = formats.filter { it.hasAudio && !it.hasVideo }.maxByOrNull { it.abr + it.tbr }
        val duration = root.optDouble("duration", 0.0).let { if (it.isNaN()) 0 else it.toInt() }

        // ---- video: una opción por resolución (el mejor formato de cada una)
        val byRes = formats.filter { it.hasVideo && it.shortSide > 0 }
            .groupBy { normalize(it.shortSide) }
            .mapValues { (_, list) ->
                list.maxWith(compareBy<F>({ it.hasAudio }, { it.ext == "mp4" }, { it.tbr }))
            }
            .toSortedMap(compareByDescending { it })

        val video = byRes.map { (res, f) ->
            val selector = if (f.hasAudio) "${f.id}/best"
            else "${f.id}+bestaudio[ext=m4a]/${f.id}+bestaudio/${f.id}/best"
            val size = f.size + if (!f.hasAudio) (bestAudio?.size ?: 0) else 0
            DownloadOption(
                id = "v$res",
                kind = MediaKind.VIDEO,
                title = "${res}p",
                subtitle = listOfNotNull(resName(res), "MP4").joinToString(" · "),
                formatSelector = selector,
                sizeBytes = size,
                premium = res >= 1080,
                badge = when { res >= 2160 -> "4K"; res >= 1440 -> "2K"; res >= 1080 -> "FHD"; res >= 720 -> "HD"; else -> null },
            )
        }.ifEmpty {
            listOf(
                DownloadOption(
                    id = "vbest", kind = MediaKind.VIDEO, title = "Mejor calidad",
                    subtitle = "MP4 · automática",
                    formatSelector = "bv*+ba/b", sizeBytes = 0,
                )
            )
        }

        val audioSize = { kbps: Int -> if (duration > 0) duration.toLong() * kbps * 1000 / 8 else 0L }
        val audio = listOf(
            DownloadOption("a320", MediaKind.AUDIO, "MP3 320 kbps", "Máxima calidad", "bestaudio/best", 320, audioSize(320), premium = true, badge = "HQ"),
            DownloadOption("a192", MediaKind.AUDIO, "MP3 192 kbps", "Alta calidad", "bestaudio/best", 192, audioSize(192)),
            DownloadOption("a128", MediaKind.AUDIO, "MP3 128 kbps", "Ligero", "bestaudio/best", 128, audioSize(128)),
        )

        return MediaInfo(
            url = originalUrl,
            webpageUrl = root.optString("webpage_url", originalUrl),
            title = root.optString("title").ifBlank { root.optString("description").take(80) }.ifBlank { "Video" },
            uploader = root.optString("uploader").ifBlank { root.optString("channel") }.ifBlank { null },
            thumbnail = root.optString("thumbnail").ifBlank { null },
            durationSec = duration,
            platform = platform,
            videoOptions = video,
            audioOptions = audio,
        )
    }

    private val ladder = listOf(4320, 2160, 1440, 1080, 720, 540, 480, 360, 240, 144)
    private fun normalize(px: Int): Int = ladder.minBy { kotlin.math.abs(it - px) }

    private fun resName(res: Int) = when {
        res >= 4320 -> "8K"; res >= 2160 -> "Ultra HD"; res >= 1440 -> "Quad HD"
        res >= 1080 -> "Full HD"; res >= 720 -> "HD"; res >= 480 -> "SD"; else -> "Baja"
    }
}
