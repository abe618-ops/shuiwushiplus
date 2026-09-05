# Blind Test Protocol / 盲测与迭代协议

## 1. 赛前冻结
每一场比赛在 kickoff 前写入一条不可覆盖的 prediction record。冻结内容必须包括：比赛唯一 ID、开球时间、freeze_time、数据来源快照、模型版本、各子模型独立输出、最终概率、是否弃权。

赛后只允许追加 postmatch，不允许修改赛前字段。

## 2. 四个必测基线
- B0 Market：去水后的市场概率；
- B1 Statistical：Dixon-Coles / Poisson / xG；
- B2 Shadow：每门术数单独测试；
- B3 Combined：M+S+F 主模型；Z 默认 0 权重。

## 3. 防泄漏
- 训练/校准只能使用 freeze_time 之前的数据；
- walk-forward：按真实日期从过去训练到未来；
- 禁止随机打乱赛季做主验收；
- T-10m 模型不能与 T-6h 模型混为一个成绩；
- 临场首发与收盘赔率只能用于对应的临场版本。

## 4. 指标
1X2：Accuracy, Brier, LogLoss, calibration/ECE
Asian：cover/win/push/loss + probability calibration
Totals：Accuracy/Brier/LogLoss + MAE(total goals)
Odd/Even：Accuracy/Brier
Score：Exact hit, Top3 coverage, probability assigned to true score
Half/Full：Accuracy + conditional slices

优先比较 OOS Brier 和 LogLoss 相对 Market Baseline 的改善，而不是只比较命中率。

## 5. 术数验收
每一门术数必须：
- 规则预注册；
- 同一版本连续运行；
- 不因比赛结果换门派/换用神；
- 保存原始随机数或起局信息；
- 至少 100 场做初评；
- 至少 500 场滚动样本外稳定，且相对 M+S+F 有独立增量，才可考虑 <=3% 权重。

## 6. Ablation
每轮版本至少计算：
- M
- S
- M+S
- M+S+F
- M+S+F+MarketPath
- 上述主模型 + 每一门 Z（逐一，不做无依据多数投票）

若某模块 OOS 指标变差，降权或移除；不能因最近几场命中而保留。

## 7. 分层复盘
按联赛、比赛类型、赔率热度、盘口深度、大小线、顺/逆/隐/均衡、首发完整度、市场流动性、临场波动进行切片。

错误标签统一：
identity_error, timezone_error, market_read_error, euro_asian_divergence, fundamental_error, lineup_shock, low_score_error, total_goals_error, odd_even_error, score_overfit, shadow_mapping_error, shadow_version_mix, data_leakage.

## 8. 版本迭代门槛
新版本上线前至少满足：
- 数据 schema 向后兼容或提供迁移；
- 旧版本 blind records 保持不可变；
- 自动化单元测试通过；
- OOS 评价报告包含与上一版本、Market baseline、S baseline 的比较；
- 明确列出改善与退化，不只展示最好切片。
