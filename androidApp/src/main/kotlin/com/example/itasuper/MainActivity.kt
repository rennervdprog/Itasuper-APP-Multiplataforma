package com.example.itasuper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.shared.platform.PushNotifications

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fase 5 item 1: registra o app para push (FCM).
        PushNotifications.register()
        setContent {
            App()
        }
    }
}
