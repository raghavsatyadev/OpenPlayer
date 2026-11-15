package io.github.raghavsatyadev.openplayer.data.worker

/** Starts the first library sync right after a cloud server login (Android: WorkManager). */
interface CloudInitialSync {
    fun startJellyfin()

    fun startNavidrome()
}
