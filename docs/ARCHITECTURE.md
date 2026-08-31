# 系统架构

## 总体分层

```text
[Android / Web UI]
       ↓
[学习任务编排层]
       ↓
[学习引擎] ── FSRS / Active Recall / Feynman / First Principles / Interleaving
       ↓
[题目引擎] ── 采集 / 解析 / 校验 / 生成 / 变式 / 解析
       ↓
[知识层] ── 科目 / 章节 / 知识点 / 法规 / 政策版本 / 知识图谱
       ↓
[数据层] ── 本地数据库 + 可选云同步
```

## 模块

### core-learning
负责学习算法，不依赖具体税法内容：
- FSRS 调度
- 掌握度估计
- 遗忘风险预测
- 学习边界探测
- 难度自适应
- 置信度校准
- 错因画像

### subjects
每个科目作为独立内容包：
- tax-law-1
- tax-law-2
- tax-service-practice
- finance-accounting
- tax-related-law

科目包只描述知识与题目，不写死 UI。

### question-engine
- SourceCollector：发现公开资料
- SourceNormalizer：标准化来源
- RuleResolver：定位适用法规
- QuestionParser：解析题目结构
- Validator：答案/法规/年度校验
- Generator：原创仿真与变式题
- ExplanationEngine：分步解析
- Deduplicator：相似题去重

### scheduler
根据一天中的时段、可用时间、知识优先级和认知负荷生成任务包。

### user-model
保存个人学习状态，不与公共题库混在一起：
- mastery
- FSRS state
- error profile
- confidence
- time budget
- preferred session windows

## 联网与离线

Android 端采用 Offline-first：
- 已下载科目、题目、错题和复习计划离线可用；
- 联网时拉取政策更新、题目候选、解析更新；
- AI 功能可采用云端模型，也预留本地规则引擎降级路径。

## 数据可靠性

法规/考试知识必须支持版本：

`ruleId + effectiveFrom + effectiveTo + sourceUrl + subject + examYear + verificationStatus`

同一道旧题可以保留，但必须知道它基于哪个年度规则。

## 推荐技术方向

Android：Kotlin + Jetpack Compose + Room + WorkManager。

Web 原型：优先轻量 PWA/TypeScript，复用同一题目 schema 和 API。

学习调度：参考 FSRS 开源实现，但保持接口抽象，避免把项目锁定到某一种算法。

后端第一阶段可尽量轻：静态 JSON/SQLite + 可选 API。等题库规模和联网采集成熟后再引入完整服务端。
