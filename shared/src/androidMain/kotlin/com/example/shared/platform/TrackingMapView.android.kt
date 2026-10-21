package com.example.shared.platform

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Android: WebView + Leaflet, igual ao app original — a WebView é criada
 * uma vez e a posição do entregador é atualizada via evaluateJavascript
 * (window.updateDriver), sem recarregar o mapa.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun TrackingMapView(
    html: String,
    driverLatitude: Double?,
    driverLongitude: Double?,
    modifier: Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                webViewClient = WebViewClient()
                loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            // Atualiza a posição do entregador sem recriar a WebView.
            if (driverLatitude != null && driverLongitude != null) {
                webView.evaluateJavascript(
                    "window.updateDriver($driverLatitude, $driverLongitude);",
                    null
                )
            }
        },
        onRelease = { webView -> webView.destroy() }
    )
}
