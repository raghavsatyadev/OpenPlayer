package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

/** Backs the audio output picker; the routing choice itself lives in [AudioOutputStateHolder]. */
class AudioOutputViewModel(
    private val audioOutputStateHolder: AudioOutputStateHolder,
) : ViewModel() {

    val outputs: StateFlow<List<AudioOutputDevice>> = audioOutputStateHolder.outputs

    /** Call while the picker is visible, so the list follows devices connecting or leaving. */
    fun setPickerActive(active: Boolean) {
        if (active) audioOutputStateHolder.start() else audioOutputStateHolder.stop()
    }

    fun selectOutput(deviceId: Int) {
        audioOutputStateHolder.select(deviceId)
    }

    override fun onCleared() {
        audioOutputStateHolder.stop()
    }
}
