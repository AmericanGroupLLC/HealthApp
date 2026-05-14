package com.myhealth.app.ui.care

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

private fun scoreColor(score: Int): Color = when {
    score >= 75 -> Color(0xFF4CAF50)
    score >= 50 -> Color(0xFFFFC107)
    else -> Color(0xFFF44336)
}

/**
 * Unified Health Score (0-100) card.
 *
 * Weights:
 *  - Bio age delta:          25%
 *  - Recovery score:         25%
 *  - Activity ring completion: 20%
 *  - Sleep quality:          15%
 *  - Nutrition adherence:    15%
 */
@Composable
fun HealthScoreCard(
    nav: NavController,
    bioAgeDeltaYears: Double = 0.0,
    recoveryScore: Int = 70,
    activityRingPct: Int = 60,
    sleepQuality: Int = 75,
    nutritionAdherence: Int = 65,
) {
    // Bio age delta → 0-100 sub-score (0 delta = 100, ±10 yr = 0)
    val bioAgeScore = (100 - (kotlin.math.abs(bioAgeDeltaYears) * 10)).coerceIn(0.0, 100.0)

    val score = (
        bioAgeScore * 0.25 +
        recoveryScore * 0.25 +
        activityRingPct * 0.20 +
        sleepQuality * 0.15 +
        nutritionAdherence * 0.15
    ).toInt().coerceIn(0, 100)

    val color = scoreColor(score)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Health Score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))

            // Circular progress with score in center
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .semantics { contentDescription = "Health score $score out of 100" },
            ) {
                val sweepAngle = score / 100f * 360f
                val trackColor = MaterialTheme.colorScheme.outlineVariant
                Canvas(Modifier.size(120.dp)) {
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                Text(
                    text = "$score",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Factor breakdown
            val factors = listOf(
                "Bio Age" to bioAgeScore.toInt(),
                "Recovery" to recoveryScore,
                "Activity" to activityRingPct,
                "Sleep" to sleepQuality,
                "Nutrition" to nutritionAdherence,
            )
            factors.forEach { (label, value) ->
                FactorRow(label, value)
            }
        }
    }
}

@Composable
private fun FactorRow(label: String, value: Int) {
    val color = scoreColor(value)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$value", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
