package io.github.raghavsatyadev.openplayer.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "OpenPlayer") {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            BasicText("OpenPlayer for desktop is on its way.")
        }
    }
}
