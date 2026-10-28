package com.beanpilot.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class GaggiMateEndpointTest {
    @Test
    fun `host resolves to documented websocket and history paths`() {
        val endpoint = GaggiMateEndpoint.parse("gaggimate.local")

        assertEquals("ws://gaggimate.local/ws", endpoint.webSocketUrl)
        assertEquals("http://gaggimate.local/api/history/recent.bin", endpoint.historyUrl)
    }

    @Test
    fun `secure host keeps secure websocket and history schemes`() {
        val endpoint = GaggiMateEndpoint.parse("https://gaggimate.local:8443")

        assertEquals("wss://gaggimate.local:8443/ws", endpoint.webSocketUrl)
        assertEquals("https://gaggimate.local:8443/api/history/recent.bin", endpoint.historyUrl)
        assertEquals(
            "https://gaggimate.local:8443/api/history/000031.slog",
            endpoint.shotLogUrl("31"),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `shot log endpoint rejects non numeric ids`() {
        GaggiMateEndpoint.parse("gaggimate.local").shotLogUrl("../index")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unsupported protocol is rejected`() {
        GaggiMateEndpoint.parse("ftp://gaggimate.local")
    }
}
