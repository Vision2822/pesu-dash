package com.pesu.pesudash.data.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class RetryInterceptor(private val maxRetries: Int) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var lastException: IOException? = null

        while (attempt <= maxRetries) {
            try {
                val response = chain.proceed(chain.request())
                if (response.isSuccessful || attempt == maxRetries) return response
                response.close()
            } catch (e: IOException) {
                lastException = e
            }
            attempt++
        }

        throw lastException ?: IOException("Request failed after $maxRetries retries")
    }
}