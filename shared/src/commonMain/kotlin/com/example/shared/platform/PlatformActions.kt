package com.example.shared.platform

import androidx.compose.runtime.Composable

/** Arquivo escolhido no seletor nativo (comprovante do PIX Direto). */
class PickedFile(
    val bytes: ByteArray,
    val mimeType: String,
    val extension: String
)

/**
 * Folha de compartilhamento nativa (Intent ACTION_SEND no Android,
 * UIActivityViewController no iOS). Devolve o lançador:
 * (texto, título do chooser) → true se a folha abriu.
 */
@Composable
expect fun rememberTextSharer(): (text: String, title: String?) -> Boolean

/**
 * Abre uma URL externa no app próprio do sistema (Intent ACTION_VIEW no
 * Android, UIApplication.openURL no iOS) — usado no botão de WhatsApp da
 * loja (wa.me) e no "abrir no mapa" (geo:), iguais ao app original.
 * Devolve true se a abertura foi disparada.
 */
@Composable
expect fun rememberUrlOpener(): (url: String) -> Boolean

/**
 * Seletor de arquivo nativo (ActivityResultContracts.OpenDocument no
 * Android, UIDocumentPickerViewController no iOS). Devolve o lançador;
 * o resultado chega em [onPicked] (null = seleção cancelada).
 */
@Composable
expect fun rememberFilePicker(
    mimeTypes: List<String>,
    onPicked: (PickedFile?) -> Unit
): () -> Unit
