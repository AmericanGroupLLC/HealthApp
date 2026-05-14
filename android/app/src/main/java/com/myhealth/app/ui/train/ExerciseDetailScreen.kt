package com.myhealth.app.ui.train

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.myhealth.app.ui.theme.CarePlusColor
import androidx.navigation.NavController
import com.myhealth.app.ui.Routes
import com.myhealth.core.exercises.ExerciseLibrary
import com.myhealth.core.health.ExerciseMedia

@Composable
fun ExerciseDetailScreen(exerciseId: String, nav: NavController) {
    val exercise = ExerciseLibrary.byId(exerciseId)
    val tint = CarePlusColor.TrainGreen

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (exercise == null) {
            Text("Exercise not found", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            return@Column
        }

        Text(exercise.name, fontSize = 28.sp, fontWeight = FontWeight.Bold)

        AsyncImage(
            model = ExerciseMedia.gifUrl(exercise.id),
            contentDescription = exercise.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                colors = CardDefaults.cardColors(tint.copy(alpha = 0.10f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    exercise.equipment.label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = tint
                )
            }
            Card(
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    exercise.difficulty.label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 12.sp
                )
            }
        }

        HorizontalDivider()

        Text("Primary muscles", fontWeight = FontWeight.SemiBold)
        Text(exercise.primaryMuscles.joinToString { it.label },
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (exercise.secondaryMuscles.isNotEmpty()) {
            Text("Secondary muscles", fontWeight = FontWeight.SemiBold)
            Text(exercise.secondaryMuscles.joinToString { it.label },
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        HorizontalDivider()

        Text("Instructions", fontWeight = FontWeight.SemiBold)
        exercise.instructions.forEachIndexed { i, step ->
            Text("${i + 1}. $step", modifier = Modifier.padding(vertical = 2.dp))
        }

        if (exercise.formTips.isNotEmpty()) {
            HorizontalDivider()
            Text("Form tips", fontWeight = FontWeight.SemiBold)
            exercise.formTips.forEach { tip ->
                Text("• $tip", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider()

        Button(
            onClick = { nav.navigate(Routes.WORKOUT_LOGGER) },
            colors = ButtonDefaults.buttonColors(containerColor = tint),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log a Set")
        }
    }
}
