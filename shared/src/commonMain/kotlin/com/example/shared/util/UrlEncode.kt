package com.example.shared.util

/**
 * Percent-encode de um componente de URL (equivalente ao Uri.encode do
 * Android, indisponível no commonMain): mantém letras, dígitos e -_.~ e
 * codifica o restante como %XX dos bytes UTF-8.
 */
fun urlEncodeComponent(value: String): String {
    val bytes = value.encodeToByteArray()
    val sb = StringBuilder()
    for (b in bytes) {
        val c = (b.toInt() and 0xFF).toChar()
        val safe = (c in 'A'..'Z') || (c in 'a'..'z') || (c in '0'..'9') ||
            c == '-' || c == '_' || c == '.' || c == '~'
        if (safe) {
            sb.append(c)
        } else {
            sb.append('%')
            sb.append((b.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase())
        }
    }
    return sb.toString()
}
