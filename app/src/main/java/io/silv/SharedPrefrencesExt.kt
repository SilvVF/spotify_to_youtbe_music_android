package io.silv

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import kotlin.reflect.KProperty


interface Stored<out T> {
    val value: T
}

inline operator fun <T> Stored<T>.getValue(thisObj: Any?, property: KProperty<*>): T = value

interface MutableStored<T> : Stored<T> {
    override var value: T
    operator fun component1(): T
    operator fun component2(): (T) -> Unit
}

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
