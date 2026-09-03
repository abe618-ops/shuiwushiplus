package com.quant.football;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class GlobalSportsDataSource {
    private final HtmlDataSource http;
    private static final String[] SOCCER={"eng.1","esp.1","ger.1","ita.1","fra.1","usa.1","bra.1","arg.1","mex.1","uefa.champions","uefa.europa","fifa.world"};
    private static final String[] BASKETBALL={"nba","wnba","mens-college-basketball","womens-college-basketball"};
    public GlobalSportsDataSource(HtmlDataSource h){http=h;}

    public List<MatchInfo> fetchFootball(String day){List<MatchInfo> out=new ArrayList<>();Set<String> seen=new HashSet<>();String d=day.replace("-","");for(String league:SOCCER){try{String u="https://site.api.espn.com/apis/site/v2/sports/soccer/"+league+"/scoreboard?dates="+d+"&limit=100";JSONObject root=new JSONObject(http.get(u,"https://www.espn.com/"));JSONArray ev=root.optJSONArray("events");if(ev==null)continue;for(int i=0;i<ev.length();i++){MatchInfo m=parseFootball(ev.optJSONObject(i),league,day);if(m!=null&&seen.add(m.key()))out.add(m);}}catch(Exception ignored){}}
        return out;}

    public List<BasketballMatch> fetchBasketball(String day){List<BasketballMatch> out=new ArrayList<>();Set<String> seen=new HashSet<>();String d=day.replace("-","");for(String league:BASKETBALL){try{String u="https://site.api.espn.com/apis/site/v2/sports/basketball/"+league+"/scoreboard?dates="+d+"&limit=100";JSONObject root=new JSONObject(http.get(u,"https://www.espn.com/"));JSONArray ev=root.optJSONArray("events");if(ev==null)continue;for(int i=0;i<ev.length();i++){BasketballMatch m=parseBasketball(ev.optJSONObject(i),league,day);if(m!=null&&seen.add(m.matchId+"|"+m.home+"|"+m.away))out.add(m);}}catch(Exception ignored){}}
        return out;}

    private MatchInfo parseFootball(JSONObject e,String league,String day){if(e==null)return null;JSONObject c=firstCompetition(e);if(c==null)return null;JSONArray cs=c.optJSONArray("competitors");if(cs==null||cs.length()<2)return null;JSONObject h=findSide(cs,"home"),a=findSide(cs,"away");if(h==null||a==null)return null;MatchInfo m=new MatchInfo();m.source="GLOBAL";m.matchId=e.optString("id");m.code=m.matchId;m.league=league;m.matchDate=day;m.kickoff=cnTime(e.optString("date"));m.home=teamName(h);m.away=teamName(a);m.status=status(e);m.score=score(h,a);parseThreeWayOdds(c,m);return m;}
    private BasketballMatch parseBasketball(JSONObject e,String league,String day){if(e==null)return null;JSONObject c=firstCompetition(e);if(c==null)return null;JSONArray cs=c.optJSONArray("competitors");if(cs==null||cs.length()<2)return null;JSONObject h=findSide(cs,"home"),a=findSide(cs,"away");if(h==null||a==null)return null;BasketballMatch m=new BasketballMatch();m.league=league;m.matchId=e.optString("id");m.matchDate=day;m.kickoff=cnTime(e.optString("date"));m.home=teamName(h);m.away=teamName(a);m.status=status(e);m.score=score(h,a);m.homeWinPct=recordPct(h);m.awayWinPct=recordPct(a);parseBasketballOdds(c,m);return m;}
    private static JSONObject firstCompetition(JSONObject e){JSONArray a=e.optJSONArray("competitions");return a!=null&&a.length()>0?a.optJSONObject(0):null;}
    private static JSONObject findSide(JSONArray a,String side){for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null&&side.equals(x.optString("homeAway")))return x;}return null;}
    private static String teamName(JSONObject c){JSONObject t=c.optJSONObject("team");if(t==null)return"";String x=t.optString("displayName");if(x.isEmpty())x=t.optString("shortDisplayName");return x;}
    private static String status(JSONObject e){JSONObject s=e.optJSONObject("status");JSONObject t=s==null?null:s.optJSONObject("type");return t==null?"":t.optString("description");}
    private static String score(JSONObject h,JSONObject a){String hs=h.optString("score"),as=a.optString("score");return hs.isEmpty()||as.isEmpty()?"":hs+":"+as;}
    private static double recordPct(JSONObject c){JSONArray r=c.optJSONArray("records");if(r==null||r.length()==0)return-1;String s=r.optJSONObject(0).optString("summary");try{String[] p=s.split("-");double w=Double.parseDouble(p[0]),l=Double.parseDouble(p[1]);return w+l>0?w/(w+l):-1;}catch(Exception e){return-1;}}
    private static void parseThreeWayOdds(JSONObject c,MatchInfo m){JSONArray o=c.optJSONArray("odds");if(o==null||o.length()==0)return;JSONObject x=o.optJSONObject(0);if(x==null)return;double home=decimalFromAmerican(x.optDouble("homeTeamOdds",0)),away=decimalFromAmerican(x.optDouble("awayTeamOdds",0)),draw=decimalFromAmerican(x.optDouble("drawOdds",0));if(home>1&&draw>1&&away>1){m.oddsHome=home;m.oddsDraw=draw;m.oddsAway=away;}}
    private static void parseBasketballOdds(JSONObject c,BasketballMatch m){JSONArray o=c.optJSONArray("odds");if(o==null||o.length()==0)return;JSONObject x=o.optJSONObject(0);if(x==null)return;m.marketDetails=x.optString("details");m.marketSpread=x.has("spread")?x.optDouble("spread",Double.NaN):Double.NaN;m.marketTotal=x.has("overUnder")?x.optDouble("overUnder",Double.NaN):Double.NaN;JSONObject h=x.optJSONObject("homeTeamOdds"),a=x.optJSONObject("awayTeamOdds");if(h!=null)m.homeMoneyline=h.optDouble("moneyLine",0);if(a!=null)m.awayMoneyline=a.optDouble("moneyLine",0);}
    private static double decimalFromAmerican(double x){if(x==0)return 0;return x<0?1+100.0/(-x):1+x/100.0;}
    private static String cnTime(String iso){try{SimpleDateFormat in=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'",Locale.US);in.setTimeZone(TimeZone.getTimeZone("UTC"));Date d=in.parse(iso);SimpleDateFormat out=new SimpleDateFormat("HH:mm",Locale.US);out.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));return out.format(d);}catch(Exception e){try{SimpleDateFormat in=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);in.setTimeZone(TimeZone.getTimeZone("UTC"));Date d=in.parse(iso);SimpleDateFormat out=new SimpleDateFormat("HH:mm",Locale.US);out.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));return out.format(d);}catch(Exception ignored){return iso;}}}
}
