package io.github.raghavsatyadev.openplayer.presentation.viewmodel

actual fun externalStorageRootPath(): String = System.getProperty("user.home").orEmpty()
