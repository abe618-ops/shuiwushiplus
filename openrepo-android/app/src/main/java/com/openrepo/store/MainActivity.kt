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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
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

data class RepoCandidate(
    val name: String,
    val fullName: String,
    val description: String,
    val stars: Int,
    val topics: List<String>,
    val htmlUrl: String,
    val updatedAt: String,
    var baseScore: Double = 0.0
)

data class StoreApp(
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
    val score: Double,
    val reason: String
)

private val http = OkHttpClient.Builder()
    .connectTimeout(12, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .callTimeout(25, TimeUnit.SECONDS)
    .build()

private val conceptMap = linkedMapOf(
    "视频下载" to listOf("video downloader", "media downloader", "yt-dlp", "stream downloader"),
    "下载器" to listOf("downloader", "download manager", "media downloader"),
    "输入法" to listOf("keyboard", "input method", "IME", "voice keyboard"),
    "剪贴板" to listOf("clipboard", "clipboard sync", "cross device clipboard"),
    "语音" to listOf("speech", "voice", "speech to text", "voice input"),
    "相机" to listOf("camera", "camera app", "camera utility"),
    "音乐" to listOf("music", "audio", "music player"),
    "阅读器" to listOf("reader", "document reader", "ebook reader"),
    "RSS" to listOf("rss", "feed reader", "news reader"),
    "浏览器" to listOf("browser", "web browser"),
    "插件" to listOf("extension", "plugin", "addon"),
    "扩展" to listOf("extension", "plugin", "addon"),
    "文件管理" to listOf("file manager", "file explorer"),
    "远程控制" to listOf("remote control", "remote desktop", "device control"),
    "笔记" to listOf("notes", "note taking", "markdown notes"),
    "聊天" to listOf("chat", "messaging", "messenger"),
    "翻译" to listOf("translator", "translation", "translate"),
    "图片" to listOf("image", "photo", "gallery"),
    "应用商店" to listOf("app store", "repository store", "app repository"),
    "广告拦截" to listOf("ad blocker", "adblock", "content blocker"),
    "密码" to listOf("password manager", "vault"),
    "终端" to listOf("terminal", "shell", "ssh client"),
    "投屏" to listOf("screen cast", "casting", "mirroring"),
    "录屏" to listOf("screen recorder", "screen recording"),
    "搜索" to listOf("search", "search engine", "finder"),
    "OCR" to listOf("ocr", "text recognition", "document scanner"),
    "PDF" to listOf("pdf", "pdf reader", "pdf editor")
)

private fun expandQuery(raw: String): List<String> {
    val q = raw.trim()
    if (q.isEmpty()) return emptyList()
    val expansions = linkedSetOf(q)
    conceptMap.forEach { (zh, en) ->
        if (q.contains(zh, ignoreCase = true)) expansions.addAll(en)
    }
    if (q.contains("安卓", true) || q.contains("Android", true)) expansions += "android app"
    if (q.contains("AI", true) || q.contains("人工智能", true)) expansions += listOf("AI assistant", "LLM", "machine learning")
    return expansions.take(5)
}

private fun getJson(url: String): JSONObject {
    val request = Request.Builder()
        .url(url)
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .header("User-Agent", "OpenRepo-Store/0.2")
        .build()
    http.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            val msg = try { JSONObject(body).optString("message") } catch (_: Exception) { body.take(160) }
            throw IllegalStateException("GitHub ${response.code}: $msg")
        }
        return JSONObject(body)
    }
}

private fun repositorySearch(term: String, perPage: Int = 12): List<RepoCandidate> {
    val encoded = URLEncoder.encode("$term in:name,description,readme archived:false", StandardCharsets.UTF_8.toString())
    val json = getJson("https://api.github.com/search/repositories?q=$encoded&sort=stars&order=desc&per_page=$perPage")
    val items = json.optJSONArray("items") ?: return emptyList()
    return buildList {
        for (i in 0 until items.length()) {
            val o = items.getJSONObject(i)
            val topicsJson = o.optJSONArray("topics")
            val topics = buildList {
                if (topicsJson != null) for (j in 0 until topicsJson.length()) add(topicsJson.optString(j))
            }
            add(
                RepoCandidate(
                    name = o.optString("name"),
                    fullName = o.optString("full_name"),
                    description = o.optString("description"),
                    stars = o.optInt("stargazers_count"),
                    topics = topics,
                    htmlUrl = o.optString("html_url"),
                    updatedAt = o.optString("updated_at")
                )
            )
        }
    }
}

private fun normalizeTokens(query: String): List<String> = expandQuery(query)
    .flatMap { it.lowercase().split(Regex("[^\\p{L}\\p{N}+-]+")) }
    .filter { it.length >= 2 }
    .distinct()

private fun relevanceScore(repo: RepoCandidate, rawQuery: String): Pair<Double, String> {
    val q = rawQuery.lowercase()
    val tokens = normalizeTokens(rawQuery)
    val name = repo.name.lowercase()
    val desc = repo.description.lowercase()
    val topics = repo.topics.joinToString(" ").lowercase()
    var score = repo.baseScore
    val reasons = mutableListOf<String>()

    if (name.contains(q) || repo.fullName.lowercase().contains(q)) {
        score += 45; reasons += "名称匹配"
    }
    val descHits = tokens.count { desc.contains(it) }
    if (descHits > 0) {
        score += descHits * 11; reasons += "简介相关"
    }
    val topicHits = tokens.count { topics.contains(it) }
    if (topicHits > 0) {
        score += topicHits * 14; reasons += "Topics 相关"
    }
    score += kotlin.math.ln(repo.stars.toDouble() + 1.0) * 2.2
    return score to reasons.distinct().joinToString(" · ").ifBlank { "语义扩展命中" }
}

private fun latestApk(repo: RepoCandidate, rawQuery: String): StoreApp? {
    val release = try { getJson("https://api.github.com/repos/${repo.fullName}/releases/latest") } catch (_: Exception) { return null }
    if (release.optBoolean("draft") || release.optBoolean("prerelease")) return null
    val assets = release.optJSONArray("assets") ?: return null
    var selected: JSONObject? = null
    for (i in 0 until assets.length()) {
        val a = assets.getJSONObject(i)
        val n = a.optString("name")
        if (n.endsWith(".apk", ignoreCase = true)) {
            if (selected == null || (!n.contains("debug", true) && selected!!.optString("name").contains("debug", true))) selected = a
        }
    }
    val apk = selected ?: return null
    val (score, reason) = relevanceScore(repo, rawQuery)
    return StoreApp(
        name = repo.name,
        repo = repo.fullName,
        description = repo.description.ifBlank { "该项目未提供仓库简介，可打开项目页面查看 README。" },
        version = release.optString("tag_name"),
        apkName = apk.optString("name"),
        apkSize = apk.optLong("size"),
        downloadUrl = apk.optString("browser_download_url"),
        releaseUrl = release.optString("html_url"),
        stars = repo.stars,
        topics = repo.topics,
        score = score,
        reason = reason
    )
}

private suspend fun twoLayerSearch(rawQuery: String): List<StoreApp> = withContext(Dispatchers.IO) {
    val expanded = expandQuery(rawQuery)
    if (expanded.isEmpty()) return@withContext emptyList()

    val merged = linkedMapOf<String, RepoCandidate>()

    // Layer 1: GitHub official search using the user's original query.
    repositorySearch(expanded.first(), 12).forEachIndexed { index, repo ->
        repo.baseScore = 32.0 - index
        merged[repo.fullName] = repo
    }

    // Layer 2: bilingual semantic expansion. Search name + description + README, then fuse candidates.
    expanded.drop(1).take(3).forEachIndexed { qIndex, term ->
        repositorySearch(term, 8).forEachIndexed { index, repo ->
            val semanticBonus = 22.0 - qIndex * 3.0 - index * 0.4
            val existing = merged[repo.fullName]
            if (existing == null) {
                repo.baseScore = semanticBonus
                merged[repo.fullName] = repo
            } else {
                existing.baseScore += semanticBonus
            }
        }
    }

    merged.values
        .map { repo -> repo to relevanceScore(repo, rawQuery).first }
        .sortedByDescending { it.second }
        .take(14)
        .mapNotNull { latestApk(it.first, rawQuery) }
        .sortedByDescending { it.score }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(onOpen: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<StoreApp>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("输入中文或英文功能描述，例如：视频下载、剪贴板同步、RSS 阅读器") }
    val scope = rememberCoroutineScope()

    fun search() {
        val q = query.trim()
        if (q.isEmpty() || loading) return
        loading = true
        results = emptyList()
        message = "正在进行 GitHub 官方搜索 + 中英文语义扩展检索…"
        scope.launch {
            try {
                val found = twoLayerSearch(q)
                results = found
                message = if (found.isEmpty()) {
                    "搜索完成，但候选项目中没有发现最新 Release 含 APK 的项目。可换一个更宽泛的功能描述。"
                } else {
                    "找到 ${found.size} 个可直接安装 APK 的项目，已按综合相关性排序"
                }
            } catch (e: Exception) {
                message = when {
                    e.message?.contains("rate limit", true) == true -> "GitHub 未登录 API 限流，请稍后再试。后续版本会加入 GitHub OAuth 提升额度。"
                    else -> "搜索失败：${e.message ?: "网络或 GitHub API 异常"}"
                }
            } finally {
                loading = false
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("OpenRepo Store") }) }) { padding ->
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
                Button(onClick = { search() }, enabled = !loading) {
                    Text(if (loading) "检索中…" else "双层检索")
                }
                AssistChip(onClick = {}, label = { Text("仅显示 APK") })
            }
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(results) { app ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(app.name, style = MaterialTheme.typography.titleMedium)
                            Text(app.repo, style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(6.dp))
                            Text(app.description, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(6.dp))
                            Text("相关原因：${app.reason}", style = MaterialTheme.typography.labelMedium)
                            Text("★ ${app.stars} · ${app.version} · ${formatBytes(app.apkSize)}", style = MaterialTheme.typography.bodySmall)
                            if (app.topics.isNotEmpty()) Text(app.topics.take(5).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            Text("风险提示：GitHub 托管不代表 GitHub 或 OpenRepo 已审核该 APK。安装前请确认发布者、权限与签名。", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onOpen(app.downloadUrl) }) { Text("直接下载 APK") }
                                OutlinedButton(onClick = { onOpen(app.releaseUrl) }) { Text("查看 Release") }
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
