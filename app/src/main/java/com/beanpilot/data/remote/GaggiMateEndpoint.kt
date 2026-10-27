package com.beanpilot.data.remote

import java.net.URI

data class GaggiMateEndpoint private constructor(
    private val authority: String,
    private val secure: Boolean,
) {
    val webSocketUrl: String
        get() = "${if (secure) "wss" else "ws"}://$authority/ws"

    val historyUrl: String
        get() = "${if (secure) "https" else "http"}://$authority/api/history/recent.bin"

    companion object {
        fun parse(address: String): GaggiMateEndpoint {
            val input = address.trim()
            require(input.isNotEmpty()) { "Enter the GaggiMate host or address." }

            val uri = try {
                URI(if (input.contains("://")) input else "http://$input")
            } catch (exception: Exception) {
                throw IllegalArgumentException("Enter a valid GaggiMate host or URL.", exception)
            }

            val scheme = uri.scheme?.lowercase()
            require(scheme in setOf("http", "https", "ws", "wss")) {
                "Use an http, https, ws, or wss address."
            }
            require(!uri.host.isNullOrBlank() && uri.rawUserInfo == null) {
                "Enter a host name or IP address without credentials."
            }
            require(uri.rawQuery == null && uri.rawFragment == null) {
                "Do not include a query or fragment in the GaggiMate address."
            }

            val authority = uri.rawAuthority
                ?: throw IllegalArgumentException("Enter a valid GaggiMate host or URL.")
            return GaggiMateEndpoint(
                authority = authority,
                secure = scheme == "https" || scheme == "wss",
            )
        }
    }
}
