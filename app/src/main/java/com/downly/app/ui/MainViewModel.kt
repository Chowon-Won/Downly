package com.downly.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.downly.app.engine.DownloadOption
import com.downly.app.engine.DownloadRepository
import com.downly.app.engine.Engine
import com.downly.app.engine.MediaInfo
import com.downly.app.engine.Platform
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AnalyzeState {
    data object Idle : AnalyzeState
    data object Loading : AnalyzeState
    data class Result(val info: MediaInfo) : AnalyzeState
    data class Error(val message: String) : AnalyzeState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val _tab = MutableStateFlow(0)
    val tab: StateFlow<Int> = _tab.asStateFlow()

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _analyze = MutableStateFlow<AnalyzeState>(AnalyzeState.Idle)
    val analyze: StateFlow<AnalyzeState> = _analyze.asStateFlow()

    private val _updating = MutableStateFlow(false)
    val updating: StateFlow<Boolean> = _updating.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    val downloads = DownloadRepository.items
    val engineState = Engine.state

    private var analyzeJob: Job? = null

    fun selectTab(i: Int) { _tab.value = i }
    fun setUrl(v: String) { _url.value = v }
    fun consumeToast() { _toast.value = null }
    fun toast(msg: String) { _toast.value = msg }

    fun onSharedUrl(url: String) {
        _tab.value = 0
        _url.value = url
        analyzeNow()
    }

    fun reset() {
        analyzeJob?.cancel()
        _url.value = ""
        _analyze.value = AnalyzeState.Idle
    }

    fun analyzeNow() {
        val link = Platform.extractUrl(_url.value) ?: run {
            _analyze.value = AnalyzeState.Error("Pega un enlace válido (https://…)")
            return
        }
        _url.value = link
        analyzeJob?.cancel()
        analyzeJob = viewModelScope.launch {
            _analyze.value = AnalyzeState.Loading
            _analyze.value = runCatching { Engine.analyze(link) }.fold(
                onSuccess = { AnalyzeState.Result(it) },
                onFailure = { AnalyzeState.Error(humanize(it)) },
            )
        }
    }

    fun download(info: MediaInfo, option: DownloadOption) {
        DownloadRepository.enqueue(info, option)
        _toast.value = "Descarga iniciada · ${option.title}"
    }

    fun updateEngine() {
        if (_updating.value) return
        viewModelScope.launch {
            _updating.value = true
            val msg = runCatching { Engine.update(getApplication()) }.fold(
                onSuccess = { s ->
                    if (s.contains("ALREADY", true)) "El motor ya está actualizado"
                    else "Motor actualizado correctamente"
                },
                onFailure = { "No se pudo actualizar: ${it.message?.take(80)}" },
            )
            _updating.value = false
            _toast.update { msg }
        }
    }

    private fun humanize(t: Throwable): String {
        val m = t.message.orEmpty()
        return when {
            m.contains("Unsupported URL", true) -> "Este enlace no es compatible"
            m.contains("login", true) || m.contains("private", true) || m.contains("cookies", true) ->
                "El contenido es privado o requiere iniciar sesión"
            m.contains("Unable to resolve", true) || m.contains("timed out", true) -> "Revisa tu conexión a internet"
            m.contains("404") -> "No se encontró el video (¿fue eliminado?)"
            else -> m.lines().lastOrNull { it.contains("ERROR") }?.substringAfter("ERROR:")?.trim()?.take(160)
                ?: "No se pudo analizar el enlace. Prueba a actualizar el motor en Ajustes."
        }
    }
}
