package com.downly.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.downly.app.ads.AdManager
import com.downly.app.engine.Platform
import com.downly.app.ui.DownlyRoot
import com.downly.app.ui.MainViewModel
import com.downly.app.ui.theme.DownlyTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_DOWNLOADS = "open_downloads"
    }

    private val vm: MainViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    private val appOpenObserver = object : DefaultLifecycleObserver {
        private var first = true
        override fun onStart(owner: LifecycleOwner) {
            // No interrumpimos la primera apertura; sí cuando el usuario vuelve a la app.
            if (first) { first = false; return }
            AdManager.maybeShowAppOpen(this@MainActivity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        ProcessLifecycleOwner.get().lifecycle.addObserver(appOpenObserver)

        setContent {
            DownlyTheme {
                DownlyRoot(vm = vm, activity = this, onNeedPermissions = ::requestPermissionsIfNeeded)
            }
        }
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(appOpenObserver)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        if (intent.getBooleanExtra(EXTRA_OPEN_DOWNLOADS, false)) vm.selectTab(1)
        if (intent.action == Intent.ACTION_SEND) {
            Platform.extractUrl(intent.getStringExtra(Intent.EXTRA_TEXT))?.let { vm.onSharedUrl(it) }
        }
    }

    fun requestPermissionsIfNeeded() {
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT <= 28) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }
}
