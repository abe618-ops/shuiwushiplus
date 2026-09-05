# Blind Sequential Validation Protocol

## Purpose
Run 5–10 sequential experiments without outcome leakage. Each round is frozen before the result is opened. Model changes may affect only later rounds.

## Priority scoreboards
### Football
1. total goals / over-under (primary)
2. odd-even total goals (primary)
3. 1X2
4. handicap
5. score shape / HTFT

### Basketball
1. game total / over-under (primary)
2. spread cover (secondary)
3. winner
4. margin band

## Round lifecycle
1. `SELECT`: choose match without reading final score/result.
2. `FREEZE_INPUT`: record fixture identity, kickoff, market line/snapshot and source timestamps.
3. `DERIVE`: run each divination method from multiple pre-registered derivation paths.
4. `INTERNAL_VOTE`: collapse correlated evidence groups; one independence group <= one effective vote.
5. `CROSS_METHOD_VOTE`: separate boards for total, odd-even, result, handicap/spread, score/script.
6. `QUANT`: compute market + quantitative baseline separately.
7. `FREEZE_PREDICTION`: save prediction JSON, rule versions, weights, seed and confidence. Commit/hash it before reveal.
8. `REVEAL`: only now retrieve the final score and grade each board independently.
9. `REVIEW`: classify errors as data / rule / conflict / calibration / variance.
10. `ITERATE`: update only the next model version; never rewrite the frozen round.

## Anti-leak rule
If the final score/result is accidentally exposed during match selection, that match is marked `CONTAMINATED` and cannot count in blind accuracy. It may be used only as an engineering/replay example.

## Version progression
- R1 uses V2.0 rules.
- After reveal, produce V2.1 only if a pre-registered change is justified.
- R2 uses V2.1, and so on.
- Keep a parallel `V2.0 frozen` benchmark through all rounds so iteration is compared with a fixed baseline.

## Required per-round record
```json
{
  "round": 1,
  "status": "SELECTED|FROZEN|REVEALED|CONTAMINATED",
  "sport": "football|basketball",
  "match": "home vs away",
  "kickoff": "ISO-8601",
  "market_snapshot_time": "ISO-8601",
  "football": {
    "total_line": null,
    "total_pick": null,
    "odd_even": null,
    "result": null,
    "handicap_line": null,
    "handicap_pick": null
  },
  "basketball": {
    "total_line": null,
    "total_pick": null,
    "spread_line": null,
    "spread_pick": null,
    "winner": null
  },
  "method_votes": {},
  "quant_baseline": {},
  "fusion": {},
  "model_version": "V2.0",
  "seed": "",
  "frozen_at": "ISO-8601",
  "final_score": null,
  "grading": null,
  "change_for_next_round": null
}
```

## Evaluation
Do not report one blended accuracy number alone. Always show:
- football totals hit rate
- football odd/even hit rate
- football 1X2/handicap separately
- basketball totals hit rate
- basketball spread hit rate
- per-method × dimension hit rate
- fixed V2.0 vs sequentially updated model

5–10 rounds are exploratory only. They are useful for debugging and hypothesis generation, not evidence of stable predictive edge.