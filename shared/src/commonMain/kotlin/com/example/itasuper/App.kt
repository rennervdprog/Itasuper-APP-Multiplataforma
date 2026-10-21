package com.example.itasuper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.shared.model.Store
import com.example.shared.network.SupabaseApi
import com.example.shared.platform.SecureStorage
import com.example.shared.session.UserSessionRepository
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.theme.ItaSuperTheme
import com.example.shared.ui.auth.AuthScreen
import com.example.shared.ui.cart.CartScreen
import com.example.shared.ui.cart.CheckoutScreen
import com.example.shared.ui.cart.OrderSuccessScreen
import com.example.shared.ui.cart.rememberCartState
import com.example.shared.ui.home.HomeScreen
import com.example.shared.ui.legal.LegalConsentGate
import com.example.shared.ui.legal.rememberLegalConsentState
import com.example.shared.ui.notifications.NotificationsScreen
import com.example.shared.ui.orders.DeliveryTrackingScreen
import com.example.shared.ui.orders.OrdersScreen
import com.example.shared.ui.permissions.LocationOnboardingPreferences
import com.example.shared.ui.permissions.LocationPermissionOnboarding
import com.example.shared.ui.permissions.NotificationOnboardingPreferences
import com.example.shared.ui.permissions.NotificationPermissionOnboarding
import com.example.shared.ui.profile.ProfileScreen
import com.example.shared.ui.navigation.AppScreen
import com.example.shared.ui.search.SearchScreen
import com.example.shared.ui.store.StoreDetailScreen
import com.example.shared.ui.store.StoreInfoScreen
import com.example.shared.ui.navigation.ItaSuperBottomNavBar
import com.example.shared.ui.navigation.rememberAppNavigator

/**
 * Ponto de entrada compartilhado do app.
 * Navegação por estado + sessão real do cliente (UserSessionRepository):
 * sem sessão, mostra a AuthScreen em tela cheia; com sessão, o app abre na
 * Home e o gate legal verifica aceites pendentes com a sessão real.
 * Carrinho + checkout + confirmação compartilham o estado em CartState.
 * Rastreio: DeliveryTrackingScreen ("Acompanhar entrega") abre em tela cheia
 * via navigator.trackingOrderId, a partir do botão no card do pedido em andamento.
 */
@Composable
fun App() {
    val navigator = rememberAppNavigator()
    // Estado do carrinho vive no topo: Home (badge), Cart, Checkout e
    // confirmação compartilham a mesma instância.
    val cartState = rememberCartState()
    // Sessão real do cliente (UserSessionRepository): restaurada do storage na
    // abertura e sincronizada com o perfil remoto, como no app original.
    // A restauração roda ANTES do primeiro frame decidir a tela — sem isso, a
    // abertura piscava a tela de login e só depois "percebia" o login salvo.
    remember {
        // Fase 4 (C2) do app original: quando a API retornar 401 (token
        // expirado), a camada de rede renova a sessão e repete a chamada 1x.
        // Sem essa ligação, Pedidos e Avisos paravam de atualizar ~1h depois
        // do login, porque o token vencia e ninguém renovava.
        SupabaseApi.onUnauthorized = { UserSessionRepository.refreshAccessToken() }
        UserSessionRepository.initialize()
        // Equivalente ao refreshSession do onResume do original, na abertura:
        // renova o token restaurado (que pode ter vencido com o app fechado).
        if (UserSessionRepository.current.isLoggedIn) {
            UserSessionRepository.refreshSession()
        }
    }
    val session by UserSessionRepository.session.collectAsState()
    val isLoggedIn = session.isLoggedIn
    // ITEM 5/6 — gate de aceite dos Termos/Privacidade (espelha o MainActivity do
    // app Android): após o login, verifica mudanças legais pendentes no backend e
    // prende a tela quando o aceite é obrigatório (modo binding).
    val legalConsentState = rememberLegalConsentState()
    val legalSession = if (isLoggedIn) session else null
    LaunchedEffect(legalSession) {
        val session = legalSession ?: return@LaunchedEffect
        legalConsentState.checkForUpdates(session)
    }
    ItaSuperTheme {
        if (!session.isSessionRestored) {
            // Splash neutra enquanto a sessão local é restaurada (fundo branco,
            // igual à splash do app original — nada de tela de login piscando).
            Box(
                modifier = Modifier.fillMaxSize().background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ItaSuperPrimary)
            }
        } else if (!isLoggedIn) {
            AuthScreen(
                onAuthSuccess = {
                    navigator.navigateTo(AppScreen.HOME)
                }
            )
        } else {
            // ITEM 6/6 — onboarding de permissões (espelha o HomeScreen do app
            // Android): após o login, mostra em tela cheia o pedido de localização
            // e depois o de notificações, somente para quem ainda não decidiu
            // (conclusão persistida por usuário via SecureStorage).
            val permissionStorage = remember { SecureStorage() }
            val permissionUserId = session.userId
            var showLocationOnboarding by remember(isLoggedIn) {
                mutableStateOf(
                    isLoggedIn && LocationOnboardingPreferences.shouldShow(
                        permissionStorage,
                        permissionUserId
                    )
                )
            }
            var showNotificationOnboarding by remember(isLoggedIn) {
                mutableStateOf(
                    isLoggedIn && NotificationOnboardingPreferences.shouldShow(
                        permissionStorage,
                        permissionUserId
                    )
                )
            }
            if (showLocationOnboarding && permissionUserId.isNotBlank()) {
                LocationPermissionOnboarding(
                    userId = permissionUserId,
                    onFinished = { showLocationOnboarding = false }
                )
            } else if (showNotificationOnboarding && permissionUserId.isNotBlank()) {
                NotificationPermissionOnboarding(
                    userId = permissionUserId,
                    onFinished = { showNotificationOnboarding = false }
                )
            } else {
                val placedOrder = cartState.placedOrder
                if (placedOrder != null) {
                    // Tela de confirmação ocupa a tela toda (substitui a navegação).
                    OrderSuccessScreen(
                        order = placedOrder,
                        cart = cartState,
                        onTrackOrders = {
                            cartState.dismissSuccess()
                            navigator.navigateTo(AppScreen.ORDERS)
                        },
                        onBackHome = {
                            cartState.dismissSuccess()
                            navigator.navigateTo(AppScreen.HOME)
                        }
                    )
                } else {
                    val storeDetailId = navigator.storeDetailId
                    val storeInfoId = navigator.storeInfoId
                    val trackingOrderId = navigator.trackingOrderId
                    val fullScreenCartFlow = navigator.currentScreen == AppScreen.CART ||
                            navigator.currentScreen == AppScreen.CHECKOUT
                    Scaffold(
                        bottomBar = {
                            // Vitrine, "Sobre a loja", rastreio e fluxo do carrinho ocupam a tela toda
                            // (têm botão voltar próprio).
                            if (storeDetailId == null && storeInfoId == null && trackingOrderId == null && !fullScreenCartFlow) {
                                ItaSuperBottomNavBar(
                                    currentScreen = navigator.currentScreen,
                                    onNavigate = { navigator.navigateTo(it) }
                                )
                            }
                        }
                    ) { padding ->
                        // A vitrine da loja desenha a capa ate o topo da tela
                        // (atras da status bar), igual ao app original — a faixa
                        // branca acima da capa era o padding do topo deste
                        // Scaffold. Pra vitrine, o padding do topo nao se aplica;
                        // as demais telas seguem com o padding normal.
                        val rendersStoreDetail = trackingOrderId == null &&
                                storeInfoId == null && storeDetailId != null
                        val contentModifier = if (rendersStoreDetail) {
                            // calculateStartPadding/EndPadding nao existem no
                            // Compose Multiplatform — equivalente via Left/Right
                            // (o app e LTR; em RTL, start/end trocam de lado).
                            val layoutDirection = LocalLayoutDirection.current
                            val isRtl = layoutDirection == LayoutDirection.Rtl
                            val startPad = if (isRtl) {
                                padding.calculateRightPadding(layoutDirection)
                            } else {
                                padding.calculateLeftPadding(layoutDirection)
                            }
                            val endPad = if (isRtl) {
                                padding.calculateLeftPadding(layoutDirection)
                            } else {
                                padding.calculateRightPadding(layoutDirection)
                            }
                            Modifier.fillMaxSize().padding(
                                start = startPad,
                                end = endPad,
                                bottom = padding.calculateBottomPadding(),
                                top = 0.dp
                            )
                        } else {
                            Modifier.fillMaxSize().padding(padding)
                        }
                        Box(modifier = contentModifier) {
                            if (trackingOrderId != null) {
                                // Rastreio da entrega (a partir do pedido em andamento).
                                DeliveryTrackingScreen(
                                    orderId = trackingOrderId,
                                    onBack = { navigator.closeTracking() }
                                )
                            } else if (storeInfoId != null) {
                                // "Sobre a loja" abre por cima da vitrine (loja real via API).
                                var infoStore by remember(storeInfoId) { mutableStateOf<Store?>(null) }
                                LaunchedEffect(storeInfoId) {
                                    infoStore = SupabaseApi.fetchStoreById(storeInfoId)
                                }
                                val loadedInfoStore = infoStore
                                if (loadedInfoStore != null) {
                                    StoreInfoScreen(
                                        store = loadedInfoStore,
                                        onBackClick = { navigator.closeStoreInfo() }
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            } else if (storeDetailId != null) {
                                // Vitrine da loja (cardápio + produtos).
                                StoreDetailScreen(
                                    storeId = storeDetailId,
                                    initialProductId = navigator.storeDetailProductId,
                                    cart = cartState,
                                    onBackClick = { navigator.closeStore() },
                                    onNavigateToCart = {
                                        // Fecha a vitrine antes de abrir o carrinho.
                                        // Sem o closeStore(), o storeDetailId continua setado e o
                                        // if de renderizacao prioriza a vitrine: a tela do carrinho
                                        // nunca aparece (bug "toca em Ver sacola e nada acontece").
                                        navigator.closeStore()
                                        navigator.navigateTo(AppScreen.CART)
                                    },
                                    onNavigateToInfo = { navigator.openStoreInfo(storeDetailId) }
                                )
                            } else {
                                when (navigator.currentScreen) {
                                    AppScreen.HOME -> HomeScreen(
                                        onSearchClick = { navigator.navigateTo(AppScreen.SEARCH) },
                                        onStoreClick = { navigator.openStore(it) },
                                        onProductClick = { p -> navigator.openStore(p.storeId, p.id) },
                                        onCartClick = { navigator.navigateTo(AppScreen.CART) },
                                        cartItemCount = cartState.totalItemCount,
                                        cart = cartState
                                    )
                                    AppScreen.SEARCH -> SearchScreen(
                                        onStoreClick = { navigator.openStore(it) },
                                        onProductClick = { p -> navigator.openStore(p.storeId, p.id) },
                                        cart = cartState
                                    )
                                    AppScreen.ORDERS -> OrdersScreen(
                                        onExploreClick = { navigator.navigateTo(AppScreen.HOME) },
                                        onTrackDelivery = { navigator.openTracking(it) },
                                        cart = cartState,
                                        onNavigateToCart = { navigator.navigateTo(AppScreen.CART) }
                                    )
                                    AppScreen.NOTIFICATIONS -> NotificationsScreen(
                                        onOpenOrder = { navigator.navigateTo(AppScreen.ORDERS) }
                                    )
                                    AppScreen.PROFILE -> ProfileScreen(
                                        onLogout = { UserSessionRepository.logout() },
                                        onNavigateToOrders = { navigator.navigateTo(AppScreen.ORDERS) }
                                    )
                                    AppScreen.AUTH -> AuthScreen(
                                        onAuthSuccess = {
                                            navigator.navigateTo(AppScreen.HOME)
                                        }
                                    )
                                    AppScreen.CART -> CartScreen(
                                        cart = cartState,
                                        onNavigateBack = { navigator.navigateTo(AppScreen.HOME) },
                                        onNavigateToCheckout = { navigator.navigateTo(AppScreen.CHECKOUT) },
                                        onNavigateToHome = { navigator.navigateTo(AppScreen.HOME) }
                                    )
                                    AppScreen.CHECKOUT -> CheckoutScreen(
                                        cart = cartState,
                                        onNavigateBack = { navigator.navigateTo(AppScreen.CART) }
                                    )
                                }
                            }
                        }
                    }
                }
                }
            // ITEM 5/6 — overlay do gate legal (só renderiza quando há aceite
            // pendente; espelha o LegalConsentGate do MainActivity do Android).
            val gateSession = legalSession
            if (gateSession != null) {
                LegalConsentGate(
                    session = gateSession,
                    state = legalConsentState,
                    onNavigateToProfile = {
                        legalConsentState.setRefusalPanelVisible(false)
                        navigator.navigateTo(AppScreen.PROFILE)
                    }
                )
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
