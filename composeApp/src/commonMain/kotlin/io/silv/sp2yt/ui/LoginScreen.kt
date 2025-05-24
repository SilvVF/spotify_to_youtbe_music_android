package io.silv.sp2yt.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
expect fun LoginScreen(
    onBack: () -> Unit,
)