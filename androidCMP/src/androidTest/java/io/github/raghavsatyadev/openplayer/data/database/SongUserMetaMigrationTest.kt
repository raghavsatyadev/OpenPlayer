package io.github.raghavsatyadev.openplayer.data.database

import androidx.room3.testing.MigrationTestHelper
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
class SongUserMetaMigrationTest {

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath(DATABASE_NAME),
        driver = BundledSQLiteDriver(),
        databaseClass = OpenPlayerDatabase::class,
    )

    @Test
    fun migrationFromSevenToEightKeepsListensAndAddsMetaAndQueues() = runTest {
        val initialDb = migrationHelper.createDatabase(7)
        try {
            initialDb.execSQL(
                "INSERT INTO listen_history (song_id, started_at, ended_at, listened_ms, track_duration_ms, source) " +
                    "VALUES ('42', 1, 2, 1, 180000, 0)"
            )
        } finally {
            initialDb.close()
        }

        val database = migrationHelper.runMigrationsAndValidate(
            version = 8,
            migrations = listOf(MIGRATION_7_8),
        )
        try {
            database.execSQL(
                "INSERT INTO song_user_meta (song_id, updated_at) VALUES ('42', 5)"
            )
            database.prepare("SELECT rating, moods, tags FROM song_user_meta WHERE song_id = '42'").use { statement ->
                assertThat(statement.step()).isTrue()
                assertThat(statement.getLong(0)).isEqualTo(0L)
                assertThat(statement.getText(1)).isEqualTo("")
                assertThat(statement.getText(2)).isEqualTo("")
            }
            database.execSQL(
                "INSERT INTO saved_queues (name, created_at, song_ids, current_index, position_ms) " +
                    "VALUES ('q', 1, '[\"1\",\"2\"]', 1, 1500)"
            )
            database.prepare("SELECT id, current_index FROM saved_queues").use { statement ->
                assertThat(statement.step()).isTrue()
                assertThat(statement.getLong(0)).isEqualTo(1L)
                assertThat(statement.getLong(1)).isEqualTo(1L)
            }
            database.prepare("SELECT COUNT(*) FROM listen_history").use { statement ->
                assertThat(statement.step()).isTrue()
                assertThat(statement.getLong(0)).isEqualTo(1L)
            }
        } finally {
            database.close()
        }
    }

    private companion object {
        const val DATABASE_NAME = "song-user-meta-migration-test"
    }
}
