package com.quant.football;

import org.json.JSONObject;
import java.util.Locale;

public class GoalEnsemble {
    public static class Result {
        public double over25,expectedGoals,p01,p23,p4plus,oddProb,confidence;
        public int activeVoteCount,configuredModelCount;
        public String pick="",range="",oddPick="",bestTotals="",familySummary="",divinationSummary="",paritySummary="",zeroRule="",dfcem="";
        public JSONObject toJson(){JSONObject o=new JSONObject();try{o.put("over25",over25);o.put("expectedGoals",expectedGoals);o.put("p01",p01);o.put("p23",p23);o.put("p4plus",p4plus);o.put("oddProb",oddProb);o.put("confidence",confidence);o.put("activeVoteCount",activeVoteCount);o.put("configuredModelCount",configuredModelCount);o.put("pick",pick);o.put("range",range);o.put("oddPick",oddPick);o.put("bestTotals",bestTotals);o.put("familySummary",familySummary);o.put("divinationSummary",divinationSummary);o.put("paritySummary",paritySummary);o.put("zeroRule",zeroRule);o.put("dfcem",dfcem);}catch(Exception ignored){}return o;}
    }
    public Result predict(MatchInfo m,Prediction p,MarketSnapshot snap,DivinationEngine.Bundle d){
        Result r=new Result();double stat=p.over25,wStat=.42,market=0,wMarket=0,form=0,wForm=0,heur=0,wHeur=.10,shadow=.5,wShadow=.05;
        if(snap!=null&&snap.totalLine>0){market=clamp(.5+(p.expectedGoals-snap.totalLine)*.16,.25,.75);wMarket=.28;}
        if(m.recentHomeAttack>=0&&m.recentAwayAttack>=0){double recent=m.recentHomeAttack+m.recentAwayAttack;form=clamp(.5+(recent-2.5)*.12,.25,.75);wForm=.15;}
        if(d!=null){shadow=clamp(.5+(d.totalOverCount-d.totalUnderCount)*.018,.35,.65);r.divinationSummary=DivinationEngine.summary(d);}else wShadow=0;
        heur=clamp(.5+(p.expectedGoals-2.5)*.09,.35,.65);double z=wStat+wMarket+wForm+wHeur+wShadow;r.over25=(wStat*stat+wMarket*market+wForm*form+wHeur*heur+wShadow*shadow)/z;r.expectedGoals=p.expectedGoals;r.pick=r.over25>=.56?"大2.5":r.over25<=.44?"小2.5":"大小PASS";
        double e=p.expectedGoals;r.p01=clamp(Math.exp(-e)*(1+e),0,1);r.p4plus=clamp(1-(Math.exp(-e)*(1+e+e*e/2+e*e*e/6)),0,1);r.p23=clamp(1-r.p01-r.p4plus,0,1);r.range=r.p23>=Math.max(r.p01,r.p4plus)?"2-3球":r.p01>=r.p4plus?"0-1球":"4+球";
        r.oddProb=clamp(.5+(r.over25-.5)*.08,.44,.56);r.oddPick=r.oddProb>.525?"奇数":r.oddProb<.475?"偶数":"中性";r.confidence=clamp(.50+Math.abs(r.over25-.5)*.8,.50,.78);r.activeVoteCount=(wMarket>0?1:0)+(wForm>0?1:0)+3;r.configuredModelCount=r.activeVoteCount;r.bestTotals=r.pick+" / "+r.range;r.familySummary=String.format(Locale.US,"统计%.0f%% · 市场%s · 基本面%s · 经验 · 术数Shadow",wStat/z*100,wMarket>0?"启用":"缺失",wForm>0?"启用":"缺失");r.paritySummary="奇偶反转实验："+r.oddPick;r.zeroRule="ZeroGoal 至少一队0球 "+Prediction.pct(p.anyZero);r.dfcem="D/F/C/EP/M经验链仅作低权重启发";return r;
    }
    private static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}
}
