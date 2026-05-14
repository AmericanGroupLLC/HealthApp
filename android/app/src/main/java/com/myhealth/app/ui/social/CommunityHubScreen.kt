package com.myhealth.app.ui.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.myhealth.app.ui.shell.AppHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityHubScreen(
    nav: NavController,
    viewModel: SocialViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddFriend by remember { mutableStateOf(false) }
    var showCreateChallenge by remember { mutableStateOf(false) }

    if (showAddFriend) {
        AddFriendDialog(
            onDismiss = { showAddFriend = false },
            onAdd = { name, handle ->
                viewModel.addFriend(name, handle)
                showAddFriend = false
            },
        )
    }

    if (showCreateChallenge) {
        ModalBottomSheet(
            onDismissRequest = { showCreateChallenge = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            CreateChallengeSheet(
                onDismiss = { showCreateChallenge = false },
                onCreate = { title, kind, days, target ->
                    viewModel.createChallenge(title, kind, days, target)
                    showCreateChallenge = false
                },
            )
        }
    }

    Column {
        AppHeader(title = "Community", nav = nav)

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Active Challenges
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Active Challenges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showCreateChallenge = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create")
                    }
                }
            }

            items(state.challenges) { challenge ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(challenge.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${challenge.kind} • Target: ${challenge.target.toInt()}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.joinChallenge(challenge.id.toInt()) }) { Text("Join") }
                            OutlinedButton(onClick = { nav.navigate("challenge_detail/${challenge.id}") }) { Text("Details") }
                        }
                    }
                }
            }

            // Leaderboard
            if (state.leaderboard.isNotEmpty()) {
                item {
                    Text("Leaderboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                itemsIndexed(state.leaderboard) { index, entry ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val badge = when (index) {
                                0 -> "🥇"
                                1 -> "🥈"
                                2 -> "🥉"
                                else -> "#${index + 1}"
                            }
                            Text(badge, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(40.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.name, fontWeight = FontWeight.Bold)
                                Text(entry.email, style = MaterialTheme.typography.bodySmall)
                            }
                            Text("${entry.score.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Friends
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Friends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showAddFriend = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }

            items(state.friends) { friend ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(friend.name, fontWeight = FontWeight.Bold)
                            Text("@${friend.handle}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun AddFriendDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Friend") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(value = handle, onValueChange = { handle = it }, label = { Text("Handle") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onAdd(name, handle) }, enabled = name.isNotBlank() && handle.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CreateChallengeSheet(onDismiss: () -> Unit, onCreate: (String, String, Int, Double) -> Unit) {
    var title by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("steps") }
    var days by remember { mutableIntStateOf(7) }
    var target by remember { mutableDoubleStateOf(10000.0) }

    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Create Challenge", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

        Text("Kind: $kind")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("steps", "active_minutes", "workouts").forEach { k ->
                OutlinedButton(onClick = { kind = k }) { Text(k.replace("_", " ")) }
            }
        }

        Text("Days: $days")
        Slider(value = days.toFloat(), onValueChange = { days = it.toInt() }, valueRange = 1f..30f, steps = 28)

        Text("Target: ${target.toInt()}")
        Slider(value = target.toFloat(), onValueChange = { target = it.toDouble() }, valueRange = 100f..50000f)

        Button(
            onClick = { onCreate(title, kind, days, target) },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank(),
        ) { Text("Create") }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
