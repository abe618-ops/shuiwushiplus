package com.abe618.seamlessclip

import android.app.*
import android.content.*
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.IBinder
import java.io.*
import java.net.Socket
import java.util.UUID
import java.util.concurrent.Executors

class ClipboardSyncService : Service() {
    companion object {
        const val ACTION_SEND_CLIPBOARD = "send_clipboard"
        const val ACTION_SEND_TEXT = "send_text"
        const val EXTRA_TEXT = "text"
        private const val CHANNEL = "seamclip"
        private const val TYPE = "_seamclip._tcp."
    }
    private val io = Executors.newSingleThreadExecutor()
    @Volatile private var socket: Socket? = null
    @Volatile private var writer: PrintWriter? = null
    private lateinit var nsd: NsdManager
    private val prefs by lazy { getSharedPreferences("peer", MODE_PRIVATE) }
    private val deviceId by lazy { prefs.getString("deviceId", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("deviceId", it).apply() } }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(1, Notification.Builder(this, CHANNEL).setContentTitle("无感剪贴板").setContentText("正在自动发现同一 Wi‑Fi 的电脑").setSmallIcon(android.R.drawable.stat_notify_sync).build())
        nsd = getSystemService(NSD_SERVICE) as NsdManager
        discover()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SEND_CLIPBOARD -> {
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.let(::send)
            }
            ACTION_SEND_TEXT -> intent.getStringExtra(EXTRA_TEXT)?.let(::send)
        }
        return START_STICKY
    }

    private fun discover() {
        runCatching {
            nsd.discoverServices(TYPE, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(t: String) {}
                override fun onDiscoveryStopped(t: String) {}
                override fun onStartDiscoveryFailed(t: String, e: Int) { restartLater() }
                override fun onStopDiscoveryFailed(t: String, e: Int) {}
                override fun onServiceLost(s: NsdServiceInfo) { if (socket?.isClosed != false) restartLater() }
                override fun onServiceFound(s: NsdServiceInfo) {
                    if (socket?.isConnected == true) return
                    nsd.resolveService(s, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(si: NsdServiceInfo, code: Int) {}
                        override fun onServiceResolved(si: NsdServiceInfo) { connect(si.host.hostAddress ?: return, si.port) }
                    })
                }
            })
        }
    }

    private fun connect(host: String, port: Int) = io.execute {
        if (socket?.isConnected == true) return@execute
        while (true) {
            try {
                val s = Socket(host, port).apply { keepAlive = true; tcpNoDelay = true }
                socket = s
                writer = PrintWriter(BufferedWriter(OutputStreamWriter(s.getOutputStream())), true)
                writer?.println("HELLO\t$deviceId\t${prefs.getString("token", "")}")
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                while (!s.isClosed) {
                    val line = reader.readLine() ?: break
                    when {
                        line.startsWith("PAIRED\t") -> prefs.edit().putString("token", line.substringAfter('\t')).apply()
                        line.startsWith("CLIP\t") -> writeClipboard(line.substringAfter('\t').replace("\\n", "\n").replace("\\t", "\t"))
                        line == "PING" -> writer?.println("PONG")
                    }
                }
            } catch (_: Exception) { }
            socket = null; writer = null
            Thread.sleep(2500)
        }
    }

    private fun send(text: String) = io.execute {
        writer?.println("CLIP\t" + text.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n"))
    }

    private fun writeClipboard(text: String) {
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("SeamlessClipboard", text))
    }

    private fun restartLater() = io.execute { Thread.sleep(3000); discover() }
    private fun createChannel() {
        if (android.os.Build.VERSION.SDK_INT >= 26) (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(NotificationChannel(CHANNEL, "剪贴板同步", NotificationManager.IMPORTANCE_LOW))
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
