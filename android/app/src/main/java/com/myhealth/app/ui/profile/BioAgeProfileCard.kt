package com.myhealth.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myhealth.core.intelligence.BiologicalAgeEngine
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun BioAgeProfileCard(result: BiologicalAgeEngine.Result) {
    val delta = result.deltaYears
    val deltaText = when {
        delta < -0.5 -> "${abs(delta).roundToInt()} years younger!"
        delta > 0.5 -> "${abs(delta).roundToInt()} years older"
        else -> "Right on track"
    }
    val deltaColor = when {
        delta < -0.5 -> Color(0xFF4CAF50)
        delta > 0.5 -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Biological Age",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                AgeColumn("Bio Age", result.biologicalYears.roundToInt().toString())
                AgeColumn("Chrono Age", result.chronologicalYears.roundToInt().toString())
            }

            Spacer(Modifier.height(8.dp))
            Text(
                deltaText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = deltaColor,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            if (result.factors.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Factor breakdown",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                result.factors.forEach { factor ->
                    FactorRow(factor)
                }
            }
        }
    }
}

@Composable
private fun AgeColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FactorRow(factor: BiologicalAgeEngine.Factor) {
    val arrow = when (factor.direction) {
        BiologicalAgeEngine.Direction.better -> "↓"
        BiologicalAgeEngine.Direction.worse -> "↑"
        BiologicalAgeEngine.Direction.neutral -> "→"
    }
    val arrowLabel = when (factor.direction) {
        BiologicalAgeEngine.Direction.better -> "improving"
        BiologicalAgeEngine.Direction.worse -> "worsening"
        BiologicalAgeEngine.Direction.neutral -> "neutral"
    }
    val arrowColor = when (factor.direction) {
        BiologicalAgeEngine.Direction.better -> Color(0xFF4CAF50)
        BiologicalAgeEngine.Direction.worse -> Color(0xFFF44336)
        BiologicalAgeEngine.Direction.neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .semantics {
                contentDescription = "${factor.name}: ${factor.value}, $arrowLabel by ${"%.1f".format(abs(factor.deltaYears))} years"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("${factor.name}: ${factor.value}", fontSize = 13.sp)
        Text(
            "$arrow ${"%.1f".format(abs(factor.deltaYears))} yr",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = arrowColor,
        )
    }
}
