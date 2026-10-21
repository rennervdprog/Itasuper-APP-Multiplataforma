package com.example.shared.platform

import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Android: folha de compartilhamento via Intent ACTION_SEND com chooser
 * (igual ao ProfileScreen do app original).
 */
@Composable
actual fun rememberTextSharer(): (text: String, title: String?) -> Boolean {
    val context = LocalContext.current
    return remember(context) {
        { text: String, title: String? ->
            runCatching {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                val chooser = Intent.createChooser(share, title ?: "Compartilhar").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                true
            }.getOrDefault(false)
        }
    }
}

/**
 * Android: Intent ACTION_VIEW (igual ao app original nos botões de
 * WhatsApp e mapa da loja).
 */
@Composable
actual fun rememberUrlOpener(): (url: String) -> Boolean {
    val context = LocalContext.current
    return remember(context) {
        { url: String ->
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }
    }
}

/**
 * Android: ActivityResultContracts.OpenDocument (igual ao app original).
 * Lê os bytes via ContentResolver; extensão derivada do MIME
 * (jpeg→jpg, png→png, pdf→pdf).
 */
@Composable
actual fun rememberFilePicker(
    mimeTypes: List<String>,
    onPicked: (PickedFile?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val callbackState = rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            callbackState.value(null)
            return@rememberLauncherForActivityResult
        }
        val mimeType = context.contentResolver.getType(uri).orEmpty()
            .ifBlank { "application/octet-stream" }
        val extension = when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "application/pdf" -> "pdf"
            else -> MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: ""
        }
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        callbackState.value(
            bytes?.let { PickedFile(bytes = it, mimeType = mimeType, extension = extension) }
        )
    }
    val typesState = rememberUpdatedState(mimeTypes)
    return remember(launcher) {
        { launcher.launch(typesState.value.toTypedArray()) }
    }
}
