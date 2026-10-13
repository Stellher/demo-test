package com.example.bangumioauthdemo

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BangumiRequestFactoryTest {

    private fun assertJsonRequest(
        method: String,
        route: String,
        payload: String,
        expectedContent: String
    ) {
        val mockServer = MockWebServer()
        mockServer.start()
        try {
            mockServer.enqueue(MockResponse().setResponseCode(204))
            val request = BangumiRequestFactory.create(
                url = mockServer.url(route),
                method = method,
                payload = payload,
                accessToken = "placeholder-token-for-local-test"
            )
            OkHttpClient().newCall(request).execute().use {
                assertEquals(204, it.code)
            }
            val recorded = mockServer.takeRequest()
            assertEquals(method, recorded.method)
            assertEquals(route, recorded.path)
            // Check the *actual* HTTP request, after OkHttp BridgeInterceptor.
            assertEquals("application/json", recorded.getHeader("Content-Type"))
            assertEquals("application/json", request.body!!.contentType().toString())
            assertEquals("Bearer placeholder-token-for-local-test",
                recorded.getHeader("Authorization"))
            assertEquals(expectedContent, recorded.body.readUtf8())
        } finally {
            mockServer.shutdown()
        }
    }

    @Test
    fun createOrUpdateCollectionPostsExactJsonMediaType() {
        assertJsonRequest(
            "POST",
            "/v0/users/-/collections/975",
            """{"type":3,"rate":8,"tags":["冒险"],"comment":"喜欢"}""",
            """{"type":3,"rate":8,"tags":["冒险"],"comment":"喜欢"}"""
        )
    }

    @Test
    fun updateEpisodeUsesExactJsonMediaType() {
        assertJsonRequest(
            "PUT",
            "/v0/users/-/collections/-/episodes/1234",
            """{"type":2}""",
            """{"type":2}"""
        )
    }

    @Test
    fun searchUsesExactJsonMediaType() {
        assertJsonRequest(
            "POST",
            "/v0/search/subjects?limit=30",
            """{"keyword":"海贼王"}""",
            """{"keyword":"海贼王"}"""
        )
    }

    @Test
    fun getDoesNotSendContentTypeOrBody() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
            val request = BangumiRequestFactory.create(
                url = server.url("/v0/subjects/975"),
                method = "GET",
                payload = null,
                accessToken = null
            )
            OkHttpClient().newCall(request).execute().close()
            val actual = server.takeRequest()
            assertEquals("GET", actual.method)
            assertNull(actual.getHeader("Content-Type"))
            assertNull(actual.getHeader("Authorization"))
            assertTrue(actual.body.readUtf8().isEmpty())
        } finally {
            server.shutdown()
        }
    }
}
