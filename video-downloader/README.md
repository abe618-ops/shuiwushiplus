# 通用视频下载器 Android

目标：在 Android 上提供一个统一的“分享/粘贴链接 → 识别平台 → 下载”入口。首要支持：

- X / Twitter
- TikTok
- 抖音
- 快手
- Bilibili
- 今日头条
- 微信视频号（仅处理用户有权访问/保存的媒体；不绕过登录、DRM、权限或平台保护）

## 引擎策略

### A. yt-dlp / youtubedl-android 主引擎
适用于 X/Twitter、TikTok、Bilibili 及其它 yt-dlp 已支持站点。Android 层使用 youtubedl-android 嵌入 yt-dlp，而不是把 Python 解释器手工塞进 APK。

### B. 抖音专用解析器
对于 yt-dlp 不稳定的抖音分享链接，增加 Douyin 专用 fallback：
1. 展开 v.douyin.com 短链；
2. 解析公开作品页/公开接口返回的媒体信息；
3. 优先最高码率、无水印公开资源；
4. 失败时返回清晰错误，不使用第三方商业解析站。

参考实现思路：jiji262/douyin-downloader 的短链、重试、最高码率选择和浏览器兜底架构。

### C. 快手专用解析器
快手旧接口已大量失效，不能继续依赖旧 REST 方案。新版采用：
1. 优先 yt-dlp（若当前 extractor 可用）；
2. 对公开分享页做 HTML/JSON 媒体信息解析 fallback；
3. 需要登录 Cookie 的内容，只允许用户主动导入自己的 Cookie，不绕过登录/风控。

### D. 今日头条
优先 yt-dlp；失败后从公开文章/视频页解析规范化视频信息。拒绝接入来源不明的“万能解析 API”。

### E. 微信视频号
视频号不适合做“仅凭分享链接匿名解析”。可靠路线是：
1. 用户从自己有权播放的会话/页面发起分享；
2. 若拿到公开直链，则直接下载；
3. 对需要登录态、临时签名或 HLS 的内容，只在用户自己的授权播放会话中处理，并支持 MP4/HLS 合并；
4. 不绕过 DRM、账号权限、付费或私密限制。

## Android 交互

- 单输入框，自动读取用户主动粘贴的链接
- Android 分享面板 ACTION_SEND 入口
- 自动识别平台
- 下载前显示标题、封面、时长、平台、预计格式
- 一键下载最高画质
- 高级选项：视频/音频、画质、字幕、封面
- 下载历史、失败重试
- 保存到系统 Downloads/VideoDownloader

## 解析器路由

```text
URL
 └─ PlatformDetector
     ├─ X/Twitter ────── yt-dlp
     ├─ TikTok ───────── yt-dlp
     ├─ Bilibili ─────── yt-dlp
     ├─ Douyin ───────── yt-dlp -> DouyinFallback
     ├─ Kuaishou ─────── yt-dlp -> KuaishouFallback
     ├─ Toutiao ──────── yt-dlp -> ToutiaoFallback
     └─ WeChat Channels ─ AuthorizedMediaResolver
```

## 关键原则

- 不依赖单一在线解析网站，避免接口今天能用、明天失效。
- 主引擎可更新：yt-dlp extractor 更新不要求重写整个 UI。
- 专用 fallback 与平台解耦，某个平台失效不会拖垮全部下载。
- 只下载用户有权访问和保存的内容。
