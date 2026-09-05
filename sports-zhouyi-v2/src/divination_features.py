from __future__ import annotations

from dataclasses import dataclass, asdict
from typing import List, Optional

VALID_METHODS = {"liuyao", "qimen", "meihua", "daliuren", "experimental"}


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
        if self.strength not in {"weak", "medium", "strong", "conflict"}:
            raise ValueError("invalid strength")

    def to_dict(self) -> dict:
        self.validate()
        return asdict(self)


def agreement(cards: List[DivinationEventCard]) -> dict:
    """Summarize independent shadow signals. This does not alter production probabilities."""
    for c in cards:
        c.validate()
    active = [c for c in cards if c.side != "neutral"]
    counts = {k: 0 for k in ("home", "draw", "away")}
    for c in active:
        if c.side in counts:
            counts[c.side] += 1
    winner = max(counts, key=counts.get) if active else "neutral"
    nmax = counts.get(winner, 0) if active else 0
    tied = active and list(counts.values()).count(nmax) > 1
    return {
        "counts": counts,
        "consensus": "conflict" if tied else winner,
        "n_active": len(active),
        "warning": "shadow-only until chronological validation passes",
    }
