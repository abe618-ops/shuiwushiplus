package com.quant.football;

import org.json.JSONArray;
import org.json.JSONObject;

public class MatchInfo {
    public String source="",code="",league="",kickoff="",home="",away="",handicap="",status="",score="",matchId="",matchDate="";
    public double oddsHome=0,oddsDraw=0,oddsAway=0;
    public int homeRank=-1,awayRank=-1;
    public boolean secondLeg=false;
    public int aggregateLeadHome=0;
    public double[] ttgSp=new double[8];
    public int rematchDays=-1,lastH2HHomeGoals=-1,lastH2HAwayGoals=-1;
    public boolean lastH2HBtts=false;
    public double recentHomeAttack=-1,recentAwayAttack=-1,recentHomeDefense=-1,recentAwayDefense=-1;

    public String key(){if(!matchId.isEmpty())return source+"|id|"+matchId;if(!code.isEmpty())return source+"|code|"+code;return source+"|"+home+"|"+away+"|"+kickoff;}
    public JSONObject toJson(){JSONObject o=new JSONObject();try{
        o.put("source",source);o.put("code",code);o.put("league",league);o.put("kickoff",kickoff);o.put("matchDate",matchDate);o.put("home",home);o.put("away",away);o.put("handicap",handicap);o.put("status",status);o.put("score",score);o.put("matchId",matchId);
        o.put("oddsHome",oddsHome);o.put("oddsDraw",oddsDraw);o.put("oddsAway",oddsAway);o.put("homeRank",homeRank);o.put("awayRank",awayRank);o.put("secondLeg",secondLeg);o.put("aggregateLeadHome",aggregateLeadHome);
        JSONArray a=new JSONArray();for(double v:ttgSp)a.put(v);o.put("ttgSp",a);o.put("rematchDays",rematchDays);o.put("lastH2HHomeGoals",lastH2HHomeGoals);o.put("lastH2HAwayGoals",lastH2HAwayGoals);o.put("lastH2HBtts",lastH2HBtts);o.put("recentHomeAttack",recentHomeAttack);o.put("recentAwayAttack",recentAwayAttack);o.put("recentHomeDefense",recentHomeDefense);o.put("recentAwayDefense",recentAwayDefense);
    }catch(Exception ignored){}return o;}
    public static MatchInfo fromJson(JSONObject m){MatchInfo x=new MatchInfo();if(m==null)return x;x.source=m.optString("source");x.code=m.optString("code");x.league=m.optString("league");x.kickoff=m.optString("kickoff");x.matchDate=m.optString("matchDate");x.home=m.optString("home");x.away=m.optString("away");x.handicap=m.optString("handicap");x.status=m.optString("status");x.score=m.optString("score");x.matchId=m.optString("matchId");x.oddsHome=m.optDouble("oddsHome");x.oddsDraw=m.optDouble("oddsDraw");x.oddsAway=m.optDouble("oddsAway");x.homeRank=m.optInt("homeRank",-1);x.awayRank=m.optInt("awayRank",-1);x.secondLeg=m.optBoolean("secondLeg",false);x.aggregateLeadHome=m.optInt("aggregateLeadHome",0);JSONArray a=m.optJSONArray("ttgSp");if(a!=null)for(int i=0;i<Math.min(8,a.length());i++)x.ttgSp[i]=a.optDouble(i,0);x.rematchDays=m.optInt("rematchDays",-1);x.lastH2HHomeGoals=m.optInt("lastH2HHomeGoals",-1);x.lastH2HAwayGoals=m.optInt("lastH2HAwayGoals",-1);x.lastH2HBtts=m.optBoolean("lastH2HBtts",false);x.recentHomeAttack=m.optDouble("recentHomeAttack",-1);x.recentAwayAttack=m.optDouble("recentAwayAttack",-1);x.recentHomeDefense=m.optDouble("recentHomeDefense",-1);x.recentAwayDefense=m.optDouble("recentAwayDefense",-1);return x;}
}
