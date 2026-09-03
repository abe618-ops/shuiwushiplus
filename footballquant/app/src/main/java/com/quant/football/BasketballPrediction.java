package com.quant.football;

public class BasketballPrediction {
    public double homeWin,awayWin,expectedMargin,expectedTotal,homePoints,awayPoints;
    public double coverHome=0.5,overMarket=0.5,margin1to5,margin6to10,margin11to15,margin16plus;
    public String winner="",spreadPick="",totalPick="",grade="C",dataQuality="REJECTED/LOW DATA",disagreement="无明显分歧",comment="";
    public boolean disagreementAlert=false;
}
