package com.example.itasuper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.shared.theme.ItaSuperTheme
import com.example.shared.ui.home.HomeScreen
import com.example.shared.ui.mock.findMockStore
import com.example.shared.ui.notifications.NotificationsScreen
import com.example.shared.ui.orders.OrdersScreen
import com.example.shared.ui.profile.ProfileScreen
import com.example.shared.ui.navigation.AppScreen
import com.example.shared.ui.search.SearchScreen
import com.example.shared.ui.store.StoreInfoScreen
import com.example.shared.ui.navigation.ItaSuperBottomNavBar
import com.example.shared.ui.navigation.rememberAppNavigator

/**
 * Ponto de entrada compartilhado do app.
 * Fase 3a: navegação por estado + HomeScreen (dados mockados).
 */
@Composable
fun App() {
    val navigator = rememberAppNavigator()
    ItaSuperTheme {
        val storeDetailId = navigator.storeDetailId
        Scaffold(
            bottomBar = {
                // Detalhe da loja ocupa a tela toda (tem botão voltar próprio)
                if (storeDetailId == null) {
                    ItaSuperBottomNavBar(
                        currentScreen = navigator.currentScreen,
                        onNavigate = { navigator.navigateTo(it) }
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (storeDetailId != null) {
                    StoreInfoScreen(
                        store = findMockStore(storeDetailId),
                        onBackClick = { navigator.closeStore() }
                    )
                } else {
                    when (navigator.currentScreen) {
                        AppScreen.HOME -> HomeScreen(
                            onSearchClick = { navigator.navigateTo(AppScreen.SEARCH) },
                            onStoreClick = { navigator.openStore(it) }
                        )
                        AppScreen.SEARCH -> SearchScreen(
                            onStoreClick = { navigator.openStore(it) }
                        )
                        AppScreen.ORDERS -> OrdersScreen(
                            onExploreClick = { navigator.navigateTo(AppScreen.HOME) }
                        )
                        AppScreen.NOTIFICATIONS -> NotificationsScreen()
                        AppScreen.PROFILE -> ProfileScreen()
                        else -> PlaceholderScreen(navigator.currentScreen.title)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title)
            Text(text = "Em breve nesta fase de migração")
        }
    }
}
