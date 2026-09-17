# FootballQuant 竞彩 / 北京单场 Daily V1

## 目标

建立当天竞彩与北京单场统一预测 APK。正式层只使用可追溯统计/市场数据；术数保持影子层。

## 正式模型

- 赔率去水与多公司市场共识
- Poisson + Dixon-Coles 低比分修正
- Skellam / 净胜球分布
- Elo / xG 外部数据（仅在来源与时间可追溯时进入）
- 主客场、联赛层级、杯赛/次回合 Game-State
- 初盘/即时盘、亚盘、大小球、返还率、Kelly
- ZeroGoal、BTTS、单双、总进球、半全场、2-4 个比分由统一概率体系派生
- ModelDisagreement、UpsetTail、LowScore 作为 Challenger

## 数据政策

1. 竞彩与北单必须保留独立赛事身份，不用相似队名硬匹配。
2. 每个赔率点保存 source/provider、capturedAt、state(open/current/close)、market、line、odds。
3. 赛后只追加 result/settlement；不得覆盖赛前快照。
4. “百家欧赔”定义为多家公司报价聚合展示，不是单一机构。
5. 球探/球坛页面只能证明页面展示字段；其未公开采集接口、采购关系或内部数据源一律不推断为事实。
6. 可用合法替代源优先：官方赛程/赛果、授权数据 API、公开博彩公司页面或具有明确授权的数据供应商。

## ABSTAIN

满足以下任一条件时，胜平负允许主动放弃：

- 数据质量 REJECTED/LOW DATA；
- 基线与市场/外部模型明显冲突，且 Top1 < 56% 或 Top1-Top2 < 12pp；
- Top1 < 42%；
- Top1-Top2 < 5.5pp。

ABSTAIN 不影响大小球、BTTS 等独立市场继续给出条件性结论。

## Champion / Challenger

Champion：市场去水 + Poisson/DC + 可验证 Elo/xG。

Challenger：盘口动量、ZeroGoal、LowScore、UpsetTail、外部模型分歧等。少于 100 场样本外不宣称增量优势；500-1000 场样本外前不宣称长期盈利。

## APK 首版页面

- 今日竞彩
- 今日北单
- 比赛详情（概率、盘口、比分矩阵、来源）
- Champion / Challenger
- 历史冻结快照
- 复盘指标
- 数据质量与缺失报告
- 术数影子层（默认折叠，不进入正式概率）

## 下一步

- 接入竞彩与北京单场当天列表适配器
- 增加 immutable snapshot schema
- 增加 09:30 / 13:00 临时切片与 20:00 正式冻结
- 增加 Brier / Log Loss / RPS / Poisson deviance / Top1 / Top3 / CLV 计算
- GitHub Actions 构建签名 APK，并在 Android 16 真机验证
