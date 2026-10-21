package com.downly.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.downly.app.MainActivity
import com.downly.app.ads.AdaptiveBanner
import com.downly.app.ui.screens.DownloadsScreen
import com.downly.app.ui.screens.HomeScreen
import com.downly.app.ui.screens.SettingsScreen
import com.downly.app.ui.theme.DColors

@Composable
fun DownlyRoot(vm: MainViewModel, activity: MainActivity, onNeedPermissions: () -> Unit) {
    val tab by vm.tab.collectAsState()
    val toast by vm.toast.collectAsState()
    val downloads by vm.downloads.collectAsState()
    val active = downloads.count { it.isActive }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(toast) {
        toast?.let { snackbar.showSnackbar(it); vm.consumeToast() }
    }

    Scaffold(
        containerColor = DColors.Bg,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column {
                AdaptiveBanner()
                NavigationBar(containerColor = DColors.Surface) {
                    val colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DColors.Coral,
                        selectedTextColor = DColors.Text,
                        indicatorColor = DColors.SurfaceHigh,
                        unselectedIconColor = DColors.TextDim,
                        unselectedTextColor = DColors.TextDim,
                    )
                    NavigationBarItem(
                        selected = tab == 0, onClick = { vm.selectTab(0) }, colors = colors,
                        icon = { Icon(Icons.Rounded.Home, null) }, label = { Text("Inicio") },
                    )
                    NavigationBarItem(
                        selected = tab == 1, onClick = { vm.selectTab(1) }, colors = colors,
                        icon = {
                            BadgedBox(badge = { if (active > 0) Badge(containerColor = DColors.Coral) { Text("$active") } }) {
                                Icon(Icons.Rounded.Download, null)
                            }
                        },
                        label = { Text("Descargas") },
                    )
                    NavigationBarItem(
                        selected = tab == 2, onClick = { vm.selectTab(2) }, colors = colors,
                        icon = { Icon(Icons.Rounded.Settings, null) }, label = { Text("Ajustes") },
                    )
                }
            }
        },
    ) { pad ->
        val m = Modifier.fillMaxSize().padding(pad)
        when (tab) {
            0 -> HomeScreen(vm, activity, onNeedPermissions, m)
            1 -> DownloadsScreen(vm, m)
            else -> SettingsScreen(vm, m)
        }
    }
}
