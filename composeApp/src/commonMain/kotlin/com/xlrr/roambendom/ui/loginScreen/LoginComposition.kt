package com.xlrr.roambendom.ui.loginScreen

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope

@Composable
expect fun AskConfirmButton(viewModel: LoginViewModel, dismiss: () -> Unit, cs: CoroutineScope)