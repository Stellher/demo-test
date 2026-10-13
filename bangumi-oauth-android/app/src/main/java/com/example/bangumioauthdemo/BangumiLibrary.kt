package com.example.bangumioauthdemo

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val libraryTabs = listOf(3 to "在看", 1 to "想看", 2 to "看过", 4 to "搁置", 5 to "抛弃")

@Composable
fun LibraryScreen(vm: BangumiViewModel, loggedIn: Boolean, onLogin: () -> Unit) {
    if (!loggedIn) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ScreenTitle("MY LIBRARY", "我的番剧收藏", "授权后自动同步 Bangumi 片单")
            GlassEmpty("尚未授权账号",
                "登录后可查看想看、在看、看过及搁置作品，并实时更新观看进度。",
                "前往登录", onLogin)
        }
        return
    }
    var editing by remember { mutableStateOf<UserCollection?>(null) }
    editing?.let { entry ->
        CollectionEditor(
            subjectName = entry.subject.displayName,
            initialType = entry.status,
            onDismiss = { editing = null },
            onSave = { t, rating, comment ->
                vm.saveCollection(entry.subject.id, t, rating, comment)
                editing = null
            }
        )
    }
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenTitle("MY COLLECTION", "我的收藏", "和 Bangumi 云端双向同步")
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                libraryTabs.forEach { (status, name) ->
                    GlassTag(name, selected = status == vm.collectionType,
                        onClick = { vm.setCollectionType(status) })
                }
            }
            Spacer(Modifier.height(18.dp))
            SectionTitle("收藏作品", "当前分类 ${vm.collectionTotal} 部",
                "刷新") { vm.loadCollections() }
            Spacer(Modifier.height(9.dp))
            ErrorInline(vm.notices["library"]) { vm.loadCollections() }
        }
        if ("library" in vm.loadingKeys && vm.collections.isEmpty()) item { BusyRow() }
        if (vm.collections.isEmpty() && "library" !in vm.loadingKeys) item {
            GlassEmpty("暂无收藏作品", "可以从作品详情添加到想看或在看列表",
                "去发现作品") { vm.changeTab(2) }
        }
        items(vm.collections, key = { it.subject.id }) { entry ->
            AnimeListRow(
                subject = entry.subject,
                subtitle = listOfNotNull(
                    libraryTabs.find { it.first == entry.status }?.second,
                    entry.comment.takeIf { it.isNotBlank() }
                ).joinToString(" · ").ifBlank { entry.subject.date },
                trailing = {
                    Box(Modifier
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.White.copy(alpha = .10f))
                        .clickable { editing = entry }
                        .padding(10.dp)) {
                        Icon(Icons.Default.Edit, "编辑收藏", tint = G.accent,
                            modifier = Modifier.size(20.dp))
                    }
                }
            ) { vm.openSubject(entry.subject.id) }
            VerticalDividerLine()
        }
        if (vm.collections.size < vm.collectionTotal) item {
            PrimaryGlassButton("加载更多 · ${vm.collections.size}/${vm.collectionTotal}",
                Modifier.fillMaxWidth(), enabled = "library" !in vm.loadingKeys) {
                vm.loadCollections(more = true)
            }
        }
        item {
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth().glassPanel()
                    .clickable { vm.changeTab(4) }
                    .padding(17.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CollectionsBookmark, null, tint = G.lavender)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("管理我的目录", fontWeight = FontWeight.Bold)
                    Text("建立专题 · 收藏作品 · 编辑目录", color = G.faint, fontSize = 12.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = G.faint)
            }
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
fun ProfileScreen(
    vm: BangumiViewModel,
    oauth: OAuthViewModel,
    onLogin: () -> Unit,
    onDeveloper: () -> Unit,
    onOpenLink: (String) -> Unit
) {
    val session by oauth.session.collectAsStateWithLifecycle()
    val secret by oauth.clientSecret.collectAsStateWithLifecycle()
    val status by oauth.refreshState.collectAsStateWithLifecycle()
    val busy by oauth.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSecretField by remember { mutableStateOf(false) }
    var indexId by remember { mutableStateOf("") }
    var clearConfirm by remember { mutableStateOf(false) }

    if (clearConfirm) {
        AlertDialog(
            onDismissRequest = { clearConfirm = false },
            title = { Text("退出 Bangumi 账号？") },
            text = { Text("清除本地 Token 和 App Secret。云端授权不会被自动撤销，但下次使用需重新登录。") },
            confirmButton = {
                TextButton(onClick = { oauth.clear(); clearConfirm = false }) { Text("清除本地授权") }
            },
            dismissButton = { TextButton(onClick = { clearConfirm = false }) { Text("取消") } }
        )
    }
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenTitle("ACCOUNT", "个人中心", "账号资料、目录管理和授权设置")
        }
        item {
            Row(
                Modifier.fillMaxWidth().glassPanel(RoundedCornerShape(28.dp)).padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                val pic = vm.profile?.optJSONObject("avatar")?.str("large").orEmpty()
                Box(Modifier.size(70.dp).clip(CircleShape).background(G.panel),
                    contentAlignment = Alignment.Center) {
                    if (pic.isNotBlank()) AsyncImage(pic.replace("http://", "https://"),
                        contentDescription = "头像", contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize())
                    else Icon(Icons.Default.AccountCircle, null, tint = G.faint,
                        modifier = Modifier.size(43.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(vm.profile?.str("nickname").orEmpty().ifBlank {
                        if (session != null) "Bangumi 用户" else "尚未授权"
                    }, style = MaterialTheme.typography.titleLarge)
                    Text(if (session != null)
                        "@${vm.username.ifBlank { "同步中…" }}" else "登录同步你的进度",
                        color = G.faint, fontSize = 12.sp)
                    Text(status.message, color = G.mint, fontSize = 11.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (session == null) {
            item {
                SectionTitle("首次授权", "App Secret 在本机 Android Keystore 加密保存")
                OutlinedTextField(value = secret, onValueChange = oauth::updateClientSecret,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Bangumi App Secret") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                Spacer(Modifier.height(10.dp))
                PrimaryGlassButton("使用 Bangumi 登录", Modifier.fillMaxWidth(),
                    enabled = secret.isNotBlank() && !busy, onClick = onLogin)
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f).glassPanel().padding(17.dp)) {
                        StateLabel("USER ID", session?.userId.orEmpty().ifBlank { "—" })
                    }
                    Box(Modifier.weight(1f).glassPanel().padding(17.dp)) {
                        StateLabel("授权状态", if (status.kind == "REAUTH_REQUIRED") "需重授权" else "已连接")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryGlassButton("刷新云端资料", Modifier.weight(1f)) { vm.loadProfile() }
                    GlassTag("更新 Token", onClick = oauth::refresh)
                }
                ErrorInline(vm.notices["profile"]) { vm.loadProfile() }
            }
        }
        item {
            SectionTitle("专题目录", "创建与管理属于你的作品列表")
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().glassPanel().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(indexId, onValueChange = { indexId = it },
                    label = { Text("目录 ID") }, modifier = Modifier.weight(1f),
                    singleLine = true)
                Spacer(Modifier.width(8.dp))
                PrimaryGlassButton("查看") {
                    indexId.toIntOrNull()?.let(vm::showIndex) ?: vm.toast("请输入数字目录 ID")
                }
            }
            if (session != null) {
                Spacer(Modifier.height(8.dp))
                GlassTag("＋ 创建新目录", onClick = vm::createIndex)
            }
        }
        if (vm.rememberedIndices.isNotEmpty()) {
            item { SectionTitle("最近访问的目录") }
            items(vm.rememberedIndices) { id ->
                Row(
                    Modifier.fillMaxWidth().glassPanel(RoundedCornerShape(17.dp))
                        .clickable { vm.showIndex(id) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FolderOpen, null, tint = G.accent)
                    Spacer(Modifier.width(12.dp))
                    Text("专题目录 #$id", Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Default.ChevronRight, null, tint = G.faint)
                }
            }
        }
        item {
            SectionTitle("更多功能")
            Spacer(Modifier.height(9.dp))
            MenuLink("API 实验室", "56 个官方 API · 查询、编辑与开发者检查",
                Icons.Default.Code, onDeveloper)
            Spacer(Modifier.height(9.dp))
            MenuLink("官方 Bangumi", "网页版账号与作品资料",
                Icons.Default.Public) { onOpenLink("https://bgm.tv/") }
            Spacer(Modifier.height(9.dp))
            MenuLink("开发者 API 文档", "了解 OAuth 和 API 使用规范",
                Icons.Default.MenuBook) {
                onOpenLink("https://github.com/bangumi/api")
            }
        }
        if (session != null) {
            item {
                SectionTitle("授权安全")
                Spacer(Modifier.height(10.dp))
                Text("Access Token 和 Refresh Token 已加密存储。后台每约 12 小时检查续期，前台使用时自动续期；平台撤销授权后仍需手动重新登录。",
                    color = G.faint, fontSize = 12.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(10.dp))
                GlassTag("退出账号并清理本机凭据", onClick = { clearConfirm = true })
            }
        }
        item { Spacer(Modifier.height(105.dp)) }
    }
}

@Composable
private fun MenuLink(
    title: String, caption: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().glassPanel(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = G.accent, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(caption, color = G.faint, fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = G.faint)
    }
}

@Composable
fun DirectoryDetailsPage(vm: BangumiViewModel, modifier: Modifier = Modifier) {
    val index = vm.openedIndex ?: return
    val indexId = index.optInt("id")
    var editing by remember(indexId) { mutableStateOf(false) }
    var removeSubject by remember(indexId) { mutableIntStateOf(0) }
    var addSubject by remember(indexId) { mutableStateOf(false) }

    if (editing) {
        var title by remember(indexId) { mutableStateOf(index.str("title")) }
        var description by remember(indexId) { mutableStateOf(index.str("description")) }
        AlertDialog(onDismissRequest = { editing = false },
            title = { Text("编辑专题目录") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(title, onValueChange = { title = it },
                        label = { Text("目录名称") })
                    OutlinedTextField(description, onValueChange = { description = it },
                        label = { Text("目录简介") }, minLines = 3)
                }
            },
            confirmButton = { TextButton(onClick = {
                vm.updateIndex(indexId, title, description)
                editing = false
            }) { Text("同步保存") } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("取消") } }
        )
    }
    if (addSubject) {
        var subjectId by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { addSubject = false },
            title = { Text("添加作品到目录") },
            text = { OutlinedTextField(subjectId, onValueChange = { subjectId = it },
                label = { Text("作品 Bangumi ID") }) },
            confirmButton = { TextButton(onClick = {
                subjectId.toIntOrNull()?.let { vm.addToIndex(indexId, it, true) }
                addSubject = false
            }) { Text("添加") } },
            dismissButton = { TextButton(onClick = { addSubject = false }) { Text("取消") } }
        )
    }
    if (removeSubject > 0) {
        AlertDialog(onDismissRequest = { removeSubject = 0 },
            title = { Text("从目录移除？") },
            text = { Text("此操作会同步删除目录中的关联，但不会删除 Bangumi 作品本身。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.addToIndex(indexId, removeSubject, false)
                    removeSubject = 0
                }) { Text("确认移除") }
            },
            dismissButton = { TextButton(onClick = { removeSubject = 0 }) { Text("取消") } }
        )
    }
    LazyColumn(
        modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 19.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        item {
            TextButton(onClick = { vm.openedIndex = null }) {
                Icon(Icons.Default.ArrowBack, null)
                Spacer(Modifier.width(8.dp))
                Text("返回个人中心")
            }
            Spacer(Modifier.height(9.dp))
            ScreenTitle("COLLECTION INDEX", index.str("title").ifBlank { "专题目录 #$indexId" },
                index.str("description").ifBlank { "自定义的 Bangumi 主题片单" })
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryGlassButton("编辑目录", Modifier.weight(1f)) { editing = true }
                GlassTag("＋ 添加条目", onClick = { addSubject = true })
            }
            Spacer(Modifier.height(15.dp))
            SectionTitle("目录作品", "当前列表 ${vm.indexEntries.size} 部")
            ErrorInline(vm.notices["index"]) { vm.showIndex(indexId) }
        }
        if (vm.indexEntries.isEmpty()) item {
            GlassEmpty("目录里还没有作品", "点击「添加条目」输入 Bangumi 作品 ID")
        }
        items(vm.indexEntries, key = { it.id }) { entry ->
            AnimeListRow(entry, trailing = {
                IconButton(onClick = { removeSubject = entry.id }) {
                    Icon(Icons.Default.RemoveCircleOutline, "从目录移除", tint = G.gold)
                }
            }) { vm.openedIndex = null; vm.openSubject(entry.id) }
            VerticalDividerLine()
        }
        item { Spacer(Modifier.height(55.dp)) }
    }
}
