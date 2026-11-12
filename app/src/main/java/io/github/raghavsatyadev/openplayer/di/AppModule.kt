package io.github.raghavsatyadev.openplayer.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.work.WorkManager
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import io.github.raghavsatyadev.openplayer.BuildConfig
import io.github.raghavsatyadev.openplayer.OpenPlayerApplication
import io.github.raghavsatyadev.openplayer.data.database.AlbumArtThemeDao
import io.github.raghavsatyadev.openplayer.data.database.EngagementDao
import io.github.raghavsatyadev.openplayer.data.database.FavoritesDao
import io.github.raghavsatyadev.openplayer.data.database.LyricsDao
import io.github.raghavsatyadev.openplayer.data.database.LocalPlaylistDao
import io.github.raghavsatyadev.openplayer.data.database.ListenBrainzDao
import io.github.raghavsatyadev.openplayer.data.database.AudioBookmarkDao
import io.github.raghavsatyadev.openplayer.data.database.GenreFillDao
import io.github.raghavsatyadev.openplayer.data.database.SavedQueueDao
import io.github.raghavsatyadev.openplayer.data.database.SongUserMetaDao
import io.github.raghavsatyadev.openplayer.data.database.ListenHistoryDao
import io.github.raghavsatyadev.openplayer.data.database.MusicDao
import io.github.raghavsatyadev.openplayer.data.database.OfflineTrackDao
import io.github.raghavsatyadev.openplayer.data.database.OpenPlayerDatabase
import io.github.raghavsatyadev.openplayer.data.database.SearchHistoryDao
import io.github.raghavsatyadev.openplayer.data.database.TransitionDao
import io.github.raghavsatyadev.openplayer.data.preferences.UserPreferencesRepository
import io.github.raghavsatyadev.openplayer.data.preferences.PlaylistPreferencesRepository
import io.github.raghavsatyadev.openplayer.data.preferences.dataStore
import io.github.raghavsatyadev.openplayer.data.media.SongMetadataEditor
import io.github.raghavsatyadev.openplayer.data.network.deezer.DeezerApiService
import io.github.raghavsatyadev.openplayer.data.network.lyrics.LrcLibApiService
import io.github.raghavsatyadev.openplayer.data.repository.ArtistImageRepository
import io.github.raghavsatyadev.openplayer.data.repository.AudioBookmarkRepository
import io.github.raghavsatyadev.openplayer.data.repository.AudioBookmarkRepositoryImpl
import io.github.raghavsatyadev.openplayer.data.repository.LyricsRepository
import io.github.raghavsatyadev.openplayer.data.repository.LyricsRepositoryImpl
import io.github.raghavsatyadev.openplayer.data.repository.MediaStoreSongRepository
import io.github.raghavsatyadev.openplayer.data.repository.MusicRepository
import io.github.raghavsatyadev.openplayer.data.repository.MusicRepositoryImpl
import io.github.raghavsatyadev.openplayer.data.repository.SongRepository
import io.github.raghavsatyadev.openplayer.data.repository.TransitionRepository
import io.github.raghavsatyadev.openplayer.data.repository.TransitionRepositoryImpl
import io.github.raghavsatyadev.openplayer.data.repository.FolderTreeBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideApplication(@ApplicationContext app: Context): OpenPlayerApplication {
        return app as OpenPlayerApplication
    }

    @Singleton
    @Provides
    fun provideGson(): com.google.gson.Gson {
        return com.google.gson.Gson()
    }

    @OptIn(UnstableApi::class)
    @Singleton
    @Provides
    fun provideSessionToken(@ApplicationContext context: Context): androidx.media3.session.SessionToken {
        return androidx.media3.session.SessionToken(
            context,
            android.content.ComponentName(context, io.github.raghavsatyadev.openplayer.data.service.MusicService::class.java)
        )
    }

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.dataStore

    @Singleton
    @Provides
    fun provideJson(): Json {
        return Json {
            isLenient = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    @Singleton
    @Provides
    @AppScope
    fun provideAppCoroutineScope(dispatchers: DispatcherProvider): CoroutineScope {
        return CoroutineScope(SupervisorJob() + dispatchers.io)
    }

    @Singleton
    @Provides
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Singleton
    @Provides
    fun provideOpenPlayerDatabase(@ApplicationContext context: Context): OpenPlayerDatabase {
        return OpenPlayerDatabase.build(context)
    }

    @Singleton
    @Provides
    fun provideAlbumArtThemeDao(database: OpenPlayerDatabase): AlbumArtThemeDao {
        return database.albumArtThemeDao()
    }

    @Singleton
    @Provides
    fun provideSearchHistoryDao(database: OpenPlayerDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }

    @Singleton
    @Provides
    fun provideMusicDao(database: OpenPlayerDatabase): MusicDao {
        return database.musicDao()
    }

    @Singleton
    @Provides
    fun provideTransitionDao(database: OpenPlayerDatabase): TransitionDao {
        return database.transitionDao()
    }

    @Singleton
    @Provides
    fun provideEngagementDao(database: OpenPlayerDatabase): EngagementDao {
        return database.engagementDao()
    }

    @Singleton
    @Provides
    fun provideListenHistoryDao(database: OpenPlayerDatabase): ListenHistoryDao {
        return database.listenHistoryDao()
    }

    @Singleton
    @Provides
    fun provideFavoritesDao(database: OpenPlayerDatabase): FavoritesDao {
        return database.favoritesDao()
    }

    @Singleton
    @Provides
    fun provideLyricsDao(database: OpenPlayerDatabase): LyricsDao {
        return database.lyricsDao()
    }

    @Singleton
    @Provides
    fun provideLocalPlaylistDao(database: OpenPlayerDatabase): LocalPlaylistDao {
        return database.localPlaylistDao()
    }

    @Singleton
    @Provides
    fun provideNavidromeDao(database: OpenPlayerDatabase): io.github.raghavsatyadev.openplayer.data.database.NavidromeDao {
        return database.navidromeDao()
    }
    
    @Singleton
    @Provides
    fun provideJellyfinDao(database: OpenPlayerDatabase): io.github.raghavsatyadev.openplayer.data.database.JellyfinDao {
        return database.jellyfinDao()
    }

    @Singleton
    @Provides
    fun provideListenBrainzDao(database: OpenPlayerDatabase): ListenBrainzDao {
        return database.listenBrainzDao()
    }

    @Singleton
    @Provides
    fun provideSongUserMetaDao(database: OpenPlayerDatabase): SongUserMetaDao {
        return database.songUserMetaDao()
    }

    @Singleton
    @Provides
    fun provideGenreFillDao(database: OpenPlayerDatabase): GenreFillDao {
        return database.genreFillDao()
    }

    @Singleton
    @Provides
    fun provideSavedQueueDao(database: OpenPlayerDatabase): SavedQueueDao {
        return database.savedQueueDao()
    }

    @Singleton
    @Provides
    fun provideAudioBookmarkDao(database: OpenPlayerDatabase): AudioBookmarkDao {
        return database.audioBookmarkDao()
    }

    @Singleton
    @Provides
    fun provideOfflineTrackDao(database: OpenPlayerDatabase): OfflineTrackDao {
        return database.offlineTrackDao()
    }

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .dispatcher(Dispatchers.Default)
            .allowHardware(true)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizeBytes(40 * 1024 * 1024)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }

    @Provides
    @Singleton
    fun provideLyricsRepository(
        @ApplicationContext context: Context,
        lrcLibApiService: LrcLibApiService,
        lyricsDao: LyricsDao,
        okHttpClient: OkHttpClient,
        userPreferencesRepository: UserPreferencesRepository
    ): LyricsRepository {
        return LyricsRepositoryImpl(
            context = context,
            lrcLibApiService = lrcLibApiService,
            lyricsDao = lyricsDao,
            okHttpClient = okHttpClient,
            userPreferencesRepository = userPreferencesRepository
        )
    }

    @Provides
    @Singleton
    fun provideSongRepository(
        @ApplicationContext context: Context,
        mediaStoreObserver: io.github.raghavsatyadev.openplayer.data.observer.MediaStoreObserver,
        favoritesDao: FavoritesDao,
        userPreferencesRepository: UserPreferencesRepository,
        musicDao: MusicDao
    ): SongRepository {
        return MediaStoreSongRepository(
            context = context,
            mediaStoreObserver = mediaStoreObserver,
            favoritesDao = favoritesDao,
            userPreferencesRepository = userPreferencesRepository,
            musicDao = musicDao
        )
    }

    @Provides
    @Singleton
    fun provideFolderTreeBuilder(): FolderTreeBuilder {
        return FolderTreeBuilder()
    }

    @Provides
    @Singleton
    fun provideMusicRepository(
        @ApplicationContext context: Context,
        userPreferencesRepository: UserPreferencesRepository,
        playlistPreferencesRepository: PlaylistPreferencesRepository,
        searchHistoryDao: SearchHistoryDao,
        musicDao: MusicDao,
        lyricsRepository: LyricsRepository,
        songRepository: SongRepository,
        favoritesDao: FavoritesDao,
        artistImageRepository: ArtistImageRepository,
        folderTreeBuilder: FolderTreeBuilder
    ): MusicRepository {
        return MusicRepositoryImpl(
            context = context,
            userPreferencesRepository = userPreferencesRepository,
            playlistPreferencesRepository = playlistPreferencesRepository,
            searchHistoryDao = searchHistoryDao,
            musicDao = musicDao,
            lyricsRepository = lyricsRepository,
            songRepository = songRepository,
            favoritesDao = favoritesDao,
            artistImageRepository = artistImageRepository,
            folderTreeBuilder = folderTreeBuilder
        )

    }

    @Provides
    @Singleton
    fun provideTransitionRepository(
        transitionRepositoryImpl: TransitionRepositoryImpl
    ): TransitionRepository {
        return transitionRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideAudioBookmarkRepository(
        audioBookmarkRepositoryImpl: AudioBookmarkRepositoryImpl
    ): AudioBookmarkRepository {
        return audioBookmarkRepositoryImpl
    }

    @Singleton
    @Provides
    fun provideSongMetadataEditor(
        @ApplicationContext context: Context,
        musicDao: MusicDao,
        userPreferencesRepository: UserPreferencesRepository
    ): SongMetadataEditor {
        return SongMetadataEditor(context, musicDao, userPreferencesRepository)
    }

    /**
     * Provides a singleton OkHttpClient instance with logging and a User-Agent interceptor.
     * Retry logic with backoff is handled in coroutine-based callers.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
            redactHeader("Proxy-Authorization")
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
            redactHeader("X-Emby-Token")
            redactHeader("X-Emby-Authorization")
            redactHeader("X-MediaBrowser-Token")
        }
        
        val connectionPool = okhttp3.ConnectionPool(
            maxIdleConnections = 5,
            keepAliveDuration = 30,
            timeUnit = java.util.concurrent.TimeUnit.SECONDS
        )
        
        return OkHttpClient.Builder()
            .connectionPool(connectionPool)
            .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestWithUserAgent = originalRequest.newBuilder()
                    .header("User-Agent", "OpenPlayer/1.0 (Android; Music Player)")
                    .build()
                chain.proceed(requestWithUserAgent)
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * Provides an OkHttpClient instance with timeouts for lyrics searches.
     * Includes DNS resolver, modern TLS, connection pool, and connection retry.
     */
    @Provides
    @Singleton
    @FastOkHttpClient
    fun provideFastOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
            redactHeader("Proxy-Authorization")
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
            redactHeader("X-Emby-Token")
            redactHeader("X-Emby-Authorization")
            redactHeader("X-MediaBrowser-Token")
        }
        
        val connectionPool = okhttp3.ConnectionPool(
            maxIdleConnections = 5,
            keepAliveDuration = 30,
            timeUnit = java.util.concurrent.TimeUnit.SECONDS
        )
        
        val dns = okhttp3.Dns { hostname ->
            try {
                okhttp3.Dns.SYSTEM.lookup(hostname)
            } catch (e: Exception) {
                java.net.InetAddress.getAllByName(hostname).toList()
            }
        }

        return OkHttpClient.Builder()
            .dns(dns)
            .connectionPool(connectionPool)
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .connectionSpecs(listOf(
                okhttp3.ConnectionSpec.MODERN_TLS,
                okhttp3.ConnectionSpec.COMPATIBLE_TLS
            ))
            .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestWithHeaders = originalRequest.newBuilder()
                    .header("User-Agent", "OpenPlayer/1.0 (Android; Music Player)")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(requestWithHeaders)
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * Provides a singleton Retrofit instance for the LRCLIB API.
     */
    @Provides
    @Singleton
    fun provideRetrofit(@FastOkHttpClient okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://lrclib.net/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides a singleton instance of the LRCLIB API service.
     */
    @Provides
    @Singleton
    fun provideLrcLibApiService(retrofit: Retrofit): LrcLibApiService {
        return retrofit.create(LrcLibApiService::class.java)
    }

    /**
     * Provides a Retrofit instance for the Deezer API.
     */
    @Provides
    @Singleton
    @DeezerRetrofit
    fun provideDeezerRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.deezer.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides the Deezer API service.
     */
    @Provides
    @Singleton
    fun provideDeezerApiService(@DeezerRetrofit retrofit: Retrofit): DeezerApiService {
        return retrofit.create(DeezerApiService::class.java)
    }

    /**
     * Provides the artist image repository.
     */
    @Provides
    @Singleton
    fun provideArtistImageRepository(
        deezerApiService: DeezerApiService,
        musicDao: MusicDao,
        userPreferencesRepository: UserPreferencesRepository,
        @ApplicationContext context: Context
    ): ArtistImageRepository {
        return ArtistImageRepository(deezerApiService, musicDao, userPreferencesRepository, context)
    }
}
