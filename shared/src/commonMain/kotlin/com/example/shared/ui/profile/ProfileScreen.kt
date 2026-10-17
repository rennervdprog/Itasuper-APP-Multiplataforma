package com.example.shared.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.model.UserSession
import com.example.shared.theme.ItaSuperHighlightBg
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.ui.mock.mockUserSession

// TODO(fase-rede): trocar mockUserSession pela sessão real (UserSessionRepository).

private data class ProfileMenuItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String = ""
)

@Composable
fun ProfileScreen(onLogout: () -> Unit = {}) {
    val session = remember { mockUserSession() }
    val showLogoutConfirmation = remember { mutableStateOf(false) }

    if (showLogoutConfirmation.value) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation.value = false },
            title = { Text("Sair da conta?") },
            text = { Text("Você precisará fazer login novamente para pedir.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirmation.value = false
                    onLogout()
                }) { Text("Sair", color = Color(0xFFD32F2F)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation.value = false }) { Text("Cancelar") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA)),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            // Cabeçalho do perfil
            Column(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(84.dp).clip(CircleShape).background(ItaSuperHighlightBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        session.name.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = ItaSuperPrimary
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(session.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(4.dp))
                Text(session.email, color = Color(0xFF737373), fontSize = 14.sp)
            }
            Spacer(Modifier.height(12.dp))
        }

        val menuItems = listOf(
            ProfileMenuItem(Icons.Default.Person, "Meus dados", "Nome, CPF, WhatsApp"),
            ProfileMenuItem(Icons.Default.LocationOn, "Meus endereços", "Entrega e retirada"),
            ProfileMenuItem(Icons.Default.ShoppingBag, "Carteira", "Saldo e extrato"),
            ProfileMenuItem(Icons.Default.HelpOutline, "Ajuda", "Fale conosco")
        )
        items(menuItems.size) { index ->
            val item = menuItems[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .clickable { /* TODO(fase-rede): navegar para a seção */ }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(ItaSuperHighlightBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, null, tint = ItaSuperPrimary, modifier = Modifier.size(24.dp))
                }
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    if (item.subtitle.isNotBlank()) {
                        Text(item.subtitle, color = Color(0xFF737373), fontSize = 13.sp)
                    }
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFBDBDBD))
            }
            HorizontalDivider(color = Color(0xFFF0F0F0))
        }

        item {
            Spacer(Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogoutConfirmation.value = true }
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Logout, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(24.dp))
                    Text(
                        "Sair da conta",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "ItaSuper v1.0.0",
                color = Color(0xFFBDBDBD),
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
    }
}
