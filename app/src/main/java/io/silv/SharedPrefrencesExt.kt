package io.silv

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.app.PendingIntentCompat.send
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.reflect.KProperty


interface Stored<out T> {
    val value: T
}

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> Stored<T>.getValue(thisObj: Any?, property: KProperty<*>): T = value

interface MutableStored<T> : Stored<T> {
    override var value: T
    operator fun component1(): T
    operator fun component2(): (T) -> Unit
}

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> MutableStored<T>.setValue(thisObj: Any?, property: KProperty<*>, value: T) {
    this.value = value
}

inline fun <reified T> SharedPreferences.stored(key: String) = object : MutableStored<T> {
    override var value: T
        get() = this@stored.get<T>(key)
        set(value) = this@stored.set(key, value)

    override fun component1(): T = value
    override fun component2(): (T) -> Unit = { v -> value = v }
}

inline fun <reified T> SharedPreferences.set(key: String, value: T) {
    edit(commit = true) {
        when(T::class) {
            String::class -> putString(key, value as String)
            Long::class -> putLong(key, value as Long)
            Int::class -> putInt(key, value as Int)
            Boolean::class -> putBoolean(key, value as Boolean)
            else -> error("Incompatible type")
        }
    }
}

inline fun <reified T> SharedPreferences.get(key: String): T {
    return when(T::class) {
        String::class -> getString(key, "")
        Long::class -> getLong(key, 0)
        Int::class -> getInt(key, 0)
        Boolean::class -> getBoolean(key, false)
        else -> error("Incompatible type")
    } as T
}

suspend inline fun <reified T> SharedPreferences.get(key: String, default: T): T {
    return runCatching {
        withContext(Dispatchers.IO) {
            when(T::class) {
                String::class -> getString(key, default as String)
                Long::class -> getLong(key, default as Long)
                Int::class -> getInt(key, default as Int)
                Boolean::class -> getBoolean(key, default as Boolean)
                else -> error("Incompatible type")
            } as T
        }
    }
        .onFailure { it.printStackTrace() }
        .getOrDefault(default)
}

inline fun <reified T> SharedPreferences.changesAsFlow(watchKey: String, default: T) = callbackFlow {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == watchKey) {
            runBlocking {
                prefs.get<T>(watchKey, default)?.let {
                    trySend(it)
                }
            }
        }
    }
    try {
        registerOnSharedPreferenceChangeListener(listener)
        awaitCancellation()
    } finally {
        unregisterOnSharedPreferenceChangeListener(listener)
    }
}

val LocalSharedPreferences = staticCompositionLocalOf<SharedPreferences> { error("SharedPreferences not provided in scope") }


@Composable
inline fun <reified T> producePreferenceAsState(key: String, default: T): MutableState<T> {

    val lifecycle = LocalLifecycleOwner.current
    val preferences = LocalSharedPreferences.current

    val events = remember { Channel<T>() }

    val state = produceState(initialValue = default) {
        val scope = this
        value = preferences.get(key, default)
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            events.consumeAsFlow().onEach {
                preferences.set<T>(key, default)
            }
                .launchIn(scope)
            callbackFlow {
                val listener =
                    SharedPreferences.OnSharedPreferenceChangeListener { prefs, prefKey ->
                        if (prefKey == key) {
                            scope.launch {
                                send(prefs.get<T>(key, default))
                            }
                        }
                    }
                try {
                    preferences.registerOnSharedPreferenceChangeListener(listener)
                    awaitCancellation()
                } finally {
                    preferences.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }
                .flowOn(Dispatchers.IO)
                .collect { value = it }
        }
    }
    return object : MutableState<T> {
        override var value: T
            get() = state.value
            set(value) {
                events.trySend(value)
            }

        override fun component1(): T = state.value

        override fun component2(): (T) -> Unit = { v -> value = v }
    }
}
