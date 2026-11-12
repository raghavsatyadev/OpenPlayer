package io.github.raghavsatyadev.openplayer.di

import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import io.github.raghavsatyadev.openplayer.BuildConfig
import io.github.raghavsatyadev.openplayer.data.image.JellyfinCoilFetcher
import io.github.raghavsatyadev.openplayer.data.image.LocalArtworkCoilFetcher
import io.github.raghavsatyadev.openplayer.data.image.NavidromeCoilFetcher
import io.github.raghavsatyadev.openplayer.data.listenbrainz.ListenBrainzApiService
import io.github.raghavsatyadev.openplayer.data.listenbrainz.ListenBrainzEndpoint
import io.github.raghavsatyadev.openplayer.data.musicbrainz.MusicBrainzApiService
import io.github.raghavsatyadev.openplayer.data.network.deezer.DeezerApiService
import io.github.raghavsatyadev.openplayer.data.network.itunes.ITunesSearchClient
import io.github.raghavsatyadev.openplayer.data.network.jellyfin.JellyfinApiService
import io.github.raghavsatyadev.openplayer.data.network.lyrics.LrcLibApiService
import io.github.raghavsatyadev.openplayer.data.network.navidrome.NavidromeApiService
import kotlinx.coroutines.Dispatchers
import okhttp3.ConnectionPool
import okhttp3.ConnectionSpec
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetAddress
import java.util.concurrent.TimeUnit

val networkModule = module {
    single {
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

        val connectionPool = ConnectionPool(
            maxIdleConnections = 5,
            keepAliveDuration = 30,
            timeUnit = TimeUnit.SECONDS
        )

        OkHttpClient.Builder()
            .connectionPool(connectionPool)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
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

    single(QUALIFIER_FAST_OKHTTP_CLIENT) {
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

        val connectionPool = ConnectionPool(
            maxIdleConnections = 5,
            keepAliveDuration = 30,
            timeUnit = TimeUnit.SECONDS
        )

        val dns = Dns { hostname ->
            try {
                Dns.SYSTEM.lookup(hostname)
            } catch (e: Exception) {
                InetAddress.getAllByName(hostname).toList()
            }
        }

        OkHttpClient.Builder()
            .dns(dns)
            .connectionPool(connectionPool)
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectionSpecs(listOf(
                ConnectionSpec.MODERN_TLS,
                ConnectionSpec.COMPATIBLE_TLS
            ))
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
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

    single {
        Retrofit.Builder()
            .baseUrl("https://lrclib.net/")
            .client(get(QUALIFIER_FAST_OKHTTP_CLIENT))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single { get<Retrofit>().create(LrcLibApiService::class.java) }

    single(QUALIFIER_DEEZER_RETROFIT) {
        Retrofit.Builder()
            .baseUrl("https://api.deezer.com/")
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single { get<Retrofit>(QUALIFIER_DEEZER_RETROFIT).create(DeezerApiService::class.java) }

    single(QUALIFIER_LISTEN_BRAINZ_RETROFIT) {
        val endpoint: ListenBrainzEndpoint = get()
        val client = get<OkHttpClient>().newBuilder()
            .addInterceptor { chain ->
                val request = chain.request()
                val rewritten = endpoint.rewrite(request.url)
                if (rewritten == request.url) {
                    chain.proceed(request)
                } else {
                    chain.proceed(request.newBuilder().url(rewritten).build())
                }
            }
            .build()
        Retrofit.Builder()
            .baseUrl(ListenBrainzEndpoint.DEFAULT_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single { get<Retrofit>(QUALIFIER_LISTEN_BRAINZ_RETROFIT).create(ListenBrainzApiService::class.java) }

    single {
        val context = androidContext()
        val okHttpClient: OkHttpClient = get()
        ImageLoader.Builder(context)
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

    singleOf(::ListenBrainzEndpoint)
    singleOf(::MusicBrainzApiService)
    singleOf(::JellyfinApiService)
    singleOf(::NavidromeApiService)
    singleOf(::ITunesSearchClient)
    factory { LocalArtworkCoilFetcher.Factory(lazy { get() }) }
    factory { NavidromeCoilFetcher.Factory(get(), get()) }
    factory { JellyfinCoilFetcher.Factory(get(), get()) }
}
