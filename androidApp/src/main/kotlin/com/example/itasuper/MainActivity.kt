package com.example.itasuper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.shared.platform.LocationProvider
import com.example.shared.platform.PushNotifications

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fase 5 item 1: registra o app para push (FCM).
        PushNotifications.register()
        // Fase 5 item 2: inicializa o provedor de localização.
        LocationProvider.init(applicationContext)
        setContent {
            App()
        }
    }
}
