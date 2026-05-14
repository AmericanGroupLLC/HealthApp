package com.myhealth.app.ui.train

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.myhealth.app.ui.Routes
import com.myhealth.app.ui.theme.CarePlusColor
import com.myhealth.core.exercises.Equipment
import com.myhealth.core.exercises.ExerciseLibrary
import com.myhealth.core.exercises.MuscleGroup
import com.myhealth.core.health.ExerciseMedia

@Composable
fun ExerciseLibraryScreen(nav: NavController) {
    var query by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf<MuscleGroup?>(null) }
    var selectedEquipment by remember { mutableStateOf<Equipment?>(null) }
    var muscleExpanded by remember { mutableStateOf(false) }
    var equipmentExpanded by remember { mutableStateOf(false) }
    val tint = CarePlusColor.TrainGreen

    val filtered = ExerciseLibrary.exercises.filter { e ->
        (query.isBlank() || e.name.contains(query, ignoreCase = true)) &&
        (selectedMuscle == null || e.primaryMuscles.contains(selectedMuscle) || e.secondaryMuscles.contains(selectedMuscle)) &&
        (selectedEquipment == null || e.equipment == selectedEquipment)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Exercise Library", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(
            "${ExerciseLibrary.exercises.size} exercises",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text("Search exercises") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { muscleExpanded = true }) {
                Text(selectedMuscle?.label ?: "All muscles", color = tint)
            }
            DropdownMenu(expanded = muscleExpanded, onDismissRequest = { muscleExpanded = false }) {
                DropdownMenuItem(text = { Text("All muscles") }, onClick = {
                    selectedMuscle = null; muscleExpanded = false
                })
                MuscleGroup.values().forEach { m ->
                    DropdownMenuItem(text = { Text(m.label) }, onClick = {
                        selectedMuscle = m; muscleExpanded = false
                    })
                }
            }

            TextButton(onClick = { equipmentExpanded = true }) {
                Text(selectedEquipment?.label ?: "All equipment", color = tint)
            }
            DropdownMenu(expanded = equipmentExpanded, onDismissRequest = { equipmentExpanded = false }) {
                DropdownMenuItem(text = { Text("All equipment") }, onClick = {
                    selectedEquipment = null; equipmentExpanded = false
                })
                Equipment.values().forEach { eq ->
                    DropdownMenuItem(text = { Text(eq.label) }, onClick = {
                        selectedEquipment = eq; equipmentExpanded = false
                    })
                }
            }
        }

        Text(
            "${filtered.size} results", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { exercise ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate("${Routes.EXERCISE_DETAIL}/${exercise.id}") },
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = ExerciseMedia.thumbnailUrl(exercise.id),
                            contentDescription = exercise.name,
                            modifier = Modifier
                                .width(56.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(exercise.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                exercise.primaryMuscles.joinToString { it.label },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${exercise.equipment.label} · ${exercise.difficulty.label}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
