# Seamless Clipboard Sync

一个面向 Android ↔ Linux 的“无感”局域网剪贴板同步项目。

目标体验：

> 两端安装 → 首次确认一次 → 以后在同一 Wi‑Fi / 局域网内自动发现、自动认证、自动重连，不再输入 IP、网址、端口或配对码。

后续将预留 iOS / macOS / Windows 客户端。

## V1 目标

- Android ↔ Linux 文本剪贴板同步
- mDNS/DNS-SD 自动发现
- 首次仅一次设备确认
- 首次配对后保存设备身份与密钥
- 同一 LAN 自动重连
- Wi‑Fi 切换、休眠、网络瞬断后自动恢复
- 心跳 + 指数退避重连
- 消息去重，避免剪贴板回环
- TLS/端到端身份认证
- Linux 支持 Wayland / X11
- 中文、Emoji、URL、换行保持完整
- 不依赖公网服务器
- 不要求 Root / ADB / 手工输入地址

## 关键原则

### 1. 一次配对，长期信任

首次发现新设备后，用户只需确认一次。双方交换长期设备公钥，并将对端身份写入本地安全存储。之后只要位于可达的同一局域网，自动完成认证和连接。

### 2. 不依赖固定 IP

每台设备通过 mDNS/DNS-SD 广播 `_seamclip._tcp.local`。IP 地址变化不影响信任关系，设备身份由长期公钥决定，而不是由 IP 决定。

### 3. 自动恢复，而不是“永不掉线”假承诺

Wi‑Fi 切换、休眠、Android 省电策略等都可能让 TCP 连接被系统中断。项目目标是做到用户层面“无感”：连接断开后自动发现并恢复，而不是假设底层连接永远不会断。

### 4. 默认局域网直连

V1 不需要账号和云服务器。后续若增加异地同步，可在协议层加入端到端加密中继，且中继无法读取剪贴板明文。

## 计划结构

```text
projects/seamless-clipboard-sync/
├── README.md
├── docs/
│   ├── ARCHITECTURE.md
│   └── PROTOCOL.md
├── protocol/
├── android/
├── linux/
└── ios/             # 后续
```

## Android 平台说明

Android 10+ 对普通后台应用读取系统剪贴板有限制。因此 V1 将采用双模式：

- 普通模式：Linux → Android 自动接收；Android → Linux 提供系统分享/快捷入口等兼容路径。
- 增强模式：提供轻量 InputMethodService（输入法组件），在用户启用后获得更自然的 Android → Linux 同步体验。

不使用 Root、ADB 或侵入式无障碍权限作为默认方案。

## Linux 平台说明

- Wayland：优先 `wl-paste --watch` + `wl-copy`
- X11：事件驱动监听 X11 Selection / XFixes
- 用户级后台服务开机启动
- mDNS 广播与发现
- 自动认证、自动重连

## 开发阶段

- [x] 架构与体验目标
- [x] 无固定 IP 的自动发现方案
- [x] 一次配对、长期设备身份方案
- [ ] 协议核心实现
- [ ] Linux daemon MVP
- [ ] Android APK MVP
- [ ] Android 输入法增强模式
- [ ] 断网/休眠恢复测试
- [ ] GitHub Actions 构建 APK / Linux 包
- [ ] iOS 客户端

## License

建议首版使用 Apache-2.0；正式加入第三方依赖后再完成 LICENSE/NOTICE 核对。
