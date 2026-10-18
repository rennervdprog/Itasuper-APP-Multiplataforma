package com.example.shared.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.theme.ItaSuperHighlightBg
import com.example.shared.theme.ItaSuperHighlightText
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.theme.ItaSuperSecondary

/**
 * ITEM 4/6 — Bottom sheet de suporte/ajuda.
 *
 * Migração 1:1 de `com.example.ui.home.SupportBottomSheet` (app Android,
 * Itasuper-APP-NATIVO). Estrutura, textos, ordem, espaçamentos (dp), tamanhos
 * de fonte (sp) e cores reproduzidos literalmente do original.
 *
 * Adaptações KMP documentadas:
 * - `android.content.Intent`/`android.net.Uri` não existem no commonMain: os
 *   intents de WhatsApp (`ACTION_VIEW` no wa.me) e de ligação (`ACTION_DIAL`)
 *   viraram stubs [openSupportWhatsApp]/[dialSupportCenter] com
 *   TODO(plataforma) — navegação externa via expect/actual na fase de
 *   plataforma.
 * - `LocalContext.current` removido (não existe no commonMain).
 * - A opção "Perguntas Frequentes (FAQ)" no original só fecha o sheet
 *   (sem link/tela); comportamento preservado.
 * - No app original o gatilho (`HomeViewModel.openSupportSheet`) não tinha
 *   chamador na UI; no KMP o sheet é aberto pelo menu Perfil
 *   ("Ajuda e suporte" → "Falar com o suporte" e a ação rápida "Suporte").
 */

/** Link do WhatsApp oficial do suporte (idêntico ao original). */
private const val SUPPORT_WHATSAPP_URL =
    "https://wa.me/5522992796291?text=Olá,%20preciso%20de%20ajuda%20no%20ItaSuper"

/** URI de discagem da Central ItaSuper (idêntico ao original). */
private const val SUPPORT_PHONE_URI = "tel:08007778899"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportBottomSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // TODO(plataforma): abrir o WhatsApp externo em SUPPORT_WHATSAPP_URL via
    // navegação externa da plataforma (expect/actual).
    fun openSupportWhatsApp() {
        // stub — nada a fazer no commonMain
    }

    // TODO(plataforma): discar para SUPPORT_PHONE_URI via navegação externa da
    // plataforma (expect/actual).
    fun dialSupportCenter() {
        // stub — nada a fazer no commonMain
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = ItaSuperHighlightBg
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.SupportAgent,
                            contentDescription = null,
                            tint = ItaSuperPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Suporte ItaSuper",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Como podemos te ajudar hoje?",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SupportOptionCard(
                icon = Icons.Filled.Chat,
                title = "Falar pelo WhatsApp",
                subtitle = "Atendimento oficial (21) 97123-4567 • 08h às 22h",
                iconColor = Color(0xFF25D366),
                onClick = {
                    openSupportWhatsApp()
                    onDismiss()
                }
            )

            SupportOptionCard(
                icon = Icons.Filled.Phone,
                title = "Ligar para Central ItaSuper",
                subtitle = "0800 777 8899 (Gratuito)",
                iconColor = ItaSuperPrimary,
                onClick = {
                    dialSupportCenter()
                    onDismiss()
                }
            )

            SupportOptionCard(
                icon = Icons.Filled.HelpOutline,
                title = "Perguntas Frequentes (FAQ)",
                subtitle = "Tire dúvidas sobre entregas, pagamentos e trocas",
                iconColor = ItaSuperHighlightText,
                onClick = {
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun SupportOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ItaSuperSecondary
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
