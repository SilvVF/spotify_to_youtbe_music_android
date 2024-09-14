package io.silv

import android.app.Application
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import okhttp3.Cache
import java.io.File

class App: Application() {

    override fun onCreate() {
        super.onCreate()

        store = EncryptedSharedPreferences.create(
            "encrypted-prefs",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        cache = Cache(File(this.cacheDir, "net_cache"), 5_000)
        YtMusicApi.cookie = store.get<String>("innerTubeCookie").ifEmpty { null }
    }


    companion object {
        lateinit var store: SharedPreferences
        lateinit var cache: Cache

        val spotifyApi by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { SpotifyApi(store) { cache(cache) } }
        val ytMusicApi by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { YtMusicApi(store) { cache(cache) } }
    }
}