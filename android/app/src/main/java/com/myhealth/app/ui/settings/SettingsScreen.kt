package com.myhealth.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myhealth.app.data.prefs.SettingsRepository
import com.myhealth.core.health.HealthCondition
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val ctx: android.content.Context,
    private val tokenStore: com.myhealth.app.data.secure.SecureTokenStore,
) : ViewModel() {
    val units: StateFlow<Boolean> = settings.unitsImperial
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val isGuest: StateFlow<Boolean> = settings.isGuest
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val themeMode: StateFlow<String> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")
    val language: StateFlow<String> = settings.language
        .stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    /** Names of HealthCondition enum values currently selected. */
    val healthConditions: StateFlow<Set<String>> = settings.healthConditions
        .stateIn(viewModelScope, SharingStarted.Eagerly, setOf(HealthCondition.none.name))
    val reminderHydration: StateFlow<Boolean> = settings.reminderHydration
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val reminderExercise: StateFlow<Boolean> = settings.reminderExercise
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val reminderSleep: StateFlow<Boolean> = settings.reminderSleep
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _crashReports = kotlinx.coroutines.flow.MutableStateFlow(
        com.myhealth.app.crash.CrashReportingService.isEnabled(ctx)
    )
    val crashReports: StateFlow<Boolean> = _crashReports

    private val _analytics = kotlinx.coroutines.flow.MutableStateFlow(
        com.myhealth.app.analytics.AnalyticsService.isEnabled(ctx)
    )
    val analytics: StateFlow<Boolean> = _analytics

    fun setUnits(v: Boolean) { viewModelScope.launch { settings.setUnitsImperial(v) } }
    fun setGuest(v: Boolean) { viewModelScope.launch { settings.setGuest(v) } }
    fun setCrashReports(v: Boolean) {
        com.myhealth.app.crash.CrashReportingService.setEnabled(ctx, v)
        _crashReports.value = v
    }
    fun setAnalytics(v: Boolean) {
        com.myhealth.app.analytics.AnalyticsService.setEnabled(ctx, v)
        _analytics.value = v
    }

    fun setReminderHydration(v: Boolean) {
        viewModelScope.launch { settings.setReminderHydration(v) }
        if (v) com.myhealth.app.notifications.HealthReminders.scheduleHydrationReminder(ctx)
        else com.myhealth.app.notifications.HealthReminders.cancelHydration(ctx)
    }

    fun setReminderExercise(v: Boolean) {
        viewModelScope.launch { settings.setReminderExercise(v) }
        if (v) com.myhealth.app.notifications.HealthReminders.scheduleExerciseReminder(ctx)
        else com.myhealth.app.notifications.HealthReminders.cancelExercise(ctx)
    }

    fun setReminderSleep(v: Boolean) {
        viewModelScope.launch { settings.setReminderSleep(v) }
        if (v) com.myhealth.app.notifications.HealthReminders.scheduleSleepReminder(ctx)
        else com.myhealth.app.notifications.HealthReminders.cancelSleep(ctx)
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settings.setThemeMode(mode) }
        val nightMode = when (mode) {
            "light" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { settings.setLanguage(lang) }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            tokenStore.clearJwt()
            settings.setDidOnboard(false)
            onDone()
        }
    }

    fun eraseAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            tokenStore.clearJwt()
            settings.clearAll()
            onDone()
        }
    }

    /**
     * Toggle a single condition. Mirrors iOS `HealthConditionsStore.toggle`:
     * selecting `.none` clears all others; selecting any real condition
     * removes `.none`; an empty set falls back to `[.none]`.
     */
    fun toggleCondition(c: HealthCondition) {
        viewModelScope.launch {
            val current = healthConditions.value.toMutableSet()
            if (c == HealthCondition.none) {
                settings.setHealthConditions(setOf(HealthCondition.none.name))
                return@launch
            }
            if (current.contains(c.name)) {
                current.remove(c.name)
            } else {
                current.add(c.name)
                current.remove(HealthCondition.none.name)
            }
            if (current.isEmpty()) current.add(HealthCondition.none.name)
            settings.setHealthConditions(current)
        }
    }
}

@Composable
fun SettingsScreen(nav: androidx.navigation.NavController, vm: SettingsViewModel = hiltViewModel()) {
    val units by vm.units.collectAsStateWithLifecycle(false)
    val guest by vm.isGuest.collectAsStateWithLifecycle(true)
    val crashReports by vm.crashReports.collectAsStateWithLifecycle(false)
    val analytics by vm.analytics.collectAsStateWithLifecycle(false)
    val theme by vm.themeMode.collectAsStateWithLifecycle("system")
    val language by vm.language.collectAsStateWithLifecycle("en")
    val conditions by vm.healthConditions.collectAsStateWithLifecycle(
        setOf(HealthCondition.none.name)
    )
    val remHydration by vm.reminderHydration.collectAsStateWithLifecycle(false)
    val remExercise by vm.reminderExercise.collectAsStateWithLifecycle(false)
    val remSleep by vm.reminderSleep.collectAsStateWithLifecycle(false)
    var showEraseDialog by androidx.compose.runtime.mutableStateOf(false)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Account", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp))
        Text(if (guest) "Guest mode (local only)" else "Cloud-synced",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Row("Imperial units", units) { vm.setUnits(it) }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("Privacy", color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        Row("Send crash reports (Sentry)", crashReports) { vm.setCrashReports(it) }
        Text(
            "Off by default. Anonymous crash stack traces only.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row("Share anonymous usage analytics (PostHog)", analytics) { vm.setAnalytics(it) }
        Text(
            "Off by default. Anonymous feature-use events only — no health data, meal contents, or medicine names ever leave the device.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text(
            "Health Conditions",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            "Stored only on this device. Used to filter unsafe exercises and tune recommendations. Doctor's advice always wins.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        HealthCondition.values().forEach { condition ->
            Row(
                "${condition.symbol}  ${condition.label}",
                conditions.contains(condition.name)
            ) { vm.toggleCondition(condition) }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // ── Reminders ───────────────────────────────────────────
        Text("Reminders", color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        Text(
            "Receive periodic nudges for hydration, exercise, and sleep.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row("Hydration reminders", remHydration) { vm.setReminderHydration(it) }
        Row("Exercise reminders", remExercise) { vm.setReminderExercise(it) }
        Row("Sleep reminders", remSleep) { vm.setReminderSleep(it) }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // ── Appearance ──────────────────────────────────────────
        Text("Appearance", color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        Text("Theme", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
                androidx.compose.material3.FilterChip(
                    selected = theme == key,
                    onClick = { vm.setThemeMode(key) },
                    label = { Text(label) }
                )
            }
        }
        Text("Language", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            listOf("en" to "English", "es" to "Spanish", "fr" to "French").forEach { (code, label) ->
                androidx.compose.material3.FilterChip(
                    selected = language == code,
                    onClick = { vm.setLanguage(code) },
                    label = { Text(label) }
                )
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        androidx.compose.material3.TextButton(
            onClick = { vm.signOut { nav.navigate("onboarding") { popUpTo(0) { inclusive = true } } } },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Sign out", color = MaterialTheme.colorScheme.primary) }

        // ── Functional export/erase ─────────────────────────────
        androidx.compose.material3.Button(
            onClick = { /* TODO: DataExportService — query all Room tables, build JSON, write to Downloads, open share sheet */ },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) { Text("Export my data (JSON)") }
        androidx.compose.material3.Button(
            onClick = { showEraseDialog = true },
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) { Text("Erase all on-device data", color = androidx.compose.ui.graphics.Color.White) }

        if (showEraseDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showEraseDialog = false },
                title = { Text("Erase all data?") },
                text = { Text("This will permanently delete all local data including profiles, meals, activities, and health records. This action cannot be undone.") },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showEraseDialog = false
                            vm.eraseAllData { nav.navigate("onboarding") { popUpTo(0) { inclusive = true } } }
                        }
                    ) { Text("Erase", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showEraseDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun Row(label: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.padding(end = 16.dp))
        Switch(checked = value, onCheckedChange = onToggle)
    }
}
