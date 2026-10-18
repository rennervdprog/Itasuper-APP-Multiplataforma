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
    PROFILE("perfil", "Perfil"),
    // Fase 3d: tela de login/cadastro. Não entra na bottom bar (NAV_ITEMS),
    // é exibida em tela cheia pelo App quando não há sessão ativa.
    AUTH("auth", "Entrar"),
    // Fase 3e: carrinho e checkout. Não entram na bottom bar (NAV_ITEMS);
    // ocupam a tela toda com botão voltar próprio.
    CART("carrinho", "Carrinho"),
    CHECKOUT("checkout", "Checkout")
}

/**
 * Navegador simples observável. Uso:
 *   val navigator = rememberAppNavigator()
 *   navigator.navigateTo(AppScreen.SEARCH)
 */
class AppNavigator {
    var currentScreen by mutableStateOf(AppScreen.HOME)
        private set

    /** Id da loja aberta na vitrine; null = nenhuma aberta. */
    var storeDetailId by mutableStateOf<String?>(null)
        private set

    /** Id da loja aberta no "Sobre a loja" (a partir da vitrine); null = fechado. */
    var storeInfoId by mutableStateOf<String?>(null)
        private set

    /** Id do pedido aberto no rastreio ("Acompanhar entrega"); null = nenhum aberto. */
    var trackingOrderId by mutableStateOf<String?>(null)
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

    fun openStoreInfo(storeId: String) {
        storeInfoId = storeId
    }

    fun closeStoreInfo() {
        storeInfoId = null
    }

    fun openTracking(orderId: String) {
        trackingOrderId = orderId
    }

    fun closeTracking() {
        trackingOrderId = null
    }
}

@Composable
fun rememberAppNavigator(): AppNavigator = remember { AppNavigator() }
