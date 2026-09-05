package com.mingyang.videodownloader

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo

class YtDlpEngine(private val context: Context) {
    fun init() {
        YoutubeDL.getInstance().init(context)
    }

    fun getInfo(url: String): VideoInfo {
        val request = YoutubeDLRequest(url)
        request.addOption("--no-playlist")
        request.addOption("--no-warnings")
        return YoutubeDL.getInstance().getInfo(request)
    }

    fun download(url: String, outputTemplate: String, onProgress: (Float, Long, String) -> Unit): String {
        val request = YoutubeDLRequest(url)
        request.addOption("-f", "bv*+ba/b")
        request.addOption("--merge-output-format", "mp4")
        request.addOption("--no-playlist")
        request.addOption("-o", outputTemplate)
        return YoutubeDL.getInstance().execute(request, null) { progress, eta, line ->
            onProgress(progress, eta, line)
        }.out
    }
}
