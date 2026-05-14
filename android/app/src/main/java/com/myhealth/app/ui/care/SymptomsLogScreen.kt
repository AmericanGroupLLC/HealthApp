package com.myhealth.app.ui.care

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.myhealth.app.ui.shell.AppHeader

@Composable
fun SymptomsLogScreen(
    nav: NavController,
    viewModel: SymptomsLogViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var bodyLocation by remember { mutableStateOf("") }
    var painScale by remember { mutableIntStateOf(5) }
    var durationHours by remember { mutableDoubleStateOf(1.0) }
    var notes by remember { mutableStateOf("") }
    var showValidationError by remember { mutableStateOf(false) }

    val bodyLocations = listOf("Head", "Chest", "Abdomen", "Back", "Left Arm", "Right Arm", "Left Leg", "Right Leg")

    Column {
        AppHeader(title = "Symptoms Log", nav = nav)

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Log new symptom
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Log Symptom", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Body Location")
                        if (showValidationError && bodyLocation.isBlank()) {
                            Text("Please select a body location", color = androidx.compose.ui.graphics.Color.Red,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            bodyLocations.take(4).forEach { loc ->
                                androidx.compose.material3.FilterChip(
                                    selected = bodyLocation == loc,
                                    onClick = { bodyLocation = loc },
                                    label = { Text(loc, style = MaterialTheme.typography.labelSmall) },
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            bodyLocations.drop(4).forEach { loc ->
                                androidx.compose.material3.FilterChip(
                                    selected = bodyLocation == loc,
                                    onClick = { bodyLocation = loc },
                                    label = { Text(loc, style = MaterialTheme.typography.labelSmall) },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Pain Scale: $painScale / 10")
                        if (showValidationError && (painScale < 1 || painScale > 10)) {
                            Text("Pain scale must be between 1 and 10", color = androidx.compose.ui.graphics.Color.Red,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Slider(
                            value = painScale.toFloat(),
                            onValueChange = { painScale = it.toInt() },
                            valueRange = 1f..10f,
                            steps = 8,
                        )

                        Text("Duration: ${"%.1f".format(durationHours)} hours")
                        Slider(
                            value = durationHours.toFloat(),
                            onValueChange = { durationHours = it.toDouble() },
                            valueRange = 0.5f..72f,
                        )

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                showValidationError = true
                                if (bodyLocation.isNotBlank() && painScale in 1..10) {
                                    viewModel.logSymptom(bodyLocation, painScale, durationHours, notes)
                                    bodyLocation = ""
                                    painScale = 5
                                    notes = ""
                                    showValidationError = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = bodyLocation.isNotBlank() && painScale in 1..10,
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log Symptom")
                        }
                    }
                }
            }

            // History
            item {
                Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(state.symptoms) { symptom ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(symptom.bodyLocation, fontWeight = FontWeight.Bold)
                            Text("Pain: ${symptom.painScale}/10", style = MaterialTheme.typography.bodySmall)
                            symptom.notes?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { viewModel.deleteSymptom(symptom.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
