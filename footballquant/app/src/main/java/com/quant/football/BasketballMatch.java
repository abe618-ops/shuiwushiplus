package com.quant.football;

public class BasketballMatch {
    public String source="ESPN",league="",kickoff="",home="",away="",status="",score="",matchId="",matchDate="";
    public double homeWinPct=-1,awayWinPct=-1;
    public double marketSpread=Double.NaN,marketTotal=Double.NaN;
    public double homeMoneyline=0,awayMoneyline=0;
    public String marketDetails="";

    public MatchInfo asShadowMatch(){
        MatchInfo m=new MatchInfo();m.source="BB";m.code=matchId;m.league=league;m.kickoff=kickoff;m.home=home;m.away=away;m.matchId=matchId;m.matchDate=matchDate;return m;
    }
}
