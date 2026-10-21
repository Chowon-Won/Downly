package com.downly.app.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/** Banner adaptable (parte inferior de la app). */
@Composable
fun AdaptiveBanner(modifier: Modifier = Modifier) {
    if (!AdConfig.ADS_ENABLED) return
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        val context = LocalContext.current
        val adView = remember(widthDp) {
            AdView(context).apply {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
                adUnitId = AdConfig.BANNER
                runCatching { loadAd(AdRequest.Builder().build()) }
            }
        }
        DisposableEffect(adView) { onDispose { adView.destroy() } }
        AndroidView(factory = { adView })
    }
}

/** Rectángulo mediano 300x250 para colocar dentro del contenido. */
@Composable
fun MediumRectangleAd(modifier: Modifier = Modifier) {
    if (!AdConfig.ADS_ENABLED) return
    val context = LocalContext.current
    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.MEDIUM_RECTANGLE)
            adUnitId = AdConfig.MREC
            runCatching { loadAd(AdRequest.Builder().build()) }
        }
    }
    DisposableEffect(adView) { onDispose { adView.destroy() } }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AndroidView(factory = { adView })
    }
}
