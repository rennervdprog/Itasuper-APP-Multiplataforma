package com.example.itasuper

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.shared.platform.ItaSuperFirebaseMessagingService
import com.example.shared.platform.LocationProvider
import com.example.shared.platform.PushNavigation
import com.example.shared.platform.PushNotifications
import com.example.shared.platform.PushRegistration
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
        // Push real: inicializa o Firebase (manualmente, ver PushNotifications)
        // e registra o app para receber FCM.
        PushNotifications.init(applicationContext)
        PushNotifications.register()
        // Fase 5 item 2: inicializa o provedor de localização.
        LocationProvider.init(applicationContext)
        // ITEM 6/6: contexto para as checagens de permissão
        // (PlatformPermissions.hasLocationPermission/hasNotificationPermission).
        PlatformPermissions.init(applicationContext)
        // Toque numa notificação push: guarda o destino pro App navegar.
        handlePushIntent(intent)
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Igual ao app original: ao voltar pro app, renova o token da sessão
        // (o token vence em ~1h; sem renovar, Pedidos/Avisos param de atualizar).
        UserSessionRepository.refreshSession()
        // Igual ao original (registerCurrentDevice no onResume): reafirma o
        // registro do aparelho pro push com a sessão atual.
        PushRegistration.syncAsync()
    }

    private fun handlePushIntent(intent: Intent?) {
        val orderId = intent?.getStringExtra(ItaSuperFirebaseMessagingService.EXTRA_ORDER_ID)
        if (!orderId.isNullOrBlank()) {
            PushNavigation.request(orderId)
        }
    }
}
