package com.example.shared.ui.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.platform.PushNotifications
import com.example.shared.platform.SecureStorage
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.theme.ItaSuperTextPrimary
import com.example.shared.theme.ItaSuperTextSecondary

/**
 * Migração 1:1 de `Itasuper-APP-NATIVO/.../ui/permissions/NotificationPermissionOnboarding.kt`.
 *
 * Mostra o pedido de notificações somente após contextualizar as atualizações de
 * pedido. Estrutura, textos, ordem, espaçamentos (dp), tamanhos de fonte (sp) e
 * cores reproduzidos literalmente do original. Adaptações KMP documentadas:
 *
 * - `NotificationOnboardingPreferences` (SharedPreferences via `Context`) virou a
 *   versão com [SecureStorage] em `PermissionOnboardingPreferences.kt`.
 * - `PushNotificationManager.hasNotificationPermission(context)` (com o corte de
 *   `Build.VERSION_CODES.TIRAMISU`) virou [PlatformPermissions.hasNotificationPermission]
 *   (expect/actual; o corte de SDK vive no actual Android).
 * - `PushNotificationManager.registerCurrentDevice(context)` (obtém o token FCM e
 *   vincula à conta) virou [PushNotifications.register] (expect object da Fase 5;
 *   o vínculo do token à conta fica para a fase-rede).
 * - `rememberLauncherForActivityResult(RequestPermission())` virou
 *   [rememberNotificationPermissionLauncher] (expect/actual).
 * - Ícones via `Icons.Filled.*` (material-icons-extended, já declarado no shared).
 */
@Composable
fun NotificationPermissionOnboarding(
    userId: String,
    onFinished: (granted: Boolean) -> Unit
) {
    val storage = remember { SecureStorage() }
    var isRequesting by remember { mutableStateOf(false) }

    fun complete(granted: Boolean) {
        NotificationOnboardingPreferences.markCompleted(storage, userId)
        if (granted) PushNotifications.register()
        onFinished(granted)
    }

    val launchNotificationPermission = rememberNotificationPermissionLauncher { granted ->
        complete(granted || PlatformPermissions.hasNotificationPermission())
    }

    Surface(
        modifier = Modifier.fillMaxSize().testTag("notification_permission_onboarding"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(38.dp))
                    .background(Brush.verticalGradient(listOf(ItaSuperPrimary.copy(alpha = 0.18f), Color.White))),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(82.dp).clip(CircleShape).background(ItaSuperPrimary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = ItaSuperPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(38.dp))
            Text(
                text = "Acompanhe seu pedido",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 27.sp,
                    color = ItaSuperTextPrimary
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Receba avisos quando a loja aceitar, preparar e enviar seu pedido para entrega.",
                style = MaterialTheme.typography.bodyLarge.copy(color = ItaSuperTextSecondary, lineHeight = 23.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 340.dp)
            )
            Spacer(modifier = Modifier.height(46.dp))
            OutlinedButton(
                onClick = { complete(false) },
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("notification_onboarding_skip"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Pular por enquanto", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                enabled = !isRequesting,
                onClick = {
                    isRequesting = true
                    if (PlatformPermissions.hasNotificationPermission()) {
                        complete(true)
                    } else {
                        launchNotificationPermission()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(58.dp).testTag("notification_onboarding_allow"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
            ) {
                Text(
                    text = "Permitir notificações",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
