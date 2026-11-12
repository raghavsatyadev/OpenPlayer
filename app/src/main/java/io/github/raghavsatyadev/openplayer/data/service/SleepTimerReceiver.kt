package io.github.raghavsatyadev.openplayer.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import co.touchlab.kermit.Logger

class SleepTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Logger.withTag("SleepTimerReceiver").d { "Sleep timer expired. Sending intent to MusicService" }
        val serviceIntent = Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_SLEEP_TIMER_EXPIRED
        }
        try {
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Logger.withTag("SleepTimerReceiver").e(e) { "Failed to start service for sleep timer" }
        }
    }
}
