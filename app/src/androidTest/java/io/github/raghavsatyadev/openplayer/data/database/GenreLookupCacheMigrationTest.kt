package io.github.raghavsatyadev.openplayer.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GenreLookupCacheMigrationTest {

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = OpenPlayerDatabase::class.java,
    )

    @Test
    fun migrationFromEightToNineAddsGenreLookupCache() {
        migrationHelper.createDatabase(DATABASE_NAME, 8).close()

        migrationHelper.runMigrationsAndValidate(
            name = DATABASE_NAME,
            version = 9,
            validateDroppedTables = true,
            MIGRATION_8_9,
        ).use { database ->
            database.execSQL(
                "INSERT INTO genre_lookup_cache (lookup_key, genre, checked_at) VALUES ('a|b', NULL, 1)"
            )
            database.query("SELECT COUNT(*) FROM genre_lookup_cache WHERE genre IS NULL").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(1)
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "genre-lookup-cache-migration-test"
    }
}
