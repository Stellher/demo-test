package com.example.bangumioauthdemo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AnimeSubject(
    val id: Int,
    val name: String,
    val chineseName: String,
    val image: String,
    val score: Double,
    val rank: Int,
    val date: String,
    val summary: String,
    val type: Int,
    val episodes: Int,
    val tags: List<String>,
    val collectionCount: Int = 0
) {
    val displayName get() = chineseName.ifBlank { name }
}

data class EpisodeItem(
    val id: Int,
    val sort: Double,
    val name: String,
    val chineseName: String,
    val airDate: String,
    val duration: String,
    val description: String
) {
    val displayName get() = chineseName.ifBlank { name }
}

data class BangumiPerson(
    val id: Int,
    val name: String,
    val image: String,
    val summary: String,
    val type: String,
    val role: String = ""
)

data class UserCollection(
    val subject: AnimeSubject,
    val status: Int,
    val rate: Int,
    val comment: String,
    val tags: List<String>
)

data class BroadcastDay(val weekday: Int, val title: String, val subjects: List<AnimeSubject>)

data class ApiPage<T>(val items: List<T>, val total: Int, val offset: Int, val limit: Int)

fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
fun JSONObject.str(key: String): String = optString(key).takeUnless { it == "null" }.orEmpty()
fun JSONObject.tags(): List<String> = optJSONArray("tags")?.objects()?.map {
    it.str("name")
}?.filter { it.isNotBlank() } ?: emptyList()

fun JSONObject.asSubject(): AnimeSubject {
    val images = optJSONObject("images")
    val image = images?.str("large").orEmpty()
        .ifBlank { images?.str("common").orEmpty() }
        .ifBlank { images?.str("medium").orEmpty() }
        .ifBlank { images?.str("small").orEmpty() }
    val rating = optJSONObject("rating")
    val collection = optJSONObject("collection")
    val eps = optInt("eps").takeIf { it > 0 } ?: optInt("total_episodes")
    return AnimeSubject(
        id = optInt("id"),
        name = str("name"),
        chineseName = str("name_cn"),
        image = image.replace("http://", "https://"),
        score = rating?.optDouble("score", 0.0) ?: 0.0,
        rank = optInt("rank"),
        date = str("date"),
        summary = str("summary"),
        type = optInt("type", 2),
        episodes = eps,
        tags = tags(),
        collectionCount = collection?.optInt("doing", 0) ?: 0
    )
}

fun JSONObject.asEpisode() = EpisodeItem(
    id = optInt("id"),
    sort = optDouble("sort"),
    name = str("name"),
    chineseName = str("name_cn"),
    airDate = str("airdate"),
    duration = str("duration"),
    description = str("desc")
)

fun JSONObject.asPerson(kind: String, role: String = ""): BangumiPerson {
    val nested = optJSONObject(kind) ?: this
    val images = nested.optJSONObject("images")
    return BangumiPerson(
        id = nested.optInt("id"),
        name = nested.str("name"),
        image = (images?.str("large").orEmpty().ifBlank { images?.str("medium").orEmpty() })
            .replace("http://", "https://"),
        summary = nested.str("summary"),
        type = kind,
        role = role.ifBlank { str("relation") }
    )
}

/** First-party public Bangumi APIs only; no unofficial scraping or screen scraping. */
class BangumiClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private suspend fun request(
        path: String, params: Map<String, String> = emptyMap(),
        method: String = "GET", json: JSONObject? = null,
        bearerRequired: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        require(path.startsWith("/v0/") || path == "/calendar") { "Unrecognized Bangumi API path" }
        val base = ("https://api.bgm.tv" + path).toHttpUrl().newBuilder()
        params.forEach { (key, value) -> if (value.isNotBlank()) base.addQueryParameter(key, value) }
        val url = base.build()
        suspend fun call(accessToken: String?): Pair<Int, String> {
            val builder = Request.Builder().url(url)
                .header("User-Agent", "BangumiGlass/1.0 (Android; https://github.com/Stellher/demo-test)")
                .header("Accept", "application/json")
            if (!accessToken.isNullOrBlank()) builder.header("Authorization", "Bearer " + accessToken)
            val body = if (method == "GET") null else
                (json?.toString() ?: "{}").toRequestBody("application/json; charset=utf-8".toMediaType())
            builder.method(method, body)
            return http.newCall(builder.build()).execute().use { response ->
                response.code to response.body?.string().orEmpty()
            }
        }
        val token = OAuthSessionManager.ensureValidToken()
        if (bearerRequired && token == null) {
            error("此操作需要 Bangumi 登录授权，请到「我的」页面登录")
        }
        var pair = call(token)
        if (pair.first == 401 && !token.isNullOrBlank()) {
            val fresh = OAuthSessionManager.ensureValidToken(forceRefresh = true, failedToken = token)
            if (method == "GET" && fresh != null && fresh != token) pair = call(fresh)
        }
        if (pair.first !in 200..299) {
            val msg = try { JSONObject(pair.second).str("description").ifBlank {
                JSONObject(pair.second).str("error")
            } } catch (_: Exception) { "" }
            error("Bangumi HTTP ${pair.first}" + if (msg.isBlank()) "" else " · " + msg.take(140))
        }
        pair.second
    }

    private suspend fun obj(path: String, params: Map<String, String> = emptyMap(),
                            method: String = "GET", body: JSONObject? = null,
                            auth: Boolean = false) =
        JSONObject(request(path, params, method, body, auth).ifBlank { "{}" })

    private fun <T> page(json: JSONObject, parser: (JSONObject) -> T): ApiPage<T> {
        val items = json.optJSONArray("data")?.objects().orEmpty().map(parser)
        return ApiPage(items, json.optInt("total", items.size), json.optInt("offset"), json.optInt("limit", items.size))
    }

    suspend fun calendar(): List<BroadcastDay> {
        val raw = JSONArray(request("/calendar"))
        return raw.objects().mapIndexed { index, item ->
            val week = item.optJSONObject("weekday")
            BroadcastDay(
                weekday = week?.optInt("id", index + 1) ?: index + 1,
                title = week?.str("cn").orEmpty().ifBlank { "星期${index + 1}" },
                subjects = item.optJSONArray("items")?.objects()?.map { it.asSubject() }.orEmpty()
            )
        }.sortedBy { it.weekday }
    }

    /** Official browsing endpoint: year/month are server-side classification, not a fake season endpoint. */
    suspend fun browse(
        type: Int = 2, year: Int? = null, month: Int? = null,
        sort: String = "rank", offset: Int = 0, limit: Int = 24
    ): ApiPage<AnimeSubject> {
        val query = mutableMapOf("type" to "$type", "sort" to sort,
            "offset" to "$offset", "limit" to "$limit")
        if (year != null) query["year"] = "$year"
        if (month != null) query["month"] = "$month"
        return page(obj("/v0/subjects", query), JSONObject::asSubject)
    }

    suspend fun search(
        keyword: String, type: Int? = null, sort: String = "match",
        offset: Int = 0, limit: Int = 24
    ): ApiPage<AnimeSubject> {
        val body = JSONObject().put("keyword", keyword).put("sort", sort)
        if (type != null) body.put("filter", JSONObject().put("type", JSONArray().put(type)))
        return page(obj("/v0/search/subjects",
            mapOf("offset" to "$offset", "limit" to "$limit"), "POST", body), JSONObject::asSubject)
    }

    suspend fun subject(id: Int): AnimeSubject = obj("/v0/subjects/$id").asSubject()
    suspend fun episodes(subjectId: Int): ApiPage<EpisodeItem> =
        page(obj("/v0/episodes", mapOf("subject_id" to "$subjectId", "limit" to "200")), JSONObject::asEpisode)
    suspend fun cast(subjectId: Int, kind: String): List<BangumiPerson> {
        val path = when (kind) {
            "characters" -> "/v0/subjects/$subjectId/characters"
            else -> "/v0/subjects/$subjectId/persons"
        }
        val json = JSONArray(request(path))
        return json.objects().map { it.asPerson(if (kind == "characters") "character" else "person") }
    }
    suspend fun relations(subjectId: Int): List<AnimeSubject> =
        JSONArray(request("/v0/subjects/$subjectId/subjects")).objects().mapNotNull {
            (it.optJSONObject("subject") ?: it).takeIf { j -> j.optInt("id") > 0 }?.asSubject()
        }
    suspend fun person(id: Int, kind: String): BangumiPerson =
        obj("/v0/${if (kind == "character") "characters" else "persons"}/$id").asPerson(kind)
    suspend fun personSubjects(id: Int, kind: String): List<AnimeSubject> =
        JSONArray(request("/v0/${if (kind == "character") "characters" else "persons"}/$id/subjects"))
            .objects().mapNotNull { (it.optJSONObject("subject") ?: it).takeIf { j -> j.optInt("id") > 0 }?.asSubject() }

    suspend fun collectPerson(id: Int, kind: String, collect: Boolean) {
        request("/v0/${if (kind == "character") "characters" else "persons"}/$id/collect",
            method = if (collect) "POST" else "DELETE", bearerRequired = true)
    }
    suspend fun myself() = obj("/v0/me", auth = true)
    suspend fun user(name: String) = obj("/v0/users/${encode(name)}")
    private fun encode(value: String) = java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    suspend fun collections(
        name: String, collectionType: Int? = null, offset: Int = 0
    ): ApiPage<UserCollection> {
        val query = mutableMapOf("limit" to "30", "offset" to "$offset")
        if (collectionType != null) query["type"] = "$collectionType"
        return page(obj("/v0/users/${encode(name)}/collections", query)) { item ->
            UserCollection(
                subject = (item.optJSONObject("subject") ?: item).asSubject(),
                status = item.optInt("type"),
                rate = item.optInt("rate"),
                comment = item.str("comment"),
                tags = item.optJSONArray("tags")?.let { tags ->
                    (0 until tags.length()).map { index -> tags.optString(index) }
                }.orEmpty()
            )
        }
    }
    suspend fun userCollection(name: String, subjectId: Int) =
        obj("/v0/users/${encode(name)}/collections/$subjectId")

    suspend fun saveCollection(
        subjectId: Int, type: Int, rate: Int? = null, comment: String? = null
    ) {
        val body = JSONObject().put("type", type)
        if (rate != null) body.put("rate", rate)
        if (comment != null) body.put("comment", comment)
        request("/v0/users/-/collections/$subjectId", method = "POST", json = body, bearerRequired = true)
    }

    suspend fun setEpisodeProgress(subjectId: Int, episodeIds: List<Int>, status: Int) {
        val data = JSONObject().put("episode_id", JSONArray(episodeIds)).put("type", status)
        request("/v0/users/-/collections/$subjectId/episodes", method = "PATCH",
            json = data, bearerRequired = true)
    }

    suspend fun episodeProgress(subjectId: Int): JSONObject =
        obj("/v0/users/-/collections/$subjectId/episodes", auth = true)

    suspend fun index(id: Int): JSONObject = obj("/v0/indices/$id")
    suspend fun indexSubjects(id: Int): JSONObject = obj("/v0/indices/$id/subjects")
    suspend fun createIndex(): JSONObject = obj("/v0/indices", method = "POST", auth = true)
    suspend fun modifyIndex(id: Int, title: String, description: String) {
        request("/v0/indices/$id", method = "PUT", json = JSONObject()
            .put("title", title).put("description", description), bearerRequired = true)
    }
    suspend fun indexEntry(indexId: Int, subjectId: Int, add: Boolean) {
        val path = "/v0/indices/$indexId/subjects" + if (add) "" else "/$subjectId"
        val body = if (add) JSONObject().put("subject_id", subjectId) else null
        request(path, method = if (add) "POST" else "DELETE", json = body, bearerRequired = true)
    }
}
