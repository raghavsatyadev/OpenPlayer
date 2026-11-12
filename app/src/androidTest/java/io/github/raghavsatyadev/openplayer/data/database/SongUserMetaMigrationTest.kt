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
class SongUserMetaMigrationTest {

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = OpenPlayerDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrationFromSevenToEightKeepsListensAndAddsMetaAndQueues() {
        migrationHelper.createDatabase(DATABASE_NAME, 7).apply {
            execSQL(
                "INSERT INTO listen_history (song_id, started_at, ended_at, listened_ms, track_duration_ms, source) " +
                    "VALUES ('42', 1, 2, 1, 180000, 0)"
            )
            close()
        }

        migrationHelper.runMigrationsAndValidate(
            name = DATABASE_NAME,
            version = 8,
            validateDroppedTables = true,
            MIGRATION_7_8,
        ).use { database ->
            database.execSQL(
                "INSERT INTO song_user_meta (song_id, updated_at) VALUES ('42', 5)"
            )
            database.query("SELECT rating, moods, tags FROM song_user_meta WHERE song_id = '42'").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(0)
                assertThat(cursor.getString(1)).isEqualTo("")
                assertThat(cursor.getString(2)).isEqualTo("")
            }
            database.execSQL(
                "INSERT INTO saved_queues (name, created_at, song_ids, current_index, position_ms) " +
                    "VALUES ('q', 1, '[\"1\",\"2\"]', 1, 1500)"
            )
            database.query("SELECT id, current_index FROM saved_queues").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getLong(0)).isEqualTo(1L)
                assertThat(cursor.getInt(1)).isEqualTo(1)
            }
            database.query("SELECT COUNT(*) FROM listen_history").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(1)
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "song-user-meta-migration-test"
    }
}
