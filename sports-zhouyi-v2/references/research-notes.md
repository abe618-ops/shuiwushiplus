# Public Research Notes

Updated: 2026-09-05

## 1. GitHub findings

### football-match-predict
Public football prediction code/data project. Useful chiefly for its historical football-data CSV field conventions and bookmaker/total-goals feature vocabulary. It is not a Zhouyi project, but it is useful for constructing the quantitative/market baseline.

### worldcup-2026-predictor-skill
A portable AI skill combining Elo-style ratings, football-randomness-adjusted Poisson and Monte Carlo, with an optional divination mode. Important design lesson: rational model and divination mode are separable rather than silently mixed.

### PitchSignals
Evidence-driven pre-match workflow with 1X2 market data, structured public intelligence and uncertainty/confidence. Useful design principle: prediction should be auditable and uncertainty should remain visible.

### daily-football-predictor / football-prediction-skill / jwang0127 football
Recent projects emphasize frozen pre-match snapshots, chronological evaluation, Dixon-Coles, league calibration, multiple prediction horizons and post-match verification. These are adopted as validation infrastructure, not copied as proprietary logic.

## 2. Bilibili / public Chinese material

### 六爻纳甲筮法预测足球比赛 series — 仨智易数
Potentially codable modules mentioned publicly include:
- true solar time / time selection
- heavenly stems and earthly branches
- same-hexagram/different-outcome issue
- numeric calculation and terminal value
-旬空
-墓库

Research use: turn each claimed rule into a versioned feature and test prospectively. Do not inherit claimed accuracy.

### 奇门遁甲预测足球 — 梅花小孩国学分享
Public titles expose a more explicit workflow:
- match chart setup
- home/away positioning
- palace auspiciousness/inauspiciousness
- ranking-number distinction for simultaneous matches
- alternative home/away localization schemes
- northern/southern hemisphere yin/yang-dun discussion

Research use: these should be separate rule variants, not silently merged. The match identity and rule variant must be frozen before kickoff.

### Recent football / basketball examples
Public Bilibili examples exist for six-line football forecasts, Qimen football/basketball forecasts and Da Liu Ren basketball cases. They establish that the mapping is actively practiced, but isolated videos are case reports rather than statistical evidence.

## 3. WeChat / public-account limitation

Public web indexing of `mp.weixin.qq.com` is incomplete and unstable. Earlier research located leads such as 大六壬足球案例、策轨数课程、奇门足球内容, but author/account identity and full rules should be re-verified inside WeChat before being encoded as a canonical rule set. Reposts and marketing claims must not enter the training target.

## 4. V2 hypothesis registry

The following are hypotheses to test, not truths:

- H1: 六爻的动变/旺衰/空墓 may add information about direction or match volatility after controlling for market probability.
- H2: 奇门的主客落宫 + 宫门星神 state may be more useful for direction/script than exact score.
- H3: 大六壬初中末传 may map more naturally to match-process/HTFT labels than exact goals.
- H4: 梅花/数理 variants may be correlated with one another; treating them as independent votes will inflate confidence.
- H5: Divination signals may have no stable out-of-sample edge. The system must be able to conclude this and leave their production weight at zero.

## 5. Experiment design

For each frozen match store:
- identity key and kickoff UTC
- prediction timestamp
- market snapshots
- quant inputs and model version
- every divination event card + rule version + seed/setup time
- final frozen output
- post-match result

Run four comparisons:
1. market-only
2. quant-only
3. each divination method alone
4. market + quant + validated divination features

Use chronological walk-forward splits. Evaluate football 1X2 with Accuracy/Brier/LogLoss/calibration; totals with MAE and line-based calibration; scores with Top-3 coverage; basketball winner with Brier/LogLoss and Margin/Total with MAE/RMSE.

No rule may be promoted based on one match. First-stage target: >=100 homogeneous frozen samples per sport/market family before judging stable incremental value.
