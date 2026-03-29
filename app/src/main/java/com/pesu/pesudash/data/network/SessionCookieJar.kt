package com.pesu.pesudash.data.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.locks.ReentrantReadWriteLock

class SessionCookieJar : CookieJar {

    private val lock  = ReentrantReadWriteLock()
    private val store = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        lock.writeLock().lock()
        try {
            store.removeAll { existing -> cookies.any { it.name == existing.name } }
            store.addAll(cookies)
        } finally {
            lock.writeLock().unlock()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        lock.readLock().lock()
        try {
            return store.filter { it.matches(url) }
        } finally {
            lock.readLock().unlock()
        }
    }

    fun clear() {
        lock.writeLock().lock()
        try {
            store.clear()
        } finally {
            lock.writeLock().unlock()
        }
    }
}