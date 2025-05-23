package io.silv.sp2yt

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
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

inline fun <reified T : Any> Settings.settingsMutableState(key: String, default: T): MutableState<T> {
    return object : MutableState<T> {
        var state by mutableStateOf(this@settingsMutableState.get<T>(key) ?: default)

        override var value: T
            get() = state
            set(value) {
                this@settingsMutableState[key] = value
                state = value
            }

        override fun component1(): T = value

        override fun component2(): (T) -> Unit = { v -> value = v }
    }
}

inline fun <reified T : Any> Settings.stored(key: String, default: T) = object : MutableStored<T> {
    override var value: T
        get() = this@stored.get<T>(key) ?: default
        set(value) = this@stored.set(key, value)

    override fun component1(): T = value
    override fun component2(): (T) -> Unit = { v -> value = v }
}