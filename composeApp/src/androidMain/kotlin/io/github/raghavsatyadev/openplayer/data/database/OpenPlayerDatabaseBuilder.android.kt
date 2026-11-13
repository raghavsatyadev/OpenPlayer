package io.github.raghavsatyadev.openplayer.data.database

import android.content.Context
import androidx.room3.Room
import io.github.raghavsatyadev.openplayer.data.database.OpenPlayerDatabase.Companion.DATABASE_NAME

fun createOpenPlayerDatabase(context: Context): OpenPlayerDatabase {
    val dbFile = context.getDatabasePath(DATABASE_NAME)
    val builder = Room.databaseBuilder<OpenPlayerDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath,
    )
    return OpenPlayerDatabase.build(builder)
}
