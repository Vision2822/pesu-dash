package com.pesu.pesudash.data.network

import java.io.IOException

sealed class PesuError : Exception() {
    data class Auth(override val message: String) : PesuError()
    data class Network(override val cause: Throwable) : PesuError()
    data class Parse(override val cause: Throwable) : PesuError()
    object SessionExpired : PesuError()
    object NoData : PesuError()
}

class AuthException(message: String) : Exception(message)