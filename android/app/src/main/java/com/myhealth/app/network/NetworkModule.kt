package com.myhealth.app.network

import com.myhealth.app.data.prefs.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import com.myhealth.app.BuildConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Resolves the API base URL. Reads from [SettingsRepository] so the user
 * can override at runtime via Settings, falling back to the build config default.
 */
@Singleton
class ApiBaseUrl @Inject constructor(
    private val settings: SettingsRepository,
) {
    val value: String
        get() {
            val stored = runBlocking { settings.apiBaseURL.first() }
            return if (stored.isNotBlank()) stored else BuildConfig.API_BASE_URL
        }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // ApiBaseUrl is @Singleton-annotated and constructor-injectable;
    // exposed here for clarity/discoverability.
}
