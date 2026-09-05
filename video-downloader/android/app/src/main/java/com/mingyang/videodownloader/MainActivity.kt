package com.mingyang.videodownloader

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shared = if (intent?.action == Intent.ACTION_SEND) intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty() else ""
        setContent { DownloaderScreen(shared) }
    }

    @Composable
    private fun DownloaderScreen(initialText: String) {
        var url by remember { mutableStateOf(extractUrl(initialText)) }
        var status by remember { mutableStateOf("粘贴或分享视频链接") }
        var progress by remember { mutableFloatStateOf(0f) }
        val scope = rememberCoroutineScope()

        MaterialTheme {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("视频下载器", style = MaterialTheme.typography.headlineMedium)
                    Text("X / TikTok / 抖音 / 快手 / B站 / 今日头条 / 视频号")
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it.trim() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("视频链接") },
                        minLines = 3
                    )
                    val platform = PlatformDetector.detect(url)
                    Text("识别：${platform.name}")
                    if (progress > 0f && progress < 100f) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                    Button(
                        enabled = url.startsWith("http"),
                        onClick = {
                            scope.launch {
                                status = "正在解析…"
                                try {
                                    val engine = YtDlpEngine(this@MainActivity)
                                    withContext(Dispatchers.IO) { engine.init() }
                                    val info = withContext(Dispatchers.IO) { engine.getInfo(url) }
                                    status = "已识别：${info.title ?: "视频"}，开始下载…"
                                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "VideoDownloader")
                                    if (!dir.exists()) dir.mkdirs()
                                    val output = File(dir, "%(title).120s [%(id)s].%(ext)s").absolutePath
                                    withContext(Dispatchers.IO) {
                                        engine.download(url, output) { p, _, _ -> progress = p }
                                    }
                                    progress = 100f
                                    status = "下载完成：Downloads/VideoDownloader"
                                } catch (e: Exception) {
                                    val p = PlatformDetector.detect(url)
                                    if (p == VideoPlatform.WECHAT_CHANNELS) {
                                        status = "视频号需要用户自己的授权播放会话；当前不绕过登录、DRM 或私密权限。"
                                    } else {
                                        val fallback = withContext(Dispatchers.IO) { runCatching { FallbackResolver().resolvePublicPage(url, p) }.getOrNull() }
                                        status = if (fallback != null) {
                                            "主引擎失败，但已找到公开媒体直链；下一步由下载队列保存。"
                                        } else {
                                            "解析失败：${e.message ?: "平台接口可能已变化"}"
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("一键下载最高画质") }
                    Text(status)
                    Text("仅用于下载你有权访问和保存的内容；不绕过登录、付费、DRM 或私密权限。", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    private fun extractUrl(text: String): String = Regex("https?://\\S+").find(text)?.value?.trimEnd('.', ',', '，', '。', ')', ']') ?: text.trim()
}
