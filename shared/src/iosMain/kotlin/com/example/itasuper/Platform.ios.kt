package com.example.itasuper

import androidx.compose.ui.window.ComposeUIViewController

actual fun platformName(): String = "iOS"

/** Entry point usado pelo iosApp (Swift) para montar a UI Compose. */
fun MainViewController() = ComposeUIViewController { App() }
