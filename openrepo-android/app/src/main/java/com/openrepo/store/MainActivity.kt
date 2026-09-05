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
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlin.math.ln

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
    var baseScore: Double = 0.0,
    var androidSignal: Int = 0
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

data class SearchOutcome(
    val apps: List<StoreApp>,
    val candidateCount: Int,
    val checkedCount: Int,
    val noApkCount: Int,
    val apiErrorCount: Int,
    val expandedTerms: List<String>
)

private val http = OkHttpClient.Builder()
    .connectTimeout(12, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .callTimeout(30, TimeUnit.SECONDS)
    .build()

private val conceptMap = linkedMapOf(
    "视频下载" to listOf("video downloader", "media downloader", "yt-dlp", "youtube downloader"),
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
    return expansions.take(6)
}

private fun requestText(url: String): String {
    val request = Request.Builder()
        .url(url)
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .header("User-Agent", "OpenRepo-Store/0.3")
        .build()
    http.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            val msg = try { JSONObject(body).optString("message") } catch (_: Exception) { body.take(180) }
            throw IllegalStateException("GitHub ${response.code}: $msg")
        }
        return body
    }
}

private fun getObject(url: String): JSONObject = JSONObject(requestText(url))
private fun getArray(url: String): JSONArray = JSONArray(requestText(url))

private fun repositorySearch(query: String, perPage: Int): List<RepoCandidate> {
    val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
    val json = getObject("https://api.github.com/search/repositories?q=$encoded&sort=stars&order=desc&per_page=$perPage")
    val items = json.optJSONArray("items") ?: return emptyList()
    return buildList {
        for (i in 0 until items.length()) {
            val o = items.getJSONObject(i)
            val topicsJson = o.optJSONArray("topics")
            val topics = buildList {
                if (topicsJson != null) for (j in 0 until topicsJson.length()) add(topicsJson.optString(j))
            }
            val description = o.optString("description")
            val combined = (o.optString("name") + " " + description + " " + topics.joinToString(" ")).lowercase()
            val signal = listOf("android", "apk", "kotlin", "mobile", "compose").count { combined.contains(it) }
            add(
                RepoCandidate(
                    name = o.optString("name"),
                    fullName = o.optString("full_name"),
                    description = description,
                    stars = o.optInt("stargazers_count"),
                    topics = topics,
                    htmlUrl = o.optString("html_url"),
                    updatedAt = o.optString("updated_at"),
                    androidSignal = signal
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
    var score = repo.baseScore + repo.androidSignal * 16.0
    val reasons = mutableListOf<String>()

    if (name.contains(q) || repo.fullName.lowercase().contains(q)) {
        score += 45; reasons += "名称匹配"
    }
    val descHits = tokens.count { desc.contains(it) }
    if (descHits > 0) {
        score += descHits * 10; reasons += "简介相关"
    }
    val topicHits = tokens.count { topics.contains(it) }
    if (topicHits > 0) {
        score += topicHits * 14; reasons += "Topics 相关"
    }
    if (repo.androidSignal > 0) reasons += "Android 信号"
    score += ln(repo.stars.toDouble() + 1.0) * 2.0
    return score to reasons.distinct().joinToString(" · ").ifBlank { "语义扩展命中" }
}

private fun findApkInRecentReleases(repo: RepoCandidate, rawQuery: String): StoreApp? {
    val releases = getArray("https://api.github.com/repos/${repo.fullName}/releases?per_page=6")
    var prereleaseFallback: StoreApp? = null

    for (i in 0 until releases.length()) {
        val release = releases.getJSONObject(i)
        if (release.optBoolean("draft")) continue
        val assets = release.optJSONArray("assets") ?: continue
        var selected: JSONObject? = null
        for (j in 0 until assets.length()) {
            val a = assets.getJSONObject(j)
            val n = a.optString("name")
            if (!n.endsWith(".apk", ignoreCase = true)) continue
            val isBad = n.contains("source", true) || n.contains("mapping", true) || n.contains("unsigned", true)
            if (isBad) continue
            if (selected == null) selected = a
            if (!n.contains("debug", true) && !n.contains("test", true)) {
                selected = a
                break
            }
        }
        val apk = selected ?: continue
        val (score, reason) = relevanceScore(repo, rawQuery)
        val app = StoreApp(
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
            score = score + if (release.optBoolean("prerelease")) -15 else 8,
            reason = reason + if (release.optBoolean("prerelease")) " · 预发布版" else " · 稳定 Release"
        )
        if (!release.optBoolean("prerelease")) return app
        if (prereleaseFallback == null) prereleaseFallback = app
    }
    return prereleaseFallback
}

private suspend fun discoverApps(rawQuery: String): SearchOutcome = withContext(Dispatchers.IO) {
    val expanded = expandQuery(rawQuery)
    if (expanded.isEmpty()) return@withContext SearchOutcome(emptyList(), 0, 0, 0, 0, emptyList())

    val merged = linkedMapOf<String, RepoCandidate>()

    fun addBatch(batch: List<RepoCandidate>, base: Double) {
        batch.forEachIndexed { index, repo ->
            val bonus = base - index * 0.5
            val old = merged[repo.fullName]
            if (old == null) {
                repo.baseScore = bonus
                merged[repo.fullName] = repo
            } else {
                old.baseScore += bonus * 0.55
                old.androidSignal = maxOf(old.androidSignal, repo.androidSignal)
            }
        }
    }

    // Layer 1: user's original intent, normal GitHub repository search.
    addBatch(repositorySearch("${expanded.first()} in:name,description,readme archived:false", 15), 35.0)

    // Layer 2a: bilingual semantic expansions, but force Android/mobile context.
    expanded.drop(1).take(3).forEachIndexed { idx, term ->
        addBatch(repositorySearch("$term android in:name,description,readme archived:false", 10), 30.0 - idx * 2)
    }

    // Layer 2b: explicit APK/Android-focused searches. This prevents desktop/CLI repos from filling the shortlist.
    val strongTerms = expanded.drop(1).ifEmpty { expanded }.take(2)
    strongTerms.forEachIndexed { idx, term ->
        addBatch(repositorySearch("$term apk android archived:false", 10), 34.0 - idx * 2)
        addBatch(repositorySearch("$term topic:android archived:false", 8), 32.0 - idx * 2)
    }

    val ranked = merged.values
        .map { it to relevanceScore(it, rawQuery).first }
        .sortedByDescending { it.second }
        .map { it.first }
        .take(28)

    val apps = mutableListOf<StoreApp>()
    var checked = 0
    var noApk = 0
    var apiErrors = 0

    for (repo in ranked) {
        if (apps.size >= 12) break
        checked++
        try {
            val app = findApkInRecentReleases(repo, rawQuery)
            if (app != null) apps += app else noApk++
        } catch (e: Exception) {
            apiErrors++
            if (e.message?.contains("rate limit", true) == true || e.message?.contains("403") == true) break
        }
    }

    SearchOutcome(
        apps = apps.sortedByDescending { it.score },
        candidateCount = merged.size,
        checkedCount = checked,
        noApkCount = noApk,
        apiErrorCount = apiErrors,
        expandedTerms = expanded
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(onOpen: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<StoreApp>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("输入中文或英文功能描述，例如：视频下载、剪贴板同步、RSS 阅读器") }
    var debugLine by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun search() {
        val q = query.trim()
        if (q.isEmpty() || loading) return
        loading = true
        results = emptyList()
        debugLine = ""
        message = "正在检索 GitHub，并优先筛选 Android / APK 项目…"
        scope.launch {
            try {
                val outcome = discoverApps(q)
                results = outcome.apps
                debugLine = "候选 ${outcome.candidateCount} · 已检查 ${outcome.checkedCount} · 无 APK ${outcome.noApkCount} · API 错误 ${outcome.apiErrorCount}"
                message = if (outcome.apps.isEmpty()) {
                    if (outcome.apiErrorCount > 0) {
                        "没有得到可展示 APK；部分 GitHub 请求失败或触发未登录额度限制。"
                    } else {
                        "已搜索相关仓库，但最近 6 条 Release 中未发现可安装 APK。建议换更宽泛的功能词。"
                    }
                } else {
                    "找到 ${outcome.apps.size} 个带 APK 的项目。语义扩展：${outcome.expandedTerms.drop(1).take(3).joinToString(" / ").ifBlank { "无需扩展" }}"
                }
            } catch (e: Exception) {
                message = when {
                    e.message?.contains("rate limit", true) == true || e.message?.contains("403") == true -> "GitHub 未登录 API 已限流。请稍后重试；下一步会加入 GitHub 登录提升额度。"
                    else -> "搜索失败：${e.message ?: "网络或 GitHub API 异常"}"
                }
            } finally {
                loading = false
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("OpenRepo Store 0.3") }) }) { padding ->
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
                AssistChip(onClick = {}, label = { Text("Android + APK") })
            }
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall)
            if (debugLine.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(debugLine, style = MaterialTheme.typography.labelSmall)
            }
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
                            Text(app.apkName, style = MaterialTheme.typography.bodySmall)
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
