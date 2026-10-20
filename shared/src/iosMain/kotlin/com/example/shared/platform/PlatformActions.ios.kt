package com.example.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeJPEG
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.UniformTypeIdentifiers.UTTypePNG
import platform.darwin.NSObject

/** ViewController mais ao topo (para apresentar folhas/pickers no iOS). */
@OptIn(ExperimentalForeignApi::class)
private fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}

/**
 * iOS: UIActivityViewController com o texto (equivalente à folha de
 * compartilhamento do app original).
 */
@Composable
actual fun rememberTextSharer(): (text: String, title: String?) -> Boolean {
    return remember {
        { text: String, _: String? ->
            val presenter = topViewController()
            if (presenter == null) {
                false
            } else {
                val activity = UIActivityViewController(
                    activityItems = listOf(text),
                    applicationActivities = null
                )
                presenter.presentViewController(activity, animated = true, completion = null)
                true
            }
        }
    }
}

private class DocumentPickerDelegate(
    private val onPicked: (PickedFile?) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? platform.Foundation.NSURL
        if (url == null) {
            onPicked(null)
            return
        }
        val accessing = url.startAccessingSecurityScopedResource()
        try {
            val data = NSData.dataWithContentsOfURL(url)
            if (data == null) {
                onPicked(null)
                return
            }
            val bytes = ByteArray(data.length.toInt())
            if (bytes.isNotEmpty()) {
                bytes.usePinned { pinned ->
                    platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length)
                }
            }
            val pathExtension = url.pathExtension?.lowercase().orEmpty()
            val mimeType = when (pathExtension) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "pdf" -> "application/pdf"
                else -> "application/octet-stream"
            }
            val extension = when (pathExtension) {
                "jpeg" -> "jpg"
                else -> pathExtension
            }
            onPicked(PickedFile(bytes = bytes, mimeType = mimeType, extension = extension))
        } finally {
            if (accessing) url.stopAccessingSecurityScopedResource()
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onPicked(null)
    }
}

/**
 * iOS: UIDocumentPickerViewController restrito aos tipos pedidos
 * (jpeg/png/pdf, como no app original).
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberFilePicker(
    mimeTypes: List<String>,
    onPicked: (PickedFile?) -> Unit
): () -> Unit {
    val callbackState = rememberUpdatedState(onPicked)
    val delegate = remember {
        DocumentPickerDelegate { picked -> callbackState.value(picked) }
    }
    val typesState = rememberUpdatedState(mimeTypes)
    return remember {
        {
            val types: List<UTType> = typesState.value.mapNotNull { mime ->
                when (mime) {
                    "image/jpeg" -> UTTypeJPEG
                    "image/png" -> UTTypePNG
                    "application/pdf" -> UTTypePDF
                    else -> UTType.typeWithMIMEType(mime)
                }
            }
            val picker = UIDocumentPickerViewController(forOpeningContentTypes = types)
            picker.delegate = delegate
            picker.allowsMultipleSelection = false
            topViewController()?.presentViewController(picker, animated = true, completion = null)
        }
    }
}
