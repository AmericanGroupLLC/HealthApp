package com.myhealth.app.ui.vitals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myhealth.core.intelligence.BiologicalAgeEngine

@Composable
fun BiologicalAgeScreen() {
    var chrono by remember { mutableFloatStateOf(30f) }
    var sexIndex by remember { mutableIntStateOf(1) }
    var rhr by remember { mutableFloatStateOf(60f) }
    var hrv by remember { mutableFloatStateOf(45f) }
    var vo2 by remember { mutableFloatStateOf(38f) }
    var sleep by remember { mutableFloatStateOf(7.5f) }
    var bmi by remember { mutableFloatStateOf(23f) }
    var bodyFat by remember { mutableFloatStateOf(0.20f) }
    var systolicBP by remember { mutableFloatStateOf(120f) }
    var diastolicBP by remember { mutableFloatStateOf(80f) }
    var exerciseMin by remember { mutableFloatStateOf(150f) }
    var stepsPerDay by remember { mutableFloatStateOf(7500f) }
    var smoker by remember { mutableStateOf(false) }
    var heavyAlcohol by remember { mutableStateOf(false) }

    val sexValues = listOf(BiologicalAgeEngine.Sex.female, BiologicalAgeEngine.Sex.male, BiologicalAgeEngine.Sex.other)
    val sexLabels = listOf("Female", "Male", "Other")

    val result = BiologicalAgeEngine.estimate(
        BiologicalAgeEngine.Inputs(
            chronologicalYears = chrono.toDouble(),
            sex = sexValues[sexIndex],
            restingHR = rhr.toDouble(),
            hrv = hrv.toDouble(),
            vo2Max = vo2.toDouble(),
            avgSleepHours = sleep.toDouble(),
            bmi = bmi.toDouble(),
            bodyFatPct = bodyFat.toDouble(),
            systolicBP = systolicBP.toDouble(),
            diastolicBP = diastolicBP.toDouble(),
            weeklyExerciseMin = exerciseMin.toDouble(),
            stepsPerDay = stepsPerDay.toDouble(),
            smoker = smoker,
            heavyAlcohol = heavyAlcohol,
        )
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Biological Age", fontSize = 28.sp, fontWeight = FontWeight.Bold)

        // ── Comparison card ─────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Row(
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AgeColumn("Chronological", result.chronologicalYears.toInt(), Color.Gray)
                AgeColumn(
                    "Biological", result.biologicalYears.toInt(),
                    if (result.deltaYears < 0) Color(0xFF34C759) else Color(0xFFFF9500)
                )
            }
        }

        // ── Verdict + confidence ────────────────────────────────
        Text(result.verdict, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Text(
            "Confidence: %.0f%%".format(result.confidence * 100),
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()
        Text("Your profile", fontWeight = FontWeight.Bold)

        // ── Sex picker ──────────────────────────────────────────
        Text("Sex", fontWeight = FontWeight.SemiBold)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            sexLabels.forEachIndexed { i, label ->
                SegmentedButton(
                    selected = sexIndex == i,
                    onClick = { sexIndex = i },
                    shape = SegmentedButtonDefaults.itemShape(i, sexLabels.size)
                ) { Text(label) }
            }
        }

        // ── All 12 input sliders ────────────────────────────────
        SliderRow("Chronological age", chrono, 13f..100f, "${chrono.toInt()} yr") { chrono = it }
        SliderRow("Resting HR", rhr, 40f..120f, "${rhr.toInt()} bpm") { rhr = it }
        SliderRow("HRV (SDNN)", hrv, 10f..120f, "${hrv.toInt()} ms") { hrv = it }
        SliderRow("VO₂ Max", vo2, 20f..70f, "%.1f ml/kg/min".format(vo2)) { vo2 = it }
        SliderRow("Avg sleep", sleep, 4f..10f, "%.1f h".format(sleep)) { sleep = it }
        SliderRow("BMI", bmi, 16f..40f, "%.1f".format(bmi)) { bmi = it }
        SliderRow("Body fat", bodyFat, 0.05f..0.50f, "${(bodyFat * 100).toInt()}%") { bodyFat = it }
        SliderRow("Systolic BP", systolicBP, 80f..200f, "${systolicBP.toInt()} mmHg") { systolicBP = it }
        SliderRow("Diastolic BP", diastolicBP, 50f..130f, "${diastolicBP.toInt()} mmHg") { diastolicBP = it }
        SliderRow("Weekly exercise", exerciseMin, 0f..500f, "${exerciseMin.toInt()} min") { exerciseMin = it }
        SliderRow("Daily steps", stepsPerDay, 0f..20000f, "${stepsPerDay.toInt()}") { stepsPerDay = it }

        // ── Boolean toggles ─────────────────────────────────────
        ToggleRow("Smoker", smoker) { smoker = it }
        ToggleRow("Heavy alcohol use", heavyAlcohol) { heavyAlcohol = it }

        HorizontalDivider()

        // ── Factor breakdown ────────────────────────────────────
        Text("Top contributors", fontWeight = FontWeight.Bold)
        result.factors.take(7).forEach { f ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${f.name} (${f.value})",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (f.deltaYears >= 0) "+%.1f yr".format(f.deltaYears)
                    else "%.1f yr".format(f.deltaYears),
                    fontWeight = FontWeight.SemiBold,
                    color = when (f.direction) {
                        BiologicalAgeEngine.Direction.better -> Color(0xFF34C759)
                        BiologicalAgeEngine.Direction.worse -> Color(0xFFFF9500)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        // ── HIPAA disclaimer ────────────────────────────────────
        Text(
            "This data stays on your device. Heuristic estimate — not a medical diagnosis.",
            fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(display, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Switch(checked = value, onCheckedChange = onToggle)
    }
}

@Composable
private fun AgeColumn(label: String, years: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp
        )
        Text("$years", fontSize = 56.sp, fontWeight = FontWeight.Black, color = color)
        Text("years", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
