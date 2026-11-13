package io.github.raghavsatyadev.openplayer.di

import io.github.raghavsatyadev.openplayer.data.database.OpenPlayerDatabase
import io.github.raghavsatyadev.openplayer.data.database.createOpenPlayerDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single { createOpenPlayerDatabase(androidContext()) }

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
