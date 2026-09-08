package com.abe618.oddsflow;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.content.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class MainActivity extends Activity {
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private EditText query, apiKey;
    private TextView status, result, details;
    private LinearLayout matchList;
    private CheckBox autoTrack;
    private SharedPreferences prefs;
    private static final String BASE = "https://tipsme.hk/en";
    private static final String API = "https://api.tipsme.hk/v1/football";

    static class MatchItem { String id,name; MatchItem(String i,String n){id=i;name=n;} }
    static class Snap {
        long ts; double h,d,a,r,kh,kd,ka; String source="public";
        Snap copy(){ Snap x=new Snap(); x.ts=ts;x.h=h;x.d=d;x.a=a;x.r=r;x.kh=kh;x.kd=kd;x.ka=ka;x.source=source;return x; }
    }
    static class CompanyPath { String name; Snap open, current; }

    @Override public void onCreate(Bundle b){
        super.onCreate(b); prefs=getSharedPreferences("oddsflow",MODE_PRIVATE);
        ScrollView sc=new ScrollView(this); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(26,24,26,40); sc.addView(root);
        TextView title=tv("OddsFlow · 12BET轨迹分析",24,true); root.addView(title);
        TextView sub=tv("12BET主权重｜初盘-中盘-当前｜赔率+返还率+凯利+J=K/R｜多公司校验",13,false); sub.setTextColor(Color.DKGRAY); root.addView(sub);

        query=new EditText(this); query.setHint("输入球队名 / 比赛名 / Tipsme Match ID"); query.setSingleLine(true); root.addView(query,mp(-1,56));
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        Button analyze=new Button(this); analyze.setText("分析"); analyze.setOnClickListener(v->autoAnalyze()); row.addView(analyze,new LinearLayout.LayoutParams(0,55,1));
        Button today=new Button(this); today.setText("今日比赛"); today.setOnClickListener(v->loadToday()); row.addView(today,new LinearLayout.LayoutParams(0,55,1)); root.addView(row);

        apiKey=new EditText(this); apiKey.setHint("可选：Tipsme API Key（增强完整赔率历史）"); apiKey.setSingleLine(true); apiKey.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); apiKey.setText(prefs.getString("api_key","")); root.addView(apiKey,mp(-1,52));
        Button saveKey=new Button(this); saveKey.setText("保存API Key"); saveKey.setOnClickListener(v->{prefs.edit().putString("api_key",apiKey.getText().toString().trim()).apply();Toast.makeText(this,"已保存在本机",Toast.LENGTH_SHORT).show();}); root.addView(saveKey,mp(-1,48));

        autoTrack=new CheckBox(this); autoTrack.setText("本页停留时每15分钟自动记录一次中间快照"); autoTrack.setChecked(prefs.getBoolean("auto_track",true)); autoTrack.setOnCheckedChangeListener((b2,c)->{prefs.edit().putBoolean("auto_track",c).apply(); scheduleTrack();}); root.addView(autoTrack);
        status=tv("公开模式可直接使用；每次分析都会保存12BET快照。",13,false); status.setPadding(0,8,0,8); root.addView(status);
        result=tv("等待分析…",18,true); result.setPadding(18,18,18,18); result.setBackgroundColor(Color.rgb(245,247,250)); root.addView(result);
        details=tv("",14,false); details.setPadding(4,16,4,16); root.addView(details);
        root.addView(tv("今日/进行中比赛",17,true)); matchList=new LinearLayout(this); matchList.setOrientation(LinearLayout.VERTICAL); root.addView(matchList);
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

    private void loadToday(){status.setText("正在抓取今日比赛…");matchList.removeAllViews();pool.submit(()->{try{
        List<MatchItem> ms=parseMatches(get(BASE,null));ui(()->{status.setText("已获取 "+ms.size()+" 场");for(MatchItem m:ms){Button b=new Button(this);b.setAllCaps(false);b.setText(m.name+"  #"+m.id);b.setOnClickListener(v->{query.setText(m.id);analyzeId(m.id,m.name);});matchList.addView(b);}});
    }catch(Exception e){ui(()->status.setText("今日列表失败："+e.getMessage()));}});}

    private void autoAnalyze(){String q=query.getText().toString().trim();if(q.isEmpty()){Toast.makeText(this,"请输入球队名或Match ID",Toast.LENGTH_SHORT).show();return;}
        if(q.matches("\\d{4,}")){analyzeId(q,"Match #"+q);return;} status.setText("正在匹配："+q);pool.submit(()->{try{List<MatchItem> ms=parseMatches(get(BASE,null));MatchItem best=null;String key=q.toLowerCase(Locale.ROOT);for(MatchItem m:ms)if(m.name.toLowerCase(Locale.ROOT).contains(key)){best=m;break;}if(best==null)throw new Exception("今日列表未匹配，可尝试输入Match ID");MatchItem f=best;ui(()->query.setText(f.id));analyzeIdWorker(f.id,f.name);}catch(Exception e){ui(()->status.setText("匹配失败："+e.getMessage()));}});}
    private void analyzeId(String id,String name){status.setText("抓取12BET及参照公司…");pool.submit(()->analyzeIdWorker(id,name));}

    private void analyzeIdWorker(String id,String fallback){try{
        String html=get(BASE+"/match/"+id+"/odds/kerry",null);String title=parseTitle(html);
        LinkedHashMap<String,CompanyPath> all=parseKerryTable(html);CompanyPath p12=findCompany(all,"12bet");if(p12==null)throw new Exception("该场公开页暂未出现12BET行");
        saveSnapshot(id,p12.current); List<Snap> local=loadSnapshots(id); Snap mid=pickMiddle(local,p12.open,p12.current);
        String apiNote="";String key=apiKey.getText().toString().trim();if(!key.isEmpty()) apiNote=tryApiHistory(id,key);
        String conclusion=analyzeTrajectory(p12.open,mid,p12.current,all);
        String detail=renderDetail(title.isEmpty()?fallback:title,id,p12.open,mid,p12.current,all,apiNote);
        ui(()->{status.setText("完成 · 12BET主分析 · 已保存第"+local.size()+"个本机快照");result.setText(conclusion);details.setText(detail);});
    }catch(Exception e){ui(()->{status.setText("分析失败");result.setText("未生成结论");details.setText(e.getMessage()+"\n\nOddsFlow不会用0或演示值替代缺失数据。可换Match ID或稍后刷新。");});}}

    private void silentCapture(String id){try{LinkedHashMap<String,CompanyPath> all=parseKerryTable(get(BASE+"/match/"+id+"/odds/kerry",null));CompanyPath p=findCompany(all,"12bet");if(p!=null)saveSnapshot(id,p.current);}catch(Exception ignored){}}

    private String analyzeTrajectory(Snap o,Snap m,Snap c,Map<String,CompanyPath> all){
        double[] base={supportDelta(o,c,0),supportDelta(o,c,1),supportDelta(o,c,2)};
        double[] pen={excess(c.kh,c.r),excess(c.kd,c.r),excess(c.ka,c.r)};
        for(int i=0;i<3;i++) base[i]-=Math.max(0,pen[i])*2.2;
        // “返还率上升但某一凯利明显高于返还率并连续偏高”视为该项相对弱化，而非机械判死。
        if(m!=null){double[] mk={m.kh,m.kd,m.ka}, ck={c.kh,c.kd,c.ka};for(int i=0;i<3;i++){if(mk[i]>m.r/100.0+0.035&&ck[i]>c.r/100.0+0.035)base[i]-=.18;}}
        // 参照公司只给小权重，避免淹没12BET。
        double[] consensus=consensusMove(all);for(int i=0;i<3;i++)base[i]+=consensus[i]*0.30;
        Integer[] idx={0,1,2};Arrays.sort(idx,(a,b)->Double.compare(base[b],base[a]));String[] lab={"主胜","平局","客胜"};
        boolean drawSuppressed=pen[1]>.04 && (m==null||m.kd>m.r/100.0+.035);
        String structure=drawSuppressed?"平局相对受抑，优先在主/客之间分胜负":"平局未被明显排除";
        String risk=Math.abs(base[idx[0]]-base[idx[1]])<.08?"分歧较小，低置信":"首选与次选已有可见差距";
        return "冻结方向："+lab[idx[0]]+" ＞ "+lab[idx[1]]+" ＞ "+lab[idx[2]]+"\n"+structure+"\n"+risk+"\n12BET权重最高，其他公司仅作校验";
    }

    private double supportDelta(Snap o,Snap c,int i){double jo=j(o,i),jc=j(c,i);if(jo<=0||jc<=0)return 0;return Math.log(jo/jc);}
    private double j(Snap s,int i){double k=i==0?s.kh:i==1?s.kd:s.ka;return k/(s.r/100.0);}
    private double excess(double k,double r){return k-r/100.0;}
    private double[] consensusMove(Map<String,CompanyPath> all){double[] z={0,0,0};int n=0;for(CompanyPath p:all.values()){if(p==null||p.open==null||p.current==null||p.name.toLowerCase(Locale.ROOT).contains("12bet"))continue;String q=p.name.toLowerCase(Locale.ROOT);if(!(q.contains("10bet")||q.contains("365")||q.contains("macau")||q.contains("hkjc")||q.contains("libo")||q.contains("ladbrokes")||q.contains("betvictor")))continue;for(int i=0;i<3;i++)z[i]+=supportDelta(p.open,p.current,i);n++;}if(n>0)for(int i=0;i<3;i++)z[i]/=n;return z;}

    private String renderDetail(String name,String id,Snap o,Snap m,Snap c,Map<String,CompanyPath> all,String apiNote){StringBuilder s=new StringBuilder();s.append("比赛：").append(name).append("\nMatch ID：").append(id).append("\n\n【12BET 五项轨迹】\n");s.append(line("初盘",o));if(m!=null)s.append(line("中间",m));else s.append("中间  尚无独立凯利快照（继续自动跟踪后补齐）\n");s.append(line("当前",c));s.append("\nJ=K/R：小于1表示该项相对参考市场更受支持；大于1表示相对偏高。\n");s.append(String.format(Locale.US,"当前J 主 %.3f / 平 %.3f / 客 %.3f\n",j(c,0),j(c,1),j(c,2)));
        if(m!=null)s.append(String.format(Locale.US,"中→今 ΔE 主 %+.3f / 平 %+.3f / 客 %+.3f\n",supportDelta(m,c,0),supportDelta(m,c,1),supportDelta(m,c,2)));
        s.append(String.format(Locale.US,"初→今 ΔE 主 %+.3f / 平 %+.3f / 客 %+.3f\n",supportDelta(o,c,0),supportDelta(o,c,1),supportDelta(o,c,2)));
        s.append("\n【参照组】\n");for(CompanyPath p:all.values()){String q=p.name.toLowerCase(Locale.ROOT);if(q.contains("12bet")||q.contains("10bet")||q.contains("365")||q.contains("macau")||q.contains("hkjc")||q.contains("libo")||q.contains("ladbrokes")||q.contains("betvictor"))s.append(p.name).append("  R ").append(fmt(p.open.r)).append("→").append(fmt(p.current.r)).append("  K ").append(fmt(p.open.kh)).append("/").append(fmt(p.open.kd)).append("/").append(fmt(p.open.ka)).append(" → ").append(fmt(p.current.kh)).append("/").append(fmt(p.current.kd)).append("/").append(fmt(p.current.ka)).append("\n");}
        s.append("\n【规则说明】\n• 返还率和凯利是联动量，不按‘最低凯利必出’处理。\n• 若返还率上升，而某项凯利相对R明显偏高且中间、当前连续偏高，该项降权。\n• 若平项被持续降权，则主/客重新比较；不会自动把‘无平’等同于客胜。\n• 初盘、中盘、当前分别保存，缺中盘时明确标缺，不伪造。\n");if(!apiNote.isEmpty())s.append("\n【API历史】\n").append(apiNote);return s.toString();}
    private String line(String tag,Snap x){return String.format(Locale.US,"%s  O %.2f/%.2f/%.2f  R %.2f%%  K %.3f/%.3f/%.3f  J %.3f/%.3f/%.3f\n",tag,x.h,x.d,x.a,x.r,x.kh,x.kd,x.ka,j(x,0),j(x,1),j(x,2));}
    private String fmt(double v){return String.format(Locale.US,"%.2f",v);}

    private LinkedHashMap<String,CompanyPath> parseKerryTable(String html)throws Exception{LinkedHashMap<String,CompanyPath> out=new LinkedHashMap<>();Matcher tr=Pattern.compile("(?is)<tr[^>]*>(.*?)</tr>").matcher(html);while(tr.find()){List<String> cells=cells(tr.group(1));if(cells.size()<9)continue;String name=cells.get(0).trim();if(name.isEmpty()||name.equalsIgnoreCase("Company"))continue;try{CompanyPath p=new CompanyPath();p.name=name;p.open=new Snap();p.current=new Snap();double[][] v=new double[7][];for(int i=0;i<7;i++)v[i]=nums(cells.get(i+2));for(int i=0;i<7;i++)if(v[i].length<2)throw new Exception("pair missing");p.open.h=v[0][0];p.current.h=v[0][1];p.open.d=v[1][0];p.current.d=v[1][1];p.open.a=v[2][0];p.current.a=v[2][1];p.open.r=v[3][0];p.current.r=v[3][1];p.open.kh=v[4][0];p.current.kh=v[4][1];p.open.kd=v[5][0];p.current.kd=v[5][1];p.open.ka=v[6][0];p.current.ka=v[6][1];p.open.ts=0;p.current.ts=System.currentTimeMillis();out.put(name,p);}catch(Exception ignored){}}
        if(out.isEmpty())throw new Exception("凯利表结构未识别");return out;}
    private List<String> cells(String row){ArrayList<String>a=new ArrayList<>();Matcher m=Pattern.compile("(?is)<t[dh][^>]*>(.*?)</t[dh]>").matcher(row);while(m.find())a.add(strip(m.group(1)).replaceAll("\\s+"," ").trim());return a;}
    private double[] nums(String x){Matcher m=Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(x);ArrayList<Double>a=new ArrayList<>();while(m.find())a.add(Double.parseDouble(m.group()));double[]z=new double[a.size()];for(int i=0;i<z.length;i++)z[i]=a.get(i);return z;}
    private CompanyPath findCompany(Map<String,CompanyPath> m,String key){for(Map.Entry<String,CompanyPath>e:m.entrySet())if(e.getKey().toLowerCase(Locale.ROOT).replace(" ","").contains(key.toLowerCase(Locale.ROOT).replace(" ","")))return e.getValue();return null;}

    private void saveSnapshot(String id,Snap s){try{JSONArray a=new JSONArray(prefs.getString("snap_"+id,"[]"));JSONObject j=new JSONObject();j.put("ts",System.currentTimeMillis());j.put("h",s.h);j.put("d",s.d);j.put("a",s.a);j.put("r",s.r);j.put("kh",s.kh);j.put("kd",s.kd);j.put("ka",s.ka);if(a.length()==0||different(a.getJSONObject(a.length()-1),j))a.put(j);while(a.length()>60){JSONArray b=new JSONArray();for(int i=1;i<a.length();i++)b.put(a.get(i));a=b;}prefs.edit().putString("snap_"+id,a.toString()).apply();}catch(Exception ignored){}}
    private boolean different(JSONObject a,JSONObject b){return Math.abs(a.optDouble("h")-b.optDouble("h"))>.0001||Math.abs(a.optDouble("d")-b.optDouble("d"))>.0001||Math.abs(a.optDouble("a")-b.optDouble("a"))>.0001||Math.abs(a.optDouble("kh")-b.optDouble("kh"))>.0001||Math.abs(a.optDouble("kd")-b.optDouble("kd"))>.0001||Math.abs(a.optDouble("ka")-b.optDouble("ka"))>.0001||Math.abs(a.optDouble("r")-b.optDouble("r"))>.0001;}
    private List<Snap> loadSnapshots(String id){ArrayList<Snap>z=new ArrayList<>();try{JSONArray a=new JSONArray(prefs.getString("snap_"+id,"[]"));for(int i=0;i<a.length();i++){JSONObject j=a.getJSONObject(i);Snap s=new Snap();s.ts=j.optLong("ts");s.h=j.optDouble("h");s.d=j.optDouble("d");s.a=j.optDouble("a");s.r=j.optDouble("r");s.kh=j.optDouble("kh");s.kd=j.optDouble("kd");s.ka=j.optDouble("ka");z.add(s);}}catch(Exception ignored){}return z;}
    private Snap pickMiddle(List<Snap> a,Snap open,Snap current){if(a.size()<2)return null;if(a.size()==2)return a.get(0);return a.get(a.size()/2);}

    private String tryApiHistory(String id,String key){try{String books=get(API+"/odds/bookmakers",key);JSONObject br=new JSONObject(books);JSONArray ba=br.optJSONArray("data");String bid=null;if(ba!=null)for(int i=0;i<ba.length();i++){JSONObject b=ba.getJSONObject(i);String n=(b.optString("name")+" "+b.optString("nameEn")).toLowerCase(Locale.ROOT);if(n.contains("12bet")){bid=String.valueOf(b.optInt("id"));break;}}if(bid==null)return "API已连接，但博彩公司目录未解析到12BET。";String raw=get(API+"/matches/"+id+"/odds/had/"+bid,key);JSONObject d=new JSONObject(raw).optJSONObject("data");if(d==null)return "API历史返回为空。";JSONArray h=d.optJSONArray("history");if(h==null||h.length()==0)return "API已连接，但该场暂无12BET HAD历史。";JSONObject first=h.getJSONObject(0), mid=h.getJSONObject(h.length()/2), last=h.getJSONObject(h.length()-1);return "12BET完整赔率历史共 "+h.length()+" 个节点\nOPEN "+apiOdds(first)+"\nMID  "+apiOdds(mid)+"\nLAST "+apiOdds(last)+"\n注：API movement提供赔率历史；中间凯利仍以本机实际抓到的公开凯利快照为准，不反推伪造。";}catch(Exception e){return "增强接口未启用/无权限："+e.getMessage();}}
    private String apiOdds(JSONObject j){return j.optString("recordedUtc")+"  "+fmt(j.optDouble("home"))+"/"+fmt(j.optDouble("draw"))+"/"+fmt(j.optDouble("away"));}

    private List<MatchItem> parseMatches(String html){LinkedHashMap<String,String>map=new LinkedHashMap<>();Matcher m=Pattern.compile("(?is)<a[^>]+href=\"/en/match/(\\d+)[^\"]*\"[^>]*>(.*?)</a>").matcher(html);while(m.find()){String id=m.group(1),name=strip(m.group(2)).replaceAll("\\s+"," ").trim();if(name.length()>5&&(name.toLowerCase(Locale.ROOT).contains("vs")))map.put(id,name);}List<MatchItem>out=new ArrayList<>();for(Map.Entry<String,String>e:map.entrySet()){out.add(new MatchItem(e.getKey(),e.getValue()));if(out.size()>=60)break;}return out;}
    private String parseTitle(String html){Matcher m=Pattern.compile("(?is)<h[12][^>]*>(.*?)</h[12]>").matcher(html);while(m.find()){String s=strip(m.group(1));if(s.toLowerCase(Locale.ROOT).contains("vs"))return s.replace("-Kerry Index and Return","").trim();}return "";}
    private String strip(String s){return s.replaceAll("(?is)<script.*?</script>"," ").replaceAll("(?is)<style.*?</style>"," ").replaceAll("(?is)<[^>]+>"," ").replace("&nbsp;"," ").replace("&amp;","&").replace("&#39;","'").replace("&quot;","\"").trim();}
    private String get(String u,String token)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(18000);c.setRequestProperty("User-Agent","Mozilla/5.0 (Android) OddsFlow/0.2");c.setRequestProperty("Accept","text/html,application/json");c.setRequestProperty("Accept-Language","zh-CN,zh;q=0.9,en;q=0.8");if(token!=null&&!token.isEmpty())c.setRequestProperty("Authorization","Bearer "+token);int code=c.getResponseCode();if(code<200||code>=400)throw new IOException("HTTP "+code);try(InputStream in=c.getInputStream();ByteArrayOutputStream o=new ByteArrayOutputStream()){byte[]b=new byte[8192];int n;while((n=in.read(b))>0)o.write(b,0,n);return o.toString(StandardCharsets.UTF_8.name());}}
}
