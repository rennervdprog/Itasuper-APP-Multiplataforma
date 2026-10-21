package com.example.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Mapa do rastreio da entrega — expect/actual.
 *
 * O app original usa WebView + Leaflet (OpenStreetMap): o HTML é gerado
 * uma única vez e a posição do entregador é atualizada via
 * `window.updateDriver(lat, lng)` sem recarregar a página.
 *
 * Android: AndroidView com WebView (idêntico ao original).
 * iOS: WKWebView com o mesmo HTML/JS.
 *
 * [html] é a página completa já montada (ver trackingMapHtml na tela de
 * rastreio). [driverLatitude]/[driverLongitude] mudam a cada polling e
 * disparam a atualização do pino do entregador.
 */
@Composable
expect fun TrackingMapView(
    html: String,
    driverLatitude: Double?,
    driverLongitude: Double?,
    modifier: Modifier = Modifier
)
