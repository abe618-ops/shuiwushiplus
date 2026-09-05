# Zhouyi Sports Prediction V2

传统术数 × 统计基线 × 市场信号的可冻结、可复演、可回测研究框架。

> 定位：传统文化/算法研究与体育预测实验。术数信号不视为已被科学验证的稳定预测器；任何模型必须与市场/统计基线做盲测比较。

## V2 核心原则

1. **比赛身份先锁定**：`competition_id | season | kickoff_utc | home_id | away_id`。
2. **预测冻结**：赛前保存输入、赔率快照、术数盘面、模型版本、随机种子、输出；赛后只新增复盘，不覆盖原预测。
3. **四层架构**：
   - Data：赛程、球队、阵容、伤停、xG/效率、赔率与盘口时间序列；
   - Quant：足球 Dixon–Coles/双泊松；篮球 possessions + ORtg/DRtg + Margin/Total；
   - Divination：六爻、奇门、梅花、大六壬等独立输出事件标签；
   - Fusion：只有在足够 walk-forward 样本证明有增量后，才学习术数权重。
4. **不重复计票**：同源术数派生量、同源赔率站、同源媒体叙事不能当成多个独立信号。
5. **统一事件卡**：术数只产出标准化标签，不直接凭一个数硬断精确比分。
6. **可复演随机数**：禁止无记录的 `Math.random()`；所有随机实验使用显式 seed。
7. **按玩法拆模型**：Winner、Handicap/Margin、Total、Odd/Even、Exact Score、HT/FT 独立评价。
8. **以 proper scoring rule 为主**：Accuracy 之外必须记录 Brier、LogLoss、Calibration；比分记录 Top-K coverage 与 MAE。

## 目录

```text
sports-zhouyi-v2/
├── README.md
├── SKILL.md
├── references/
│   └── research-notes.md
├── schemas/
│   └── match-event.schema.json
├── src/
│   ├── quant_baseline.py
│   ├── divination_features.py
│   └── fusion.py
└── experiments/
    └── demo.py
```

## 统一事件卡

每个术数模块先归一化为以下信号：

- `side`: home / draw / away / neutral
- `tempo`: slow / medium / fast
- `goal_band`: 0-1 / 2 / 3 / 4+
- `odd_even`: odd / even / neutral
- `script`: early-home / early-away / stalemate / late-swing / open-game / unknown
- `strength`: weak / medium / strong / conflict
- `provenance`: 起局时间、取数方式、主客定义、规则版本、seed

这使六爻、奇门、梅花、大六壬能够进入同一回测表，而不允许各门在赛后用不同语言重新解释。

## 足球基线

- 双泊松 / Dixon–Coles：统一生成 1X2、总进球、单双、比分、HT/FT。
- 市场概率：欧赔去水后作为强基线，而非额外“投票”。
- 后续加入：时间衰减攻击/防守强度、xG、主客拆分、伤停、赛程密度与盘口快照。

## 篮球基线

- possessions × 每百回合攻防效率得到两队期望得分；
- Winner、Margin、Total 分开建模；
- 对末节垃圾时间和尾部方差单独建误差项；
- 术数层只提供方向/节奏/大小/分差区间标签。

## 当前术数主线

优先正式编码：

1. 六爻纳甲：世应、动变、旺衰、旬空、墓库、真太阳时/时间取用；
2. 奇门遁甲：比赛定局、主客落宫、宫门星神、生克旺衰、反吟伏吟、排名数区分同刻比赛；
3. 梅花易数：体用、生克、互卦/变卦、数理；
4. 大六壬：四课三传、十二天将、初中末传过程脚本。

实验区：小成图、飞宫小奇门、策轨数、太玄数、灵棋经、神易数、地占等。实验区必须先积累冻结样本，不直接进入融合权重。

## 实验晋级规则

一个术数模块只有在至少一个同质样本集达到预设数量后才能参与融合：

- Chronological walk-forward，不随机打乱未来；
- 与 market-only、quant-only 比较；
- 多窗口结果稳定；
- Brier/LogLoss 或 Total/Margin 相应误差有稳定增益；
- 校准没有明显恶化；
- 不依赖单一联赛/单一日期偶然命中。

第一阶段建议每个玩法、同类联赛至少累计 100 场冻结样本后再判断术数是否有增量。
