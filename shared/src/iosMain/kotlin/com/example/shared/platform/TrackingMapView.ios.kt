package com.example.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

/**
 * iOS: WKWebView com o mesmo HTML/JS Leaflet do Android/original.
 * A posição do entregador é reaplicada ao fim do carregamento da página
 * (um evaluateJavaScript antes do JS ficar pronto seria descartado).
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun TrackingMapView(
    html: String,
    driverLatitude: Double?,
    driverLongitude: Double?,
    modifier: Modifier
) {
    val latState by rememberUpdatedState(driverLatitude)
    val lngState by rememberUpdatedState(driverLongitude)

    val webView = remember {
        WKWebView(
            frame = CGRectZero.readValue(),
            configuration = WKWebViewConfiguration()
        )
    }
    val navDelegate = remember {
        object : NSObject(), WKNavigationDelegateProtocol {
            override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                val lat = latState
                val lng = lngState
                if (lat != null && lng != null) {
                    webView.evaluateJavaScript("window.updateDriver($lat, $lng);", null)
                }
            }
        }
    }

    LaunchedEffect(webView) {
        webView.navigationDelegate = navDelegate
        webView.loadHTMLString(html, baseURL = NSURL(string = "https://localhost/"))
    }
    LaunchedEffect(driverLatitude, driverLongitude) {
        val lat = driverLatitude
        val lng = driverLongitude
        if (lat != null && lng != null) {
            webView.evaluateJavaScript("window.updateDriver($lat, $lng);", null)
        }
    }

    // WKWebView é uma UIView (não um UIViewController): o interop correto
    // é o UIKitView — com UIKitViewController o build iOS quebrava
    // ("actual type is WKWebView, but UIViewController was expected").
    UIKitView(
        factory = { webView },
        modifier = modifier
    )
}
