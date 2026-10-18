package com.example.shared.ui.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.shared.theme.ItaSuperHighlightBg
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.theme.ItaSuperTextPrimary
import com.example.shared.theme.ItaSuperTextSecondary

/**
 * Migração 1:1 de `Itasuper-APP-NATIVO/.../ui/permissions/LocationAndPermissionsDialog.kt`.
 *
 * Modal contextual para ações que exigem localização. Estrutura, textos, ordem,
 * espaçamentos (dp) e cores reproduzidos literalmente do original. Adaptações KMP
 * documentadas:
 *
 * - `PermissionUtils.hasLocationPermission(context)` virou
 *   [PlatformPermissions.hasLocationPermission] (expect/actual; o contexto da
 *   aplicação é registrado em `MainActivity.onCreate`).
 * - `rememberLauncherForActivityResult(RequestMultiplePermissions)` virou
 *   [rememberLocationPermissionLauncher] (expect/actual: Android usa a
 *   ActivityResult API real; iOS é stub que trata como negado).
 * - Ícones via `Icons.Filled.*` (material-icons-extended, já declarado no shared).
 */
@Composable
fun LocationAndPermissionsDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onLocationPermissionResult: (locationGranted: Boolean) -> Unit
) {
    if (!showDialog) return

    val launchLocationPermission = rememberLocationPermissionLauncher { granted ->
        onLocationPermissionResult(granted)
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("location_permission_dialog"),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ItaSuperHighlightBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = ItaSuperPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Permitir localização",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ItaSuperTextPrimary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Para encontrar lojas que atendem sua região e calcular a entrega corretamente, permita o acesso à sua localização.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ItaSuperTextSecondary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { launchLocationPermission() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("allow_location_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ItaSuperPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Permitir localização", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        onLocationPermissionResult(false)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("skip_location_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Agora não",
                        color = ItaSuperTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
