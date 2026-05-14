package com.myhealth.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorBoundary(
    fallback: @Composable (Throwable) -> Unit = { error ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Something went wrong", style = MaterialTheme.typography.headlineSmall)
            Text(
                error.localizedMessage ?: "Unknown error",
                color = MaterialTheme.colorScheme.error,
            )
        }
    },
    content: @Composable () -> Unit,
) {
    // Compose does not support try-catch around composables.
    // Use UiState.Error pattern in ViewModels to surface errors,
    // then render `fallback` when UiState is Error.
    content()
}
