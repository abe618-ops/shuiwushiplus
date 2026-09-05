from __future__ import annotations

from dataclasses import dataclass
from math import exp, factorial, erf, sqrt
from random import Random
from typing import Dict, Tuple


def _poisson(k: int, lam: float) -> float:
    return exp(-lam) * lam ** k / factorial(k)


def football_score_matrix(lambda_home: float, lambda_away: float, max_goals: int = 8) -> Dict[Tuple[int, int], float]:
    """Independent Poisson baseline. Dixon-Coles correction can be layered on top."""
    matrix = {}
    z = 0.0
    for h in range(max_goals + 1):
        for a in range(max_goals + 1):
            p = _poisson(h, lambda_home) * _poisson(a, lambda_away)
            matrix[(h, a)] = p
            z += p
    return {k: v / z for k, v in matrix.items()}


def football_summary(lambda_home: float, lambda_away: float, seed: str = "demo") -> dict:
    m = football_score_matrix(lambda_home, lambda_away)
    home = sum(p for (h, a), p in m.items() if h > a)
    draw = sum(p for (h, a), p in m.items() if h == a)
    away = sum(p for (h, a), p in m.items() if h < a)
    odd = sum(p for (h, a), p in m.items() if (h + a) % 2 == 1)
    zero_goal = m.get((0, 0), 0.0)
    top3 = sorted(m.items(), key=lambda kv: kv[1], reverse=True)[:3]

    rng = Random(seed)
    r = rng.random()
    acc = 0.0
    sampled = (0, 0)
    for score, p in sorted(m.items()):
        acc += p
        if r <= acc:
            sampled = score
            break

    return {
        "lambda_home": lambda_home,
        "lambda_away": lambda_away,
        "1x2": {"home": home, "draw": draw, "away": away},
        "odd_even": {"odd": odd, "even": 1 - odd},
        "zero_goal": zero_goal,
        "top3_scores": [{"score": f"{s[0]}-{s[1]}", "p": p} for s, p in top3],
        "sampled_score": f"{sampled[0]}-{sampled[1]}",
        "seed": seed,
    }


def _normal_cdf(x: float, mean: float, sd: float) -> float:
    return 0.5 * (1.0 + erf((x - mean) / (sd * sqrt(2.0))))


@dataclass
class BasketballInputs:
    possessions: float
    home_ortg: float
    home_drtg: float
    away_ortg: float
    away_drtg: float
    home_advantage_points: float = 2.5
    margin_sd: float = 12.0
    total_sd: float = 16.0


def basketball_summary(x: BasketballInputs, spread_home: float | None = None, total_line: float | None = None) -> dict:
    # Symmetric attack/defense blend per 100 possessions.
    home_pp100 = (x.home_ortg + x.away_drtg) / 2.0
    away_pp100 = (x.away_ortg + x.home_drtg) / 2.0
    home_score = x.possessions * home_pp100 / 100.0 + x.home_advantage_points / 2.0
    away_score = x.possessions * away_pp100 / 100.0 - x.home_advantage_points / 2.0
    margin = home_score - away_score
    total = home_score + away_score
    p_home = 1.0 - _normal_cdf(0.0, margin, x.margin_sd)

    out = {
        "expected_home": home_score,
        "expected_away": away_score,
        "expected_margin_home": margin,
        "expected_total": total,
        "winner": {"home": p_home, "away": 1.0 - p_home},
    }
    if spread_home is not None:
        out["spread"] = {
            "line_home": spread_home,
            "p_home_cover": 1.0 - _normal_cdf(-spread_home, margin, x.margin_sd),
        }
    if total_line is not None:
        out["total"] = {
            "line": total_line,
            "p_over": 1.0 - _normal_cdf(total_line, total, x.total_sd),
        }
    return out
