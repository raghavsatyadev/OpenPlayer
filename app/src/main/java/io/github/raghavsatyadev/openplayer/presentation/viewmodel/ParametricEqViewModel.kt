package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.raghavsatyadev.openplayer.data.equalizer.EqualizerManager
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.AutoEqParser
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.ParametricBand
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.ParametricEqPreferences
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.ParametricEqProfile
import io.github.raghavsatyadev.openplayer.data.service.player.DualPlayerEngine
import io.github.raghavsatyadev.openplayer.di.AppScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import co.touchlab.kermit.Logger
import javax.inject.Inject

enum class ParametricMessage { IMPORT_NO_FILTERS, IMPORT_FAILED, EXPORT_FAILED, EXPORTED }

data class ParametricEqUiState(
    val enabled: Boolean = false,
    val available: Boolean = true,
    val profile: ParametricEqProfile = ParametricEqProfile.EMPTY,
    val message: ParametricMessage? = null,
)

/** State holder for the parametric EQ editor; the effect itself lives in [EqualizerManager]. */
@HiltViewModel
class ParametricEqViewModel @Inject constructor(
    private val equalizerManager: EqualizerManager,
    private val preferences: ParametricEqPreferences,
    private val dualPlayerEngine: DualPlayerEngine,
    @param:AppScope private val appScope: CoroutineScope,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val engine = equalizerManager.parametric
    private val message = MutableStateFlow<ParametricMessage?>(null)
    private var persistJob: Job? = null

    val uiState: StateFlow<ParametricEqUiState> = combine(
        engine.enabled, engine.available, engine.profile, message,
    ) { enabled, available, profile, msg ->
        ParametricEqUiState(enabled, available, profile, msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),
        ParametricEqUiState(engine.enabled.value, engine.available.value, engine.profile.value))

    init {
        viewModelScope.launch {
            if (!equalizerManager.isAttached && !engine.enabled.value && !engine.profile.value.hasEnabledBands) {
                equalizerManager.restoreParametric(preferences.enabledFlow.first(), preferences.profileFlow.first())
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        equalizerManager.setParametricEnabled(enabled)
        viewModelScope.launch {
            if (enabled) equalizerManager.attachToAudioSessionIfNeeded(dualPlayerEngine.getAudioSessionId())
            preferences.setEnabled(engine.enabled.value)
        }
    }

    fun setPreamp(db: Float) = edit { it.copy(preampDb = db.coerceIn(-ParametricEqProfile.MAX_PREAMP_DB, ParametricEqProfile.MAX_PREAMP_DB)) }

    fun addBand() = edit { it.copy(bands = it.bands + ParametricBand()) }

    fun updateBand(index: Int, band: ParametricBand) = edit { p ->
        if (index !in p.bands.indices) p else p.copy(bands = p.bands.toMutableList().also { it[index] = band.clamped() })
    }

    fun removeBand(index: Int) = edit { p ->
        if (index !in p.bands.indices) p else p.copy(bands = p.bands.filterIndexed { i, _ -> i != index })
    }

    fun clearBands() = edit { it.copy(preampDb = 0f, bands = emptyList()) }

    fun dismissMessage() {
        message.value = null
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } }
                    .onFailure { Logger.w(it) { "Parametric EQ import read failed" } }
                    .getOrNull()
            }
            if (text == null) {
                message.value = ParametricMessage.IMPORT_FAILED
                return@launch
            }
            val parsed = AutoEqParser.parse(text)
            if (parsed.bands.isEmpty()) {
                message.value = ParametricMessage.IMPORT_NO_FILTERS
            } else {
                message.value = null
                edit { parsed }
            }
        }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val text = AutoEqParser.export(engine.profile.value)
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
                }.onFailure { Logger.w(it) { "Parametric EQ export failed" } }.getOrDefault(false)
            }
            message.value = if (ok) ParametricMessage.EXPORTED else ParametricMessage.EXPORT_FAILED
        }
    }

    private fun edit(transform: (ParametricEqProfile) -> ParametricEqProfile) {
        val updated = transform(engine.profile.value)
        message.update { null }
        equalizerManager.parametric.setProfile(updated)
        persistJob?.cancel()
        persistJob = appScope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            preferences.setProfile(updated)
        }
    }

    private companion object {
        const val PERSIST_DEBOUNCE_MS = 300L
    }
}
