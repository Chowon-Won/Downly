package com.downly.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Gestor central de anuncios. Todo está protegido: si un anuncio falla,
 * la app sigue funcionando con normalidad.
 */
object AdManager {
    private const val TAG = "DownlyAds"
    private const val PREFS = "downly_ads"

    private lateinit var app: Context
    private var initialized = false

    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var appOpen: AppOpenAd? = null
    private var showingFullScreen = false

    private var downloadsSinceInterstitial = 0
    private var lastInterstitialAt = 0L
    private var lastAppOpenAt = 0L

    private val _unlockedUntil = MutableStateFlow(0L)
    /** Momento (epoch ms) hasta el que están desbloqueadas las calidades premium. */
    val unlockedUntil: StateFlow<Long> = _unlockedUntil

    fun isPremiumUnlocked() = !AdConfig.ADS_ENABLED || System.currentTimeMillis() < _unlockedUntil.value

    fun init(context: Context) {
        app = context.applicationContext
        _unlockedUntil.value = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("unlocked_until", 0L)
        if (!AdConfig.ADS_ENABLED) return
        runCatching {
            MobileAds.initialize(app) {
                initialized = true
                loadInterstitial(); loadRewarded(); loadAppOpen()
            }
        }.onFailure { Log.w(TAG, "MobileAds init", it) }
    }

    private fun request() = AdRequest.Builder().build()

    // ------------------------------------------------------------ intersticial (repentino)

    private fun loadInterstitial() {
        if (!AdConfig.ADS_ENABLED || interstitial != null) return
        InterstitialAd.load(app, AdConfig.INTERSTITIAL, request(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { interstitial = null; Log.d(TAG, "interstitial: ${error.message}") }
        })
    }

    /** Se llama al iniciar una descarga; aplica el límite de frecuencia. */
    fun onDownloadStarted(activity: Activity) {
        if (!AdConfig.ADS_ENABLED) return
        downloadsSinceInterstitial++
        val now = System.currentTimeMillis()
        if (downloadsSinceInterstitial < AdConfig.INTERSTITIAL_EVERY_N_DOWNLOADS) return
        if (now - lastInterstitialAt < AdConfig.INTERSTITIAL_MIN_INTERVAL_MS) return
        val ad = interstitial ?: run { loadInterstitial(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { showingFullScreen = false; interstitial = null; loadInterstitial() }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { showingFullScreen = false; interstitial = null; loadInterstitial() }
            override fun onAdShowedFullScreenContent() { showingFullScreen = true }
        }
        downloadsSinceInterstitial = 0
        lastInterstitialAt = now
        runCatching { ad.show(activity) }
    }

    // ------------------------------------------------------------ recompensa

    private fun loadRewarded() {
        if (!AdConfig.ADS_ENABLED || rewarded != null) return
        RewardedAd.load(app, AdConfig.REWARDED, request(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewarded = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { rewarded = null; Log.d(TAG, "rewarded: ${error.message}") }
        })
    }

    /**
     * Muestra un anuncio de recompensa. Si no hay ninguno disponible no castigamos
     * al usuario: se le concede el desbloqueo igualmente.
     */
    fun showRewarded(activity: Activity, onResult: (earned: Boolean, adWasShown: Boolean) -> Unit) {
        val ad = rewarded
        if (!AdConfig.ADS_ENABLED || ad == null) {
            grantUnlock(); onResult(true, false); loadRewarded(); return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                showingFullScreen = false; rewarded = null; loadRewarded(); onResult(earned, true)
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                showingFullScreen = false; rewarded = null; loadRewarded(); grantUnlock(); onResult(true, false)
            }
            override fun onAdShowedFullScreenContent() { showingFullScreen = true }
        }
        runCatching {
            ad.show(activity) { earned = true; grantUnlock() }
        }.onFailure { grantUnlock(); onResult(true, false) }
    }

    private fun grantUnlock() {
        val until = System.currentTimeMillis() + AdConfig.REWARD_UNLOCK_MS
        _unlockedUntil.value = until
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong("unlocked_until", until).apply()
    }

    // ------------------------------------------------------------ app open

    private fun loadAppOpen() {
        if (!AdConfig.ADS_ENABLED || appOpen != null) return
        AppOpenAd.load(app, AdConfig.APP_OPEN, request(), object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) { appOpen = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { appOpen = null }
        })
    }

    /** Llamar cuando la app vuelve a primer plano. */
    fun maybeShowAppOpen(activity: Activity) {
        if (!AdConfig.ADS_ENABLED || showingFullScreen) return
        val now = System.currentTimeMillis()
        if (now - lastAppOpenAt < AdConfig.APP_OPEN_MIN_INTERVAL_MS) return
        val ad = appOpen ?: run { loadAppOpen(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { showingFullScreen = false; appOpen = null; loadAppOpen() }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { showingFullScreen = false; appOpen = null; loadAppOpen() }
            override fun onAdShowedFullScreenContent() { showingFullScreen = true }
        }
        lastAppOpenAt = now
        runCatching { ad.show(activity) }
    }
}
