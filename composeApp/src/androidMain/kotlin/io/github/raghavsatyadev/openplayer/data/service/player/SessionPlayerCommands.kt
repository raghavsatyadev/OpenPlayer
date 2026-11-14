package io.github.raghavsatyadev.openplayer.data.service.player

import androidx.media3.common.Player
import androidx.media3.session.MediaSession

/**
 * Player commands to grant a connecting controller.
 *
 * Media3 1.12.0-beta01's default `MediaSession.Callback.onConnect` result carries an empty
 * player-command set (1.10 granted [MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS]), so
 * every controller, the app's own included, is refused play/pause, seek and skip. Fall back to
 * the 1.10 default whenever the default result grants nothing.
 */
internal fun grantedPlayerCommands(defaultResult: MediaSession.ConnectionResult): Player.Commands =
    defaultResult.availablePlayerCommands.takeIf { it.size() > 0 }
        ?: MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
