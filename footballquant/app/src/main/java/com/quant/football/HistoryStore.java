package com.quant.football;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class HistoryStore {
    private final SharedPreferences sp;
    private final SecureRandom secure=new SecureRandom();
    public HistoryStore(Context c){sp=c.getSharedPreferences("history_v6_full",Context.MODE_PRIVATE);}

    public void saveBase(String source,String day,List<MatchInfo> ms,Predictor predictor,GoalEnsemble goals,DivinationEngine div){
        if(has(source,day))return;
        JSONArray arr=new JSONArray();
        for(MatchInfo m:ms){
            Prediction p=predictor.predict(m);String seed=getOrCreateDivinationSeed(source,day,m);DivinationEngine.Bundle db=div.run(m,day,seed);GoalEnsemble.Result g=goals.predict(m,p,null,db);
            arr.put(pack(m,p,g,"BASE"));
        }
        sp.edit().putString(key(source,day),arr.toString()).apply();
    }

    public void update(String source,String day,MatchInfo m,Prediction p,GoalEnsemble.Result g){
        String dk=deepKey(source,day);JSONArray old;
        try{old=new JSONArray(sp.getString(dk,"[]"));}catch(Exception e){old=new JSONArray();}
        JSONArray out=new JSONArray();boolean done=false;
        for(int i=0;i<old.length();i++)try{JSONObject x=old.getJSONObject(i),mj=x.optJSONObject("match");if(!done&&same(m,mj)){out.put(pack(m,p,g,"DEEP"));done=true;}else out.put(x);}catch(Exception ignored){}
        if(!done)out.put(pack(m,p,g,"DEEP"));sp.edit().putString(dk,out.toString()).apply();
    }

    public JSONArray load(String source,String day){try{return new JSONArray(sp.getString(key(source,day),"[]"));}catch(Exception e){return new JSONArray();}}
    public boolean has(String source,String day){return load(source,day).length()>0;}

    public List<MatchInfo> loadMatches(String source,String day){List<MatchInfo> out=new ArrayList<>();JSONArray arr=load(source,day);for(int i=0;i<arr.length();i++)try{MatchInfo m=MatchInfo.fromJson(arr.getJSONObject(i).optJSONObject("match"));if(!m.home.isEmpty()&&!m.away.isEmpty())out.add(m);}catch(Exception ignored){}return out;}

    public String getOrCreateDivinationSeed(String source,String day,MatchInfo m){String k=seedKey(source,day,m);String s=sp.getString(k,"");if(!s.isEmpty())return s;s=String.format(Locale.US,"R%09d",secure.nextInt(1000000000));sp.edit().putString(k,s).apply();return s;}
    public String reseed(String source,String day,MatchInfo m){String s=String.format(Locale.US,"R%09d",secure.nextInt(1000000000));sp.edit().putString(seedKey(source,day,m),s).apply();return s;}

    public static JSONObject findPrediction(JSONArray arr,MatchInfo result){for(int i=0;i<arr.length();i++)try{JSONObject p=arr.getJSONObject(i),m=p.getJSONObject("match");String id=m.optString("matchId"),code=m.optString("code"),home=m.optString("home"),away=m.optString("away");if(!id.isEmpty()&&!result.matchId.isEmpty()&&id.equals(result.matchId))return p;if(!code.isEmpty()&&!result.code.isEmpty()&&(result.code.contains(code)||code.contains(result.code)))return p;if(!home.isEmpty()&&!away.isEmpty()&&similar(result.home,home)&&similar(result.away,away))return p;}catch(Exception ignored){}return null;}

    private static JSONObject pack(MatchInfo m,Prediction p,GoalEnsemble.Result g,String snapshotType){JSONObject o=p.toJson(m);try{o.put("goalEnsemble",g.toJson());o.put("snapshotType",snapshotType);o.put("capturedAt",System.currentTimeMillis());}catch(Exception ignored){}return o;}
    private static boolean same(MatchInfo m,JSONObject x){if(x==null)return false;String id=x.optString("matchId"),code=x.optString("code");if(!id.isEmpty()&&!m.matchId.isEmpty()&&id.equals(m.matchId))return true;if(!code.isEmpty()&&!m.code.isEmpty()&&code.equals(m.code))return true;return similar(m.home,x.optString("home"))&&similar(m.away,x.optString("away"));}
    private static boolean similar(String a,String b){if(a==null||b==null||a.isEmpty()||b.isEmpty())return false;String x=a.replaceAll("\\s+",""),y=b.replaceAll("\\s+","");return x.contains(y)||y.contains(x);}
    private static String key(String source,String day){return source+"|"+(day==null||day.isEmpty()?today():day);}
    private static String deepKey(String source,String day){return "deep|"+key(source,day);}
    private static String seedKey(String source,String day,MatchInfo m){return "divseed|"+key(source,day)+"|"+m.key();}
    public static String today(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));return f.format(new Date());}
}
