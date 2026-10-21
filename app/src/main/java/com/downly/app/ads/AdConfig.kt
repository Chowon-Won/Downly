package com.downly.app.ads

/**
 * Configuración de anuncios.
 *
 * Los IDs actuales son los de PRUEBA oficiales de Google AdMob (muestran "Test Ad").
 * Para ganar dinero, crea tu app en https://admob.google.com y reemplaza:
 *  - el App ID en res/values/strings.xml (admob_app_id)
 *  - los IDs de bloque de anuncio de abajo
 */
object AdConfig {
    /** Interruptor global: false = la app funciona sin ningún anuncio. */
    const val ADS_ENABLED = true

    const val BANNER = "ca-app-pub-3940256099942544/9214589741"        // banner adaptable
    const val MREC = "ca-app-pub-3940256099942544/6300978111"          // rectángulo mediano (300x250)
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"  // anuncio repentino (pantalla completa)
    const val REWARDED = "ca-app-pub-3940256099942544/5224354917"      // anuncio de recompensa
    const val APP_OPEN = "ca-app-pub-3940256099942544/9257395921"      // al abrir / volver a la app

    /** Mostrar intersticial como máximo 1 de cada N descargas… */
    const val INTERSTITIAL_EVERY_N_DOWNLOADS = 2
    /** …y nunca más seguido que esto. */
    const val INTERSTITIAL_MIN_INTERVAL_MS = 60_000L
    /** App Open: como mucho una vez cada X tiempo. */
    const val APP_OPEN_MIN_INTERVAL_MS = 4 * 60 * 60 * 1000L
    /** Lo que dura el desbloqueo de calidad HD/4K/MP3 320 tras ver un anuncio de recompensa. */
    const val REWARD_UNLOCK_MS = 30 * 60 * 1000L
}
