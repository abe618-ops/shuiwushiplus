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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                StoreScreen(
                    onOpen = { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                )
            }
        }
    }
}

data class StoreApp(
    val name: String,
    val repo: String,
    val description: String,
    val version: String,
    val apkName: String,
    val downloadUrl: String,
    val releaseUrl: String,
    val risk: String = "第三方 GitHub Release。安装前请确认发布者、权限与签名信息。"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(onOpen: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val demo = remember {
        listOf(
            StoreApp(
                name = "OpenRepo 示例应用",
                repo = "owner/repository",
                description = "第一版先验证 APK-only 商店界面与直链安装流程。",
                version = "v1.0.0",
                apkName = "app-release.apk",
                downloadUrl = "https://github.com/owner/repository/releases/download/v1.0.0/app-release.apk",
                releaseUrl = "https://github.com/owner/repository/releases/tag/v1.0.0"
            )
        )
    }
    val visible = demo.filter { it.name.contains(query, true) || it.repo.contains(query, true) }

    Scaffold(topBar = { TopAppBar(title = { Text("OpenRepo Store") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("搜索 Android 应用") },
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            Text("仅显示最新 GitHub Release 中包含 APK 的公开项目", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(visible) { app ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(app.name, style = MaterialTheme.typography.titleLarge)
                            Text(app.repo, style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(app.description)
                            Spacer(Modifier.height(8.dp))
                            Text("${app.version} · ${app.apkName}", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(10.dp))
                            AssistChip(onClick = {}, label = { Text("来源：GitHub Release") })
                            Spacer(Modifier.height(8.dp))
                            Text(app.risk, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onOpen(app.downloadUrl) }) { Text("直接下载 APK") }
                                OutlinedButton(onClick = { onOpen(app.releaseUrl) }) { Text("Release 兜底") }
                            }
                        }
                    }
                }
            }
        }
    }
}
