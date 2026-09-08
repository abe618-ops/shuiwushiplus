# Football Fusion V3 APK

## Product goal
Football-only integrated prediction/research APK. Basketball must be a separate product because the settlement, scoring and model structure differ.

## Top result card
Always place the concise synthesis first:
- Match / prediction cut-off / data quality
- 1X2: single primary pick + secondary protection
- Handicap: exact market, side, line and direction
- Total goals: interval + center + over/under direction
- Odd/even: derived from score matrix only
- Score: global Top1 + representative scenario + Top3 coverage
- HT/FT: only when a separate time model is available
- 12BET: open -> current price, return rate, Kelly, K/R and relative-support path
- Manual axis / auto axis divination agreement or conflict
- Final synthesis + risk / abstain state

## Tabs
1. Summary
2. Odds & market path
3. Quant model
4. Zero-goal / goal-count experiments
5. Manual number axis
6. Auto number axis
7. Divination families
8. Cross-synthesis
9. History / forward review
10. Settings / data sources

## Market layer
12BET is pinned first. Other comparison books can include Macau, HKJC, Betfair/market data when truly available, Pinnacle/Bet365/average market according to provider permissions.

For one bookmaker and one time point:
- S = 1/OH + 1/OD + 1/OA
- R = 1/S
- p_i = (1/O_i)/S
- If provider Kelly is verified: J_i = K_i/R; E_i=-log(J_i)
- Compare OPEN, T-24h, T-6h, T-60m, T-15m and final pre-kickoff snapshots.

Important: odds / return rate / Kelly are related transforms, not three independent votes. 12BET-specific rules must be learned by competition/time/odds-range strata; do not hard-code “the highest 12BET odds cannot win”. Preserve such observations as testable features.

## Quant layer
Generate one coherent score distribution. All full-time outputs must be marginals of that same distribution: 1X2, totals, odd/even, BTTS, team-to-score-zero, margin, Asian handicap settlement, score Top-N. Candidate models include attack/defence Poisson, Dixon-Coles low-score correction, Elo/Glicko and sourced xG/lineup/rest/travel features.

## Goal-count experimental layer
Keep two different concepts separate:
- Statistical ZeroGoal = probability of home/away/any team scoring zero from the score matrix.
- Folk “0-goal SP positioning” = a separately versioned heuristic based on the public method historically discussed by the creator formerly known as “将军说球” (now “江小满不懂球正本”). Publicly visible pages confirm a 0-goal positioning tutorial, reviews and a goal-count reference-table series, but do not expose enough stable material to claim a complete verified algorithm or hit-rate. The app therefore stores its SP inputs, tolerance, source/time and evaluates it prospectively before any formal weighting.

## Dual-number workflow
- Auto number: SecureRandom uniform 000-999, preserve leading zeroes.
- Manual number: user enters 000-999.
- Generate button starts a new number round: create the new auto number and clear the previous manual input in UI; do not silently overwrite a frozen forecast for the same event.
- Each axis is read independently before cross-synthesis.
- Legacy Meihua V1: A upper trigram, B lower trigram, moving line=(A+B+C) mod 6 with zero -> line 6; 0 maps Kun, 9 maps Qian.

## Divination adapter contract
Every method returns auditable fields, not just a result label:
`method_id, family_id, ruleset_id, input_mode, input_snapshot, raw_chart, derivation_trace, result_axes, status`.

Status must be one of:
- reproducible_rule
- digit_adaptation_experiment
- structural_description
- insufficient_input
- not_implemented

Planned families:
- Meihua Yishu
- Liuyao/Najia (requires calendar fields for full version)
- Qimen Dunjia
- Da Liu Ren
- Taiyi
- Xiao Liu Ren / digit methods
- Heluo
- Xiaochengtu / Shenyishu variants (deduplicated by family)
- Jinkoujue
- Bingfa Qimen / Feigong Qimen
- Taixuan / range-number systems
- Lingqijing
- Qinghua-jian divination experiments
- geomancy
- Vedic Prashna (time/location input kept distinct from match kickoff/location)

Never implement missing traditions as the old shared random-label function.

## Fusion policy
Formal calibrated probabilities are Market + Quant (+ validated sourced features). Divination formal weight starts at 0. Divination directions are displayed as a research/interaction layer. Only after time-split forward validation may a frozen future model learn coefficients for pre-registered interactions such as market-away-support + both number axes away-oriented.

## Review
Append-only by event_id + forecast_id. Keep formal market baseline, quant baseline, combined model, odds-path model, manual axis, auto axis, dual-axis families and candidate interactions separately. Report 1X2 Brier/log-loss/calibration; handicap settlement; totals probability score; odd/even; score Top1/Top3; HTFT when modeled; missingness/abstention. Never retro-edit a forecast after the result.
