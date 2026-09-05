# Sports Zhouyi Research Skill

## Goal
For a football or basketball match, produce a frozen, auditable research prediction from independent quantitative, market and divination layers, then support post-match review.

## Input contract
- match identity and kickoff timezone
- pre-match team data
- market snapshots if available
- optional divination setup data
- explicit `seed` for reproducible experiments

## Mandatory workflow
1. Verify match identity. Stop on identity conflict.
2. Freeze all source timestamps.
3. Run market baseline.
4. Run sport quantitative baseline.
5. Run each divination method independently.
6. Inside each method, derive the match from multiple pre-registered dimensions/rule paths.
7. Perform an internal vote for each output dimension, capping correlated sub-rules.
8. Perform a second cross-method vote for each output dimension. One method contributes at most one vote per dimension.
9. Keep winner/result, handicap, total, odd-even, score shape and HT/FT as separate vote boards.
10. Fuse validated signals with quantitative/market probabilities; unvalidated divination remains shadow-only.
11. Save model version, rule versions, seed, input completeness and disagreement flags.
12. After the event, append result and score the frozen forecast. Never rewrite the pre-match forecast.

## Hierarchical divination architecture

The divination layer is no longer `one method -> one final answer`. It uses two voting stages:

```text
Liuyao
  ├─ strength/prosperity derivation ┐
  ├─ shi-ying relation derivation   ├─> internal result vote ─┐
  ├─ moving-line derivation         ┘                         │
  ├─ odd/even derivations ─────────────> internal O/E vote    │
  └─ process derivations ──────────────> internal HT/FT vote  │
                                                               │
Qimen / Meihua / Daliuren / ... do the same                    │
                                                               v
                           cross-method vote board by dimension
```

A derivation is an `EvidenceUnit` with:
- method
- output dimension
- derivation/rule path
- signal
- strength
- independence group
- rule version

Sub-rules that come from the same source variable must share an `independence_group`. Within one method and one dimension, that group contributes at most one effective vote. This prevents e.g. three transformations of the same palace/hexagram value from becoming three fake independent confirmations.

## Method-specific multidimensional derivation map

### Liuyao / 六爻
Use several independent or partially independent paths:
- `result`: 世应强弱、生克制化、日月旺衰、动爻变爻、旬空月破、墓绝进退神
- `handicap`: 主客强弱差、用神受制程度、动变是否扩大/缩小优势
- `total`: 动爻数量与动静、六亲组合、旺衰与冲合、进退趋势
- `odd_even`: 阴阳爻结构、动爻奇偶、预注册数理派生
- `score_shape`: 强弱差 + 总进球区间，只判断 0差/1差/2+差等结构，不凭单一数字硬断比分
- `htft/script`: 初中上爻位置、动变先后、伏吟反复等过程标签

### Qimen / 奇门
- `result`: 主客落宫、生克、旺衰、门星神组合、空亡入墓、击刑门迫
- `handicap`: 宫位力量差及主客受生受克幅度
- `total`: 开生景 vs 休死杜、九星动静、门星组合对比赛开放度的映射
- `odd_even`: 宫数/门数等数理仅作为独立实验支路，不与同源数值重复计票
- `score_shape`: 强弱差 + 开放度
- `htft/script`: 伏吟/反吟、主客宫状态与过程变化

### Meihua / 梅花易数
- `result`: 体用生克、体用旺衰、主互变卦关系
- `total`: 卦象动静、互变、五行生克链
- `odd_even`: 卦数、动爻数、体用数理的不同预注册版本
- `score_shape`: 体用强弱差与总量信号交叉
- `htft/script`: 本卦 -> 互卦 -> 变卦作为前中后过程假设

### Daliuren / 大六壬
- `result`: 四课主客、三传生克、课体、天将、旺衰空墓
- `total`: 三传递进/递退、课传动静、将神组合
- `score_shape`: 主客课传力量差 + 总量
- `htft/script`: 初传 -> 中传 -> 末传天然用于前段/中段/终局脚本
- `odd_even`: only separately pre-registered numerical variants; do not infer after result

### Experimental methods
小成图、河洛数、飞宫小奇门、策轨数、太玄数、灵棋经、神易数等，必须按相同规范拆成多条 derivation。任何新派别先进入 shadow-only，不直接加权。

## Separate vote boards

Football maintains at least:
- `result`: home / draw / away
- `handicap`: favorite / underdog / push-zone
- `total`: low / mid / high or line-specific over/under
- `odd_even`: odd / even
- `score_shape`: draw-score / one-goal-margin / two-plus-margin; plus goal band
- `htft`: HH / HD / HA / DH / DD / DA / AH / AD / AA or a reduced script class
- `tempo/script`: slow / balanced / open / late-swing etc.

Basketball maintains:
- `result`: home / away
- `handicap`: home-cover / away-cover
- `total`: over / under
- `margin_band`: close / medium / large
- `script`: home-fast-start / away-fast-start / close-late / reversal etc.

Do not add all these boards into one meaningless grand total. Each market/output has its own ensemble result.

## Blind-validation priority metrics

For the iterative blind-test loop, use the following evaluation priority so that useful specialist signals are not hidden by one coarse overall accuracy number.

### Football priority
1. `total`: total-goals over/under. Primary judgment uses the actual pre-match market total line when available; also log a standardized 2.5-goal audit label for cross-match comparison.
2. `odd_even`: odd/even of the final total number of goals.
3. `result`: 1X2.
4. `handicap`: line-specific handicap outcome when a frozen pre-match line is available.
5. `score_shape`, exact-score Top-K and HT/FT as secondary diagnostic outputs.

For each football round, freeze both the line and the prediction. If the market line is 2.75/3.0/3.25, score the actual line according to its own push/half-win structure, while the standardized 2.5 label remains a separate research metric.

### Basketball priority
1. `total`: final combined-score over/under against the frozen pre-match total line. This is the primary basketball target.
2. `handicap`: spread-cover result against the frozen pre-match spread; record pushes separately.
3. `result`: moneyline winner as a supporting target.
4. `margin_band` and projected score interval as diagnostics.

Never compare a prediction against a closing line that was not available at prediction time. Each round records the exact total/spread snapshot used.

## Iterative blind-test protocol

Run rounds sequentially rather than tuning all matches at once:

1. Randomly select an eligible historical match without using its final score as a feature.
2. Lock match identity, kickoff, pre-match data, odds/lines, divination setup time, model/rule versions and seed.
3. Generate every method's multidimensional internal votes.
4. Freeze the cross-method vote boards and quantitative/market outputs.
5. Only after the prediction artifact is frozen, reveal the final score/result.
6. Score football total + odd/even first; score basketball total + spread first; then score secondary targets.
7. Attribute errors to method × derivation × output dimension rather than declaring a whole method right/wrong.
8. Modify only pre-registered rule weights/thresholds justified by accumulated earlier rounds. Never repair the just-finished prediction retroactively.
9. Version the model before the next round.
10. After 5 rounds, do an exploratory review; after 10 rounds, compare V1/V2/V3 chronologically. Treat 5–10 matches as pilot evidence only, not proof of predictive validity.

## Voting policy

### Stage 1 — internal method vote
For one method + one dimension:
- weak = 0.5
- medium = 1.0
- strong = 1.5
- conflict = 0
- same `independence_group` contributes at most one strongest vote
- output internal consensus + agreement ratio + raw derivation count + independent-group count

### Stage 2 — cross-method vote
- each method contributes at most one vote to each output dimension
- internal agreement ratio moderates the method vote
- severe internal conflict becomes abstention
- result/total/odd-even/HTFT are tallied independently

### Stage 3 — statistical fusion
- historical performance decides method reliability by `method × dimension × sport/league family`
- reliability weights must be learned only from earlier frozen matches
- before validation, divination votes are shown as shadow signals and have zero production-probability weight
- after validation, a vote can enter probability fusion only if it improves chronological OOS scoring in multiple windows

## Football outputs
- 1X2 probability and display direction
- handicap probability if line supplied
- totals distribution and line-specific over/under
- standardized 2.5-goal over/under audit label
- odd/even
- score Top-3 and coverage
- HT/FT
- zero-goal probability
- per-method internal vote tables
- cross-method vote tables
- disagreement and upset-risk flags

## Basketball outputs
- winner probability
- spread-cover probability when line supplied
- expected margin and uncertainty
- expected total and over/under probability when line supplied
- score interval / sampled score
- per-method internal vote tables
- cross-method vote tables
- disagreement flag

## Fusion policy
Until a method passes chronological out-of-sample validation for the specific output dimension, its coefficient is zero in production probability and it is shown only as an independent shadow signal.

Validated coefficients must be estimated on past data only. The challenger must improve the relevant proper score or error metric in multiple future windows before promotion.

## Review metrics
Football: total-line hit/push/half result, standardized O/U 2.5 accuracy, odd/even accuracy, 1X2 accuracy/Brier/LogLoss, calibration, goal MAE, score Top-K coverage and handicap metrics when a valid line exists.
Basketball: total-line hit/push, spread-cover hit/push, winner Brier/LogLoss, margin MAE/RMSE, total MAE/RMSE and spread/total calibration.

Additionally maintain a matrix:
`method × derivation × output dimension × sample class × hit/proper-score contribution`
so we can learn that, for example, a method may be useful for totals but useless for winner direction.

## Anti-leak rules
- no post-result recasting of a divination chart
- no odds snapshot from after the prediction timestamp
- no silent rule-version change
- no tuning from one match
- no combining variants of the same derived signal as independent votes
- no using one successful output (e.g. total) to retroactively claim the whole method predicted the match correctly
