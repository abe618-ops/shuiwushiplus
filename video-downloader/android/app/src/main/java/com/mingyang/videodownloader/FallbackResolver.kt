package com.mingyang.videodownloader

import okhttp3.OkHttpClient
import okhttp3.Request

class FallbackResolver {
    private val client = OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).build()

    data class ResolvedMedia(
        val title: String,
        val mediaUrl: String,
        val referer: String? = null
    )

    /**
     * Conservative fallback for public pages only. It intentionally does not bypass
     * authentication, CAPTCHA, DRM, private/paid access or platform protections.
     */
    fun resolvePublicPage(url: String, platform: VideoPlatform): ResolvedMedia? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126 Mobile Safari/537.36")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val html = response.body?.string().orEmpty()
            val title = Regex("<title[^>]*>(.*?)</title>", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.getOrNull(1)?.replace(Regex("\\s+"), " ")?.trim().orEmpty()

            // Generic OpenGraph/direct-MP4 fallback. Platform-specific signed/obfuscated
            // media that needs an authenticated playback session is deliberately not bypassed.
            val ogVideo = Regex("<meta[^>]+property=[\\\"']og:video(?::url)?[\\\"'][^>]+content=[\\\"']([^\\\"']+)", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.getOrNull(1)
            val directMp4 = Regex("https?://[^\\\"'<> ]+\\.mp4(?:\\?[^\\\"'<> ]*)?", RegexOption.IGNORE_CASE)
                .find(html)?.value
            val media = ogVideo ?: directMp4 ?: return null
            return ResolvedMedia(if (title.isBlank()) platform.name else title, media.replace("&amp;", "&"), url)
        }
    }
}
