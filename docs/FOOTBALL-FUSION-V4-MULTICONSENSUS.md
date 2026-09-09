# Football Fusion V4.0 Multi-Consensus

Branch: `football-fusion-v4-multiconsensus`

## 1. Goal
V4 keeps the existing market/statistical predictor as the formal probability layer and adds a reproducible multi-consensus shadow layer. Traditional/number features are recorded and backtested, but do not alter formal probabilities until forward validation proves incremental value.

## 2. Formal layer
- 1X2 de-vig market prior
- Poisson / Dixon-Coles score matrix
- ZeroGoal / BTTS / margin distribution
- total-goal / odd-even / Top score outputs from one score distribution
- bookmaker consensus, 12BET priority research path, Kelly/return-rate comparison where available
- model-disagreement downgrade

## 3. V4 shadow feature families
### Dual three-digit axes
- manual number: 000-999, once entered it is frozen for the event
- automatic number: deterministically derived from the already-frozen event seed; refresh does not reroll
- legacy Meihua V1: A upper trigram, B lower trigram, moving line = (A+B+C) mod 6, 0=>line 6

### Tai Xuan / number layer
- Tai Xuan 81-state experiment: `state = number % 81 + 1`; state is expanded to four ternary digits 1/2/3
- Five-element number table: 1/6 water, 2/7 fire, 3/8 wood, 4/9 metal, 5 earth
- digit 0 has no classical entry in that 1-9 table; V4 labels it `earth*` explicitly as an experiment, never as an original classical rule
- Tai Xuan stem numbers: Jia/Ji 9, Yi/Geng 8, Bing/Xin 7, Ding/Ren 6, Wu/Gui 5
- Tai Xuan branch numbers: Zi/Wu 9, Chou/Wei 8, Yin/Shen 7, Mao/You 6, Chen/Xu 5, Si/Hai 4
- day stem/branch uses a frozen Gregorian-day algorithm anchored at 2000-01-07 = Jia-Zi day
- branch -> 12-lu mapping is stored as an explicitly experimental feature

### Random control
A separate SHA-256-derived random-control index is stored per event. It is never included as a traditional vote. Any future traditional model should be compared against this placebo/control baseline.

## 4. Experimental consensus
Current coefficients are pre-registered only for generating a shadow label:
- legacy Meihua: 1.00
- five-element experiment: 0.55
- Tai Xuan ternary state: 0.35
- stem/branch number feature: 0.15

Formal traditional weight = 0.0.

The shadow output contains:
- experimental over/under tendency
- experimental odd/even tendency
- family agreement count
- random-control index
- complete source features for replay

It must not be displayed as a calibrated probability.

## 5. Four-quadrant total goals
The four labels are derived from the formal total-goal and formal odd/even outputs:
- small-even: 0/2 goals at the 2.5-goal research boundary
- small-odd: 1 goal
- big-odd: 3/5/7... goals
- big-even: 4/6/8... goals

If either formal dimension is PASS/near-50-50, the four-quadrant output is `borderline / no pick` rather than forcing a label.

## 6. Half/full-time model
V4 adds a separate time-joint model instead of inferring half/full directly from the full-time Top score.
- first-half expected goals = 45% of each team's full-time lambda
- second-half increment = remaining 55%
- enumerate HT score and second-half increments
- aggregate the nine HT/FT outcomes
- output Top1 probability and Top3 scenarios

The 45% share is frozen in V4.0 and must later be calibrated by league/time-window. It is an approximate time model, not a claim that every league has the same scoring profile.

## 7. Audit and persistence
- BASE prediction snapshot remains immutable
- DEEP market snapshot is stored separately
- V4 shadow snapshots are append-only and contain system version, manual/auto numbers, Tai Xuan state, five-element features, stem/branch/lv features, experimental signals, random control and half/full result
- rerolling the same event is prohibited

## 8. Validation protocol
Compare at least:
1. market baseline
2. statistical baseline
3. statistical + each individual shadow family as a challenger
4. statistical + selected family interactions
5. random-control placebo

Recommended gates:
- 30 matches: process/data audit only
- 50 matches: stability and obvious structural bias
- 100 matches: first weight discussion
- 500-1000 forward/OOS matches: stronger stability claims

Report separately:
- 1X2 accuracy/Brier/log-loss/calibration
- total-goal MAE and over/under accuracy
- odd/even accuracy
- four-quadrant accuracy
- Top1/Top3 score coverage
- half/full accuracy
- favorite margin buckets
- shadow family vs random-control incremental value

Do not rewrite prior forecasts after results are known. Rule changes start from a new version and the next event.
