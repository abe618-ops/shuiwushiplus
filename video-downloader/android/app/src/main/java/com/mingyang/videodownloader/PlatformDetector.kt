package com.mingyang.videodownloader

enum class VideoPlatform {
    TWITTER, TIKTOK, DOUYIN, KUAISHOU, BILIBILI, TOUTIAO, WECHAT_CHANNELS, GENERIC
}

object PlatformDetector {
    fun detect(url: String): VideoPlatform {
        val u = url.lowercase()
        return when {
            "twitter.com" in u || "x.com" in u -> VideoPlatform.TWITTER
            "tiktok.com" in u -> VideoPlatform.TIKTOK
            "douyin.com" in u || "iesdouyin.com" in u -> VideoPlatform.DOUYIN
            "kuaishou.com" in u || "chenzhongtech.com" in u -> VideoPlatform.KUAISHOU
            "bilibili.com" in u || "b23.tv" in u -> VideoPlatform.BILIBILI
            "toutiao.com" in u || "ixigua.com" in u -> VideoPlatform.TOUTIAO
            "channels.weixin.qq.com" in u || "finder.video.qq.com" in u -> VideoPlatform.WECHAT_CHANNELS
            else -> VideoPlatform.GENERIC
        }
    }
}
