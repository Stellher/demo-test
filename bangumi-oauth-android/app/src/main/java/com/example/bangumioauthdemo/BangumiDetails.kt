package com.example.bangumioauthdemo

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val collectionOptions = listOf(
    1 to "想看", 2 to "看过", 3 to "在看", 4 to "搁置", 5 to "抛弃"
)

@Composable
fun AnimeDetailPage(
    vm: BangumiViewModel,
    modifier: Modifier = Modifier,
    onOpenLink: (String) -> Unit
) {
    val subject = vm.detail ?: return
    var editDialog by remember(subject.id) { mutableStateOf(false) }
    var showAllChapters by remember(subject.id) { mutableStateOf(false) }
    var chapterToToggle by remember(subject.id) { mutableStateOf<EpisodeItem?>(null) }
    var addToIndex by remember(subject.id) { mutableStateOf(false) }

    if (editDialog) {
        CollectionEditor(
            subjectName = subject.displayName,
            initialType = 3,
            onDismiss = { editDialog = false },
            onSave = { status, rating, comment ->
                vm.saveCollection(subject.id, status, rating, comment)
                editDialog = false
            }
        )
    }
    chapterToToggle?.let { episode ->
        AlertDialog(
            onDismissRequest = { chapterToToggle = null },
            title = { Text(if (episode.id in vm.progressIds) "撤销已看状态？" else "标记为已看？") },
            text = { Text("将更新 Bangumi 上《${subject.displayName}》的章节进度：${episode.displayName}") },
            confirmButton = {
                TextButton(onClick = {
                    vm.toggleWatched(subject.id, episode.id)
                    chapterToToggle = null
                }) { Text("确认同步") }
            },
            dismissButton = {
                TextButton(onClick = { chapterToToggle = null }) { Text("取消") }
            }
        )
    }
    if (addToIndex) {
        var indexId by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addToIndex = false },
            title = { Text("加入 Bangumi 目录") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("填写要添加到的目录 ID，修改将实时同步到 Bangumi。")
                    OutlinedTextField(indexId, onValueChange = { indexId = it },
                        label = { Text("目录 ID") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = indexId.toIntOrNull()
                    if (parsed != null) vm.addToIndex(parsed, subject.id, true)
                    addToIndex = false
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { addToIndex = false }) { Text("取消") } }
        )
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(19.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().height(380.dp)
                .background(G.panel)
        ) {
            if (subject.image.isNotBlank()) {
                AsyncImage(subject.image, contentDescription = subject.displayName,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Box(Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0x88090D1B), Color(0x18090D1B), G.night),
                    startY = 0f, endY = 1000f
                )
            ))
            Row(
                Modifier.align(Alignment.TopStart).padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleIcon(Icons.Default.ArrowBack, "返回") { vm.closeSubject() }
                Spacer(Modifier.weight(1f))
                CircleIcon(Icons.Default.OpenInNew, "在 Bangumi 打开") {
                    onOpenLink("https://bgm.tv/subject/${subject.id}")
                }
            }
            Column(
                Modifier.align(Alignment.BottomStart).padding(horizontal = 22.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                GlassTag(categoryName(subject.type) + " · #${subject.id}")
                Text(subject.displayName, fontSize = 29.sp,
                    fontWeight = FontWeight.Bold, color = G.ink, lineHeight = 35.sp)
                if (subject.name != subject.displayName)
                    Text(subject.name, color = G.faint, fontSize = 13.sp, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (subject.score > 0) Text("★ %.1f".format(subject.score),
                        color = G.gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    if (subject.rank > 0) Text("Rank #${subject.rank}", color = G.faint)
                    if (subject.date.isNotBlank()) Text(subject.date, color = G.faint)
                }
            }
        }
        Column(
            Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryGlassButton("＋ 加入收藏", Modifier.weight(1f)) {
                    if (OAuthSessionManager.session.value == null) vm.toast("请先在「我的」页面登录")
                    else editDialog = true
                }
                Box(
                    Modifier.clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = .10f))
                        .border(1.dp, G.border, RoundedCornerShape(18.dp))
                        .clickable { addToIndex = true }
                        .padding(horizontal = 18.dp, vertical = 13.dp)
                ) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "加入目录", tint = G.ink)
                }
            }
            Row(
                Modifier.fillMaxWidth().glassPanel().padding(15.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StateLabel("Bangumi 评分", if (subject.score > 0) "%.1f / 10".format(subject.score) else "—")
                StateLabel("总章节", if (subject.episodes > 0) "${subject.episodes} 话" else "未知")
                StateLabel("正在观看", subject.collectionCount.toString())
            }
            SectionTitle("作品简介", "内容来自 Bangumi")
            Text(subject.summary.ifBlank { "暂无作品简介" },
                style = MaterialTheme.typography.bodyMedium, lineHeight = 23.sp, color = G.faint)
            if (subject.tags.isNotEmpty()) {
                SectionTitle("作品标签")
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    subject.tags.take(20).forEach { tag ->
                        GlassTag("# $tag", onClick = {
                            vm.updateSearchQuery(tag)
                            vm.changeTab(2)
                            vm.submitSearch()
                            vm.closeSubject()
                        })
                    }
                }
            }
            SectionTitle("章节进度", "直接点击章节更改「已看」状态")
            ErrorInline(vm.notices["chapters"]) { vm.openSubject(subject.id) }
            if ("chapters" in vm.loadingKeys && vm.chapters.isEmpty()) BusyRow()
            else if (vm.chapters.isEmpty()) Text("暂无章节信息", color = G.faint)
            val chapters = if (showAllChapters) vm.chapters else vm.chapters.take(16)
            chapters.forEachIndexed { idx, ep ->
                val isWatched = ep.id in vm.progressIds
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            if (OAuthSessionManager.session.value != null) chapterToToggle = ep
                            else vm.toast("请先在「我的」页面授权后修改进度")
                        }
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(13.dp))
                            .background(
                                if (isWatched) G.mint.copy(alpha = .20f)
                                else Color.White.copy(alpha = .07f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isWatched) Icon(Icons.Default.Done, null, tint = G.mint)
                        else Text("${idx + 1}", color = G.faint, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ep.displayName.ifBlank { "Episode ${ep.sort}" },
                            color = G.ink, fontWeight = FontWeight.Medium, maxLines = 2)
                        Text(listOf(ep.airDate, ep.duration).filter { it.isNotBlank() }.joinToString(" · ")
                            .ifBlank { "EP ${ep.sort}" }, color = G.faint, fontSize = 11.sp)
                    }
                    GlassTag(if (isWatched) "已看" else "未看", selected = isWatched)
                }
                VerticalDividerLine()
            }
            if (vm.chapters.size > 16) {
                GlassTag(if (showAllChapters) "收起章节" else "查看全部 ${vm.chapters.size} 话",
                    onClick = { showAllChapters = !showAllChapters })
            }
            PeopleStrip("登场角色", vm.characters) { vm.openPerson(it) }
            PeopleStrip("制作与工作人员", vm.staff) { vm.openPerson(it) }
            if (vm.related.isNotEmpty()) {
                SectionTitle("关联作品", "续作 · 前传 · 系列作品")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(vm.related.take(18), key = { it.id }) { item ->
                        AnimePosterTile(item, Modifier.width(118.dp)) {
                            vm.openSubject(item.id)
                        }
                    }
                }
            }
            ErrorInline(vm.notices["detail"])
            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
private fun PeopleStrip(
    title: String, people: List<BangumiPerson>,
    onTap: (BangumiPerson) -> Unit
) {
    if (people.isEmpty()) return
    SectionTitle(title, "点击查看人物资料与关联作品")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        items(people.take(24), key = { "${it.type}${it.id}" }) { person ->
            Column(
                Modifier.width(92.dp).clickable { onTap(person) },
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                SmallPoster(person.image, person.name,
                    Modifier.size(92.dp).clip(RoundedCornerShape(17.dp)))
                Text(person.name, color = G.ink,
                    fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                if (person.role.isNotBlank()) Text(person.role,
                    color = G.faint, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun CircleIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        Modifier.size(43.dp)
            .clip(CircleShape)
            .background(G.night.copy(alpha = .72f))
            .border(1.dp, G.border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, description, tint = G.ink, modifier = Modifier.size(22.dp)) }
}

@Composable
fun CollectionEditor(
    subjectName: String,
    initialType: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int, String) -> Unit
) {
    var type by remember(subjectName) { mutableIntStateOf(initialType) }
    var rate by remember(subjectName) { mutableIntStateOf(0) }
    var comment by remember(subjectName) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("我的收藏 · $subjectName", maxLines = 2) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("观看状态", color = G.faint)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    collectionOptions.forEach { (id, name) ->
                        GlassTag(name, selected = type == id, onClick = { type = id })
                    }
                }
                Text("我的评分：${rate}/10", color = G.faint)
                Slider(
                    value = rate.toFloat(),
                    onValueChange = { rate = it.toInt() },
                    valueRange = 0f..10f, steps = 9,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("短评 / 观看笔记") },
                    placeholder = { Text("这部作品给你的感受…") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5
                )
                Text("提交后会修改 Bangumi 云端收藏。", color = G.gold, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(type, rate, comment) }) { Text("保存到 Bangumi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

fun categoryName(type: Int): String = when(type) {
    1 -> "书籍"
    2 -> "动画"
    3 -> "音乐"
    4 -> "游戏"
    6 -> "三次元"
    else -> "作品"
}

@Composable
fun PersonDetailsPage(vm: BangumiViewModel, modifier: Modifier = Modifier) {
    val person = vm.focusedPerson ?: return
    Column(
        modifier.verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        TextButton(onClick = vm::closePerson) {
            Icon(Icons.Default.ArrowBack, null)
            Spacer(Modifier.width(8.dp))
            Text("返回作品")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallPoster(person.image, person.name, Modifier.width(132.dp).height(165.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (person.type == "character") "CHARACTER" else "CREATOR",
                    color = G.mint, fontSize = 11.sp, letterSpacing = 2.sp)
                Text(person.name, style = MaterialTheme.typography.headlineMedium)
                Text(person.role.ifBlank { if (person.type == "character") "角色资料" else "人物资料" },
                    color = G.faint)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryGlassButton("＋ 收藏人物", Modifier.weight(1f),
                enabled = OAuthSessionManager.session.value != null) {
                vm.collectPerson(person, true)
            }
            GlassTag("取消收藏", onClick = { vm.collectPerson(person, false) })
        }
        SectionTitle("人物介绍")
        Text(person.summary.ifBlank { "暂无简介" },
            color = G.faint, fontSize = 14.sp, lineHeight = 22.sp)
        ErrorInline(vm.notices["person"])
        SectionTitle("关联作品", "与该人物相关的动画、音乐、游戏及其它条目")
        if ("personWorks" in vm.loadingKeys) BusyRow()
        vm.personWorks.forEach { item ->
            AnimeListRow(item) {
                vm.closePerson()
                vm.openSubject(item.id)
            }
            VerticalDividerLine()
        }
        Spacer(Modifier.height(50.dp))
    }
}
