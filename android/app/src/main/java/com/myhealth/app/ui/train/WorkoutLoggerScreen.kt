package com.myhealth.app.ui.train

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.myhealth.app.ui.shell.AppHeader
import com.myhealth.app.ui.theme.CareTab
import com.myhealth.app.ui.Routes
import com.myhealth.core.models.LoggedSet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutLoggerScreen(
    nav: NavController,
    viewModel: WorkoutLoggerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showRestTimer by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) nav.popBackStack()
    }

    if (showRestTimer) {
        ModalBottomSheet(
            onDismissRequest = { showRestTimer = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            RestTimerSheet(
                totalSeconds = state.restTimerSeconds,
                onDismiss = { showRestTimer = false },
            )
        }
    }

    Column {
        AppHeader(
            tab = CareTab.Train,
            onProfile = { nav.navigate(Routes.PROFILE) },
            onBell = { nav.navigate(Routes.NEWS_DRAWER) },
        )

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    state.exerciseName.ifEmpty { "Select an exercise" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            itemsIndexed(state.sets) { index, set ->
                SetRow(
                    index = index + 1,
                    set = set,
                    onUpdate = { viewModel.updateSet(index, it) },
                    onRemove = { viewModel.removeSet(index) },
                    canRemove = state.sets.size > 1,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { viewModel.addSet() }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Set")
                    }
                    TextButton(onClick = { showRestTimer = true }) {
                        Icon(Icons.Default.Timer, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rest Timer")
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { viewModel.updateNotes(it) },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )
            }

            item {
                Button(
                    onClick = { viewModel.save() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !state.isSaving && state.sets.isNotEmpty(),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                    } else {
                        Text("Save Workout")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SetRow(
    index: Int,
    set: LoggedSet,
    onUpdate: (LoggedSet) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Set $index", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(48.dp))
            OutlinedTextField(
                value = set.reps.toString(),
                onValueChange = { it.toIntOrNull()?.let { r -> onUpdate(set.copy(reps = r)) } },
                label = { Text("Reps") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            OutlinedTextField(
                value = "%.1f".format(set.weight),
                onValueChange = { it.toDoubleOrNull()?.let { w -> onUpdate(set.copy(weight = w)) } },
                label = { Text("kg") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove set")
                }
            }
        }
    }
}

@Composable
private fun RestTimerSheet(totalSeconds: Int, onDismiss: () -> Unit) {
    var remaining by remember { mutableStateOf(totalSeconds) }
    var isRunning by remember { mutableStateOf(true) }
    val progress by animateFloatAsState(
        targetValue = remaining.toFloat() / totalSeconds,
        label = "rest_progress",
    )

    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        while (remaining > 0 && isRunning) {
            kotlinx.coroutines.delay(1000)
            remaining--
        }
        if (remaining <= 0) onDismiss()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Rest Timer", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        androidx.compose.foundation.layout.Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.height(120.dp).width(120.dp),
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.height(120.dp).width(120.dp),
                strokeWidth = 8.dp,
            )
            Text(
                "%d:%02d".format(remaining / 60, remaining % 60),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = { remaining = totalSeconds }) { Text("Restart") }
            Button(onClick = onDismiss) { Text("Skip") }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
