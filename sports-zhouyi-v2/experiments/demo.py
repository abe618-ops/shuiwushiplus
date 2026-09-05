from pprint import pprint
from src.quant_baseline import football_summary, BasketballInputs, basketball_summary
from src.divination_features import DivinationEventCard, agreement
from src.fusion import production_fusion


def football_demo():
    # Synthetic reproducible experiment; replace inputs with frozen real match data.
    quant = football_summary(1.55, 1.05, seed="football-demo-001")
    market = {"home": 0.47, "draw": 0.28, "away": 0.25}
    shadow = [
        DivinationEventCard(method="liuyao", side="home", tempo="medium", goal_band="2", strength="medium", rule_version="ly-v0.1"),
        DivinationEventCard(method="qimen", side="home", tempo="fast", goal_band="3", strength="weak", rule_version="qm-v0.1"),
        DivinationEventCard(method="meihua", side="draw", tempo="slow", goal_band="2", strength="weak", rule_version="mh-v0.1"),
    ]
    fused = production_fusion(market, quant["1x2"], divination_validated=False)
    return {"quant": quant, "divination_shadow": agreement(shadow), "fusion": fused}


def basketball_demo():
    x = BasketballInputs(
        possessions=99.0,
        home_ortg=116.0,
        home_drtg=111.0,
        away_ortg=113.0,
        away_drtg=114.0,
    )
    return basketball_summary(x, spread_home=-3.5, total_line=228.5)


if __name__ == "__main__":
    print("=== football ===")
    pprint(football_demo())
    print("=== basketball ===")
    pprint(basketball_demo())
