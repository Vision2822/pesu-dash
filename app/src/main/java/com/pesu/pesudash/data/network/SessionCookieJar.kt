package com.pesu.pesudash.data.network

import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class SessionCookieJar : CookieJar {

    private val TAG = "PesuApi"
    val store = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        store.removeAll { existing ->
            cookies.any { it.name == existing.name }
        }
        store.addAll(cookies)
        Log.d(TAG, "cookieJar: saved ${cookies.size} from ${url.host}. Total=${store.size}")
        cookies.forEach { Log.d(TAG, "  + ${it.name}") }
        Log.d(TAG, "cookieJar: jar now has: ${store.map { it.name }}")
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val matching = store.filter { it.matches(url) }
        Log.d(TAG, "cookieJar: sending ${matching.size} to ${url.host}: ${matching.map { it.name }}")
        return matching
    }

    fun clear() {
        store.clear()
        Log.d(TAG, "cookieJar: cleared")
    }
}