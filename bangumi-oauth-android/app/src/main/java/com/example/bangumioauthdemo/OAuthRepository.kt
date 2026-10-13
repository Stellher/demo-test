package com.example.bangumioauthdemo

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object OAuthRepository {
    const val REDIRECT = "bgmoauthdemo://callback"
    private val http = OkHttpClient()

    fun authorizeUrl(state: String): String = Uri.parse("https://bgm.tv/oauth/authorize")
        .buildUpon()
        .appendQueryParameter("client_id", BuildConfig.BGM_CLIENT_ID)
        .appendQueryParameter("response_type", "code")
        .appendQueryParameter("redirect_uri", REDIRECT)
        .appendQueryParameter("state", state).build().toString()

    private suspend fun post(fields: Map<String, String>, path: String): JSONObject = withContext(Dispatchers.IO) {
        val form = FormBody.Builder().apply { fields.forEach { (key, value) -> add(key, value) } }.build()
        val request = Request.Builder().url("https://bgm.tv/oauth/" + path)
            .post(form).header("User-Agent", "BangumiOAuthDemo/1.0 (Android; demo)").build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("HTTP " + response.code + ": " + body)
            JSONObject(body)
        }
    }

    suspend fun exchange(code: String, secret: String): JSONObject = post(mapOf(
        "grant_type" to "authorization_code",
        "client_id" to BuildConfig.BGM_CLIENT_ID,
        "client_secret" to secret,
        "code" to code,
        "redirect_uri" to REDIRECT
    ), "access_token")

    suspend fun refresh(refreshToken: String, secret: String): JSONObject = post(mapOf(
        "grant_type" to "refresh_token",
        "client_id" to BuildConfig.BGM_CLIENT_ID,
        "client_secret" to secret,
        "refresh_token" to refreshToken,
        "redirect_uri" to REDIRECT
    ), "access_token")

    suspend fun status(token: String): JSONObject = post(mapOf("access_token" to token), "token_status")

    suspend fun me(token: String): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("https://api.bgm.tv/v0/me")
            .header("Authorization", "Bearer " + token)
            .header("User-Agent", "BangumiOAuthDemo/1.0 (Android; demo)").build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("HTTP " + response.code + ": " + body)
            JSONObject(body)
        }
    }
}
