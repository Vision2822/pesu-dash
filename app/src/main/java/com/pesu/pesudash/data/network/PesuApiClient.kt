package com.pesu.pesudash.data.network

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object PesuApiClient {

    private const val BASE_URL = "https://www.pesuacademy.com/MAcademy"

    val cookieJar = SessionCookieJar()

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logger)
        .build()

    val loginClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logger)
        .build()

    var authToken: String? = null

    fun buildDispatcherRequest(params: Map<String, String>): Request {
        val formBuilder = FormBody.Builder()
        params.forEach { (k, v) -> formBuilder.add(k, v) }
        formBuilder.add("randomNum", Math.random().toString())

        val requestBuilder = Request.Builder()
            .url("$BASE_URL/mobile/dispatcher")
            .post(formBuilder.build())
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/143.0 Mobile Safari/537.36")
            .header("X-Requested-With", "pes.pesu")

        authToken?.let {
            requestBuilder.header("mobileAppAuthenticationToken", it)
        }

        return requestBuilder.build()
    }

    fun buildLoginRequest(username: String, password: String): Request {
        val form = FormBody.Builder()
            .add("j_username", username)
            .add("j_password", password)
            .add("j_mobile", "MOBILE")
            .add("j_mobileApp", "YES")
            .add("j_social", "NO")
            .add("j_appId", "1")
            .add("action", "0")
            .add("mode", "0")
            .add("randomNum", Math.random().toString())
            .build()

        return Request.Builder()
            .url("$BASE_URL/j_spring_security_check")
            .post(form)
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/143.0 Mobile Safari/537.36")
            .header("X-Requested-With", "pes.pesu")
            .build()
    }

    fun buildGetRequest(path: String): Request {
        val requestBuilder = Request.Builder()
            .url("$BASE_URL/$path")
            .get()
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/143.0 Mobile Safari/537.36")
            .header("X-Requested-With", "pes.pesu")

        authToken?.let {
            requestBuilder.header("mobileAppAuthenticationToken", it)
        }

        return requestBuilder.build()
    }

    fun clearSession() {
        authToken = null
        cookieJar.clear()
    }
}