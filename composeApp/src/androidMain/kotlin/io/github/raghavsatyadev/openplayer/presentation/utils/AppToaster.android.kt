package io.github.raghavsatyadev.openplayer.presentation.utils

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberAppToaster(): AppToaster {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        object : AppToaster {
            override fun show(message: String, long: Boolean) {
                Toast.makeText(
                    context,
                    message,
                    if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }
}
