package io.github.raghavsatyadev.openplayer.di

import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.raghavsatyadev.openplayer.BuildConfig
import io.github.raghavsatyadev.openplayer.data.database.MIGRATION_1_2
import io.github.raghavsatyadev.openplayer.data.database.MIGRATION_2_3
import io.github.raghavsatyadev.openplayer.data.database.MIGRATION_3_4
import io.github.raghavsatyadev.openplayer.data.database.MIGRATION_4_5
import io.github.raghavsatyadev.openplayer.data.database.MIGRATION_5_6
import io.github.raghavsatyadev.openplayer.data.database.MIGRATION_6_7
import io.github.raghavsatyadev.openplayer.data.database.MIGRATION_7_8
import io.github.raghavsatyadev.openplayer.data.database.MIGRATION_8_9
import io.github.raghavsatyadev.openplayer.data.database.OpenPlayerDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        val builder = Room.databaseBuilder(
            androidContext(),
            OpenPlayerDatabase::class.java,
            "openplayer_database"
        )
            .addCallback(OpenPlayerDatabase.createRuntimeArtifactsCallback())
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
            )
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)

        if (BuildConfig.DEBUG) {
            builder.fallbackToDestructiveMigration(dropAllTables = true)
        }

        builder.build()
    }

    single { get<OpenPlayerDatabase>().albumArtThemeDao() }
    single { get<OpenPlayerDatabase>().searchHistoryDao() }
    single { get<OpenPlayerDatabase>().musicDao() }
    single { get<OpenPlayerDatabase>().transitionDao() }
    single { get<OpenPlayerDatabase>().engagementDao() }
    single { get<OpenPlayerDatabase>().listenHistoryDao() }
    single { get<OpenPlayerDatabase>().favoritesDao() }
    single { get<OpenPlayerDatabase>().lyricsDao() }
    single { get<OpenPlayerDatabase>().localPlaylistDao() }
    single { get<OpenPlayerDatabase>().navidromeDao() }
    single { get<OpenPlayerDatabase>().jellyfinDao() }
    single { get<OpenPlayerDatabase>().listenBrainzDao() }
    single { get<OpenPlayerDatabase>().songUserMetaDao() }
    single { get<OpenPlayerDatabase>().genreFillDao() }
    single { get<OpenPlayerDatabase>().savedQueueDao() }
    single { get<OpenPlayerDatabase>().audioBookmarkDao() }
    single { get<OpenPlayerDatabase>().offlineTrackDao() }
}
