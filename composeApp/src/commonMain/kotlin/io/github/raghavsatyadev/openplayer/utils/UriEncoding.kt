package io.github.raghavsatyadev.openplayer.utils

private const val URI_UNRESERVED = "_-!.~'()*"
private const val HEX = "0123456789ABCDEF"

/** Same output as android.net.Uri.encode(value): UTF-8 percent-encoding of everything but A-Z a-z 0-9 _-!.~'()*. */
fun encodeUriComponent(value: String): String {
    val out = StringBuilder(value.length)
    for (byte in value.encodeToByteArray()) {
        val c = byte.toInt() and 0xFF
        val ch = c.toChar()
        if (c < 0x80 && (ch.isLetterOrDigit() || ch in URI_UNRESERVED)) {
            out.append(ch)
        } else {
            out.append('%').append(HEX[c shr 4]).append(HEX[c and 0xF])
        }
    }
    return out.toString()
}
