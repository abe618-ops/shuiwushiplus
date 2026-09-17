package com.quant.football;

import java.util.Locale;

/**
 * Rules shared by the daily J竞彩 / 北京单场 prediction surface.
 * Keeps market identity, pre-match time slicing and abstention decisions explicit.
 */
public final class DailyCompetitionPolicy {
    public enum Competition { JINGCAI, BEIDAN, OTHER }

    public static Competition competitionOf(MatchInfo m){
        String s=(m==null||m.source==null)?"":m.source.toLowerCase(Locale.US);
        if(s.contains("竞彩")||s.contains("jingcai")||s.contains("jc"))return Competition.JINGCAI;
        if(s.contains("北单")||s.contains("beidan")||s.contains("bd"))return Competition.BEIDAN;
        return Competition.OTHER;
    }

    public static boolean shouldAbstain(Prediction p){
        if(p==null)return true;
        double top=Math.max(p.home,Math.max(p.draw,p.away));
        double second;
        if(top==p.home)second=Math.max(p.draw,p.away);
        else if(top==p.draw)second=Math.max(p.home,p.away);
        else second=Math.max(p.home,p.draw);
        double margin=top-second;
        if("REJECTED/LOW DATA".equals(p.dataQuality))return true;
        if(p.disagreementAlert && (top<0.56 || margin<0.12))return true;
        return top<0.42 || margin<0.055;
    }

    public static String riskLabel(Prediction p){
        if(p==null)return "高";
        if(shouldAbstain(p))return "高";
        double top=Math.max(p.home,Math.max(p.draw,p.away));
        if(top>=0.68 && !p.disagreementAlert)return "中";
        return "中高";
    }

    private DailyCompetitionPolicy(){}
}
