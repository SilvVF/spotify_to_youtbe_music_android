package io.silv

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneOffset

fun epochSeconds() = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)

fun String.removeHtmlTags(): String {
    val regex = Regex("<[^>]+>")
    var result = this
    val tags = regex.findAll(this).map { it.value }
    for (tag in tags) {
        result = result.replace(tag, "")
    }
    return result
}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

fun sha1(str: String): String = MessageDigest.getInstance("SHA-1").digest(str.toByteArray()).toHex()

fun parseCookieString(cookie: String): Map<String, String> =
    cookie.split("; ")
        .filter { it.isNotEmpty() }
        .associate {
            val (key, value) = it.split("=")
            key to value
        }

inline fun <T> Mutex.withLock(owner: Any? = null, crossinline action: () -> T): T = runBlocking {
    lock(owner)
    try {
        return@runBlocking action()
    } finally {
        unlock(owner)
    }
}

inline fun <T> MutableList<T>.addAll(vararg values: T) {
    for (value in values) add(value)
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend inline fun <T> Iterable<T>.pForEach(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    parallelism: Int = 3,
    crossinline action: suspend (Int, T) -> Unit
) {
    withContext(dispatcher.limitedParallelism(parallelism)) {
        this@pForEach.mapIndexed { i, v ->
            async { action(i, v) }
        }
            .joinAll()
    }
}

fun String.parseTime(): Int? {
    try {
        val parts = split(":").map { it.toInt() }
        if (parts.size == 2) {
            return parts[0] * 60 + parts[1]
        }
        if (parts.size == 3) {
            return parts[0] * 3600 + parts[1] * 60 + parts[2]
        }
    } catch (e: Exception) {
        return null
    }
    return null
}

fun <T> Result<T>.logError(tag: String) = onFailure { Log.e(tag, it.message, it) }

fun JSONObject.toMap(): Map<String, Any?> = keys().asSequence().associateWith { key ->
    when (val value = this[key])
    {
        is JSONArray ->
        {
            val map = (0 until value.length()).associate { Pair(it.toString(), value[it]) }
            JSONObject(map).toMap().values.toList()
        }
        is JSONObject -> value.toMap()
        JSONObject.NULL -> null
        else            -> value
    }
}

inline fun <reified T> Map<String, *>.getAs(key: String): T? = get(key) as? T