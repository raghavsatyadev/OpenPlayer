package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import android.os.Environment

actual fun externalStorageRootPath(): String = Environment.getExternalStorageDirectory().path
