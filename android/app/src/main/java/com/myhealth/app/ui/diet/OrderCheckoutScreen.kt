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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.myhealth.app.ui.shell.AppHeader

private data class OrderItem(val name: String, val qty: Int, val price: Double, val calories: Int)

@Composable
fun OrderCheckoutScreen(nav: NavController) {
    val items = remember {
        listOf(
            OrderItem("Grilled Chicken Bowl", 1, 12.99, 520),
            OrderItem("Green Smoothie", 1, 7.49, 180),
            OrderItem("Protein Bar", 2, 3.99, 220),
        )
    }
    var orderPlaced by remember { mutableStateOf(false) }
    var orderId by remember { mutableStateOf("") }

    Column {
        AppHeader(title = "Order Checkout", nav = nav)

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (orderPlaced) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.height(48.dp).width(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Order Confirmed!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Order #$orderId", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Your healthy meal is on its way", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(items) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold)
                                Text("${item.calories} kcal • Qty: ${item.qty}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("$${"%,.2f".format(item.price * item.qty)}", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Nutritional summary
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Nutritional Summary", style = MaterialTheme.typography.titleSmall)
                            Text("Total: ${items.sumOf { it.calories * it.qty }} kcal", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Total
                item {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "$${"%,.2f".format(items.sumOf { it.price * it.qty })}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                item {
                    Button(
                        onClick = {
                            orderId = java.util.UUID.randomUUID().toString().take(8).uppercase()
                            orderPlaced = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Place Order")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
