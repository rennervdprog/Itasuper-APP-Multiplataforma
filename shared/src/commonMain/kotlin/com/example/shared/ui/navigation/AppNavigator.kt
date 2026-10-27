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

    /** Produto que a vitrine deve abrir ao entrar (vindo de um card de produto). */
    var storeDetailProductId by mutableStateOf<String?>(null)
        private set

    /** Id da loja aberta no "Sobre a loja" (a partir da vitrine); null = fechado. */
    var storeInfoId by mutableStateOf<String?>(null)
        private set

    /** Id do pedido aberto no rastreio ("Acompanhar entrega"); null = nenhum aberto. */
    var trackingOrderId by mutableStateOf<String?>(null)
        private set

    /**
     * Fase 4 (G7): últimos 4 dígitos do WhatsApp do pedido guest em
     * rastreio; null = rastreio do cliente logado (lista de pedidos).
     */
    var trackingGuestLast4 by mutableStateOf<String?>(null)
        private set

    /** Fase 4 (G7): formulário "Acompanhar pedido" (rastreio guest); false = fechado. */
    var guestTrackingFormOpen by mutableStateOf(false)
        private set

    fun navigateTo(screen: AppScreen) {
        currentScreen = screen
    }

    fun openStore(storeId: String, productId: String? = null) {
        storeDetailId = storeId
        storeDetailProductId = productId
    }

    fun closeStore() {
        storeDetailId = null
        storeDetailProductId = null
    }

    fun openStoreInfo(storeId: String) {
        storeInfoId = storeId
    }

    fun closeStoreInfo() {
        storeInfoId = null
    }

    fun openTracking(orderId: String) {
        trackingOrderId = orderId
        trackingGuestLast4 = null
    }

    fun openGuestTracking(orderId: String, last4: String) {
        guestTrackingFormOpen = false
        trackingOrderId = orderId
        trackingGuestLast4 = last4
    }

    fun openGuestTrackingForm() {
        guestTrackingFormOpen = true
    }

    fun closeGuestTrackingForm() {
        guestTrackingFormOpen = false
    }

    fun closeTracking() {
        trackingOrderId = null
        trackingGuestLast4 = null
    }
}

@Composable
fun rememberAppNavigator(): AppNavigator = remember { AppNavigator() }
