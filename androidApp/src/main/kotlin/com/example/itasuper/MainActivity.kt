package com.example.itasuper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.shared.platform.LocationProvider
import com.example.shared.platform.PushNotifications
import com.example.shared.ui.permissions.PlatformPermissions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fase 5 item 1: registra o app para push (FCM).
        PushNotifications.register()
        // Fase 5 item 2: inicializa o provedor de localização.
        LocationProvider.init(applicationContext)
        // ITEM 6/6: contexto para as checagens de permissão
        // (PlatformPermissions.hasLocationPermission/hasNotificationPermission).
        PlatformPermissions.init(applicationContext)
        setContent {
            App()
        }
    }
}
