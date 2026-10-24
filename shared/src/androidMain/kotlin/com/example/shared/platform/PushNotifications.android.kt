package com.example.shared.platform

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Push no Android — porte do app original (Firebase Cloud Messaging).
 *
 * O applicationId agora é o mesmo do app cliente oficial
 * (app.itasuper.cliente), que JÁ está cadastrado no projeto Firebase
 * itafood-c71a1 — por isso o Firebase continua sendo inicializado
 * manualmente com a configuração oficial, sem google-services.json.
 * A chave abaixo é a chave pública de cliente do Firebase (a mesma que
 * vai embutida em todo APK publicado) — não é segredo de servidor.
 */
actual object PushNotifications {

    @Volatile
    private var appContext: Context? = null

    actual fun init(appContext: Any?) {
        this.appContext = (appContext as? Context)?.applicationContext
        ensureFirebaseInitialized()
    }

    actual fun register() {
        ensureFirebaseInitialized()
        try {
            FirebaseMessaging.getInstance()
        } catch (_: Exception) {
            // Firebase indisponível: push desabilitado silenciosamente.
        }
    }

    private fun ensureFirebaseInitialized() {
        val ctx = appContext ?: return
        try {
            if (FirebaseApp.getApps(ctx).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId(FIREBASE_APP_ID)
                    .setApiKey(FIREBASE_API_KEY)
                    .setProjectId(FIREBASE_PROJECT_ID)
                    .setGcmSenderId(FIREBASE_SENDER_ID)
                    .setStorageBucket(FIREBASE_STORAGE_BUCKET)
                    .build()
                FirebaseApp.initializeApp(ctx, options)
            }
        } catch (_: Throwable) {
            // Sem Firebase, o restante do app segue normal.
        }
    }

    actual suspend fun getToken(): String? {
        ensureFirebaseInitialized()
        return suspendCancellableCoroutine { continuation ->
            try {
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        if (continuation.isActive) continuation.resume(token)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    private const val FIREBASE_APP_ID = "1:344752263518:android:ed2b88af5eb26878d37fcf"
    private const val FIREBASE_API_KEY = "AIzaSyCgfgPRr5bDKEvTTVPFL6dCkFJwXtguqak"
    private const val FIREBASE_PROJECT_ID = "itafood-c71a1"
    private const val FIREBASE_SENDER_ID = "344752263518"
    private const val FIREBASE_STORAGE_BUCKET = "itafood-c71a1.firebasestorage.app"
}
