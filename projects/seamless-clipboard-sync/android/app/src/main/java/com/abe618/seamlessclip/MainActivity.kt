package com.abe618.seamlessclip

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.NEARBY_WIFI_DEVICES), 10)
        }
        ContextCompat.startForegroundService(this, Intent(this, ClipboardSyncService::class.java))

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 72, 48, 48) }
        root.addView(TextView(this).apply { text = "无感剪贴板\n\n同一 Wi‑Fi 下自动发现并自动重连。首次连接后会记住设备。"; textSize = 20f })
        root.addView(Button(this).apply {
            text = "发送当前剪贴板到 Linux"
            setOnClickListener {
                startService(Intent(this@MainActivity, ClipboardSyncService::class.java).setAction(ClipboardSyncService.ACTION_SEND_CLIPBOARD))
            }
        })
        setContentView(root)

        if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                startService(Intent(this, ClipboardSyncService::class.java).setAction(ClipboardSyncService.ACTION_SEND_TEXT).putExtra(ClipboardSyncService.EXTRA_TEXT, it))
            }
        }
    }
}
