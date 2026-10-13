package com.example.bangumioauthdemo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate

class BangumiViewModel(app: Application) : AndroidViewModel(app) {
    private val api = BangumiClient()
    private val prefs = app.getSharedPreferences("bangumi_glass_ui", 0)
    private val today = LocalDate.now()

    var activeTab by androidx.compose.runtime.mutableIntStateOf(0)
        private set
    var selectedDay by androidx.compose.runtime.mutableIntStateOf(today.dayOfWeek.value)
        private set
    var calendar by androidx.compose.runtime.mutableStateOf<List<BroadcastDay>>(emptyList())
        private set
    var homeHighlights by androidx.compose.runtime.mutableStateOf<List<AnimeSubject>>(emptyList())
        private set
    var seasonItems by androidx.compose.runtime.mutableStateOf<List<AnimeSubject>>(emptyList())
        private set
    var seasonTotal by androidx.compose.runtime.mutableIntStateOf(0)
        private set
    var seasonYear by androidx.compose.runtime.mutableIntStateOf(today.year)
        private set
    var seasonQuarter by androidx.compose.runtime.mutableIntStateOf((today.monthValue - 1) / 3 + 1)
        private set
    var seasonType by androidx.compose.runtime.mutableIntStateOf(2)
        private set
    var seasonSort by androidx.compose.runtime.mutableStateOf("rank")
        private set
    var discoveryQuery by androidx.compose.runtime.mutableStateOf("")
        private set
    var discoveryType by androidx.compose.runtime.mutableIntStateOf(0)
        private set
    var discoverySort by androidx.compose.runtime.mutableStateOf("match")
        private set
    var discoveryItems by androidx.compose.runtime.mutableStateOf<List<AnimeSubject>>(emptyList())
        private set
    var discoveryTotal by androidx.compose.runtime.mutableIntStateOf(0)
        private set
    var collections by androidx.compose.runtime.mutableStateOf<List<UserCollection>>(emptyList())
        private set
    var collectionTotal by androidx.compose.runtime.mutableIntStateOf(0)
        private set
    var collectionType by androidx.compose.runtime.mutableIntStateOf(3)
        private set
    var profile by androidx.compose.runtime.mutableStateOf<JSONObject?>(null)
        private set
    var detail by androidx.compose.runtime.mutableStateOf<AnimeSubject?>(null)
        private set
    var chapters by androidx.compose.runtime.mutableStateOf<List<EpisodeItem>>(emptyList())
        private set
    var characters by androidx.compose.runtime.mutableStateOf<List<BangumiPerson>>(emptyList())
        private set
    var staff by androidx.compose.runtime.mutableStateOf<List<BangumiPerson>>(emptyList())
        private set
    var related by androidx.compose.runtime.mutableStateOf<List<AnimeSubject>>(emptyList())
        private set
    var progressIds by androidx.compose.runtime.mutableStateOf<Set<Int>>(emptySet())
        private set
    var focusedPerson by androidx.compose.runtime.mutableStateOf<BangumiPerson?>(null)
        private set
    var personWorks by androidx.compose.runtime.mutableStateOf<List<AnimeSubject>>(emptyList())
        private set
    var openedIndex by androidx.compose.runtime.mutableStateOf<JSONObject?>(null)
        private set
    var indexEntries by androidx.compose.runtime.mutableStateOf<List<AnimeSubject>>(emptyList())
        private set
    var rememberedIndices by androidx.compose.runtime.mutableStateOf(
        prefs.getString("saved_indices", "").orEmpty()
            .split(",").mapNotNull { it.toIntOrNull() }.distinct()
    )
        private set
    var loadingKeys by androidx.compose.runtime.mutableStateOf<Set<String>>(emptySet())
        private set
    var notices by androidx.compose.runtime.mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var snackbar by androidx.compose.runtime.mutableStateOf<String?>(null)
        private set

    private var seasonJob: Job? = null
    private var searchJob: Job? = null
    private var selectedId: Int? = null

    init {
        OAuthSessionManager.initialize(app)
        refreshHome()
        loadSeason()
        if (OAuthSessionManager.session.value != null) {
            loadProfile()
        }
    }

    val username: String
        get() = profile?.str("username").orEmpty()

    fun toast(msg: String) { snackbar = msg }
    fun clearToast() { snackbar = null }

    private fun work(key: String, block: suspend () -> Unit): Job = viewModelScope.launch {
        loadingKeys = loadingKeys + key
        notices = notices - key
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            notices = notices + (key to (e.message ?: "加载失败，请重试"))
        } finally {
            loadingKeys = loadingKeys - key
        }
    }

    fun clearAccountData() {
        profile = null
        collections = emptyList()
        collectionTotal = 0
        progressIds = emptySet()
    }

    fun changeTab(index: Int) {
        activeTab = index
        if (index == 3 && OAuthSessionManager.session.value != null) {
            if (profile == null) loadProfile()
            else if (collections.isEmpty()) loadCollections()
        }
        if (index == 4 && OAuthSessionManager.session.value != null && profile == null) loadProfile()
    }

    fun refreshHome() {
        work("home") {
            calendar = api.calendar()
            if (calendar.isEmpty()) notices = notices + ("home" to "今天没有可用的放送信息")
        }
        work("highlights") { homeHighlights = api.browse(type = 2, sort = "rank", limit = 14).items }
    }

    fun selectDay(day: Int) { selectedDay = day }

    fun setSeason(year: Int = seasonYear, quarter: Int = seasonQuarter, type: Int = seasonType,
                  sort: String = seasonSort) {
        seasonYear = year.coerceIn(1990, today.year + 2)
        seasonQuarter = quarter.coerceIn(1, 4)
        seasonType = type
        seasonSort = sort
        loadSeason()
    }

    fun loadSeason(more: Boolean = false) {
        if (more && ("season" in loadingKeys || seasonItems.size >= seasonTotal && seasonTotal > 0)) return
        seasonJob?.cancel()
        val offset = if (more) seasonItems.size else 0
        seasonJob = work("season") {
            val result = api.browse(type = seasonType, year = seasonYear,
                month = ((seasonQuarter - 1) * 3 + 1), sort = seasonSort,
                offset = offset, limit = 24)
            seasonItems = if (more) (seasonItems + result.items).distinctBy { it.id } else result.items
            seasonTotal = result.total
        }
    }

    fun updateSearchQuery(text: String) { discoveryQuery = text }
    fun setDiscoveryType(type: Int) { discoveryType = type; submitSearch() }
    fun setDiscoverySort(sort: String) { discoverySort = sort; submitSearch() }

    fun submitSearch(more: Boolean = false) {
        if (more && ("search" in loadingKeys || discoveryItems.size >= discoveryTotal && discoveryTotal > 0)) return
        searchJob?.cancel()
        val query = discoveryQuery.trim()
        val offset = if (more) discoveryItems.size else 0
        searchJob = work("search") {
            val result = if (query.isBlank()) {
                api.browse(type = if (discoveryType == 0) 2 else discoveryType,
                    sort = "rank", offset = offset, limit = 24)
            } else {
                api.search(keyword = query, type = discoveryType.takeUnless { it == 0 },
                    sort = discoverySort, offset = offset, limit = 24)
            }
            discoveryItems = if (more) (discoveryItems + result.items).distinctBy { it.id } else result.items
            discoveryTotal = result.total
        }
    }

    fun setCollectionType(type: Int) { collectionType = type; loadCollections() }
    fun loadProfile() {
        work("profile") {
            profile = api.myself()
            if (username.isNotBlank()) loadCollections()
        }
    }

    fun loadCollections(more: Boolean = false) {
        val owner = username
        if (owner.isBlank()) return
        if (more && ("library" in loadingKeys ||
                    collections.size >= collectionTotal && collectionTotal > 0)) return
        work("library") {
            val offset = if (more) collections.size else 0
            val result = api.collections(owner, collectionType, offset)
            collections = if (more) (collections + result.items).distinctBy { it.subject.id }
                else result.items
            collectionTotal = result.total
        }
    }

    fun openSubject(id: Int) {
        if (id <= 0) return
        selectedId = id
        focusedPerson = null
        detail = null
        chapters = emptyList()
        characters = emptyList()
        staff = emptyList()
        related = emptyList()
        progressIds = emptySet()
        work("detail") {
            val item = api.subject(id)
            if (selectedId == id) detail = item
        }
        work("chapters") {
            val items = api.episodes(id).items
            if (selectedId == id) chapters = items
        }
        work("cast") {
            val list = api.cast(id, "characters")
            if (selectedId == id) characters = list
        }
        work("staff") {
            val list = api.cast(id, "persons")
            if (selectedId == id) staff = list
        }
        work("related") {
            val list = api.relations(id)
            if (selectedId == id) related = list
        }
        if (OAuthSessionManager.session.value != null) {
            work("progress") {
                val data = api.episodeProgress(id)
                val items = data.optJSONArray("data")?.objects().orEmpty()
                if (selectedId == id) progressIds = items.filter {
                    it.optInt("type") >= 2
                }.map { it.optJSONObject("episode")?.optInt("id") ?: it.optInt("episode_id") }.toSet()
            }
        }
    }

    fun closeSubject() { selectedId = null; detail = null; chapters = emptyList(); focusedPerson = null }

    fun saveCollection(subjectId: Int, status: Int, rate: Int = 0, comment: String = "") {
        work("saving") {
            api.saveCollection(subjectId, status, rate, comment)
            toast("已同步到 Bangumi 收藏")
            if (profile != null) loadCollections()
        }
    }

    fun toggleWatched(subjectId: Int, episodeId: Int) {
        val already = episodeId in progressIds
        work("progress") {
            api.setEpisodeProgress(subjectId, listOf(episodeId), if (already) 0 else 2)
            progressIds = if (already) progressIds - episodeId else progressIds + episodeId
            toast(if (already) "已标记为未看" else "已标记为已看")
        }
    }

    fun openPerson(person: BangumiPerson) {
        focusedPerson = person
        personWorks = emptyList()
        work("person") {
            val fetched = api.person(person.id, person.type)
            if (focusedPerson?.id == person.id) focusedPerson = fetched.copy(role = person.role)
        }
        work("personWorks") {
            val data = api.personSubjects(person.id, person.type)
            if (focusedPerson?.id == person.id) personWorks = data
        }
    }
    fun closePerson() { focusedPerson = null }
    fun collectPerson(person: BangumiPerson, add: Boolean) {
        work("personAction") {
            api.collectPerson(person.id, person.type, add)
            toast(if (add) "人物已收藏" else "已取消收藏")
        }
    }

    fun closeIndex() { openedIndex = null }

    fun showIndex(id: Int) {
        if (id <= 0) return
        openedIndex = null
        indexEntries = emptyList()
        work("index") {
            openedIndex = api.index(id)
            val raw = api.indexSubjects(id)
            indexEntries = raw.optJSONArray("data")?.objects().orEmpty().mapNotNull {
                (it.optJSONObject("subject") ?: it).takeIf { v -> v.optInt("id") != 0 }?.asSubject()
            }
            if (id !in rememberedIndices) rememberIndex(id)
        }
    }

    private fun rememberIndex(id: Int) {
        rememberedIndices = (listOf(id) + rememberedIndices).distinct().take(20)
        prefs.edit().putString("saved_indices", rememberedIndices.joinToString(",")).apply()
    }
    fun createIndex() {
        work("indexAction") {
            val data = api.createIndex()
            val id = data.optInt("id")
            if (id > 0) showIndex(id)
            toast(if (id > 0) "目录创建成功：#$id" else "创建请求完成，请查看返回")
        }
    }
    fun updateIndex(id: Int, title: String, description: String) {
        work("indexAction") {
            api.modifyIndex(id, title, description)
            showIndex(id)
            toast("目录已保存")
        }
    }
    fun addToIndex(indexId: Int, subjectId: Int, add: Boolean) {
        work("indexAction") {
            api.indexEntry(indexId, subjectId, add)
            showIndex(indexId)
            toast(if (add) "已加入目录" else "已从目录移除")
        }
    }
}
