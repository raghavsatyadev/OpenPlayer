package io.github.raghavsatyadev.openplayer.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListenHistoryMigrationTest {

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = OpenPlayerDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrationFromSixToSevenKeepsEngagementsAndAddsListenHistory() {
        migrationHelper.createDatabase(DATABASE_NAME, 6).apply {
            execSQL(
                "INSERT INTO song_engagements (song_id, play_count, total_play_duration_ms, last_played_timestamp) " +
                    "VALUES ('42', 3, 600000, 1700000000000)"
            )
            close()
        }

        migrationHelper.runMigrationsAndValidate(
            name = DATABASE_NAME,
            version = 7,
            validateDroppedTables = true,
            MIGRATION_6_7,
        ).use { database ->
            database.execSQL(
                "INSERT INTO listen_history (song_id, started_at, ended_at, listened_ms, track_duration_ms, source) " +
                    "VALUES ('42', 1, 2, 1, 180000, 0)"
            )
            database.query("SELECT COUNT(*) FROM listen_history").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(1)
            }
            database.query("SELECT play_count FROM song_engagements WHERE song_id = '42'").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(3)
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "listen-history-migration-test"
    }
}
