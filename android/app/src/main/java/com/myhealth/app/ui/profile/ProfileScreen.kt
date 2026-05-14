package com.myhealth.app.ui.profile

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.myhealth.app.data.secure.InsuranceCardDao
import com.myhealth.app.data.secure.MyChartIssuerDao
import com.myhealth.app.data.secure.SecureTokenStore
import com.myhealth.app.fhir.EpicSandboxConfig
import com.myhealth.app.data.prefs.SettingsRepository
import com.myhealth.app.ui.theme.CarePlusColor
import com.myhealth.core.intelligence.BiologicalAgeEngine
import com.myhealth.core.intelligence.ZodiacHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    nav: NavController,
    vm: ProfileViewModel = hiltViewModel(),
    onNavigateToBioAge: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf("") }
    var heightCm by remember { mutableFloatStateOf(170f) }
    var weightKg by remember { mutableFloatStateOf(65f) }
    var birthday by remember { mutableStateOf<LocalDate?>(null) }
    var birthLocation by remember { mutableStateOf("") }
    var selectedSex by remember { mutableStateOf("Male") }
    var selectedGoal by remember { mutableStateOf("General wellness") }
    var ageError by remember { mutableStateOf<String?>(null) }
    var heightError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }
    val tint = CarePlusColor.CareBlue
    val sources by vm.sources.collectAsState(initial = emptyList())
    val context = LocalContext.current

    val bmi = if (heightCm > 0) weightKg / ((heightCm / 100f) * (heightCm / 100f)) else 0f
    val age = birthday?.let { ZodiacHelper.ageFromBirthday(it) }
    val zodiac = birthday?.let { ZodiacHelper.fromDate(it) }

    val completion = listOf(
        name.isNotEmpty(),
        heightCm > 0,
        weightKg > 0,
        birthday != null,
        birthLocation.isNotEmpty()
    ).count { it } * 100 / 5

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold)

        // ── Completion card ─────────────────────────────────────
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Profile completion", modifier = Modifier.weight(1f))
                    Text("$completion%", color = tint, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { completion / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = tint,
                )
                Text(
                    "Complete your profile so MyChart imports merge cleanly.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Name") }, modifier = Modifier.fillMaxWidth()
        )

        // ── Birthday DatePicker ─────────────────────────────────
        Card(
            Modifier
                .fillMaxWidth()
                .clickable {
                    val cal = Calendar.getInstance()
                    birthday?.let {
                        cal.set(it.year, it.monthValue - 1, it.dayOfMonth)
                    }
                    DatePickerDialog(
                        context,
                        { _, y, m, d -> birthday = LocalDate.of(y, m + 1, d) },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Birthday", modifier = Modifier.weight(1f))
                Text(
                    birthday?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: "Tap to set",
                    color = if (birthday != null) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ── Zodiac + Age display ────────────────────────────────
        if (zodiac != null && age != null) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Age", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$age", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Zodiac", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(zodiac.symbol, fontSize = 28.sp)
                        Text(zodiac.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // ── Birthplace ──────────────────────────────────────────
        OutlinedTextField(
            value = birthLocation, onValueChange = { birthLocation = it },
            label = { Text("Birthplace") }, modifier = Modifier.fillMaxWidth()
        )

        // ── Sex picker ──────────────────────────────────────────
        var sexExpanded by remember { mutableStateOf(false) }
        Card(
            Modifier
                .fillMaxWidth()
                .clickable { sexExpanded = true },
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Sex", modifier = Modifier.weight(1f))
                Text(selectedSex, fontWeight = FontWeight.SemiBold)
            }
            DropdownMenu(expanded = sexExpanded, onDismissRequest = { sexExpanded = false }) {
                listOf("Male", "Female", "Other").forEach { s ->
                    DropdownMenuItem(text = { Text(s) }, onClick = {
                        selectedSex = s
                        sexExpanded = false
                    })
                }
            }
        }

        // ── Goal picker ─────────────────────────────────────────
        var goalExpanded by remember { mutableStateOf(false) }
        Card(
            Modifier
                .fillMaxWidth()
                .clickable { goalExpanded = true },
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Goal", modifier = Modifier.weight(1f))
                Text(selectedGoal, fontWeight = FontWeight.SemiBold)
            }
            DropdownMenu(expanded = goalExpanded, onDismissRequest = { goalExpanded = false }) {
                listOf("Lose weight", "Maintain", "Build muscle", "Improve endurance", "General wellness")
                    .forEach { g ->
                        DropdownMenuItem(text = { Text(g) }, onClick = {
                            selectedGoal = g
                            goalExpanded = false
                        })
                    }
            }
        }

        Text("Height: ${heightCm.toInt()} cm", fontWeight = FontWeight.SemiBold)
        Slider(value = heightCm, onValueChange = { heightCm = it; heightError = null }, valueRange = 50f..300f)
        heightError?.let { Text(it, color = Color.Red, fontSize = 12.sp) }

        Text("Weight: ${weightKg.toInt()} kg", fontWeight = FontWeight.SemiBold)
        Slider(value = weightKg, onValueChange = { weightKg = it; weightError = null }, valueRange = 10f..500f)
        weightError?.let { Text(it, color = Color.Red, fontSize = 12.sp) }

        // ── Save profile ─────────────────────────────────────
        ageError?.let { Text(it, color = Color.Red, fontSize = 12.sp) }
        Button(
            onClick = {
                var valid = true
                val currentAge = age
                if (currentAge == null || currentAge < 1 || currentAge > 120) {
                    ageError = "Age must be between 1 and 120"
                    valid = false
                } else ageError = null
                if (heightCm < 50 || heightCm > 300) {
                    heightError = "Height must be between 50 and 300 cm"
                    valid = false
                } else heightError = null
                if (weightKg < 10 || weightKg > 500) {
                    weightError = "Weight must be between 10 and 500 kg"
                    valid = false
                } else weightError = null
                if (valid) {
                    vm.saveProfile(name, heightCm, weightKg, birthday, selectedSex, selectedGoal)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = tint),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save profile", color = Color.White)
        }

        HorizontalDivider()

        // ── BMI card

        // ── BMI card ────────────────────────────────────────────
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Row(
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("BMI", modifier = Modifier.weight(1f))
                Text("%.1f".format(bmi), fontWeight = FontWeight.Bold)
                Text(
                    bmiLabel(bmi), color = bmiColor(bmi), fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(bmiColor(bmi).copy(alpha = 0.18f), RoundedCornerShape(99.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        // ── Biological age summary card ─────────────────────────
        if (age != null) {
            val bioResult = remember(age, bmi) {
                BiologicalAgeEngine.estimate(
                    BiologicalAgeEngine.Inputs(
                        chronologicalYears = age.toDouble(),
                        sex = when (selectedSex) {
                            "Female" -> BiologicalAgeEngine.Sex.female
                            "Other" -> BiologicalAgeEngine.Sex.other
                            else -> BiologicalAgeEngine.Sex.male
                        },
                        bmi = bmi.toDouble(),
                    )
                )
            }
            Card(
                Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToBioAge?.invoke() },
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Biological Age", fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Chronological", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "$age", fontSize = 36.sp, fontWeight = FontWeight.Black,
                                color = Color.Gray
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Biological", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${bioResult.biologicalYears.toInt()}", fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = if (bioResult.deltaYears < 0) Color(0xFF34C759)
                                else Color(0xFFFF9500)
                            )
                        }
                    }
                    Text(
                        bioResult.verdict,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        "Confidence: %.0f%%".format(bioResult.confidence * 100),
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "This data stays on your device",
                        fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // ── Connected sources ───────────────────────────────────
        Text(
            "Connected sources", fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        sources.forEach { src ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(src.label, modifier = Modifier.weight(1f))
                    when (src.status) {
                        SourceStatus.CONNECTED -> Text(
                            "✓",
                            color = CarePlusColor.Success, fontWeight = FontWeight.Bold
                        )
                        SourceStatus.NOT_CONNECTED -> Text(
                            "Connect",
                            color = tint, fontWeight = FontWeight.SemiBold, fontSize = 12.sp
                        )
                        SourceStatus.ADD -> Text(
                            "Add",
                            color = tint, fontWeight = FontWeight.SemiBold, fontSize = 12.sp
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        // ── Sign out ────────────────────────────────────────────
        Button(
            onClick = {
                vm.signOut()
                nav.navigate("onboarding") { popUpTo(0) { inclusive = true } }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign out", color = Color.White)
        }

        Spacer(Modifier.height(16.dp))
    }
}

data class ConnectedSource(val label: String, val status: SourceStatus)

enum class SourceStatus { CONNECTED, NOT_CONNECTED, ADD }

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val secureTokens: SecureTokenStore,
    private val settingsRepo: SettingsRepository,
    private val insuranceDao: InsuranceCardDao,
    private val mychartDao: MyChartIssuerDao,
) : ViewModel() {
    fun signOut() {
        viewModelScope.launch {
            secureTokens.clearJwt()
            settingsRepo.setDidOnboard(false)
        }
    }
    fun saveProfile(name: String, heightCm: Float, weightKg: Float,
                    birthday: LocalDate?, sex: String, goal: String) {
        viewModelScope.launch {
            settingsRepo.setGoal(goal)
        }
    }
    val sources = mychartDao.observeAll().map { issuers ->
        listOf(
            ConnectedSource("Health Connect", SourceStatus.CONNECTED),
            ConnectedSource(
                "Epic MyChart",
                if (issuers.isNotEmpty()
                    || secureTokens.fhirAccessToken(EpicSandboxConfig.ISSUER) != null)
                    SourceStatus.CONNECTED else SourceStatus.NOT_CONNECTED
            ),
            ConnectedSource(
                "Insurance card",
                if (kotlinx.coroutines.runBlocking { insuranceDao.latest() } != null)
                    SourceStatus.CONNECTED else SourceStatus.NOT_CONNECTED
            ),
            ConnectedSource("Pharmacy", SourceStatus.ADD),
        )
    }
}

private fun bmiLabel(b: Float) = when {
    b < 18.5f -> "Under"
    b < 25f -> "Normal"
    b < 30f -> "Over"
    else -> "Obese"
}

private fun bmiColor(b: Float): Color = when {
    b < 18.5f -> CarePlusColor.Info
    b < 25f -> CarePlusColor.Success
    b < 30f -> CarePlusColor.Warning
    else -> CarePlusColor.Danger
}
