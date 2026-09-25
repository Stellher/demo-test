package com.example.logvarremote.data.api

object LogVarApiCatalog {
    val endpoints = listOf(
        ApiEndpoint("health", "Runtime", "运行状态", ApiHttpMethod.GET, "/__health", tokenRequired = false, description = "Android Node Runtime 健康检查"),
        ApiEndpoint("shutdown", "Runtime", "关闭 Node", ApiHttpMethod.POST, "/__shutdown", description = "优雅关闭本地 Node 进程"),
        ApiEndpoint("accessGet", "Runtime", "访问控制状态", ApiHttpMethod.GET, "/__access-control"),
        ApiEndpoint("accessPatch", "Runtime", "修改访问控制", ApiHttpMethod.PATCH, "/__access-control", defaultBody = """{"mode":"off"}"""),
        ApiEndpoint("prepare", "Runtime", "预取弹幕 XML", ApiHttpMethod.GET, "/__danmaku/prepare", defaultQuery = "url=https%3A%2F%2Fexample.com%2Fvideo"),
        ApiEndpoint("prepared", "Runtime", "读取预取 XML", ApiHttpMethod.GET, "/__danmaku/prepared/{id}.xml", description = "把 {id} 替换为 prepare 返回的 id"),

        ApiEndpoint("searchAnime", "业务", "搜索动漫/影视", ApiHttpMethod.GET, "/api/v2/search/anime", "keyword=凡人修仙传"),
        ApiEndpoint("searchEpisodes", "业务", "搜索指定集", ApiHttpMethod.GET, "/api/v2/search/episodes", "anime=凡人修仙传&episode=1"),
        ApiEndpoint("match", "业务", "文件名匹配", ApiHttpMethod.POST, "/api/v2/match", defaultBody = """{"fileName":"凡人修仙传 S01E01"}"""),
        ApiEndpoint("bangumi", "业务", "番剧详情", ApiHttpMethod.GET, "/api/v2/bangumi/{animeId}", description = "把 {animeId} 替换为搜索返回 ID"),
        ApiEndpoint("commentId", "业务", "按 commentId 获取弹幕", ApiHttpMethod.GET, "/api/v2/comment/{commentId}", "format=json&duration=true", description = "支持 format、duration、segmentflag；Android Runtime 额外支持 offset/offsetMs/fontSize"),
        ApiEndpoint("commentUrl", "业务", "按播放 URL 获取弹幕", ApiHttpMethod.GET, "/api/v2/comment", "url=https%3A%2F%2Fexample.com%2Fvideo&format=json"),
        ApiEndpoint("extComment", "业务", "扩展 URL 弹幕", ApiHttpMethod.GET, "/api/v2/extcomment", "url=https%3A%2F%2Fexample.com%2Fvideo&format=json"),
        ApiEndpoint("segmentComment", "业务", "分片弹幕", ApiHttpMethod.POST, "/api/v2/segmentcomment", "format=json", """{"url":"https://example.com/video","format":"json"}"""),
        ApiEndpoint("fongmiGet", "兼容", "FongMi 弹幕 GET", ApiHttpMethod.GET, "/api/v2/fongmi/danmaku", "name=凡人修仙传&episode=1"),
        ApiEndpoint("fongmiPost", "兼容", "FongMi 弹幕 POST", ApiHttpMethod.POST, "/api/v2/fongmi/danmaku", defaultBody = """{"name":"凡人修仙传","episode":"1"}"""),
        ApiEndpoint("danmakuGet", "兼容", "短地址弹幕 GET", ApiHttpMethod.GET, "/danmaku", "name=凡人修仙传&episode=1"),
        ApiEndpoint("danmakuPost", "兼容", "短地址弹幕 POST", ApiHttpMethod.POST, "/danmaku", defaultBody = """{"name":"凡人修仙传","episode":"1"}"""),

        ApiEndpoint("favoriteList", "收藏", "收藏列表", ApiHttpMethod.GET, "/api/v2/favorite/list"),
        ApiEndpoint("favoriteAdd", "收藏", "添加收藏", ApiHttpMethod.POST, "/api/v2/favorite/add", defaultBody = """{"keyword":"凡人修仙传"}"""),
        ApiEndpoint("favoriteRefresh", "收藏", "刷新收藏", ApiHttpMethod.POST, "/api/v2/favorite/refresh", defaultBody = """{"keyword":"凡人修仙传"}"""),
        ApiEndpoint("favoriteSchedule", "收藏", "定时刷新", ApiHttpMethod.POST, "/api/v2/favorite/schedule", defaultBody = """{"keyword":"凡人修仙传","schedule":"0 */6 * * *"}""", description = "schedule=null 可关闭；Node 部署支持"),
        ApiEndpoint("favoriteRemove", "收藏", "删除收藏", ApiHttpMethod.POST, "/api/v2/favorite/remove", defaultBody = """{"keyword":"凡人修仙传"}"""),

        ApiEndpoint("localList", "本地弹幕", "本地弹幕列表", ApiHttpMethod.GET, "/api/local-danmu/list"),
        ApiEndpoint("localGet", "本地弹幕", "本地弹幕详情", ApiHttpMethod.GET, "/api/local-danmu/{resourceKey}", description = "把 {resourceKey} 替换为列表返回值"),
        ApiEndpoint("localPatch", "本地弹幕", "修改本地弹幕元数据", ApiHttpMethod.PATCH, "/api/local-danmu/{resourceKey}", defaultBody = """{"scope":"resource","episode":1}"""),
        ApiEndpoint("localDelete", "本地弹幕", "删除本地弹幕", ApiHttpMethod.DELETE, "/api/local-danmu/{resourceKey}"),
        ApiEndpoint("localUpload", "本地弹幕", "上传本地弹幕", ApiHttpMethod.POST, "/api/local-danmu/upload", description = "multipart/form-data：file、title、year、type(tv/movie)、season、episode；请在本地 Web 面板选择文件上传", multipartOnly = true),

        ApiEndpoint("config", "系统", "配置", ApiHttpMethod.GET, "/api/config"),
        ApiEndpoint("logs", "系统", "日志", ApiHttpMethod.GET, "/api/logs"),
        ApiEndpoint("logsClear", "系统", "清空日志", ApiHttpMethod.POST, "/api/logs/clear"),
        ApiEndpoint("reqrecords", "系统", "请求记录", ApiHttpMethod.GET, "/api/reqrecords"),
        ApiEndpoint("cacheAnimes", "系统", "搜索缓存", ApiHttpMethod.GET, "/api/cache/animes"),
        ApiEndpoint("cacheClear", "系统", "清理缓存", ApiHttpMethod.POST, "/api/cache/clear", defaultBody = """{"items":["animes","episodeIds","searchCache","commentCache","requestHistory","bangumiData"]}"""),
        ApiEndpoint("envSet", "系统", "设置环境变量", ApiHttpMethod.POST, "/api/env/set", defaultBody = """{"key":"LOG_LEVEL","value":"debug"}"""),
        ApiEndpoint("envAdd", "系统", "添加环境变量", ApiHttpMethod.POST, "/api/env/add", defaultBody = """{"key":"EXAMPLE_KEY","value":"example"}"""),
        ApiEndpoint("envDel", "系统", "删除环境变量", ApiHttpMethod.POST, "/api/env/del", defaultBody = """{"key":"EXAMPLE_KEY"}"""),
        ApiEndpoint("deploy", "系统", "重新部署/应用配置", ApiHttpMethod.POST, "/api/deploy", description = "Node 模式返回配置自动生效"),

        ApiEndpoint("cookieStatus", "Cookie", "Cookie 状态", ApiHttpMethod.GET, "/api/cookie/status"),
        ApiEndpoint("cookieQrGenerate", "Cookie", "生成登录二维码", ApiHttpMethod.POST, "/api/cookie/qr/generate"),
        ApiEndpoint("cookieQrCheck", "Cookie", "检查扫码状态", ApiHttpMethod.POST, "/api/cookie/qr/check", defaultBody = """{}"""),
        ApiEndpoint("cookieVerify", "Cookie", "验证 Cookie", ApiHttpMethod.POST, "/api/cookie/verify", defaultBody = """{"cookie":"SESSDATA=..."}"""),
        ApiEndpoint("cookieSave", "Cookie", "保存 Cookie", ApiHttpMethod.POST, "/api/cookie/save", defaultBody = """{"cookie":"SESSDATA=..."}"""),

        ApiEndpoint("aiVerify", "AI", "AI 连通性验证", ApiHttpMethod.POST, "/api/ai/verify", defaultBody = """{"aiBaseUrl":"https://api.openai.com/v1","aiModel":"gpt-4o","aiApiKey":""}"""),
        ApiEndpoint("forwardTrace", "调试", "Forward Trace", ApiHttpMethod.POST, "/api/debug/forward-trace", defaultBody = """{"handler":"demo","level":"info","status":"ok","message":"Android debug trace"}"""),

        ApiEndpoint("proxy", "代理", "本地代理端口", ApiHttpMethod.GET, "/", "url=https%3A%2F%2Fexample.com", port = 19322, tokenRequired = false, description = "DANMU_API_PROXY_PORT，本 Demo 为 19322")
    )

    fun byId(id: String): ApiEndpoint = endpoints.firstOrNull { it.id == id } ?: endpoints.first()
}
