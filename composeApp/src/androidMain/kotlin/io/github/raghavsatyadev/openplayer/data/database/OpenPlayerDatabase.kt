package io.github.raghavsatyadev.openplayer.data.database

import android.content.Context
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import io.github.raghavsatyadev.openplayer.AppBuildInfo
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        AlbumArtThemeEntity::class,
        SearchHistoryEntity::class,
        SongEntity::class,
        SongSearchFtsEntity::class,
        AlbumEntity::class,
        ArtistEntity::class,
        TransitionRuleEntity::class,
        SongArtistCrossRef::class,
        SongEngagementEntity::class,
        FavoritesEntity::class,
        LyricsEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        NavidromeSongEntity::class,
        NavidromePlaylistEntity::class,
        JellyfinSongEntity::class,
        JellyfinPlaylistEntity::class,
        ListenBrainzPendingListenEntity::class,
        AudioBookmarkEntity::class,
        OfflineTrackEntity::class,
        ListenHistoryEntity::class,
        SongUserMetaEntity::class,
        SavedQueueEntity::class,
        GenreLookupCacheEntity::class
    ],
    version = 9,
    exportSchema = true
)
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
abstract class OpenPlayerDatabase : RoomDatabase() {
    abstract fun albumArtThemeDao(): AlbumArtThemeDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun musicDao(): MusicDao
    abstract fun transitionDao(): TransitionDao
    abstract fun engagementDao(): EngagementDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun localPlaylistDao(): LocalPlaylistDao
    abstract fun navidromeDao(): NavidromeDao
    abstract fun jellyfinDao(): JellyfinDao
    abstract fun listenBrainzDao(): ListenBrainzDao
    abstract fun audioBookmarkDao(): AudioBookmarkDao
    abstract fun offlineTrackDao(): OfflineTrackDao
    abstract fun listenHistoryDao(): ListenHistoryDao
    abstract fun songUserMetaDao(): SongUserMetaDao
    abstract fun savedQueueDao(): SavedQueueDao
    abstract fun genreFillDao(): GenreFillDao

    companion object {
        const val DATABASE_NAME = "openplayer_database"

        fun build(context: Context): OpenPlayerDatabase {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            return Room.databaseBuilder<OpenPlayerDatabase>(
                context = context.applicationContext,
                name = dbFile.absolutePath,
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .addCallback(createRuntimeArtifactsCallback())
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
                .apply {
                    if (AppBuildInfo.debug) {
                        fallbackToDestructiveMigration(dropAllTables = true)
                    }
                }
                .build()
        }

        fun installFavoriteSyncTriggers(connection: SQLiteConnection) {
            connection.execSQL("DROP TRIGGER IF EXISTS trg_favorites_insert_sync_song")
            connection.execSQL("DROP TRIGGER IF EXISTS trg_favorites_update_sync_song")
            connection.execSQL("DROP TRIGGER IF EXISTS trg_favorites_delete_sync_song")

            connection.execSQL(
                """
                    CREATE TRIGGER IF NOT EXISTS trg_favorites_insert_sync_song
                    AFTER INSERT ON favorites
                    BEGIN
                        UPDATE songs SET is_favorite = NEW.isFavorite WHERE id = NEW.songId;
                    END
                """.trimIndent()
            )

            connection.execSQL(
                """
                    CREATE TRIGGER IF NOT EXISTS trg_favorites_update_sync_song
                    AFTER UPDATE ON favorites
                    BEGIN
                        UPDATE songs SET is_favorite = NEW.isFavorite WHERE id = NEW.songId;
                    END
                """.trimIndent()
            )

            connection.execSQL(
                """
                    CREATE TRIGGER IF NOT EXISTS trg_favorites_delete_sync_song
                    AFTER DELETE ON favorites
                    BEGIN
                        UPDATE songs SET is_favorite = 0 WHERE id = OLD.songId;
                    END
                """.trimIndent()
            )
        }

        private fun createSongsSearchVirtualTable(connection: SQLiteConnection) {
            connection.execSQL(
                """
                    CREATE VIRTUAL TABLE IF NOT EXISTS songs_fts
                    USING fts4(
                        title,
                        artist_name,
                        tokenize=unicode61
                    )
                """.trimIndent()
            )
        }

        fun installSongsSearchSyncTriggers(connection: SQLiteConnection) {
            createSongsSearchVirtualTable(connection)

            connection.execSQL("DROP TRIGGER IF EXISTS trg_songs_fts_insert")
            connection.execSQL("DROP TRIGGER IF EXISTS trg_songs_fts_update")
            connection.execSQL("DROP TRIGGER IF EXISTS trg_songs_fts_delete")

            connection.execSQL(
                """
                    CREATE TRIGGER IF NOT EXISTS trg_songs_fts_insert
                    AFTER INSERT ON songs
                    BEGIN
                        INSERT INTO songs_fts(rowid, title, artist_name)
                        VALUES (NEW.id, NEW.title, NEW.artist_name);
                    END
                """.trimIndent()
            )

            connection.execSQL(
                """
                    CREATE TRIGGER IF NOT EXISTS trg_songs_fts_update
                    AFTER UPDATE ON songs
                    BEGIN
                        DELETE FROM songs_fts WHERE rowid = OLD.id;
                        INSERT INTO songs_fts(rowid, title, artist_name)
                        VALUES (NEW.id, NEW.title, NEW.artist_name);
                    END
                """.trimIndent()
            )

            connection.execSQL(
                """
                    CREATE TRIGGER IF NOT EXISTS trg_songs_fts_delete
                    AFTER DELETE ON songs
                    BEGIN
                        DELETE FROM songs_fts WHERE rowid = OLD.id;
                    END
                """.trimIndent()
            )
        }

        fun createRuntimeArtifactsCallback(): RoomDatabase.Callback {
            return object : RoomDatabase.Callback() {
                override suspend fun onCreate(connection: SQLiteConnection) {
                    super.onCreate(connection)
                    installFavoriteSyncTriggers(connection)
                    installSongsSearchSyncTriggers(connection)
                }
            }
        }
    }
}
