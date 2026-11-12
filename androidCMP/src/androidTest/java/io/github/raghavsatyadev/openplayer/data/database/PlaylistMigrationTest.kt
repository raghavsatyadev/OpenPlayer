package io.github.raghavsatyadev.openplayer.data.database

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistMigrationTest {

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath(DATABASE_NAME),
        driver = BundledSQLiteDriver(),
        databaseClass = OpenPlayerDatabase::class,
    )

    @Test
    fun migrationFromFiveToSixPreservesRowsAndAllowsRepeatedSongs() = runTest {
        val initialDb = migrationHelper.createDatabase(5)
        try {
            initialDb.execSQL(
                "INSERT INTO playlist_songs (playlist_id, song_id, sort_order) VALUES " +
                    "('playlist-1', 'intro', 0), " +
                    "('playlist-1', 'chorus', 1), " +
                    "('playlist-1', 'outro', 2)"
            )
        } finally {
            initialDb.close()
        }

        val database = migrationHelper.runMigrationsAndValidate(
            version = 6,
            migrations = listOf(MIGRATION_5_6),
        )
        try {
            database.execSQL(
                "INSERT INTO playlist_songs (playlist_id, song_id, sort_order) " +
                    "VALUES ('playlist-1', 'chorus', 3)"
            )

            assertThat(database.playlistSongIds("playlist-1"))
                .containsExactly("intro", "chorus", "outro", "chorus")
                .inOrder()
        } finally {
            database.close()
        }
    }

    private fun SQLiteConnection.playlistSongIds(playlistId: String): List<String> {
        return prepare("SELECT song_id FROM playlist_songs WHERE playlist_id = ? ORDER BY sort_order").use { statement ->
            statement.bindText(1, playlistId)
            buildList {
                while (statement.step()) {
                    add(statement.getText(0))
                }
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "playlist-migration-test"
    }
}
