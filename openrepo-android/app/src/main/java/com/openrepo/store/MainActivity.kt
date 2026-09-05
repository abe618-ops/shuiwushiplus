package com.openrepo.store

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                StoreScreen { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            }
        }
    }
}

data class CatalogApp(
    val name: String,
    val repo: String,
    val description: String,
    val version: String,
    val apkName: String,
    val apkSize: Long,
    val downloadUrl: String,
    val releaseUrl: String,
    val stars: Int,
    val topics: List<String>,
    val semanticTags: List<String>,
    val discoveryTerms: List<String>,
    val searchText: String,
    val prerelease: Boolean
)

private const val CATALOG_URL = "https://raw.githubusercontent.com/abe618-ops/shuiwushiplus/openrepo-store-v1/data/openrepo-android-index.json"

private val http = OkHttpClient.Builder()
    .connectTimeout(12, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .callTimeout(25, TimeUnit.SECONDS)
    .build()

private val conceptMap = linkedMapOf(
    "视频下载" to listOf("video downloader", "media downloader", "yt-dlp", "youtube downloader"),
    "下载器" to listOf("downloader", "download manager", "media downloader"),
    "剪贴板" to listOf("clipboard", "clipboard sync", "cross device clipboard"),
    "RSS" to listOf("rss", "feed reader", "news reader"),
    "阅读器" to listOf("reader", "ebook", "feed reader"),
    "浏览器" to listOf("browser", "web browser"),
    "广告拦截" to listOf("ad blocker", "adblock", "content blocker"),
    "密码" to listOf("password manager", "vault"),
    "笔记" to listOf("notes", "markdown notes", "note taking"),
    "音乐" to listOf("music player", "audio player", "music"),
    "文件管理" to listOf("file manager", "file explorer"),
    "终端" to listOf("terminal", "ssh", "shell"),
    "翻译" to listOf("translator", "translation"),
    "OCR" to listOf("ocr", "scanner", "text recognition"),
    "PDF" to listOf("pdf reader", "pdf"),
    "相机" to listOf("camera", "camera app"),
    "语音" to listOf("speech", "voice", "speech to text"),
    "输入法" to listOf("keyboard", "input method", "ime")
)

private fun expandedTokens(raw: String): List<String> {
    val q = raw.trim().lowercase()
    val values = linkedSetOf<String>()
    if (q.isNotBlank()) values += q
    conceptMap.forEach { (zh, en) ->
        if (q.contains(zh.lowercase())) {
            values += zh.lowercase()
            values += en.map { it.lowercase() }
        }
    }
    return values.flatMap { it.split(Regex("[^\\p{L}\\p{N}+-]+")) }
        .filter { it.length >= 2 }
        .distinct()
}

private suspend fun loadCatalog(): Pair<List<CatalogApp>, String> = withContext(Dispatchers.IO) {
    val req = Request.Builder()
        .url(CATALOG_URL + "?t=" + System.currentTimeMillis())
        .header("User-Agent", "OpenRepo-Store/0.4")
        .build()
    http.newCall(req).execute().use { res ->
        if (!res.isSuccessful) error("目录 HTTP ${res.code}")
        val root = JSONObject(res.body?.string().orEmpty())
        val appsJson = root.optJSONArray("apps")
        val apps = buildList {
            if (appsJson != null) {
                for (i in 0 until appsJson.length()) {
                    val o = appsJson.getJSONObject(i)
                    fun strings(key: String): List<String> {
                        val a = o.optJSONArray(key) ?: return emptyList()
                        return buildList { for (j in 0 until a.length()) add(a.optString(j)) }
                    }
                    add(
                        CatalogApp(
                            name = o.optString("name"),
                            repo = o.optString("repo"),
                            description = o.optString("description"),
                            version = o.optString("version"),
                            apkName = o.optString("apkName"),
                            apkSize = o.optLong("apkSize"),
                            downloadUrl = o.optString("downloadUrl"),
                            releaseUrl = o.optString("releaseUrl"),
                            stars = o.optInt("stars"),
                            topics = strings("topics"),
                            semanticTags = strings("semanticTags"),
                            discoveryTerms = strings("discoveryTerms"),
                            searchText = o.optString("searchText"),
                            prerelease = o.optBoolean("prerelease")
                        )
                    )
                }
            }
        }
        apps to root.optString("generatedAt")
    }
}

private fun localSearch(catalog: List<CatalogApp>, rawQuery: String): List<CatalogApp> {
    val q = rawQuery.trim().lowercase()
    if (q.isBlank()) return catalog.take(30)
    val tokens = expandedTokens(rawQuery)
    return catalog.map { app ->
        val name = app.name.lowercase()
        val repo = app.repo.lowercase()
        val desc = app.description.lowercase()
        val tags = app.semanticTags.joinToString(" ").lowercase()
        val topics = app.topics.joinToString(" ").lowercase()
        val terms = app.discoveryTerms.joinToString(" ").lowercase()
        val all = (app.searchText + " " + name + " " + repo + " " + desc + " " + tags + " " + topics + " " + terms).lowercase()
        var score = 0.0
        if (name.contains(q) || repo.contains(q)) score += 90
        if (tags.contains(q)) score += 80
        if (desc.contains(q)) score += 55
        if (all.contains(q)) score += 45
        tokens.forEach { t ->
            if (name.contains(t)) score += 20
            if (tags.contains(t)) score += 18
            if (topics.contains(t)) score += 14
            if (desc.contains(t)) score += 10
            if (terms.contains(t)) score += 8
        }
        score += kotlin.math.ln(app.stars.toDouble() + 1.0)
        app to score
    }.filter { it.second > 0 }
        .sortedByDescending { it.second }
        .map { it.first }
        .take(40)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(onOpen: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var catalog by remember { mutableStateOf<List<CatalogApp>>(emptyList()) }
    var results by remember { mutableStateOf<List<CatalogApp>>(emptyList()) }
    var generatedAt by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("正在加载 OpenRepo APK 索引…") }
    val scope = rememberCoroutineScope()

    fun refreshCatalog(autoSearch: Boolean = false) {
        if (loading) return
        loading = true
        scope.launch {
            try {
                val loaded = loadCatalog()
                catalog = loaded.first
                generatedAt = loaded.second
                results = if (autoSearch && query.isNotBlank()) localSearch(catalog, query) else loaded.first.take(20)
                message = "索引已加载 ${loaded.first.size} 个可安装 APK 项目；搜索不消耗 GitHub API 额度。"
            } catch (e: Exception) {
                message = "索引加载失败：${e.message ?: "网络异常"}"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { refreshCatalog(false) }

    fun search() {
        val q = query.trim()
        if (q.isBlank()) {
            results = catalog.take(20)
            message = "显示索引中的推荐项目"
            return
        }
        val found = localSearch(catalog, q)
        results = found
        message = if (found.isEmpty()) {
            "本地索引暂未命中“$q”。索引器每 6 小时自动扩充，不会触发你的 GitHub API 限流。"
        } else {
            "命中 ${found.size} 个项目 · 本地语义搜索 · 0 次 GitHub API 请求"
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("OpenRepo Store 0.4") }) }) { padding ->
        Column(Modifier.padding(padding).padding(14.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("搜索功能、项目或用途（支持中英文）") },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { search() }, enabled = !loading) { Text("索引检索") }
                OutlinedButton(onClick = { refreshCatalog(true) }, enabled = !loading) { Text("刷新索引") }
            }
            Spacer(Modifier.height(8.dp))
            AssistChip(onClick = {}, label = { Text("无需 GitHub 登录 · 静态 APK 索引") })
            Text(message, style = MaterialTheme.typography.bodySmall)
            if (generatedAt.isNotBlank()) Text("索引时间：$generatedAt", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(10.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(results) { app ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(app.name, style = MaterialTheme.typography.titleMedium)
                            Text(app.repo, style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(5.dp))
                            Text(app.description.ifBlank { "GitHub 开源 Android 项目" })
                            if (app.semanticTags.isNotEmpty()) {
                                Spacer(Modifier.height(5.dp))
                                Text("关联：${app.semanticTags.take(5).joinToString(" · ")}", style = MaterialTheme.typography.labelMedium)
                            }
                            Text("${app.version} · ${formatBytes(app.apkSize)}${if (app.prerelease) " · 预发布" else ""}", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            Text("来源：原 GitHub Release。OpenRepo 仅建立索引，不镜像 APK。", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(9.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onOpen(app.downloadUrl) }) { Text("下载 APK") }
                                OutlinedButton(onClick = { onOpen(app.releaseUrl) }) { Text("Release") }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
    bytes >= 1024L -> String.format("%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
