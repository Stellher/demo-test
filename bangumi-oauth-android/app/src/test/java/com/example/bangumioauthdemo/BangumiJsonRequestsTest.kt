package com.example.bangumioauthdemo

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class BangumiJsonRequestsTest {
    private val client = OkHttpClient()

    @Test
    fun collectionPostSendsExactApplicationJsonWithoutCharset() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(204))
            val payload = """{"type":3,"rate":8,"comment":"航海王","tags":["热血","冒险"]}"""
            val builder = Request.Builder()
                .url(server.url("/v0/users/-/collections/975"))
                .header("Authorization", "Bearer test-token")
            BangumiJsonRequests.apply(builder, "POST", payload)

            client.newCall(builder.build()).execute().use { response ->
                assertEquals(204, response.code)
            }
            val received = requireNotNull(server.takeRequest(3, TimeUnit.SECONDS))
            assertEquals("POST", received.method)
            assertEquals("application/json", received.getHeader("Content-Type"))
            assertFalse(received.getHeader("Content-Type")!!.contains("charset", ignoreCase = true))
            assertEquals("Bearer test-token", received.getHeader("Authorization"))
            assertEquals(payload, received.body.readUtf8())
        }
    }

    @Test
    fun episodePutSendsExactApplicationJson() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(204))
            val builder = Request.Builder().url(server.url("/v0/users/-/collections/-/episodes/1234"))
            BangumiJsonRequests.apply(builder, "PUT", """{"type":2}""")

            client.newCall(builder.build()).execute().close()
            val received = requireNotNull(server.takeRequest(3, TimeUnit.SECONDS))
            assertEquals("PUT", received.method)
            assertEquals("application/json", received.getHeader("Content-Type"))
            assertEquals("""{"type":2}""", received.body.readUtf8())
        }
    }

    @Test
    fun collectionPatchSendsExactApplicationJson() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(204))
            val builder = Request.Builder().url(server.url("/v0/users/-/collections/975"))
            BangumiJsonRequests.apply(builder, "PATCH", """{"rate":9}""")

            client.newCall(builder.build()).execute().close()
            val received = requireNotNull(server.takeRequest(3, TimeUnit.SECONDS))
            assertEquals("PATCH", received.method)
            assertEquals("application/json", received.getHeader("Content-Type"))
        }
    }

    @Test
    fun getDoesNotSendJsonContentTypeOrBody() {
        val builder = Request.Builder().url("https://api.bgm.tv/v0/me")
        BangumiJsonRequests.apply(builder, "GET")
        val request = builder.build()
        assertEquals("GET", request.method)
        assertNull(request.body)
        assertNull(request.header("Content-Type"))
    }
}
