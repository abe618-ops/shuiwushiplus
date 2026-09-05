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
6. Convert divination outputs to the unified event-card schema.
7. Measure agreement/disagreement; never count correlated signals twice.
8. Produce probabilities first, then a single display direction plus confidence.
9. Save model version, seed, input completeness and disagreement flags.
10. After the event, append result and score the frozen forecast. Never rewrite the pre-match forecast.

## Football outputs
- 1X2 probability and display direction
- handicap probability if line supplied
- totals distribution
- odd/even
- score Top-3 and coverage
- HT/FT
- zero-goal probability
- disagreement and upset-risk flags

## Basketball outputs
- winner probability
- spread-cover probability when line supplied
- expected margin and uncertainty
- expected total and over/under probability when line supplied
- score interval / sampled score
- disagreement flag

## Divination event card
Each method returns:
```json
{
  "method": "liuyao|qimen|meihua|daliuren|experimental",
  "side": "home|draw|away|neutral",
  "tempo": "slow|medium|fast|neutral",
  "goal_band": "0-1|2|3|4+|neutral",
  "odd_even": "odd|even|neutral",
  "script": "early-home|early-away|stalemate|late-swing|open-game|unknown",
  "strength": "weak|medium|strong|conflict",
  "rule_version": "string",
  "setup_time": "ISO-8601",
  "seed": "string|null",
  "notes": []
}
```

## Fusion policy
Until a method passes chronological out-of-sample validation, its coefficient is zero in production probability and it is shown only as an independent shadow signal.

Validated divination coefficients must be estimated on past data only. The challenger must improve the relevant proper score or error metric in multiple future windows before promotion.

## Review metrics
Football: accuracy, Brier, LogLoss, calibration, goal MAE, score Top-K coverage, handicap/totals hit rate where legally appropriate for research.
Basketball: winner Brier/LogLoss, margin MAE/RMSE, total MAE/RMSE, spread/total calibration.

## Anti-leak rules
- no post-result recasting of a divination chart
- no odds snapshot from after the prediction timestamp
- no silent rule-version change
- no tuning from one match
- no combining variants of the same derived signal as independent votes
