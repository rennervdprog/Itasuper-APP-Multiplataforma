package com.example.shared.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.theme.ItaSuperBackground
import com.example.shared.theme.ItaSuperPrimary

/**
 * Barra de navegação inferior (migração do ItaSuperBottomNavBar do Android).
 * Usa ícones Material em vez dos drawables customizados (R.drawable) —
 * os ícones customizados virão numa fase futura via compose resources.
 */
private data class NavItem(
    val screen: AppScreen,
    val icon: ImageVector
)

private val NAV_ITEMS = listOf(
    NavItem(AppScreen.HOME, Icons.Filled.Home),
    NavItem(AppScreen.SEARCH, Icons.Filled.Search),
    NavItem(AppScreen.ORDERS, Icons.Filled.ShoppingCart),
    NavItem(AppScreen.NOTIFICATIONS, Icons.Filled.Notifications),
    NavItem(AppScreen.PROFILE, Icons.Filled.Person)
)

@Composable
fun ItaSuperBottomNavBar(
    currentScreen: AppScreen,
    unreadNotificationsCount: Int = 0,
    onNavigate: (AppScreen) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                        imageVector = item.icon,
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
