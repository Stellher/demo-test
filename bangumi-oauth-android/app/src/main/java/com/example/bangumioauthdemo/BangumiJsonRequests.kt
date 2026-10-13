package com.example.bangumioauthdemo

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Bangumi's v0 write APIs expect the exact media type "application/json".
 *
 * The String.toRequestBody(mediaType) overload in OkHttp appends "; charset=utf-8"
 * to JSON content-types. This can be rejected with HTTP 415 by Bangumi's
 * Content-Type validation. Encode UTF-8 ourselves and supply ByteArray body.
 */
internal object BangumiJsonRequests {
    const val CONTENT_TYPE = "application/json"
    private val mediaType = CONTENT_TYPE.toMediaType()

    fun body(json: String): RequestBody =
        json.toByteArray(Charsets.UTF_8).toRequestBody(mediaType)

    /**
     * Configure a request for GET/HEAD or JSON write methods.
     * POST, PUT, PATCH and DELETE never append a charset parameter.
     */
    fun apply(builder: Request.Builder, method: String, json: String? = null) {
        val verb = method.uppercase()
        if (verb == "GET" || verb == "HEAD") {
            builder.method(verb, null)
        } else {
            builder.method(verb, body(json.orEmpty()))
            builder.header("Content-Type", CONTENT_TYPE)
        }
    }
}
