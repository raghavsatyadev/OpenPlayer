package io.github.raghavsatyadev.openplayer.di

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.work.WorkerParameters
import org.junit.jupiter.api.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

/**
 * Static check that every constructor dependency of every Koin definition has a binding.
 * Hilt failed the build on a missing binding; Koin would only crash at runtime.
 */
class CheckModulesTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun verifyKoinModules() {
        module { includes(allModules) }.verify(
            extraTypes = listOf(
                Context::class,
                ComponentName::class,
                Application::class,
                SavedStateHandle::class,
                WorkerParameters::class,
                Function0::class,
                Lazy::class,
            )
        )
    }
}
