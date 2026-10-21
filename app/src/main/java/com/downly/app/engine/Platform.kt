package com.downly.app.engine

/** Plataformas reconocidas. Downly acepta además cualquier sitio que soporte yt-dlp (+1000). */
enum class Platform(val label: String, val colorHex: Long, private val hosts: List<String>) {
    TIKTOK("TikTok", 0xFF25F4EE, listOf("tiktok.com", "vm.tiktok.com", "vt.tiktok.com")),
    INSTAGRAM("Instagram", 0xFFE1306C, listOf("instagram.com", "instagr.am")),
    YOUTUBE("YouTube", 0xFFFF3B30, listOf("youtube.com", "youtu.be", "music.youtube.com")),
    FACEBOOK("Facebook", 0xFF1877F2, listOf("facebook.com", "fb.watch", "fb.com")),
    TWITTER("X / Twitter", 0xFFE7E9EA, listOf("twitter.com", "x.com", "t.co")),
    PINTEREST("Pinterest", 0xFFE60023, listOf("pinterest.com", "pin.it")),
    REDDIT("Reddit", 0xFFFF4500, listOf("reddit.com", "redd.it")),
    VIMEO("Vimeo", 0xFF1AB7EA, listOf("vimeo.com")),
    TWITCH("Twitch", 0xFF9146FF, listOf("twitch.tv")),
    SOUNDCLOUD("SoundCloud", 0xFFFF5500, listOf("soundcloud.com")),
    OTHER("Web", 0xFF9AA0B4, emptyList());

    companion object {
        fun detect(url: String): Platform {
            val host = runCatching { java.net.URI(url.trim()).host?.lowercase() }.getOrNull() ?: return OTHER
            return entries.firstOrNull { p ->
                p.hosts.any { h -> host == h || host.endsWith(".$h") }
            } ?: OTHER
        }

        /** Las que se muestran como "chips" en la pantalla de inicio. */
        val featured = listOf(TIKTOK, INSTAGRAM, YOUTUBE, FACEBOOK, TWITTER, PINTEREST, REDDIT, VIMEO)

        private val urlRegex = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)

        /** Extrae el primer enlace de un texto compartido ("Mira este video https://vm.tiktok.com/..."). */
        fun extractUrl(text: String?): String? =
            text?.let { urlRegex.find(it)?.value?.trimEnd('.', ',', ')', ']') }
    }
}
