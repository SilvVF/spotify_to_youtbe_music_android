package io.silv.sp2yt

import java.security.MessageDigest

actual fun sha1(value: String): String {
    val md = MessageDigest.getInstance("SHA-1")
    val digest = md.digest(value.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}