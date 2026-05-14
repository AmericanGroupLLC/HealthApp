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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Restaurant
import android.content.Intent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.myhealth.app.ui.shell.AppHeader

@Composable
fun AnnualReportsScreen(nav: NavController) {
    val context = LocalContext.current
    Column {
        AppHeader(title = "Annual Reports", nav = nav)

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Year in Review",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text("Your 2024 health journey", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AnnualStat(Icons.Default.FitnessCenter, "Workouts", "156", Color(0xFFE91E63), Modifier.weight(1f))
                    AnnualStat(Icons.Default.Restaurant, "Meals Logged", "892", Color(0xFF4CAF50), Modifier.weight(1f))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AnnualStat(Icons.Default.CalendarMonth, "Avg Sleep", "7.3h", Color(0xFF7E57C2), Modifier.weight(1f))
                    AnnualStat(Icons.Default.Assessment, "Avg Steps", "8,420", Color(0xFF42A5F5), Modifier.weight(1f))
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Bio Age", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("32", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("years (↓2 from start of year)", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Medication Adherence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("94%", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        Text("of scheduled doses taken on time", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val shareText = buildString {
                            appendLine("MyHealth — 2024 Year in Review")
                            appendLine("================================")
                            appendLine("Workouts: 156")
                            appendLine("Meals Logged: 892")
                            appendLine("Avg Sleep: 7.3h")
                            appendLine("Avg Steps: 8,420")
                            appendLine("Bio Age: 32 years (↓2 from start of year)")
                            appendLine("Medication Adherence: 94%")
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "MyHealth 2024 Annual Report")
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share report"))
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, tint = Color(0xFF42A5F5))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Share with Doctor", fontWeight = FontWeight.Bold)
                            Text("Generate a summary PDF", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun AnnualStat(icon: ImageVector, label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
