package com.example.bangumioauthdemo

import androidx.activity.compose.BackHandler
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val appTabs = listOf("放送", "季度", "发现", "收藏", "我的")
private val tabIcons: List<ImageVector> = listOf(
    Icons.Default.Today, Icons.Default.GridView, Icons.Default.Explore,
    Icons.Default.Bookmarks, Icons.Default.AccountCircle
)

@Composable
fun BangumiApp(
    oauth: OAuthViewModel,
    explorer: ApiExplorerViewModel,
    openUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val vm: BangumiViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val token by oauth.session.collectAsStateWithLifecycle()
    var developer by remember { mutableStateOf(false) }
    val backdrop = rememberLayerBackdrop()

    LaunchedEffect(token?.accessToken) {
        if (token != null) vm.loadProfile()
        else vm.clearAccountData()
    }
    BackHandler(enabled = developer || vm.focusedPerson != null ||
        vm.detail != null || vm.openedIndex != null) {
        when {
            developer -> developer = false
            vm.focusedPerson != null -> vm.closePerson()
            vm.detail != null -> vm.closeSubject()
            vm.openedIndex != null -> vm.closeIndex()
        }
    }

    if (developer) {
        Box(Modifier.fillMaxSize().background(G.night)) {
            DeveloperGateway(oauth, explorer, openUrl)
            GlassTag("← 返回 Bangumi", modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding().padding(12.dp), onClick = { developer = false })
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            AuroraBackground(Modifier.fillMaxSize())
            when {
                vm.focusedPerson != null ->
                    PersonDetailsPage(vm, Modifier.fillMaxSize())
                vm.detail != null ->
                    AnimeDetailPage(vm, Modifier.fillMaxSize(), onOpenLink = openUrl)
                vm.openedIndex != null ->
                    DirectoryDetailsPage(vm, Modifier.fillMaxSize())
                else -> when (vm.activeTab) {
                    0 -> TodayScreen(vm)
                    1 -> SeasonScreen(vm)
                    2 -> DiscoverScreen(vm)
                    3 -> LibraryScreen(vm, token != null, onLogin = { vm.changeTab(4) })
                    else -> ProfileScreen(vm, oauth, onLogin = {
                        openUrl(oauth.beginLogin())
                    }, onDeveloper = { developer = true }, onOpenLink = openUrl)
                }
            }
        }
        if (vm.focusedPerson == null && vm.detail == null && vm.openedIndex == null) {
            KyantGlass(
                backdrop = backdrop,
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .height(66.dp)
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    appTabs.forEachIndexed { index, name ->
                        val selected = vm.activeTab == index
                        Column(
                            Modifier.weight(1f).fillMaxHeight()
                                .clip(RoundedCornerShape(25.dp))
                                .background(if (selected) Color.White.copy(alpha = .17f) else Color.Transparent)
                                .clickable { vm.changeTab(index) }
                                .padding(vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                tabIcons[index], contentDescription = name,
                                tint = if (selected) G.ink else G.faint,
                                modifier = Modifier.size(23.dp)
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(name, fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) G.ink else G.faint)
                        }
                    }
                }
            }
        }
        vm.snackbar?.let { text ->
            LaunchedEffect(text) {
                kotlinx.coroutines.delay(2700)
                vm.clearToast()
            }
            Box(
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(18.dp)
                    .glassPanel(RoundedCornerShape(18.dp), opacity = .22f)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) { Text(text, color = G.ink, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun TodayScreen(vm: BangumiViewModel) {
    val day = vm.calendar.firstOrNull { it.weekday == vm.selectedDay }
    val today = LocalDate.now()
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("BANGUMI · GLASS", color = G.mint,
                        fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 11.sp)
                    Spacer(Modifier.height(9.dp))
                    Text("今天看什么？", style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(5.dp))
                    Text(today.format(DateTimeFormatter.ofPattern("yyyy年M月d日 · EEEE")),
                        style = MaterialTheme.typography.bodySmall)
                }
                Box(
                    Modifier.size(50.dp).glassPanel(RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.WbSunny, null, tint = G.gold, modifier = Modifier.size(24.dp)) }
            }
        }
        item {
            val featured = day?.subjects?.firstOrNull()
                ?: vm.homeHighlights.firstOrNull()
            if (featured != null) FeatureHero(featured) { vm.openSubject(featured.id) }
            else {
                ErrorInline(vm.notices["home"]) { vm.refreshHome() }
                if ("home" in vm.loadingKeys || "highlights" in vm.loadingKeys) BusyRow()
            }
        }
        item {
            SectionTitle("每日放送", "跟随 Bangumi 官方更新 · 选择星期",
                "刷新", vm::refreshHome)
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                (1..7).forEach { i ->
                    Column(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(19.dp))
                            .background(if (vm.selectedDay == i) G.accent.copy(alpha = .24f)
                                else Color.White.copy(alpha = .06f))
                            .border(1.dp,
                                if (vm.selectedDay == i) G.accent.copy(alpha = .65f) else G.border,
                                RoundedCornerShape(19.dp))
                            .clickable { vm.selectDay(i) }
                            .padding(vertical = 13.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(listOf("一","二","三","四","五","六","日")[i-1],
                            fontSize = 12.sp, color = if (vm.selectedDay == i) G.accent else G.faint)
                        Spacer(Modifier.height(5.dp))
                        Text(if (i == today.dayOfWeek.value) "今" else "•",
                            color = if (vm.selectedDay == i) G.ink else G.subdued,
                            fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
        if (day?.subjects.isNullOrEmpty()) {
            item {
                GlassEmpty(
                    if ("home" in vm.loadingKeys) "正在更新放送表" else "当天暂无放送数据",
                    "每日放送由 Bangumi /calendar 接口提供",
                    "重新加载", vm::refreshHome
                )
            }
        } else {
            item {
                SectionTitle("${day?.title ?: "今日"}的节目",
                    "共 ${day?.subjects?.size ?: 0} 部 · 点开查看详情")
            }
            items(day!!.subjects.take(25), key = { "today-${it.id}" }) { subject ->
                AnimeListRow(subject, subtitle = if (subject.date.isNotBlank())
                    "放送日期 · ${subject.date}" else "每日放送") {
                    vm.openSubject(subject.id)
                }
                VerticalDividerLine()
            }
        }
        item {
            SectionTitle("高分推荐", "按官方评分排名浏览更多作品",
                "换一批") { vm.refreshHome() }
        }
        item {
            val list = vm.homeHighlights
            if (list.isEmpty()) BusyRow()
            else LazyRow(
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                contentPadding = PaddingValues(end = 14.dp)
            ) {
                items(list, key = { it.id }) { item ->
                    AnimePosterTile(item, Modifier.width(138.dp)) { vm.openSubject(item.id) }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().glassPanel().clickable { vm.changeTab(1) }.padding(17.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = G.lavender)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("探索本季度新番", fontWeight = FontWeight.Bold)
                    Text("追番清单 · 口碑排行 · 新作速览", color = G.faint, fontSize = 12.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = G.faint)
            }
        }
    }
}

@Composable
private fun FeatureHero(item: AnimeSubject, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(300.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(G.panel).clickable(onClick = onClick)
    ) {
        if (item.image.isNotBlank()) {
            AsyncImage(model = item.image, contentDescription = item.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize())
        }
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to Color(0x33091019), .35f to Color(0x4410182A),
                1f to Color(0xF5090D1B)
            )
        ))
        Column(
            Modifier.align(Alignment.BottomStart).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            GlassTag("✦ 今日精选")
            Text(item.displayName, style = MaterialTheme.typography.headlineMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (item.score > 0) Text("★ %.1f".format(item.score), color = G.gold,
                    fontWeight = FontWeight.Bold)
                Text(item.date, color = G.faint, fontSize = 12.sp)
                Text("了解作品  →", color = G.ink, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private val typeItems = listOf(2 to "动画", 1 to "书籍", 3 to "音乐", 4 to "游戏", 6 to "三次元")
private val quarterNames = listOf("冬 · 1月", "春 · 4月", "夏 · 7月", "秋 · 10月")

@Composable
private fun SeasonScreen(vm: BangumiViewModel) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 23.dp)) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ScreenTitle("SEASONAL LIBRARY", "追新番 · 看季度", "按 Bangumi 作品年月筛选，展示真实条目")
            Row(
                Modifier.fillMaxWidth().glassPanel().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.setSeason(year = vm.seasonYear - 1) }) {
                    Icon(Icons.Default.ChevronLeft, "上一年", tint = G.ink)
                }
                Text("${vm.seasonYear} 年", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { vm.setSeason(year = vm.seasonYear + 1) }) {
                    Icon(Icons.Default.ChevronRight, "下一年", tint = G.ink)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                quarterNames.forEachIndexed { idx, item ->
                    Box(Modifier.weight(1f)) {
                        GlassTag(item, selected = vm.seasonQuarter == idx + 1,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { vm.setSeason(quarter = idx + 1) })
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                typeItems.forEach { (type, name) ->
                    GlassTag(name, selected = vm.seasonType == type,
                        onClick = { vm.setSeason(type = type) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${vm.seasonYear} · ${quarterNames[vm.seasonQuarter - 1]}",
                        style = MaterialTheme.typography.titleMedium)
                    Text("官方筛选结果 · 共 ${vm.seasonTotal} 项", color = G.faint, fontSize = 12.sp)
                }
                GlassTag("热度/评分", selected = vm.seasonSort == "rank",
                    onClick = { vm.setSeason(sort = "rank") })
                Spacer(Modifier.width(7.dp))
                GlassTag("新作", selected = vm.seasonSort == "date",
                    onClick = { vm.setSeason(sort = "date") })
            }
            ErrorInline(vm.notices["season"], retry = { vm.loadSeason() })
        }
        Spacer(Modifier.height(14.dp))
        if ("season" in vm.loadingKeys && vm.seasonItems.isEmpty()) BusyRow()
        if (vm.seasonItems.isEmpty() && "season" !in vm.loadingKeys) {
            GlassEmpty("本季度暂无条目", "可切换其它年份、季度或作品类型")
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 17.dp, end = 17.dp, top = 8.dp, bottom = 114.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(vm.seasonItems, key = { it.id }) { item ->
                AnimePosterTile(item, Modifier.fillMaxWidth().padding(horizontal = 1.dp)) {
                    vm.openSubject(item.id)
                }
            }
            if (vm.seasonItems.size < vm.seasonTotal) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                    PrimaryGlassButton(
                        if ("season" in vm.loadingKeys) "正在加载…" else "加载更多季度作品",
                        Modifier.fillMaxWidth(), enabled = "season" !in vm.loadingKeys
                    ) { vm.loadSeason(more = true) }
                }
            }
        }
    }
}

@Composable
private fun DiscoverScreen(vm: BangumiViewModel) {
    LaunchedEffect(Unit) { if (vm.discoveryItems.isEmpty()) vm.submitSearch() }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 22.dp)) {
        Column(Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)) {
            ScreenTitle("DISCOVER", "发现好作品", "搜索动画、小说、音乐、游戏和三次元作品")
            Row(
                Modifier.fillMaxWidth().glassPanel(RoundedCornerShape(20.dp))
                    .padding(horizontal = 15.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = G.accent, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = vm.discoveryQuery,
                    onValueChange = vm::updateSearchQuery,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = G.ink, fontSize = 15.sp
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { vm.submitSearch() }),
                    decorationBox = { inner ->
                        Box {
                            if (vm.discoveryQuery.isBlank()) Text("名称 / 关键词 / 作品标签",
                                color = G.subdued, fontSize = 14.sp)
                            inner()
                        }
                    }
                )
                Text("搜索", color = G.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.clickable { vm.submitSearch() }.padding(4.dp))
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassTag("全部", selected = vm.discoveryType == 0, onClick = { vm.setDiscoveryType(0) })
                typeItems.forEach { (type,name) ->
                    GlassTag(name, selected = vm.discoveryType == type, onClick = { vm.setDiscoveryType(type) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("结果 · ${vm.discoveryTotal} 项", Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium)
                GlassTag("相关", selected = vm.discoverySort == "match", onClick = { vm.setDiscoverySort("match") })
                Spacer(Modifier.width(7.dp))
                GlassTag("评分", selected = vm.discoverySort == "score", onClick = { vm.setDiscoverySort("score") })
                Spacer(Modifier.width(7.dp))
                GlassTag("热度", selected = vm.discoverySort == "heat", onClick = { vm.setDiscoverySort("heat") })
            }
            ErrorInline(vm.notices["search"]) { vm.submitSearch() }
        }
        Spacer(Modifier.height(15.dp))
        if ("search" in vm.loadingKeys && vm.discoveryItems.isEmpty()) BusyRow()
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 118.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(vm.discoveryItems, key = { it.id }) { item ->
                AnimePosterTile(item) { vm.openSubject(item.id) }
            }
            if (vm.discoveryItems.size < vm.discoveryTotal) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                    PrimaryGlassButton("继续加载", Modifier.fillMaxWidth(),
                        enabled = "search" !in vm.loadingKeys) { vm.submitSearch(more = true) }
                }
            }
        }
    }
}
