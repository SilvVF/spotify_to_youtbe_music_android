package io.silv

import android.util.Log
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneOffset

fun epochSeconds() = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)

inline fun <T> Mutex.withLock(owner: Any? = null, crossinline action: () -> T): T = runBlocking {
    lock(owner)
    try {
        return@runBlocking action()
    } finally {
        unlock(owner)
    }
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