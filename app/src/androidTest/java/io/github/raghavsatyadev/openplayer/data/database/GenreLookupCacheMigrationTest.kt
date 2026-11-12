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
class GenreLookupCacheMigrationTest {

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath(DATABASE_NAME),
        driver = BundledSQLiteDriver(),
        databaseClass = OpenPlayerDatabase::class,
    )

    @Test
    fun migrationFromEightToNineAddsGenreLookupCache() = runTest {
        migrationHelper.createDatabase(8).close()

        val connection = migrationHelper.runMigrationsAndValidate(
            version = 9,
            migrations = listOf(MIGRATION_8_9),
        )
        try {
            connection.execSQL(
                "INSERT INTO genre_lookup_cache (lookup_key, genre, checked_at) VALUES ('a|b', NULL, 1)"
            )
            connection.prepare("SELECT COUNT(*) FROM genre_lookup_cache WHERE genre IS NULL").use { statement ->
                assertThat(statement.step()).isTrue()
                assertThat(statement.getLong(0)).isEqualTo(1L)
            }
        } finally {
            connection.close()
        }
    }

    private companion object {
        const val DATABASE_NAME = "genre-lookup-cache-migration-test"
    }
}
