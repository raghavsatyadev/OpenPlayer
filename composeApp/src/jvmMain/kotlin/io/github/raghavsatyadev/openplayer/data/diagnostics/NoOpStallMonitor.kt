package io.github.raghavsatyadev.openplayer.data.diagnostics

class NoOpStallMonitor : StallMonitor {
    override fun start() = Unit
    override fun stop() = Unit
}
