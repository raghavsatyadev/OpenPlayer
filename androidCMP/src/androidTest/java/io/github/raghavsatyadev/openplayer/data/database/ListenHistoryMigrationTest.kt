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
class ListenHistoryMigrationTest {

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath(DATABASE_NAME),
        driver = BundledSQLiteDriver(),
        databaseClass = OpenPlayerDatabase::class,
    )

    @Test
    fun migrationFromSixToSevenKeepsEngagementsAndAddsListenHistory() = runTest {
        val initialDb = migrationHelper.createDatabase(6)
        try {
            initialDb.execSQL(
                "INSERT INTO song_engagements (song_id, play_count, total_play_duration_ms, last_played_timestamp) " +
                    "VALUES ('42', 3, 600000, 1700000000000)"
            )
        } finally {
            initialDb.close()
        }

        val database = migrationHelper.runMigrationsAndValidate(
            version = 7,
            migrations = listOf(MIGRATION_6_7),
        )
        try {
            database.execSQL(
                "INSERT INTO listen_history (song_id, started_at, ended_at, listened_ms, track_duration_ms, source) " +
                    "VALUES ('42', 1, 2, 1, 180000, 0)"
            )
            database.prepare("SELECT COUNT(*) FROM listen_history").use { statement ->
                assertThat(statement.step()).isTrue()
                assertThat(statement.getLong(0)).isEqualTo(1L)
            }
            database.prepare("SELECT play_count FROM song_engagements WHERE song_id = '42'").use { statement ->
                assertThat(statement.step()).isTrue()
                assertThat(statement.getLong(0)).isEqualTo(3L)
            }
        } finally {
            database.close()
        }
    }

    private companion object {
        const val DATABASE_NAME = "listen-history-migration-test"
    }
}
