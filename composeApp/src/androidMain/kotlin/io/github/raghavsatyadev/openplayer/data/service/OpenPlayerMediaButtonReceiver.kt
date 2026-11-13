@file:Suppress("DEPRECATION")
package io.github.raghavsatyadev.openplayer.data.service

import android.content.Context
import android.content.Intent
import androidx.media.session.MediaButtonReceiver
import co.touchlab.kermit.Logger

@Suppress("DEPRECATION")
class OpenPlayerMediaButtonReceiver : MediaButtonReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MEDIA_BUTTON) {
            super.onReceive(context, intent)
            return
        }

        MusicService.markPendingTemporaryForegroundStart()
        try {
            super.onReceive(context, intent)
        } catch (e: IllegalStateException) {
            MusicService.unmarkPendingTemporaryForegroundStart()
            Logger.withTag(TAG).w(e) { "Media button start denied while app cached" }
        } catch (throwable: Throwable) {
            MusicService.unmarkPendingTemporaryForegroundStart()
            Logger.withTag(TAG).w(throwable) { "Media button dispatch failed before MusicService start" }
            throw throwable
        }
    }

    companion object {
        private const val TAG = "MediaButtonReceiver"
    }
}
