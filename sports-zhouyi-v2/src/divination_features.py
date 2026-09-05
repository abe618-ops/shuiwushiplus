from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass, asdict
from typing import Dict, Iterable, List, Optional

VALID_METHODS = {"liuyao", "qimen", "meihua", "daliuren", "xiaochengtu", "heluo", "experimental"}
VALID_DIMENSIONS = {"result", "handicap", "total", "odd_even", "score_shape", "htft", "tempo", "script"}
STRENGTH_WEIGHT = {"weak": 0.5, "medium": 1.0, "strong": 1.5, "conflict": 0.0}


@dataclass(frozen=True)
class EvidenceUnit:
    """One pre-registered derivation inside one divination method.

    independence_group prevents correlated sub-rules from being counted as multiple
    independent votes. Only the strongest evidence in the same group contributes to
    one method's internal vote for a given dimension.
    """

    method: str
    dimension: str
    derivation: str
    signal: str
    strength: str = "medium"
    independence_group: str = "default"
    rule_version: str = "v0"
    notes: tuple[str, ...] = ()

    def validate(self) -> None:
        if self.method not in VALID_METHODS:
            raise ValueError(f"unknown method: {self.method}")
        if self.dimension not in VALID_DIMENSIONS:
            raise ValueError(f"unknown dimension: {self.dimension}")
        if self.strength not in STRENGTH_WEIGHT:
            raise ValueError(f"invalid strength: {self.strength}")
        if not self.signal:
            raise ValueError("signal must be non-empty")


@dataclass(frozen=True)
class DivinationEventCard:
    method: str
    side: str = "neutral"
    tempo: str = "neutral"
    goal_band: str = "neutral"
    odd_even: str = "neutral"
    script: str = "unknown"
    strength: str = "weak"
    rule_version: str = "v0"
    setup_time: str = ""
    seed: Optional[str] = None
    notes: tuple[str, ...] = ()

    def validate(self) -> None:
        if self.method not in VALID_METHODS:
            raise ValueError(f"unknown method: {self.method}")
        if self.side not in {"home", "draw", "away", "neutral"}:
            raise ValueError("invalid side")
        if self.tempo not in {"slow", "medium", "fast", "neutral"}:
            raise ValueError("invalid tempo")
        if self.goal_band not in {"0-1", "2", "3", "4+", "neutral"}:
            raise ValueError("invalid goal_band")
        if self.odd_even not in {"odd", "even", "neutral"}:
            raise ValueError("invalid odd_even")
        if self.strength not in STRENGTH_WEIGHT:
            raise ValueError("invalid strength")

    def to_dict(self) -> dict:
        self.validate()
        return asdict(self)


def _winner(scores: Dict[str, float]) -> tuple[str, bool]:
    if not scores:
        return "neutral", False
    mx = max(scores.values())
    leaders = [k for k, v in scores.items() if abs(v - mx) < 1e-12]
    return (leaders[0], False) if len(leaders) == 1 else ("conflict", True)


def aggregate_within_method(evidence: Iterable[EvidenceUnit]) -> dict:
    """Stage 1: multidimensional cross-check inside each divination method.

    Voting is dimension-specific. Correlated derivations sharing independence_group are
    capped to one contribution by retaining the strongest claim in that group.
    """
    items = list(evidence)
    for e in items:
        e.validate()

    by_method_dimension: dict[tuple[str, str], list[EvidenceUnit]] = defaultdict(list)
    for e in items:
        by_method_dimension[(e.method, e.dimension)].append(e)

    out: dict[str, dict[str, dict]] = defaultdict(dict)
    for (method, dimension), group in by_method_dimension.items():
        strongest_by_independence: dict[str, EvidenceUnit] = {}
        for e in group:
            old = strongest_by_independence.get(e.independence_group)
            if old is None or STRENGTH_WEIGHT[e.strength] > STRENGTH_WEIGHT[old.strength]:
                strongest_by_independence[e.independence_group] = e

        scores: dict[str, float] = defaultdict(float)
        raw_votes: dict[str, int] = defaultdict(int)
        for e in strongest_by_independence.values():
            if e.strength == "conflict":
                continue
            scores[e.signal] += STRENGTH_WEIGHT[e.strength]
            raw_votes[e.signal] += 1

        consensus, tied = _winner(dict(scores))
        total_weight = sum(scores.values())
        leader_weight = 0.0 if consensus in {"neutral", "conflict"} else scores[consensus]
        agreement_ratio = leader_weight / total_weight if total_weight else 0.0

        out[method][dimension] = {
            "scores": dict(scores),
            "raw_votes": dict(raw_votes),
            "consensus": consensus,
            "agreement_ratio": agreement_ratio,
            "n_independent_groups": len(strongest_by_independence),
            "n_raw_derivations": len(group),
            "tied": tied,
        }
    return dict(out)


def cross_method_vote(method_results: dict, min_internal_agreement: float = 0.50) -> dict:
    """Stage 2: vote across divination methods, separately for each output dimension.

    Each method contributes at most one vote per dimension. Internal agreement controls
    vote weight, so a method with severe internal conflict cannot overwhelm the ensemble.
    """
    dimensions: dict[str, list[tuple[str, dict]]] = defaultdict(list)
    for method, results in method_results.items():
        for dimension, result in results.items():
            dimensions[dimension].append((method, result))

    ensemble = {}
    for dimension, rows in dimensions.items():
        scores: dict[str, float] = defaultdict(float)
        method_votes = []
        for method, result in rows:
            signal = result["consensus"]
            agreement_ratio = float(result["agreement_ratio"])
            if signal in {"neutral", "conflict"} or agreement_ratio < min_internal_agreement:
                method_votes.append({"method": method, "signal": signal, "weight": 0.0, "counted": False})
                continue
            # One method = at most one cross-method vote. Agreement only moderates weight.
            weight = max(0.0, min(1.0, agreement_ratio))
            scores[signal] += weight
            method_votes.append({"method": method, "signal": signal, "weight": weight, "counted": True})

        consensus, tied = _winner(dict(scores))
        total = sum(scores.values())
        leader = 0.0 if consensus in {"neutral", "conflict"} else scores[consensus]
        ensemble[dimension] = {
            "scores": dict(scores),
            "consensus": consensus,
            "ensemble_agreement": leader / total if total else 0.0,
            "method_votes": method_votes,
            "tied": tied,
        }
    return ensemble


def hierarchical_divination_vote(evidence: Iterable[EvidenceUnit], min_internal_agreement: float = 0.50) -> dict:
    """Full two-stage vote: derivations -> method consensus -> cross-method consensus."""
    method_results = aggregate_within_method(evidence)
    return {
        "methods": method_results,
        "ensemble": cross_method_vote(method_results, min_internal_agreement=min_internal_agreement),
        "policy": "dimension-specific; correlated derivations capped; one method one vote per dimension",
    }


def agreement(cards: List[DivinationEventCard]) -> dict:
    """Legacy side-only summary kept for compatibility."""
    for c in cards:
        c.validate()
    active = [c for c in cards if c.side != "neutral"]
    counts = {k: 0 for k in ("home", "draw", "away")}
    for c in active:
        if c.side in counts:
            counts[c.side] += 1
    winner = max(counts, key=counts.get) if active else "neutral"
    nmax = counts.get(winner, 0) if active else 0
    tied = bool(active) and list(counts.values()).count(nmax) > 1
    return {
        "counts": counts,
        "consensus": "conflict" if tied else winner,
        "n_active": len(active),
        "warning": "legacy side-only view; prefer hierarchical_divination_vote",
    }
