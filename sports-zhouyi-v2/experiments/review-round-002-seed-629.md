# Review Round 002 — Seed 629

## Frozen prediction reference
Commit: `6777b855387f4d645790ef280a919e747c3f0b7f`
Match: Lecce vs Roma, Serie A 2026/27 Matchday 2
Mode: `seed-only-v2.1`

Frozen outputs:
- Result: Roma / Away win
- Total 2.5: OVER
- Odd-even: ODD
- Exact display score: Lecce 1-2 Roma
- HT/FT: Draw / Away

## Revealed result
Final: Lecce 0-4 Roma
Half-time: Lecce 0-3 Roma
Total goals: 4
Parity: EVEN

## Scoring
- 1X2: CORRECT
- O/U 2.5: CORRECT
- Odd-even: WRONG
- Exact score: WRONG
- HT/FT: WRONG

## Round-002 diagnostics

### What worked
1. Cross-method result board ended in draw/away conflict, and the registered tie-break selected AWAY. This matched the final result.
2. Total board had 3 Over vs 2 Under and correctly selected OVER 2.5.
3. Qimen/Daliuren numerical branches both carried late/expansion language, which was directionally closer to the high-scoring outcome than the constraint branches.

### What failed
1. Odd-even relied too heavily on seed-sum parity and produced ODD despite a 4-goal final total. Seed parity must not be treated as a near-direct proxy for match parity.
2. The exact-score family (1-2 / 1-3 / 2-3) assumed both teams had a meaningful scoring chance. The actual result was one-sided 0-4.
3. HT/FT predicted an early stalemate, but Roma were already 3-0 ahead after 25 minutes. The process model again underestimated early dominance.
4. The seed range was high: 9-2 = 7. The model used this mainly as a total/expansion clue, but did not allow it to widen the expected margin band enough.

## Cross-round pilot table

| Metric | Round 001 seed 617 | Round 002 seed 629 | Cumulative |
|---|---|---|---|
| 1X2 | correct | correct | 2/2 |
| O/U 2.5 | wrong | correct | 1/2 |
| Odd-even | correct | wrong | 1/2 |
| Exact score | wrong | wrong | 0/2 |
| HT/FT | wrong | wrong | 0/2 |

This is only a two-match pilot and has no evidentiary power for stable predictive validity.

## V2.2 challenger changes — prospective only
These changes apply only from Round 003 onward and must not be used to re-score Rounds 001-002.

### A. Add a dedicated dispersion channel
- digit range >= 6 -> `high_seed_dispersion = true`
- high dispersion does NOT automatically mean Over
- it widens `margin_band` and exact-score-family uncertainty
- when result direction has a clear lean, high dispersion must include at least one two-plus-goal-margin candidate in score Top-K

### B. Decouple parity from seed-sum parity
- raw digit-sum odd/even becomes only one weak EvidenceUnit (weight 0.5)
- parity requires at least two structurally different derivation groups before medium confidence is allowed
- no direct rule `odd seed sum -> odd match total`

### C. Separate first-half dominance from late-swing language
- result strength and tempo must feed a dedicated first-half board
- if two or more independent result/tempo branches indicate strong directional pressure, HT/FT cannot default to Draw/X solely from a generic 'compression first' label

### D. Maintain common-seed correlation penalty
All divination methods still share seed 629. Their raw vote counts are not independent evidence. Continue reporting raw vote and effective-confidence separately.

## Status
V2.2 challenger rules registered after Round 002. Next round must use them prospectively before result lookup.
