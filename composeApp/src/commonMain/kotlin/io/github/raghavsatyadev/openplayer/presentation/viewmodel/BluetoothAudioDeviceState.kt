package io.github.raghavsatyadev.openplayer.presentation.viewmodel

data class BluetoothAudioDeviceState(
    val name: String,
    val address: String? = null,
    val isConnected: Boolean = false,
    val batteryPercent: Int? = null
)
