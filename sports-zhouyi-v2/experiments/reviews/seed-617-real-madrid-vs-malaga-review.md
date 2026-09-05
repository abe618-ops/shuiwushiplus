# Post-match review — seed 617

Frozen prediction commit: `ed08b6a9ebfbbbda156387b3c7e924b392f2f482`
Match: Real Madrid vs Malaga, LALIGA 2026/27 Matchday 3
Final: Real Madrid 4-0 Malaga

## Frozen outputs vs actual
- 1X2: HOME -> correct
- Standard total 2.5: UNDER -> wrong (actual total 4)
- Odd/even: EVEN -> correct (4 is even)
- Exact score: 2-0 -> wrong
- Secondary scores: 1-0 / 0-0 -> wrong
- HT/FT: DRAW/HOME -> wrong; actual HOME/HOME because Madrid led 3-0 by 30'
- Score shape: low-total 1–2 goal home margin -> wrong; actual 4-goal margin

## Error attribution
1. Cross-method pseudo-independence: Liuyao, Meihua, Qimen numeric branch, Daliuren numeric branch and Heluo all consumed the same seed 617. Their five UNDER votes were not five independent observations.
2. Parity-volume leakage: sum=14 and other even/static transforms pushed both EVEN and UNDER. The EVEN call happened to be correct, but this does not validate the UNDER inference. Even totals include 0,2,4,6...
3. Strength prior missing: seed-only round ignored obvious pre-match team-strength information. This round therefore tests the divination seed layer, not the full market/quant fusion model.
4. Process bias: several mappings translated 'static/waiting' into slow first half. Actual match was 3-0 by 30', so the script mapping failed strongly.

## V2.1 corrections for next round
- Add a `shared_seed_group` across methods. Signals derived from the same seed receive a dependency penalty at Stage 2; they cannot masquerade as fully independent votes.
- Decouple `odd_even` from `total`. No parity feature may directly vote on over/under unless separately validated.
- For total magnitude, use separate registered features (e.g. dispersion, product/remainder families, trigram/open-close mapping) and track each independently.
- Add a `seed_only` vs `full_fusion` experiment flag. Seed-only results are evaluated separately from market/quant-assisted results.
- Do not tune toward 4-0 specifically. Preserve the correction as a general dependency/feature-separation rule.

## Round score
Primary football metrics: total 0/1; odd-even 1/1.
Secondary: 1X2 1/1; exact score 0/1; HT/FT 0/1.

Pilot conclusion: useful failure. The round supports keeping parity and result boards separate, while rejecting the assumption that same-seed cross-method agreement equals independent evidence.