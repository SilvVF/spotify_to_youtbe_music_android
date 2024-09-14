package io.silv.ui

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebView
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.silv.App
import io.silv.YtMusicApi
import io.silv.accountMenu
import io.silv.getValue
import io.silv.setValue
import io.silv.stored
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun LoginScreen(
    onBack: () -> Unit
) {
    val store = remember { App.store }
    var visitorData by remember { store.stored<String>("visitorData") }
    var innerTubeCookie by remember { store.stored<String>("innerTubeCookie") }
    var accountName by remember { store.stored<String>("accountName") }
    var accountEmail by remember { store.stored<String>("accountEmail") }
    var accountChannelHandle by remember { store.stored<String>("accountChannelHandle") }

    var webView: WebView? = null

    AndroidView(
        modifier = Modifier
            .windowInsetsPadding(ScaffoldDefaults.contentWindowInsets)
            .fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                        if (url.startsWith("https://music.youtube.com")) {
                            innerTubeCookie = CookieManager.getInstance().getCookie(url)
                            YtMusicApi.cookie = innerTubeCookie
                            GlobalScope.launch {
                                App.ytMusicApi.accountMenu()?.let {
                                    accountName = it.name
                                    accountEmail = it.email.orEmpty()
                                    accountChannelHandle = it.channelHandle.orEmpty()
                                } ?: Log.d("AccountMenu", "Failed to get account")
                            }
                        }
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        loadUrl("javascript:Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA)")
                    }
                }
                settings.apply {
                    javaScriptEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                }
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onRetrieveVisitorData(newVisitorData: String?) {
                        if (newVisitorData != null) {
                            visitorData = newVisitorData
                        }
                    }
                }, "Android")
                webView = this
                loadUrl("https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F")
            }
        }
    )

    TopAppBar(
        title = { Text("Login") },
        navigationIcon = {
            IconButton(
                onClick = onBack,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        }
    )

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}