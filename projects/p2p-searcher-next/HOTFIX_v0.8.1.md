# P2P Searcher Next v0.8.1 Hotfix

针对 Android v0.8.0 出现的“TCP 已连接但登录超时 / 部分 eD2K 节点无响应 / 结果覆盖过窄”进行第一阶段修复。

## 已定位的根因

v0.8.0 的 eD2K 侧当前主要是“逐服务器 TCP 登录 + 搜索”，而旧 P2PSearcher 的完整覆盖逻辑还包含 server.met 多服务器 UDP Global Search。当前 TCP connect/read/login/search 窗口也偏短，弱网络和服务器繁忙时很容易出现“TCP 已连接但登录超时”。

## v0.8.1 源码修复项

- eD2K TCP connect timeout: 3.5s -> 8s
- eD2K read timeout: 4.5s -> 10s
- login/search deadline: 6.5s -> 15s
- TCP Server 覆盖：最多 6 -> 12；工作线程 3 -> 6
- server.met：由“第一份成功就停止”改为“合并全部可用实时 server.met + 内置备用池并去重”
- 备用池补入 57.131.35.107:4232、141.227.165.99:4232
- Kad bootstrap: 24 -> 40，并加长 UDP 接收窗口
- UI 单次可见结果上限：500 -> 1500

## 下一阶段完整核心

继续补齐旧 P2PSearcher 的第三条关键链路：server.met 多服务器 UDP Global Search，支持 OP_GLOBSEARCHREQ3/2/1 (0x90/0x92/0x98) -> OP_GLOBSEARCHRES (0x99)，然后将 TCP Server、UDP Global、Kad 三路结果按 ED2K hash + size 聚合并累计来源数。

本分支不启用或新增 GitHub Actions，不消耗个人 Actions 额度。
