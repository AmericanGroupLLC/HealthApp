package com.myhealth.app.ui.diet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhealth.app.data.room.MealDao
import com.myhealth.app.data.room.MealEntity
import com.myhealth.app.ui.theme.CarePlusColor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@Composable
fun FoodDiaryScreen(vm: FoodDiaryViewModel = hiltViewModel()) {
    val tint = CarePlusColor.DietCoral
    val state by vm.state.collectAsState()
    val kcalGoal = 1800

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Food Diary", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Today", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)

        // ── Calorie summary ─────────────────────────────────────
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Row(
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { state.kcal.toFloat() / kcalGoal },
                        modifier = Modifier.size(80.dp),
                        color = tint,
                        strokeWidth = 6.dp,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${state.kcal}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("of $kcalGoal", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MacroRing("Protein", state.protein, 130, Color(0xFF34C759))
                    MacroRing("Carbs", state.carbs, 250, Color(0xFFFF9500))
                    MacroRing("Fat", state.fat, 65, Color(0xFF007AFF))
                }
            }
        }

        HorizontalDivider()
        Text("Meal history", fontWeight = FontWeight.SemiBold)

        if (state.meals.isEmpty()) {
            Text("No meals logged today.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        state.meals.forEach { meal ->
            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(meal.consumedAt))
            MealRow(meal.name, "${meal.kcal.toInt()} kcal", "P${meal.protein.toInt()} C${meal.carbs.toInt()} F${meal.fat.toInt()}", timeStr)
        }

        Text(
            "Tap + to add a meal. Barcode scanning coming soon.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MacroRing(label: String, current: Int, goal: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)) {
        CircularProgressIndicator(
            progress = { current.toFloat() / goal },
            modifier = Modifier.size(24.dp),
            color = color,
            strokeWidth = 3.dp,
        )
        Column(Modifier.padding(start = 8.dp)) {
            Text("$label: ${current}g / ${goal}g", fontSize = 11.sp)
        }
    }
}

@Composable
private fun MealRow(meal: String, description: String, kcal: String, time: String) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(meal, fontWeight = FontWeight.SemiBold)
                Text(description, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(kcal, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

data class FoodDiaryState(
    val kcal: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val meals: List<MealEntity> = emptyList(),
)

@HiltViewModel
class FoodDiaryViewModel @Inject constructor(
    mealDao: MealDao,
) : ViewModel() {
    private val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val state = mealDao.observeSince(startOfDay).map { meals ->
        FoodDiaryState(
            kcal = meals.sumOf { it.kcal }.toInt(),
            protein = meals.sumOf { it.protein }.toInt(),
            carbs = meals.sumOf { it.carbs }.toInt(),
            fat = meals.sumOf { it.fat }.toInt(),
            meals = meals,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FoodDiaryState())
}
