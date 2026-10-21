package com.example.itasuper

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.shared.platform.LocationProvider
import com.example.shared.platform.PushNotifications
import com.example.shared.session.UserSessionRepository
import com.example.shared.ui.permissions.PlatformPermissions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-edge global: barras do sistema transparentes com icones escuros
        // (tema do app e sempre claro). Sem isso, o sistema aplica padding duplo
        // e a status bar fica branca sem controle de cor.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
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

    override fun onResume() {
        super.onResume()
        // Igual ao app original: ao voltar pro app, renova o token da sessão
        // (o token vence em ~1h; sem renovar, Pedidos/Avisos param de atualizar).
        UserSessionRepository.refreshSession()
    }
}
