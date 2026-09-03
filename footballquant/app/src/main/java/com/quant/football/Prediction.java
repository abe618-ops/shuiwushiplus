package com.quant.football;

import org.json.JSONArray;
import org.json.JSONObject;

public class Prediction {
    public double home, draw, away, over25;
    public double lambdaHome,lambdaAway,expectedGoals,bttsYes,homeZero,awayZero,anyZero;
    public double favWin1,favWin2,favWin3Plus;
    public String favoriteSide="",distribution="",grade="C",primary="",secondary="",totalPick="",comment="";
    public String disagreement="无明显分歧",dataQuality="CONDITIONAL",cupState="常规赛态";
    public String[] scores=new String[0];
    public boolean marketBacked=false,disagreementAlert=false;
    public int bookmakerCount=0;

    public JSONObject toJson(MatchInfo m){JSONObject o=new JSONObject();try{
        o.put("match",m.toJson());o.put("home",home);o.put("draw",draw);o.put("away",away);o.put("over25",over25);
        o.put("lambdaHome",lambdaHome);o.put("lambdaAway",lambdaAway);o.put("expectedGoals",expectedGoals);o.put("bttsYes",bttsYes);
        o.put("homeZero",homeZero);o.put("awayZero",awayZero);o.put("anyZero",anyZero);o.put("favWin1",favWin1);o.put("favWin2",favWin2);o.put("favWin3Plus",favWin3Plus);o.put("favoriteSide",favoriteSide);
        o.put("distribution",distribution);o.put("grade",grade);o.put("primary",primary);o.put("secondary",secondary);o.put("totalPick",totalPick);o.put("comment",comment);
        o.put("marketBacked",marketBacked);o.put("disagreementAlert",disagreementAlert);o.put("disagreement",disagreement);o.put("dataQuality",dataQuality);o.put("cupState",cupState);o.put("bookmakerCount",bookmakerCount);
        JSONArray a=new JSONArray();for(String s:scores)a.put(s);o.put("scores",a);
    }catch(Exception ignored){}return o;}
}
