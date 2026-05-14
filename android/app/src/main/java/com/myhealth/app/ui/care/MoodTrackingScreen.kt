package com.myhealth.app.ui.care

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val MOOD_FACES = listOf("😢", "😟", "😐", "😊", "😁")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodTrackingScreen(
    nav: NavController,
    vm: MoodTrackingViewModel = hiltViewModel(),
) {
    val moods by vm.moods.collectAsStateWithLifecycle()
    var selectedScale by remember { mutableIntStateOf(3) }
    var journalText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mood Tracking") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "How are you feeling?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Mood scale selector
            item {
                Card(
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        val moodLabels = listOf("Very sad", "Sad", "Neutral", "Happy", "Very happy")
                        MOOD_FACES.forEachIndexed { index, face ->
                            val scale = index + 1
                            val isSelected = scale == selectedScale
                            Text(
                                text = face,
                                fontSize = if (isSelected) 40.sp else 28.sp,
                                modifier = Modifier
                                    .clickable { selectedScale = scale }
                                    .padding(4.dp)
                                    .semantics {
                                        contentDescription = "${moodLabels[index]} mood, ${if (isSelected) "selected" else "not selected"}"
                                    },
                            )
                        }
                    }
                }
            }

            // Journal entry
            item {
                OutlinedTextField(
                    value = journalText,
                    onValueChange = { journalText = it },
                    label = { Text("Journal note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                )
            }

            // Save button
            item {
                Button(
                    onClick = {
                        vm.saveMood(selectedScale, journalText)
                        journalText = ""
                        selectedScale = 3
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Save mood")
                }
            }

            // Recent entries header
            if (moods.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Recent entries",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Recent mood entries list
            items(moods, key = { it.id }) { mood ->
                MoodEntryRow(mood)
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun MoodEntryRow(mood: com.myhealth.app.data.room.MoodEntity) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    Card(
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = MOOD_FACES.getOrElse(mood.value - 1) { "😐" },
                fontSize = 28.sp,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = dateFormat.format(Date(mood.recordedAt)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!mood.note.isNullOrBlank()) {
                    Text(mood.note, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}
