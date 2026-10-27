package com.beanpilot.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GaggiMateHistoryClient @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun fetchRecentShots(address: String): List<GaggiMateShot> {
        val endpoint = GaggiMateEndpoint.parse(address)
        val response: HttpResponse = httpClient.get(endpoint.historyUrl) {
            parameter("limit", HISTORY_LIMIT)
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException(
                "GaggiMate history request failed (HTTP ${response.status.value}).",
            )
        }
        return GaggiMateShotIndexParser.parse(response.body())
    }

    private companion object {
        const val HISTORY_LIMIT = 50
    }
}
