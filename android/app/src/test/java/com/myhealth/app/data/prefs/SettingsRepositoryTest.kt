package com.myhealth.app.data.prefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    private lateinit var repo: SettingsRepository

    @Before
    fun setUp() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        repo = SettingsRepository(ctx)
        repo.clearAll()
    }

    // ── Default values ───────────────────────────────────────────────────

    @Test
    fun `default themeMode is system`() = runTest {
        assertEquals("system", repo.themeMode.first())
    }

    @Test
    fun `default language is en`() = runTest {
        assertEquals("en", repo.language.first())
    }

    @Test
    fun `default goal is empty string`() = runTest {
        assertEquals("", repo.goal.first())
    }

    @Test
    fun `default didOnboard is false`() = runTest {
        assertFalse(repo.didOnboard.first())
    }

    @Test
    fun `default isGuest is true`() = runTest {
        assertTrue(repo.isGuest.first())
    }

    @Test
    fun `default unitsImperial is false`() = runTest {
        assertFalse(repo.unitsImperial.first())
    }

    // ── Set and read back ────────────────────────────────────────────────

    @Test
    fun `set and read themeMode`() = runTest {
        repo.setThemeMode("dark")
        assertEquals("dark", repo.themeMode.first())
    }

    @Test
    fun `set and read language`() = runTest {
        repo.setLanguage("es")
        assertEquals("es", repo.language.first())
    }

    @Test
    fun `set and read goal`() = runTest {
        repo.setGoal("lose")
        assertEquals("lose", repo.goal.first())
    }

    @Test
    fun `set and read didOnboard`() = runTest {
        repo.setDidOnboard(true)
        assertTrue(repo.didOnboard.first())
    }

    @Test
    fun `set and read unitsImperial`() = runTest {
        repo.setUnitsImperial(true)
        assertTrue(repo.unitsImperial.first())
    }

    @Test
    fun `set and read healthConditions`() = runTest {
        repo.setHealthConditions(setOf("diabetes", "asthma"))
        assertEquals(setOf("diabetes", "asthma"), repo.healthConditions.first())
    }

    // ── clearAll ─────────────────────────────────────────────────────────

    @Test
    fun `clearAll resets everything to defaults`() = runTest {
        repo.setThemeMode("dark")
        repo.setLanguage("fr")
        repo.setGoal("gain")
        repo.setDidOnboard(true)
        repo.setGuest(false)

        repo.clearAll()

        assertEquals("system", repo.themeMode.first())
        assertEquals("en", repo.language.first())
        assertEquals("", repo.goal.first())
        assertFalse(repo.didOnboard.first())
        assertTrue(repo.isGuest.first())
    }
}
