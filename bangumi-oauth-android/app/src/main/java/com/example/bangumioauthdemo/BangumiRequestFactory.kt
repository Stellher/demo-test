package com.example.bangumioauthdemo

import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Produces Bangumi API requests with the exact JSON media type required by
 * the server: Content-Type: application/json (without an appended charset).
 *
 * Important: OkHttp's String.toRequestBody(MediaType("application/json"))
 * appends "; charset=utf-8" automatically. ByteArray.toRequestBody does not.
 * BridgeInterceptor derives Content-Type from RequestBody.contentType(), so
 * setting only Request.Builder.header("Content-Type") is not sufficient.
 */
internal object BangumiRequestFactory {
    private val jsonMediaType = "application/json".toMediaType()
    private const val USER_AGENT =
        "BangumiGlass/2.0.1 (Android; github.com/Stellher/demo-test)"

    fun create(
        url: HttpUrl,
        method: String,
        payload: String?,
        accessToken: String?
    ): Request {
        val verb = method.uppercase()
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")

        if (!accessToken.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $accessToken")
        }

        val body = when (verb) {
            "GET", "HEAD" -> null
            "POST", "PUT", "PATCH" -> (
                payload?.takeIf { it.isNotBlank() } ?: "{}"
            ).toByteArray(Charsets.UTF_8).toRequestBody(jsonMediaType)
            "DELETE" -> payload?.toByteArray(Charsets.UTF_8)?.toRequestBody(jsonMediaType)
            else -> throw IllegalArgumentException("Unsupported HTTP method: $verb")
        }

        // Explicit header for clarity; body.contentType() is deliberately
        // the exact same media type so OkHttp's BridgeInterceptor preserves it.
        if (body != null) {
            builder.header("Content-Type", "application/json")
        }
        return builder.method(verb, body).build()
    }
}
