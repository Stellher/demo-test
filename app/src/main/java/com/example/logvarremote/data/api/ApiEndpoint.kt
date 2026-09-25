package com.example.logvarremote.data.api

enum class ApiHttpMethod { GET, POST, PATCH, DELETE, HEAD }

data class ApiEndpoint(
    val id: String,
    val category: String,
    val name: String,
    val method: ApiHttpMethod,
    val path: String,
    val defaultQuery: String = "",
    val defaultBody: String = "",
    val description: String = "",
    val port: Int = 19321,
    val tokenRequired: Boolean = true,
    val multipartOnly: Boolean = false
)

data class ApiDebugResult(
    val requestUrl: String,
    val code: Int,
    val elapsedMs: Long,
    val contentType: String?,
    val headers: String,
    val body: String
)
