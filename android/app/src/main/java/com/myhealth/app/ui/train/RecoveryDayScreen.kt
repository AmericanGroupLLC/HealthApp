package com.myhealth.app.ui.train

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.myhealth.app.ui.shell.AppHeader

@Composable
fun RecoveryDayScreen(nav: NavController) {
    val stretches = listOf(
        "Cat-Cow Stretch" to "Spine mobility • 2 min",
        "Standing Hamstring Stretch" to "Hamstrings • 30s each side",
        "Child's Pose" to "Lower back • 1 min",
        "Pigeon Pose" to "Hip flexors • 30s each side",
        "Neck Rolls" to "Neck • 1 min",
        "Shoulder Circles" to "Shoulders • 1 min",
    )

    val tips = listOf(
        "Stay hydrated — aim for 2L of water today",
        "Light walking (20-30 min) promotes blood flow without stress",
        "Prioritize 8+ hours of sleep tonight",
        "Consider foam rolling tight muscle groups",
    )

    Column {
        AppHeader(title = "Recovery Day", nav = nav)

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Suggested Stretches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(stretches) { (name, meta) ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.SelfImprovement, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(name, fontWeight = FontWeight.Bold)
                            Text(meta, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { nav.navigate("sleep") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.NightsStay, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guided Breathing")
                }
            }

            item {
                Text("Recovery Tips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(tips) { tip ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(tip, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
