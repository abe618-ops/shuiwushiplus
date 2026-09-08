package com.abe618.oddsflow;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.content.*;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class MainActivity extends Activity {
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private EditText query, apiKey;
    private TextView status, result, details, searchHint;
    private LinearLayout matchList;
    private CheckBox autoTrack;
    private SharedPreferences prefs;
    private static final String BASE_EN = "https://tipsme.hk/en";
    private static final String BASE_ZH = "https://tipsme.hk/zh-CN";
    private static final String API = "https://api.tipsme.hk/v1/football";

    static class MatchItem {
        String id,name,status=""; long kickoff=0; int score=0;
        MatchItem(String i,String n){id=i;name=n;}
    }
    static class Snap {
        long ts; double h,d,a,r,kh,kd,ka; String source="public";
        Snap copy(){ Snap x=new Snap(); x.ts=ts;x.h=h;x.d=d;x.a=a;x.r=r;x.kh=kh;x.kd=kd;x.ka=ka;x.source=source;return x; }
    }
    static class CompanyPath { String name; Snap open, current; }

    @Override public void onCreate(Bundle b){
        super.onCreate(b); prefs=getSharedPreferences("oddsflow",MODE_PRIVATE);
        ScrollView sc=new ScrollView(this); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(26,24,26,46); sc.addView(root);
        TextView title=tv("OddsFlow · 12BET轨迹分析",24,true); root.addView(title);
        TextView sub=tv("赔率 × 返还率 × 凯利｜初盘-中盘-当前｜12BET主权重",13,false); sub.setTextColor(Color.DKGRAY); root.addView(sub);

        Space breathing=new Space(this); root.addView(breathing,mp(-1,72));
        TextView searchTitle=tv("搜索最近比赛",20,true); searchTitle.setGravity(Gravity.CENTER_HORIZONTAL); root.addView(searchTitle);
        searchHint=tv("支持中文/英文队名 · 优先检索距离现在24小时内的真实比赛",13,false); searchHint.setTextColor(Color.DKGRAY); searchHint.setGravity(Gravity.CENTER_HORIZONTAL); searchHint.setPadding(0,6,0,14); root.addView(searchHint);

        query=new EditText(this); query.setHint("例如：曼联、皇家社会、巴拉卡斯中央"); query.setSingleLine(true); query.setTextSize(18); query.setPadding(22,0,22,0); query.setImeOptions(EditorInfo.IME_ACTION_SEARCH); query.setBackgroundColor(Color.rgb(246,248,251)); query.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_SEARCH){autoAnalyze();return true;}return false;}); root.addView(query,mp(-1,66));
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setPadding(0,10,0,0);
        Button analyze=new Button(this); analyze.setText("搜索并分析"); analyze.setOnClickListener(v->autoAnalyze()); row.addView(analyze,new LinearLayout.LayoutParams(0,58,2));
        Button recent=new Button(this); recent.setText("近24小时"); recent.setOnClickListener(v->loadRecent()); row.addView(recent,new LinearLayout.LayoutParams(0,58,1)); root.addView(row);

        status=tv("输入球队名即可搜索；中文名称可直接使用。",13,false); status.setPadding(0,12,0,8); root.addView(status);
        matchList=new LinearLayout(this); matchList.setOrientation(LinearLayout.VERTICAL); root.addView(matchList);

        result=tv("等待分析…",18,true); result.setPadding(18,18,18,18); result.setBackgroundColor(Color.rgb(245,247,250)); root.addView(result);
        details=tv("",14,false); details.setPadding(4,16,4,16); root.addView(details);

        TextView advanced=tv("数据增强（可选）",16,true); advanced.setPadding(0,18,0,4); root.addView(advanced);
        apiKey=new EditText(this); apiKey.setHint("Tipsme API Key：启用严格24小时赛程和完整赔率历史"); apiKey.setSingleLine(true); apiKey.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); apiKey.setText(prefs.getString("api_key","")); root.addView(apiKey,mp(-1,50));
        Button saveKey=new Button(this); saveKey.setText("保存 API Key"); saveKey.setOnClickListener(v->{prefs.edit().putString("api_key",apiKey.getText().toString().trim()).apply();Toast.makeText(this,"已保存在本机",Toast.LENGTH_SHORT).show();}); root.addView(saveKey,mp(-1,46));
        autoTrack=new CheckBox(this); autoTrack.setText("页面停留时每15分钟记录一次12BET中间快照"); autoTrack.setChecked(prefs.getBoolean("auto_track",true)); autoTrack.setOnCheckedChangeListener((b2,c)->{prefs.edit().putBoolean("auto_track",c).apply(); scheduleTrack();}); root.addView(autoTrack);
        setContentView(sc); scheduleTrack();
    }

    private TextView tv(String s,int sp,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);if(bold)t.setTypeface(null,1);return t;}
    private LinearLayout.LayoutParams mp(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private void ui(Runnable r){runOnUiThread(r);}

    private final Runnable tracker=new Runnable(){public void run(){
        if(autoTrack!=null&&autoTrack.isChecked()){String q=query.getText().toString().trim();if(q.matches("\\d{4,}")) pool.submit(()->silentCapture(q));}
        handler.postDelayed(this,15*60*1000L);
    }};
    private void scheduleTrack(){handler.removeCallbacks(tracker); if(autoTrack!=null&&autoTrack.isChecked()) handler.postDelayed(tracker,15*60*1000L);}
    @Override protected void onDestroy(){handler.removeCallbacks(tracker);pool.shutdownNow();super.onDestroy();}

    private void loadRecent(){
        status.setText("正在获取近24小时比赛…"); matchList.removeAllViews();
        final String key=prefs.getString("api_key","").trim();
        pool.submit(()->{try{
            List<MatchItem> ms=!key.isEmpty()?apiRecentMatches("",key):publicRecentMatches("");
            ui(()->renderMatches(ms,false));
        }catch(Exception e){ui(()->status.setText("获取失败："+e.getMessage()));}});
    }

    private void autoAnalyze(){
        final String q=query.getText().toString().trim();
        if(q.isEmpty()){Toast.makeText(this,"请输入球队名或比赛名",Toast.LENGTH_SHORT).show();return;}
        if(q.matches("\\d{4,}")){analyzeId(q,"Match #"+q);return;}
        status.setText("正在搜索“"+q+"”最近24小时比赛…"); matchList.removeAllViews();
        final String key=prefs.getString("api_key","").trim();
        pool.submit(()->{try{
            List<MatchItem> ms=new ArrayList<>();
            if(!key.isEmpty()) try{ms=apiRecentMatches(q,key);}catch(Exception ignored){}
            if(ms.isEmpty()) ms=publicRecentMatches(q);
            if(ms.isEmpty()) throw new Exception("未找到近24小时相关比赛；可输入更完整的中文队名或 Match ID");
            List<MatchItem> found=ms; ui(()->renderMatches(found,true));
        }catch(Exception e){ui(()->{status.setText("搜索失败："+e.getMessage());result.setText("未生成结论");});}});
    }

    private void renderMatches(List<MatchItem> ms,boolean autoPick){
        matchList.removeAllViews();
        if(ms.isEmpty()){status.setText("近24小时暂无匹配比赛");return;}
        status.setText("找到 "+ms.size()+" 场真实比赛，已按距离现在由近到远排列");
        int max=Math.min(8,ms.size());
        for(int i=0;i<max;i++){
            MatchItem m=ms.get(i); Button b=new Button(this); b.setAllCaps(false); b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
            String when=m.kickoff>0?formatKickoff(m.kickoff):"时间以公开页为准";
            b.setText(m.name+"\n"+when+(m.status.isEmpty()?"":" · "+m.status)+"  #"+m.id);
            b.setOnClickListener(v->{query.setText(m.id);analyzeId(m.id,m.name);}); matchList.addView(b,mp(-1,70));
        }
        if(autoPick){MatchItem best=ms.get(0);query.setText(best.id);analyzeId(best.id,best.name);}
    }

    private List<MatchItem> apiRecentMatches(String q,String key)throws Exception{
        Set<String> teamIds=new HashSet<>(); Set<String> aliases=new HashSet<>(); aliases.add(normalize(q));
        if(!q.isEmpty()){
            try{
                String sr=get(API+"/search?q="+URLEncoder.encode(q,"UTF-8")+"&type=team",key);
                JSONObject root=new JSONObject(sr); JSONArray data=root.optJSONArray("data");
                if(data!=null)for(int i=0;i<data.length();i++){JSONObject o=data.optJSONObject(i);if(o==null)continue;collectStrings(o,aliases);String id=String.valueOf(o.opt("id"));if(!id.equals("null")&&!id.equals("0"))teamIds.add(id);}
            }catch(Exception ignored){}
        }
        ZoneId hk=ZoneId.of("Asia/Hong_Kong"); LocalDate today=LocalDate.now(hk); long now=System.currentTimeMillis(); long range=24L*60*60*1000;
        LinkedHashMap<String,MatchItem> map=new LinkedHashMap<>();
        for(int d=-1;d<=1;d++){
            String date=today.plusDays(d).toString(); String raw=get(API+"/matches?date="+date+"&tz=Asia%2FHong_Kong&pageSize=100",key);
            JSONObject root=new JSONObject(raw); JSONArray arr=root.optJSONArray("data"); if(arr==null)continue;
            for(int i=0;i<arr.length();i++){
                JSONObject o=arr.optJSONObject(i); if(o==null)continue; long ko=parseUtc(o.optString("kickoffUtc")); if(ko==0||Math.abs(ko-now)>range)continue;
                JSONObject h=o.optJSONObject("home"), a=o.optJSONObject("away"); String hn=teamName(h), an=teamName(a); String hid=teamId(h), aid=teamId(a);
                int matchScore=scoreMatch(q,aliases,hn,an,teamIds,hid,aid); if(!q.isEmpty()&&matchScore<=0)continue;
                MatchItem m=new MatchItem(String.valueOf(o.optInt("id")),hn+" VS "+an); m.kickoff=ko;m.status=o.optString("status");m.score=matchScore;map.put(m.id,m);
            }
        }
        ArrayList<MatchItem> out=new ArrayList<>(map.values()); out.sort((a,b)->{if(a.score!=b.score)return Integer.compare(b.score,a.score);return Long.compare(Math.abs(a.kickoff-now),Math.abs(b.kickoff-now));}); return out;
    }

    private List<MatchItem> publicRecentMatches(String q)throws Exception{
        LinkedHashMap<String,MatchItem> map=new LinkedHashMap<>();
        String[] urls={BASE_ZH,BASE_ZH+"/odds",BASE_ZH+"/results",BASE_EN,BASE_EN+"/odds",BASE_EN+"/results"};
        for(String u:urls){try{for(MatchItem m:parseMatches(get(u,null))){MatchItem old=map.get(m.id);if(old==null||containsHan(m.name))map.put(m.id,m);}}catch(Exception ignored){}}
        ArrayList<MatchItem> out=new ArrayList<>(); String nq=normalize(q);
        for(MatchItem m:map.values()){int s=q.isEmpty()?1:fuzzyScore(nq,normalize(m.name));if(s>0){m.score=s;out.add(m);}}
        out.sort((a,b)->Integer.compare(b.score,a.score)); return out;
    }

    private int scoreMatch(String raw,Set<String> aliases,String h,String a,Set<String> ids,String hid,String aid){
        if(raw.isEmpty())return 1; if(ids.contains(hid)||ids.contains(aid))return 100;
        String nh=normalize(h),na=normalize(a),both=nh+na; int best=0;
        for(String x:aliases){if(x.length()<2)continue;if(nh.equals(x)||na.equals(x))best=Math.max(best,90);else if(nh.contains(x)||na.contains(x)||x.contains(nh)||x.contains(na))best=Math.max(best,70);else if(both.contains(x))best=Math.max(best,50);} return best;
    }
    private int fuzzyScore(String q,String text){if(q.isEmpty())return 1;if(text.equals(q))return 100;if(text.contains(q))return 80;if(q.contains(text)&&text.length()>2)return 55;return 0;}
    private String normalize(String s){if(s==null)return "";String x=s.toLowerCase(Locale.ROOT).replaceAll("[\\s·・._'’`-]+","");String[][] map={{"聯","联"},{"隊","队"},{"國","国"},{"會","会"},{"爾","尔"},{"倫","伦"},{"蘭","兰"},{"馬","马"},{"達","达"},{"貝","贝"},{"澤","泽"},{"亞","亚"},{"門","门"},{"東","东"},{"廣","广"},{"華","华"},{"羅","罗"},{"維","维"},{"聖","圣"},{"薩","萨"},{"賽","赛"},{"體","体"},{"業","业"},{"漢","汉"},{"現","现"},{"車","车"},{"頭","头"},{"龍","龙"},{"慶","庆"},{"綠","绿"},{"濟","济"},{"鐵","铁"},{"喬","乔"},{"爾","尔"}};for(String[] p:map)x=x.replace(p[0],p[1]);return x;}
    private boolean containsHan(String s){return s!=null&&s.matches(".*[\\u4e00-\\u9fff].*");}

    private void collectStrings(Object x,Set<String> out){
        if(x instanceof JSONObject){JSONObject o=(JSONObject)x;Iterator<String> it=o.keys();while(it.hasNext()){String k=it.next();Object v=o.opt(k);if(v instanceof String){String s=normalize((String)v);if(s.length()>=2)out.add(s);}else if(v instanceof JSONObject||v instanceof JSONArray)collectStrings(v,out);}}
        else if(x instanceof JSONArray){JSONArray a=(JSONArray)x;for(int i=0;i<a.length();i++){Object v=a.opt(i);if(v!=null)collectStrings(v,out);}}
    }
    private String teamName(JSONObject o){if(o==null)return "?";String[] ks={"nameZh","nameCn","nameChinese","shortName","nameEn","name"};for(String k:ks){String s=o.optString(k,"").trim();if(!s.isEmpty())return s;}return "?";}
    private String teamId(JSONObject o){if(o==null)return "";Object v=o.opt("id");return v==null?"":String.valueOf(v);}
    private long parseUtc(String s){try{return Instant.parse(s).toEpochMilli();}catch(Exception e){return 0;}}
    private String formatKickoff(long t){try{SimpleDateFormat f=new SimpleDateFormat("MM-dd HH:mm",Locale.CHINA);return f.format(new Date(t));}catch(Exception e){return "";}}

    private void analyzeId(String id,String name){status.setText("抓取12BET及参照公司…");pool.submit(()->analyzeIdWorker(id,name));}
    private void analyzeIdWorker(String id,String fallback){try{
        String html=get(BASE_EN+"/match/"+id+"/odds/kerry",null);String title=parseTitle(html);
        LinkedHashMap<String,CompanyPath> all=parseKerryTable(html);CompanyPath p12=findCompany(all,"12bet");if(p12==null)throw new Exception("该场公开页暂未出现12BET行");
        saveSnapshot(id,p12.current); List<Snap> local=loadSnapshots(id); Snap mid=pickMiddle(local,p12.open,p12.current);
        String apiNote="";String key=prefs.getString("api_key","").trim();if(!key.isEmpty()) apiNote=tryApiHistory(id,key);
        String conclusion=analyzeTrajectory(p12.open,mid,p12.current,all); String detail=renderDetail(title.isEmpty()?fallback:title,id,p12.open,mid,p12.current,all,apiNote);
        ui(()->{status.setText("完成 · 12BET主分析 · 已保存第"+local.size()+"个本机快照");result.setText(conclusion);details.setText(detail);});
    }catch(Exception e){ui(()->{status.setText("分析失败");result.setText("未生成结论");details.setText(e.getMessage()+"\n\nOddsFlow不会用0或演示值替代缺失数据。可换Match ID或刷新。");});}}
    private void silentCapture(String id){try{LinkedHashMap<String,CompanyPath> all=parseKerryTable(get(BASE_EN+"/match/"+id+"/odds/kerry",null));CompanyPath p=findCompany(all,"12bet");if(p!=null)saveSnapshot(id,p.current);}catch(Exception ignored){}}

    private String analyzeTrajectory(Snap o,Snap m,Snap c,Map<String,CompanyPath> all){
        double[] base={supportDelta(o,c,0),supportDelta(o,c,1),supportDelta(o,c,2)}; double[] pen={excess(c.kh,c.r),excess(c.kd,c.r),excess(c.ka,c.r)};
        for(int i=0;i<3;i++) base[i]-=Math.max(0,pen[i])*2.2;
        if(m!=null){double[] mk={m.kh,m.kd,m.ka}, ck={c.kh,c.kd,c.ka};for(int i=0;i<3;i++)if(mk[i]>m.r/100.0+0.035&&ck[i]>c.r/100.0+0.035)base[i]-=.18;}
        double[] consensus=consensusMove(all);for(int i=0;i<3;i++)base[i]+=consensus[i]*0.30;
        Integer[] idx={0,1,2};Arrays.sort(idx,(a,b)->Double.compare(base[b],base[a]));String[] lab={"主胜","平局","客胜"};
        boolean drawSuppressed=pen[1]>.04 && (m==null||m.kd>m.r/100.0+.035); String structure=drawSuppressed?"平局相对受抑，优先在主/客之间分胜负":"平局未被明显排除"; String risk=Math.abs(base[idx[0]]-base[idx[1]])<.08?"分歧较小，低置信":"首选与次选已有可见差距";
        return "冻结方向："+lab[idx[0]]+" ＞ "+lab[idx[1]]+" ＞ "+lab[idx[2]]+"\n"+structure+"\n"+risk+"\n12BET权重最高，其他公司仅作校验";
    }
    private double supportDelta(Snap o,Snap c,int i){double jo=j(o,i),jc=j(c,i);if(jo<=0||jc<=0)return 0;return Math.log(jo/jc);}
    private double j(Snap s,int i){double k=i==0?s.kh:i==1?s.kd:s.ka;return k/(s.r/100.0);}
    private double excess(double k,double r){return k-r/100.0;}
    private double[] consensusMove(Map<String,CompanyPath> all){double[] z={0,0,0};int n=0;for(CompanyPath p:all.values()){if(p==null||p.open==null||p.current==null||p.name.toLowerCase(Locale.ROOT).contains("12bet"))continue;String q=p.name.toLowerCase(Locale.ROOT);if(!(q.contains("10bet")||q.contains("365")||q.contains("macau")||q.contains("hkjc")||q.contains("libo")||q.contains("ladbrokes")||q.contains("betvictor")))continue;for(int i=0;i<3;i++)z[i]+=supportDelta(p.open,p.current,i);n++;}if(n>0)for(int i=0;i<3;i++)z[i]/=n;return z;}

    private String renderDetail(String name,String id,Snap o,Snap m,Snap c,Map<String,CompanyPath> all,String apiNote){StringBuilder s=new StringBuilder();s.append("比赛：").append(name).append("\nMatch ID：").append(id).append("\n\n【12BET 五项轨迹】\n");s.append(line("初盘",o));if(m!=null)s.append(line("中间",m));else s.append("中间  尚无独立凯利快照（继续自动跟踪后补齐）\n");s.append(line("当前",c));s.append("\nJ=K/R：小于1表示该项相对参考市场更受支持；大于1表示相对偏高。\n");s.append(String.format(Locale.US,"当前J 主 %.3f / 平 %.3f / 客 %.3f\n",j(c,0),j(c,1),j(c,2)));if(m!=null)s.append(String.format(Locale.US,"中→今 ΔE 主 %+.3f / 平 %+.3f / 客 %+.3f\n",supportDelta(m,c,0),supportDelta(m,c,1),supportDelta(m,c,2)));s.append(String.format(Locale.US,"初→今 ΔE 主 %+.3f / 平 %+.3f / 客 %+.3f\n",supportDelta(o,c,0),supportDelta(o,c,1),supportDelta(o,c,2)));
        s.append("\n【参照组】\n");for(CompanyPath p:all.values()){String q=p.name.toLowerCase(Locale.ROOT);if(q.contains("12bet")||q.contains("10bet")||q.contains("365")||q.contains("macau")||q.contains("hkjc")||q.contains("libo")||q.contains("ladbrokes")||q.contains("betvictor"))s.append(p.name).append("  R ").append(fmt(p.open.r)).append("→").append(fmt(p.current.r)).append("  K ").append(fmt(p.open.kh)).append("/").append(fmt(p.open.kd)).append("/").append(fmt(p.open.ka)).append(" → ").append(fmt(p.current.kh)).append("/").append(fmt(p.current.kd)).append("/").append(fmt(p.current.ka)).append("\n");}
        s.append("\n【规则说明】\n• 返还率和凯利联动，不按‘最低凯利必出’处理。\n• 返还率上升而某项凯利相对R连续偏高，该项降权。\n• 平项持续降权后重新比较主客，不把‘无平’机械等同于客胜。\n• 缺中盘时明确标缺，不伪造。\n");if(!apiNote.isEmpty())s.append("\n【API历史】\n").append(apiNote);return s.toString();}
    private String line(String tag,Snap x){return String.format(Locale.US,"%s  O %.2f/%.2f/%.2f  R %.2f%%  K %.3f/%.3f/%.3f  J %.3f/%.3f/%.3f\n",tag,x.h,x.d,x.a,x.r,x.kh,x.kd,x.ka,j(x,0),j(x,1),j(x,2));}
    private String fmt(double v){return String.format(Locale.US,"%.2f",v);}

    private LinkedHashMap<String,CompanyPath> parseKerryTable(String html)throws Exception{LinkedHashMap<String,CompanyPath> out=new LinkedHashMap<>();Matcher tr=Pattern.compile("(?is)<tr[^>]*>(.*?)</tr>").matcher(html);while(tr.find()){List<String> cells=cells(tr.group(1));if(cells.size()<9)continue;String name=cells.get(0).trim();if(name.isEmpty()||name.equalsIgnoreCase("Company"))continue;try{CompanyPath p=new CompanyPath();p.name=name;p.open=new Snap();p.current=new Snap();double[][] v=new double[7][];for(int i=0;i<7;i++)v[i]=nums(cells.get(i+2));for(int i=0;i<7;i++)if(v[i].length<2)throw new Exception("pair missing");p.open.h=v[0][0];p.current.h=v[0][1];p.open.d=v[1][0];p.current.d=v[1][1];p.open.a=v[2][0];p.current.a=v[2][1];p.open.r=v[3][0];p.current.r=v[3][1];p.open.kh=v[4][0];p.current.kh=v[4][1];p.open.kd=v[5][0];p.current.kd=v[5][1];p.open.ka=v[6][0];p.current.ka=v[6][1];p.current.ts=System.currentTimeMillis();out.put(name,p);}catch(Exception ignored){}}if(out.isEmpty())throw new Exception("凯利表结构未识别");return out;}
    private List<String> cells(String row){ArrayList<String>a=new ArrayList<>();Matcher m=Pattern.compile("(?is)<t[dh][^>]*>(.*?)</t[dh]>").matcher(row);while(m.find())a.add(strip(m.group(1)).replaceAll("\\s+"," ").trim());return a;}
    private double[] nums(String x){Matcher m=Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(x);ArrayList<Double>a=new ArrayList<>();while(m.find())a.add(Double.parseDouble(m.group()));double[]z=new double[a.size()];for(int i=0;i<z.length;i++)z[i]=a.get(i);return z;}
    private CompanyPath findCompany(Map<String,CompanyPath> m,String key){for(Map.Entry<String,CompanyPath>e:m.entrySet())if(e.getKey().toLowerCase(Locale.ROOT).replace(" ","").contains(key.toLowerCase(Locale.ROOT).replace(" ","")))return e.getValue();return null;}

    private void saveSnapshot(String id,Snap s){try{JSONArray a=new JSONArray(prefs.getString("snap_"+id,"[]"));JSONObject j=new JSONObject();j.put("ts",System.currentTimeMillis());j.put("h",s.h);j.put("d",s.d);j.put("a",s.a);j.put("r",s.r);j.put("kh",s.kh);j.put("kd",s.kd);j.put("ka",s.ka);if(a.length()==0||different(a.getJSONObject(a.length()-1),j))a.put(j);while(a.length()>60){JSONArray b=new JSONArray();for(int i=1;i<a.length();i++)b.put(a.get(i));a=b;}prefs.edit().putString("snap_"+id,a.toString()).apply();}catch(Exception ignored){}}
    private boolean different(JSONObject a,JSONObject b){return Math.abs(a.optDouble("h")-b.optDouble("h"))>.0001||Math.abs(a.optDouble("d")-b.optDouble("d"))>.0001||Math.abs(a.optDouble("a")-b.optDouble("a"))>.0001||Math.abs(a.optDouble("kh")-b.optDouble("kh"))>.0001||Math.abs(a.optDouble("kd")-b.optDouble("kd"))>.0001||Math.abs(a.optDouble("ka")-b.optDouble("ka"))>.0001||Math.abs(a.optDouble("r")-b.optDouble("r"))>.0001;}
    private List<Snap> loadSnapshots(String id){ArrayList<Snap>z=new ArrayList<>();try{JSONArray a=new JSONArray(prefs.getString("snap_"+id,"[]"));for(int i=0;i<a.length();i++){JSONObject j=a.getJSONObject(i);Snap s=new Snap();s.ts=j.optLong("ts");s.h=j.optDouble("h");s.d=j.optDouble("d");s.a=j.optDouble("a");s.r=j.optDouble("r");s.kh=j.optDouble("kh");s.kd=j.optDouble("kd");s.ka=j.optDouble("ka");z.add(s);}}catch(Exception ignored){}return z;}
    private Snap pickMiddle(List<Snap> a,Snap open,Snap current){if(a.size()<2)return null;if(a.size()==2)return a.get(0);return a.get(a.size()/2);}

    private String tryApiHistory(String id,String key){try{String books=get(API+"/odds/bookmakers",key);JSONObject br=new JSONObject(books);JSONArray ba=br.optJSONArray("data");String bid=null;if(ba!=null)for(int i=0;i<ba.length();i++){JSONObject b=ba.getJSONObject(i);String n=(b.optString("name")+" "+b.optString("nameEn")).toLowerCase(Locale.ROOT);if(n.contains("12bet")){bid=String.valueOf(b.optInt("id"));break;}}if(bid==null)return "API已连接，但目录未解析到12BET。";String raw=get(API+"/matches/"+id+"/odds/had/"+bid,key);JSONObject d=new JSONObject(raw).optJSONObject("data");if(d==null)return "API历史返回为空。";JSONArray h=d.optJSONArray("history");if(h==null||h.length()==0)return "API已连接，但该场暂无12BET HAD历史。";JSONObject first=h.getJSONObject(0), mid=h.getJSONObject(h.length()/2), last=h.getJSONObject(h.length()-1);return "12BET完整赔率历史共 "+h.length()+" 个节点\nOPEN "+apiOdds(first)+"\nMID  "+apiOdds(mid)+"\nLAST "+apiOdds(last)+"\n注：中间凯利仍以本机实际抓到的公开凯利快照为准。";}catch(Exception e){return "增强接口未启用/无权限："+e.getMessage();}}
    private String apiOdds(JSONObject j){return j.optString("recordedUtc")+"  "+fmt(j.optDouble("home"))+"/"+fmt(j.optDouble("draw"))+"/"+fmt(j.optDouble("away"));}

    private List<MatchItem> parseMatches(String html){LinkedHashMap<String,String>map=new LinkedHashMap<>();Matcher m=Pattern.compile("(?is)<a[^>]+href=[\"']/(?:zh-CN|en)/match/(\\d+)[^\"']*[\"'][^>]*>(.*?)</a>").matcher(html);while(m.find()){String id=m.group(1),name=strip(m.group(2)).replaceAll("\\s+"," ").trim();String low=name.toLowerCase(Locale.ROOT);if(name.length()>4&&(low.contains("vs")||name.contains("VS")||name.contains("對")||name.contains("对")))map.put(id,name);}List<MatchItem>out=new ArrayList<>();for(Map.Entry<String,String>e:map.entrySet())out.add(new MatchItem(e.getKey(),e.getValue()));return out;}
    private String parseTitle(String html){Matcher m=Pattern.compile("(?is)<h[12][^>]*>(.*?)</h[12]>").matcher(html);while(m.find()){String s=strip(m.group(1));if(s.toLowerCase(Locale.ROOT).contains("vs"))return s.replace("-Kerry Index and Return","").trim();}return "";}
    private String strip(String s){return s.replaceAll("(?is)<script.*?</script>"," ").replaceAll("(?is)<style.*?</style>"," ").replaceAll("(?is)<[^>]+>"," ").replace("&nbsp;"," ").replace("&amp;","&").replace("&#39;","'").replace("&quot;","\"").trim();}
    private String get(String u,String token)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(18000);c.setRequestProperty("User-Agent","Mozilla/5.0 (Android) OddsFlow/0.3");c.setRequestProperty("Accept","text/html,application/json");c.setRequestProperty("Accept-Language","zh-CN,zh;q=0.9,en;q=0.8");if(token!=null&&!token.isEmpty())c.setRequestProperty("Authorization","Bearer "+token);int code=c.getResponseCode();if(code<200||code>=400)throw new IOException("HTTP "+code);try(InputStream in=c.getInputStream();ByteArrayOutputStream o=new ByteArrayOutputStream()){byte[]b=new byte[8192];int n;while((n=in.read(b))>0)o.write(b,0,n);return o.toString(StandardCharsets.UTF_8.name());}}
}
