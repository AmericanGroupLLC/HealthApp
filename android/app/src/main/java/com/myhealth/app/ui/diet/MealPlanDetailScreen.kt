package com.myhealth.app.ui.diet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.myhealth.app.data.prefs.SettingsRepository
import com.myhealth.app.data.room.MealDao
import com.myhealth.app.data.room.MealEntity
import com.myhealth.app.ui.shell.AppHeader
import com.myhealth.core.health.HealthCondition
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MealSuggestion(
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
)

data class MealPlanState(
    val weeklyPlan: Map<String, List<MealSuggestion>> = emptyMap(),
)

@HiltViewModel
class MealPlanViewModel @Inject constructor(
    settings: SettingsRepository,
    private val mealDao: MealDao,
) : ViewModel() {

    private val conditions = settings.healthConditions
        .map { names -> names.mapNotNull { n -> HealthCondition.values().firstOrNull { it.name == n } }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _state = MutableStateFlow(MealPlanState())
    val state: StateFlow<MealPlanState> = _state.asStateFlow()

    init { generateWeeklyPlan() }

    private fun generateWeeklyPlan() {
        val conds = conditions.value
        val isLowSodium = conds.contains(HealthCondition.hypertension)
        val isLowSugar = conds.contains(HealthCondition.diabetesT1) || conds.contains(HealthCondition.diabetesT2)
        val targetCal = 2000

        val mealPool = buildMealPool(isLowSodium, isLowSugar, targetCal)
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val plan = days.associateWith { day ->
            val offset = days.indexOf(day)
            listOf(
                mealPool.breakfasts[(offset) % mealPool.breakfasts.size],
                mealPool.lunches[(offset) % mealPool.lunches.size],
                mealPool.dinners[(offset) % mealPool.dinners.size],
            )
        }
        _state.value = MealPlanState(weeklyPlan = plan)
    }

    fun logMeal(meal: MealSuggestion) = viewModelScope.launch {
        mealDao.insert(
            MealEntity(
                name = meal.name,
                kcal = meal.calories.toDouble(),
                protein = meal.protein.toDouble(),
                carbs = meal.carbs.toDouble(),
                fat = meal.fat.toDouble(),
            )
        )
    }

    private data class MealPool(
        val breakfasts: List<MealSuggestion>,
        val lunches: List<MealSuggestion>,
        val dinners: List<MealSuggestion>,
    )

    private fun buildMealPool(lowSodium: Boolean, lowSugar: Boolean, targetCal: Int): MealPool {
        val bCal = (targetCal * 0.25).toInt()
        val lCal = (targetCal * 0.35).toInt()
        val dCal = (targetCal * 0.40).toInt()

        val breakfasts = listOfNotNull(
            MealSuggestion("Oatmeal & berries", bCal, 12, 55, 8),
            MealSuggestion("Greek yogurt & granola", bCal, 18, 40, 10),
            MealSuggestion("Egg & avocado toast", bCal, 16, 30, 22),
            MealSuggestion("Smoothie bowl", bCal, 14, 48, 12),
            if (!lowSugar) MealSuggestion("Banana pancakes", bCal, 10, 52, 14) else null,
            MealSuggestion("Chia pudding & fruit", bCal, 12, 42, 16),
            MealSuggestion("Veggie scramble", bCal, 20, 18, 20),
        )

        val lunches = listOfNotNull(
            MealSuggestion("Grilled chicken salad", lCal, 35, 20, 22),
            MealSuggestion("Turkey wrap", lCal, 28, 38, 16),
            MealSuggestion("Lentil soup & bread", lCal, 20, 60, 12),
            MealSuggestion("Quinoa buddha bowl", lCal, 24, 50, 18),
            if (!lowSodium) MealSuggestion("Tuna sandwich", lCal, 30, 34, 20) else null,
            MealSuggestion("Chicken & rice bowl", lCal, 32, 45, 15),
            MealSuggestion("Mediterranean wrap", lCal, 22, 42, 20),
        )

        val dinners = listOfNotNull(
            MealSuggestion("Salmon & quinoa", dCal, 40, 45, 18),
            MealSuggestion("Stir fry tofu & veggies", dCal, 22, 50, 15),
            MealSuggestion("Grilled fish & sweet potato", dCal, 38, 48, 14),
            MealSuggestion("Chicken stir fry & brown rice", dCal, 35, 52, 16),
            MealSuggestion("Baked cod & roasted veggies", dCal, 36, 40, 12),
            MealSuggestion("Turkey meatballs & pasta", dCal, 32, 55, 18),
            if (!lowSodium) MealSuggestion("Shrimp fried rice", dCal, 28, 58, 20) else null,
        )

        return MealPool(breakfasts, lunches, dinners)
    }
}

@Composable
fun MealPlanDetailScreen(nav: NavController, vm: MealPlanViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val days = state.weeklyPlan.keys.toList()
    var selectedDay by remember { mutableIntStateOf(0) }

    Column {
        AppHeader(title = "Meal Plan", nav = nav)

        if (days.isNotEmpty()) {
            ScrollableTabRow(selectedTabIndex = selectedDay) {
                days.forEachIndexed { i, day ->
                    Tab(selected = selectedDay == i, onClick = { selectedDay = i }) {
                        Text(day, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            val meals = state.weeklyPlan[days[selectedDay]] ?: emptyList()
            val mealTypes = listOf("Breakfast", "Lunch", "Dinner")
            val mealIcons = listOf(Icons.Default.Restaurant, Icons.Default.LunchDining, Icons.Default.DinnerDining)

            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    val totalCal = meals.sumOf { it.calories }
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Daily Target", style = MaterialTheme.typography.titleMedium)
                            Text("$totalCal kcal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("P: ${meals.sumOf { it.protein }}g")
                                Text("C: ${meals.sumOf { it.carbs }}g")
                                Text("F: ${meals.sumOf { it.fat }}g")
                            }
                        }
                    }
                }

                meals.forEachIndexed { i, meal ->
                    item {
                        val icon = mealIcons.getOrElse(i) { Icons.Default.Restaurant }
                        val type = mealTypes.getOrElse(i) { "Meal" }
                        MealCard(type = type, icon = icon, meal = meal)
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun MealCard(type: String, icon: ImageVector, meal: MealSuggestion) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(type, style = MaterialTheme.typography.labelMedium)
                Text(meal.name, fontWeight = FontWeight.Bold)
                Text("${meal.calories} kcal • P:${meal.protein}g C:${meal.carbs}g F:${meal.fat}g", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
