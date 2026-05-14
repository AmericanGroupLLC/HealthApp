package com.myhealth.app.ui.diet

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.myhealth.app.ui.shell.AppHeader

@Composable
fun WaterTrackerScreen(
    nav: NavController,
    viewModel: WaterTrackerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress by animateFloatAsState(
        targetValue = (state.todayMl / state.goalMl).toFloat().coerceIn(0f, 1f),
        label = "water_progress",
    )

    Column {
        AppHeader(title = "Water Tracker", nav = nav)

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.WaterDrop,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF42A5F5),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${(state.todayMl / 1000).let { "%.1f".format(it) }}L",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "of ${(state.goalMl / 1000).let { "%.1f".format(it) }}L goal",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Water fill animation
                        Box(modifier = Modifier.size(width = 120.dp, height = 160.dp)) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                drawRect(
                                    color = Color(0xFF42A5F5).copy(alpha = 0.1f),
                                    size = size,
                                )
                                val fillHeight = size.height * progress
                                drawRect(
                                    color = Color(0xFF42A5F5).copy(alpha = 0.5f),
                                    topLeft = Offset(0f, size.height - fillHeight),
                                    size = Size(size.width, fillHeight),
                                )
                            }
                        }
                    }
                }
            }

            // Quick-add buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    QuickAddButton("8 oz", 236.0) { viewModel.addWater(it) }
                    QuickAddButton("12 oz", 355.0) { viewModel.addWater(it) }
                    QuickAddButton("16 oz", 473.0) { viewModel.addWater(it) }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun QuickAddButton(label: String, ml: Double, onClick: (Double) -> Unit) {
    Button(
        onClick = { onClick(ml) },
        shape = RoundedCornerShape(12.dp),
    ) {
        Icon(Icons.Default.WaterDrop, contentDescription = null)
        Text(" +$label")
    }
}
