package io.silv

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.contracts.CallsInPlace
import kotlin.contracts.contract
import kotlin.coroutines.coroutineContext

fun epochSeconds() = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)

inline fun <T> Mutex.withLock(owner: Any? = null, crossinline action: () -> T): T = runBlocking {
    lock(owner)
    try {
        return@runBlocking action()
    } finally {
        unlock(owner)
    }
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