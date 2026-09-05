# Engine Matrix

| 平台 | 主引擎 | fallback | 登录态 | 备注 |
|---|---|---|---|---|
| X/Twitter | yt-dlp | 公开页直链解析 | 可选 | 保留昨天已验证可用方向 |
| TikTok | yt-dlp | TikTok extractor retry | 可选 | 支持公开视频 |
| Bilibili | yt-dlp | Bilibili 专项重试/格式选择 | 部分内容需要 | 音视频分离时需 FFmpeg 合并 |
| 抖音 | yt-dlp | DouyinFallback | 可选/部分场景需要 | 短链展开、最高码率、无水印公开资源优先 |
| 快手 | yt-dlp | KuaishouFallback | 部分场景需要 | 旧 REST 接口已不可靠，不再采用 |
| 今日头条 | yt-dlp | ToutiaoFallback | 通常不需要 | 视频页/文章页公开媒体解析 |
| 微信视频号 | AuthorizedMediaResolver | HLS/MP4 resolver | 通常需要用户自己的播放会话 | 不做匿名万能解析，不绕过 DRM/权限 |

## 已排除的旧方案

- 依赖某一个第三方“万能解析 API”作为唯一后端；
- 快手历史已失效 REST 接口；
- 把视频号当普通公开 URL 直接匿名解析；
- 直接复制来源不明商业接口；
- 需要规避验证码、DRM、付费墙或账号权限的方案。

## Android 推荐底座

- Kotlin + Jetpack Compose
- youtubedl-android（承载 yt-dlp）
- FFmpegKit/等价受维护 FFmpeg Android 方案用于 DASH/HLS 合并
- WorkManager 下载队列
- MediaStore / SAF 保存
- ACTION_SEND 分享入口
- Room 保存下载历史
