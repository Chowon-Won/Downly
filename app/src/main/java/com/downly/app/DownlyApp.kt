package com.downly.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.downly.app.ads.AdManager
import com.downly.app.engine.DownloadRepository
import com.downly.app.engine.Engine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DownlyApp : Application() {

    companion object {
        const val CHANNEL_DOWNLOADS = "downloads"
    }

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        createChannels()
        DownloadRepository.init(this)
        appScope.launch { Engine.init(this@DownlyApp) }
        AdManager.init(this)
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DOWNLOADS, getString(R.string.channel_downloads), NotificationManager.IMPORTANCE_LOW)
                .apply { description = getString(R.string.channel_downloads_desc) }
        )
    }
}
