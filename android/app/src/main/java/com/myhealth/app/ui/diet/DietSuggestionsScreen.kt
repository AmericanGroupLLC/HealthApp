package com.myhealth.app.ui.diet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.myhealth.app.data.prefs.SettingsRepository
import com.myhealth.app.ui.theme.CarePlusColor
import com.myhealth.core.health.HealthCondition
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DietSuggestion(
    val id: String,
    val condition: HealthCondition,
    val pattern: String,
    val prefer: List<String>,
    val avoid: List<String>,
    val dailyTargets: List<String>,
    val notes: String,
)

object DietSuggestionsData {
    val catalog: Map<HealthCondition, DietSuggestion> = mapOf(
        HealthCondition.none to DietSuggestion("balanced", HealthCondition.none, "Balanced",
            listOf("Vegetables", "Fruits", "Whole grains", "Lean protein", "Healthy fats", "Legumes", "Water"),
            listOf("Excess refined sugar", "Ultra-processed foods", "Sugary drinks"),
            listOf("~ ½ plate vegetables + fruit", "Protein with every meal", "8 glasses water"),
            "Default healthy-eating pattern based on USDA MyPlate."),
        HealthCondition.hypertension to DietSuggestion("hypertension", HealthCondition.hypertension, "DASH",
            listOf("Leafy greens", "Berries", "Bananas (potassium)", "Beets", "Oats", "Yogurt", "Salmon"),
            listOf("Salt-cured meats", "Pickles", "Canned soups", "Fast food", "Soy sauce"),
            listOf("Sodium < 1500-2300 mg", "Potassium 3500-4700 mg", "5+ servings veg/fruit"),
            "DASH diet has the strongest evidence for blood-pressure reduction."),
        HealthCondition.heartCondition to DietSuggestion("heart", HealthCondition.heartCondition, "Mediterranean",
            listOf("Olive oil", "Fatty fish", "Walnuts", "Berries", "Whole grains", "Legumes", "Avocado"),
            listOf("Trans fats", "Red meat (limit)", "Butter (limit)", "Sugary drinks"),
            listOf("Saturated fat < 7% kcal", "Fiber 25-38 g", "Omega-3 ~250 mg/day"),
            "Mediterranean diet → ~30% lower cardiovascular event risk."),
        HealthCondition.diabetesT2 to DietSuggestion("diabetes-t2", HealthCondition.diabetesT2, "Low GI",
            listOf("Non-starchy vegetables", "Lean protein", "Berries", "Beans + lentils", "Steel-cut oats"),
            listOf("White bread / rice / pasta", "Sugary drinks", "Fruit juice", "Pastries"),
            listOf("Carbs from low-GI sources", "Fiber 25+ g", "Protein at every meal"),
            "Low-GI + plate method keeps blood glucose stable."),
        HealthCondition.diabetesT1 to DietSuggestion("diabetes-t1", HealthCondition.diabetesT1, "Low GI",
            listOf("Counted carbs from whole foods", "Lean protein", "Vegetables", "Fiber-rich grains"),
            listOf("Surprise hidden sugars", "Sugar-sweetened drinks"),
            listOf("Carb count per meal (matched to insulin)", "Consistent meal timing"),
            "Always coordinate carb intake with your insulin regimen."),
        HealthCondition.obesity to DietSuggestion("obesity", HealthCondition.obesity, "High Fiber",
            listOf("Vegetables (½ plate)", "Lean protein", "Beans + lentils", "Whole fruits", "Water"),
            listOf("Sugar-sweetened drinks", "Fast food", "Liquid calories"),
            listOf("~ 500 kcal/day deficit", "Protein 1.2-1.6 g/kg", "Fiber 30+ g"),
            "Sustained ~5-10% body-weight loss reduces metabolic risk."),
        HealthCondition.kidneyIssue to DietSuggestion("kidney", HealthCondition.kidneyIssue, "Renal-Friendly",
            listOf("Cabbage", "Cauliflower", "Apples", "Berries", "Egg whites", "Olive oil"),
            listOf("Bananas (high potassium)", "Oranges", "Tomatoes", "Dairy", "Processed meats"),
            listOf("Sodium < 2000 mg", "Potassium per nephrologist", "Phosphorus 800-1000 mg"),
            "Renal targets vary by CKD stage. Defer to your nephrologist."),
        HealthCondition.anemia to DietSuggestion("anemia", HealthCondition.anemia, "Iron-Rich",
            listOf("Lean red meat", "Liver", "Spinach", "Lentils", "Tofu", "Pumpkin seeds"),
            listOf("Tea / coffee with meals", "Calcium supplements with iron-rich meals"),
            listOf("Iron 18 mg (women) / 8 mg (men)", "Pair iron foods with vitamin C"),
            "Pair plant iron with vitamin C to triple absorption."),
        HealthCondition.pregnancy to DietSuggestion("pregnancy", HealthCondition.pregnancy, "Gestational",
            listOf("Folate-rich greens", "Eggs", "Salmon", "Greek yogurt", "Legumes", "Whole grains"),
            listOf("Raw fish / sushi", "Unpasteurized cheeses", "High-mercury fish", "Alcohol"),
            listOf("+ 340-450 kcal in 2nd/3rd trimester", "Folate 600 µg", "Iron 27 mg"),
            "Always coordinate with your obstetrician."),
        HealthCondition.osteoporosis to DietSuggestion("osteoporosis", HealthCondition.osteoporosis, "Calcium-Rich",
            listOf("Greek yogurt", "Sardines", "Tofu", "Kale + collards", "Almonds", "Salmon"),
            listOf("Excess sodium", "Excess caffeine", "Soft drinks", "Heavy alcohol"),
            listOf("Calcium 1000-1200 mg", "Vitamin D 800-1000 IU", "Protein 1.0-1.2 g/kg"),
            "Pair calcium with vitamin D + weight-bearing exercise."),
        HealthCondition.asthma to DietSuggestion("asthma", HealthCondition.asthma, "Mediterranean",
            listOf("Apples", "Berries", "Leafy greens", "Carrots", "Salmon", "Walnuts"),
            listOf("Sulfite-heavy wines / dried fruit (if sensitive)"),
            listOf("Antioxidant-rich produce 5+ servings", "Omega-3 from fish 2× / week"),
            "Mediterranean pattern is associated with lower asthma severity."),
        HealthCondition.backPain to DietSuggestion("back", HealthCondition.backPain, "Anti-Inflammatory",
            listOf("Anti-inflammatory whole foods", "Hydration", "Magnesium-rich foods", "Omega-3 fish"),
            listOf("Ultra-processed foods", "Excess refined sugar"),
            listOf("Magnesium 320-420 mg", "Hydration 2-3 L"),
            "Anti-inflammatory pattern + bodyweight management ease lower-back pain."),
    )

    fun suggestions(conditions: Set<HealthCondition>): List<DietSuggestion> {
        val valid = conditions.minus(HealthCondition.none)
        if (valid.isEmpty()) return listOfNotNull(catalog[HealthCondition.none])
        return valid.mapNotNull { catalog[it] }
    }
}

@HiltViewModel
class DietSuggestionsViewModel @Inject constructor(
    settings: SettingsRepository,
) : ViewModel() {
    val conditions: StateFlow<Set<HealthCondition>> = settings.healthConditions
        .map { names -> names.mapNotNull { n -> HealthCondition.values().firstOrNull { it.name == n } }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, setOf(HealthCondition.none))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DietSuggestionsScreen(vm: DietSuggestionsViewModel = hiltViewModel()) {
    val conditions by vm.conditions.collectAsState()
    val suggestions = DietSuggestionsData.suggestions(conditions)
    val tint = CarePlusColor.DietCoral

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Diet Suggestions", fontSize = 28.sp, fontWeight = FontWeight.Bold)

        // ── Doctor disclaimer ────────────────────────────────────
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(CarePlusColor.Warning.copy(alpha = 0.10f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, null, tint = CarePlusColor.Warning,
                    modifier = Modifier.padding(end = 8.dp))
                Text(
                    "These are general dietary patterns, NOT medical prescriptions. Always consult your doctor or dietitian.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            "This data stays on your device — conditions are never sent to a server.",
            fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (suggestions.isEmpty()) {
            Text("No conditions declared. Set conditions in Settings to see personalized suggestions.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        suggestions.forEach { suggestion ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            suggestion.condition.symbol,
                            fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                suggestion.condition.label,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Pattern: ${suggestion.pattern}",
                                fontSize = 12.sp, color = tint, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(suggestion.notes,
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Text("✓ Foods to favor", fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF34C759))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        suggestion.prefer.forEach { food ->
                            Text(
                                food, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                color = Color(0xFF34C759)
                            )
                        }
                    }

                    Text("✗ Foods to limit", fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF3B30))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        suggestion.avoid.forEach { food ->
                            Text(
                                food, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color = Color(0xFFFF3B30)
                            )
                        }
                    }

                    Text("Daily targets", fontWeight = FontWeight.SemiBold)
                    suggestion.dailyTargets.forEach { target ->
                        Text("• $target", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
