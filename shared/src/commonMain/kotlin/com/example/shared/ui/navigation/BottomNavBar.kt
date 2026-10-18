package com.example.shared.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import shared.generated.resources.Res
import shared.generated.resources.ic_ita_bell
import shared.generated.resources.ic_ita_home
import shared.generated.resources.ic_ita_profile
import shared.generated.resources.ic_ita_receipt
import shared.generated.resources.ic_ita_search
import com.example.shared.theme.ItaSuperBackground
import com.example.shared.theme.ItaSuperPrimary
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Barra de navegação inferior (migração do ItaSuperBottomNavBar do Android).
 * Usa os mesmos drawables do app original (ic_ita_*), via composeResources.
 */
private data class NavItem(
    val screen: AppScreen,
    val icon: DrawableResource
)

private val NAV_ITEMS = listOf(
    NavItem(AppScreen.HOME, Res.drawable.ic_ita_home),
    NavItem(AppScreen.SEARCH, Res.drawable.ic_ita_search),
    NavItem(AppScreen.ORDERS, Res.drawable.ic_ita_receipt),
    NavItem(AppScreen.NOTIFICATIONS, Res.drawable.ic_ita_bell),
    NavItem(AppScreen.PROFILE, Res.drawable.ic_ita_profile)
)

@Composable
fun ItaSuperBottomNavBar(
    currentScreen: AppScreen,
    unreadNotificationsCount: Int = 0,
    onNavigate: (AppScreen) -> Unit
) {
    Surface(
        // Com edge-to-edge ativo, a navigation bar do sistema e transparente:
        // este padding impede que a barra do app fique sob os botoes do sistema.
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        color = ItaSuperBackground,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NAV_ITEMS.forEach { item ->
                val isSelected = currentScreen == item.screen
                val tint = if (isSelected) ItaSuperPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(item.screen) }
                ) {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = item.screen.title,
                        tint = tint,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = item.screen.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = tint
                    )
                }
            }
        }
    }
}
