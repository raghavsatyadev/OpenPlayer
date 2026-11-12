package io.github.raghavsatyadev.openplayer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerViewModel
import io.github.raghavsatyadev.openplayer.presentation.components.external.ExternalPlayerOverlay
import io.github.raghavsatyadev.openplayer.ui.theme.OpenPlayerTheme
import android.content.Intent.EXTRA_STREAM
import androidx.media3.common.util.UnstableApi
import io.github.raghavsatyadev.openplayer.data.preferences.AppThemeMode
import io.github.raghavsatyadev.openplayer.data.preferences.ThemePreferencesRepository
import io.github.raghavsatyadev.openplayer.data.preferences.ThemePreset
@androidx.annotation.OptIn(UnstableApi::class)
class ExternalPlayerActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(io.github.raghavsatyadev.openplayer.utils.AppLocaleManager.wrapContext(newBase))
    }

    private val playerViewModel: PlayerViewModel by viewModel()
    private val themePreferencesRepository: ThemePreferencesRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)

        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            val appThemeMode by themePreferencesRepository.appThemeModeFlow.collectAsStateWithLifecycle(initialValue = AppThemeMode.FOLLOW_SYSTEM)
            val themePreset by themePreferencesRepository.themePresetFlow.collectAsStateWithLifecycle(initialValue = ThemePreset.DEFAULT)
            val useDarkTheme = when (appThemeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                else -> systemDarkTheme
            }
            OpenPlayerTheme(darkTheme = useDarkTheme, themePreset = themePreset) {
                ExternalPlayerOverlay(
                    playerViewModel = playerViewModel,
                    onDismiss = { finish() },
                    onOpenFullPlayer = { openFullPlayer() }
                )
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        when {
            intent.action == Intent.ACTION_VIEW && intent.data != null -> {
                intent.data?.let { uri ->
                    persistUriPermissionIfNeeded(intent, uri)
                    playerViewModel.playExternalUri(uri)
                }
                clearExternalIntentPayload(intent)
            }

            intent.action == Intent.ACTION_SEND && intent.type?.startsWith("audio/") == true -> {
                resolveStreamUri(intent)?.let { uri ->
                    persistUriPermissionIfNeeded(intent, uri)
                    playerViewModel.playExternalUri(uri)
                }
                clearExternalIntentPayload(intent)
            }
        }
    }

    private fun openFullPlayer() {
        val fullPlayerIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("ACTION_SHOW_PLAYER", true)
        }
        startActivity(fullPlayerIntent)
        finish()
    }

    private fun resolveStreamUri(intent: Intent): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_STREAM, Uri::class.java)?.let { return it }
        } else {
            @Suppress("DEPRECATION")
            val legacyUri = intent.getParcelableExtra<Uri>(EXTRA_STREAM)
            if (legacyUri != null) return legacyUri
        }

        intent.clipData?.let { clipData ->
            if (clipData.itemCount > 0) {
                return clipData.getItemAt(0).uri
            }
        }

        return intent.data
    }

    private fun persistUriPermissionIfNeeded(intent: Intent, uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val hasPersistablePermission = intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0
            if (hasPersistablePermission) {
                val takeFlags =
                    (if (intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0) {
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    } else {
                        0
                    }) or
                        (if (intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0) {
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        } else {
                            0
                        })
                if (takeFlags != 0) {
                    runCatching { contentResolver.takePersistableUriPermission(uri, takeFlags) }
                }
            }
        }
    }

    private fun clearExternalIntentPayload(intent: Intent) {
        intent.data = null
        intent.clipData = null
        intent.removeExtra(EXTRA_STREAM)
    }
}
