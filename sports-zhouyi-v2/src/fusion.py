from __future__ import annotations

from math import exp
from typing import Dict, Iterable


def normalize(p: Dict[str, float]) -> Dict[str, float]:
    z = sum(max(v, 0.0) for v in p.values())
    if z <= 0:
        raise ValueError("probabilities must have positive mass")
    return {k: max(v, 0.0) / z for k, v in p.items()}


def logit_pool(base: Dict[str, float], feature_deltas: Iterable[Dict[str, float]]) -> Dict[str, float]:
    """Experimental calibrated pooling. Deltas must come from pre-trained, OOS-validated coefficients.

    Never hand-set a divination boost here from a single match.
    """
    p = normalize(base)
    logits = {k: 0.0 for k in p}
    # Multiclass geometric/log pool: log(p_k) + additive validated deltas.
    from math import log
    for k, v in p.items():
        logits[k] = log(max(v, 1e-12))
    for delta in feature_deltas:
        for k, v in delta.items():
            if k in logits:
                logits[k] += float(v)
    mx = max(logits.values())
    e = {k: exp(v - mx) for k, v in logits.items()}
    return normalize(e)


def production_fusion(market: Dict[str, float], quant: Dict[str, float], divination_validated: bool = False, divination_delta: Dict[str, float] | None = None) -> dict:
    """V2 production guardrail.

    Market and quant are blended conservatively. Divination has zero production weight
    until a separately documented chronological validation gate is passed.
    """
    market = normalize(market)
    quant = normalize(quant)
    keys = market.keys() & quant.keys()
    base = normalize({k: 0.55 * market[k] + 0.45 * quant[k] for k in keys})
    used_divination = bool(divination_validated and divination_delta)
    final = logit_pool(base, [divination_delta]) if used_divination else base
    return {
        "probabilities": final,
        "market_weight": 0.55,
        "quant_weight": 0.45,
        "divination_used": used_divination,
        "divination_policy": "zero weight until chronological OOS validation",
    }
