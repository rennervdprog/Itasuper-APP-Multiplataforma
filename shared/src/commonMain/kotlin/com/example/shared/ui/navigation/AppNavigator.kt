package com.example.shared.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Telas principais do app (espelha BottomNavItem do app Android).
 * Navegação manual por estado — simples e 100% multiplatform,
 * sem dependência de androidx.navigation.
 */
enum class AppScreen(val route: String, val title: String) {
    HOME("home", "Início"),
    SEARCH("busca", "Descobrir"),
    ORDERS("pedidos", "Pedidos"),
    NOTIFICATIONS("notificacoes", "Avisos"),
    PROFILE("perfil", "Perfil")
}

/**
 * Navegador simples observável. Uso:
 *   val navigator = rememberAppNavigator()
 *   navigator.navigateTo(AppScreen.SEARCH)
 */
class AppNavigator {
    var currentScreen by mutableStateOf(AppScreen.HOME)
        private set

    /** Id da loja aberta no detalhe ("Sobre a loja"); null = nenhuma aberta. */
    var storeDetailId by mutableStateOf<String?>(null)
        private set

    fun navigateTo(screen: AppScreen) {
        currentScreen = screen
    }

    fun openStore(storeId: String) {
        storeDetailId = storeId
    }

    fun closeStore() {
        storeDetailId = null
    }
}

@Composable
fun rememberAppNavigator(): AppNavigator = remember { AppNavigator() }
