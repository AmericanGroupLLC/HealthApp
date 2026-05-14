package com.myhealth.app.ui.sleep

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import com.myhealth.app.health.HealthConnectGateway
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val INHALE_SECS = 4
private const val HOLD_SECS = 4
private const val EXHALE_SECS = 6
private const val CYCLE_SECS = INHALE_SECS + HOLD_SECS + EXHALE_SECS
private const val SESSION_MINUTES = 5
private const val TOTAL_SECS = SESSION_MINUTES * 60

@Composable
fun WindDownScreen(
    healthConnect: HealthConnectGateway,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    var secondsRemaining by remember { mutableIntStateOf(TOTAL_SECS) }
    var isRunning by remember { mutableStateOf(true) }
    var phaseLabel by remember { mutableStateOf("Inhale") }

    val breathProgress = remember { Animatable(0f) }

    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        while (secondsRemaining > 0 && isRunning) {
            val cyclePos = (TOTAL_SECS - secondsRemaining) % CYCLE_SECS
            val (label, progress) = when {
                cyclePos < INHALE_SECS -> {
                    "Inhale" to (cyclePos.toFloat() / INHALE_SECS)
                }
                cyclePos < INHALE_SECS + HOLD_SECS -> {
                    "Hold" to 1f
                }
                else -> {
                    val exhalePos = cyclePos - INHALE_SECS - HOLD_SECS
                    "Exhale" to (1f - exhalePos.toFloat() / EXHALE_SECS)
                }
            }
            if (phaseLabel != label) {
                phaseLabel = label
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            breathProgress.snapTo(progress)
            delay(1000)
            secondsRemaining--
        }
        if (secondsRemaining <= 0) {
            scope.launch {
                healthConnect.writeMindfulSession(SESSION_MINUTES)
            }
            onDismiss()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Wind Down", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = { isRunning = false; onDismiss() }) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            val circleColor = when (phaseLabel) {
                "Inhale" -> Color(0xFF81D4FA)
                "Hold" -> Color(0xFFB39DDB)
                else -> Color(0xFF80CBC4)
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2 * (0.3f + 0.7f * breathProgress.value)
                drawCircle(
                    color = circleColor.copy(alpha = 0.3f),
                    radius = radius,
                    center = Offset(size.width / 2, size.height / 2),
                )
                drawCircle(
                    color = circleColor,
                    radius = radius,
                    center = Offset(size.width / 2, size.height / 2),
                    style = Stroke(width = 4.dp.toPx()),
                )
            }
            Text(
                phaseLabel,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        val minutes = secondsRemaining / 60
        val seconds = secondsRemaining % 60
        Text(
            "%d:%02d".format(minutes, seconds),
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = { isRunning = false; onDismiss() }) {
            Text("End Session")
        }
    }
}
