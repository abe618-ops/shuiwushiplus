package com.abe618.oddsflow;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class MainActivity extends Activity {
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private EditText query;
    private TextView status, result, details;
    private LinearLayout matchList;
    private static final String BASE = "https://tipsme.hk/en";

    static class MatchItem { String id, name; MatchItem(String i,String n){id=i;name=n;} }
    static class Kerry {
        double ho,hc,do_,dc,ao,ac,ro,rc,kho,khc,kdo,kdc,kao,kac;
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        ScrollView sc = new ScrollView(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(28,28,28,40); sc.addView(root);
        TextView title = tv("赔率轨迹 · 12BET工作流",24,true); root.addView(title);
        TextView sub = tv("自动匹配比赛 → 抓取12BET欧赔/返还率/凯利 → 亚盘与大小球确认 → 生成冻结结论",14,false); sub.setTextColor(Color.DKGRAY); root.addView(sub);
        query = new EditText(this); query.setHint("输入球队名、比赛名或 Tipsme Match ID"); query.setSingleLine(true); root.addView(query, mp(-1,56));
        LinearLayout buttons = new LinearLayout(this); buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button auto = new Button(this); auto.setText("自动匹配并分析"); auto.setOnClickListener(v->autoAnalyze()); buttons.addView(auto,new LinearLayout.LayoutParams(0,55,1));
        Button today = new Button(this); today.setText("今日比赛"); today.setOnClickListener(v->loadToday()); buttons.addView(today,new LinearLayout.LayoutParams(0,55,1));
        root.addView(buttons);
        status = tv("数据源优先：Tipsme公开赔率页；博彩公司优先：12BET。",13,false); status.setPadding(0,10,0,10); root.addView(status);
        result = tv("等待分析…",19,true); result.setPadding(18,18,18,18); result.setBackgroundColor(Color.rgb(245,247,250)); root.addView(result);
        details = tv("",14,false); details.setPadding(4,16,4,16); root.addView(details);
        TextView listTitle = tv("今日/进行中可匹配比赛",17,true); root.addView(listTitle);
        matchList = new LinearLayout(this); matchList.setOrientation(LinearLayout.VERTICAL); root.addView(matchList);
        setContentView(sc);
    }

    private TextView tv(String s,int sp,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); if(bold)t.setTypeface(null,1); return t; }
    private LinearLayout.LayoutParams mp(int w,int h){ return new LinearLayout.LayoutParams(w,h); }
    private void ui(Runnable r){ runOnUiThread(r); }

    private void loadToday(){
        status.setText("正在抓取今日比赛…"); matchList.removeAllViews();
        pool.submit(()->{
            try{
                String html=get(BASE);
                List<MatchItem> ms=parseMatches(html);
                ui(()->{ status.setText("已获取 "+ms.size()+" 场可匹配比赛"); for(MatchItem m:ms){
                    Button b=new Button(this); b.setAllCaps(false); b.setText(m.name+"  #"+m.id); b.setOnClickListener(v->{query.setText(m.id); analyzeId(m.id,m.name);}); matchList.addView(b);
                }});
            }catch(Exception e){ ui(()->status.setText("获取失败："+e.getMessage())); }
        });
    }

    private void autoAnalyze(){
        String q=query.getText().toString().trim(); if(q.isEmpty()){Toast.makeText(this,"请输入球队名或Match ID",Toast.LENGTH_SHORT).show();return;}
        if(q.matches("\\d{4,}")){ analyzeId(q,"Match #"+q); return; }
        status.setText("正在自动匹配比赛："+q);
        pool.submit(()->{
            try{
                List<MatchItem> ms=parseMatches(get(BASE)); MatchItem best=null; String key=q.toLowerCase(Locale.ROOT);
                for(MatchItem m:ms) if(m.name.toLowerCase(Locale.ROOT).contains(key)){best=m;break;}
                if(best==null) throw new Exception("今日列表未匹配到该比赛，可输入Tipsme Match ID重试");
                MatchItem f=best; ui(()->query.setText(f.id)); analyzeIdWorker(f.id,f.name);
            }catch(Exception e){ui(()->status.setText("匹配失败："+e.getMessage()));}
        });
    }

    private void analyzeId(String id,String name){ status.setText("正在抓取12BET数据…"); pool.submit(()->analyzeIdWorker(id,name)); }

    private void analyzeIdWorker(String id,String fallbackName){
        try{
            String khtml=get(BASE+"/match/"+id+"/odds/kerry");
            String ohtml=get(BASE+"/match/"+id+"/odds");
            String name=parseTitle(khtml); Kerry k=parse12BetKerry(khtml);
            String asian=parse12BetRow(ohtml,"Asian Handicap Football Odds Changes","Total over/under Football Odds Changes");
            String ou=parse12BetRow(ohtml,"Total over/under Football Odds Changes","1X2 Football Odds Changes");
            String conclusion=analyze(k,asian,ou);
            String detail="比赛："+(name.isEmpty()?fallbackName:name)+"\nMatch ID："+id+"\n\n12BET 欧赔/返还率/凯利\n"+
                    String.format(Locale.US,"初盘  %.2f / %.2f / %.2f   返还率 %.2f%%   K %.2f / %.2f / %.2f\n临场  %.2f / %.2f / %.2f   返还率 %.2f%%   K %.2f / %.2f / %.2f\n\n",k.ho,k.do_,k.ao,k.ro,k.kho,k.kdo,k.kao,k.hc,k.dc,k.ac,k.rc,k.khc,k.kdc,k.kac)+
                    "12BET 亚盘："+asian+"\n12BET 大小球："+ou+"\n\n判读逻辑\n"+explain(k);
            ui(()->{status.setText("抓取完成 · 12BET优先");result.setText(conclusion);details.setText(detail);});
        }catch(Exception e){ ui(()->{status.setText("分析失败");result.setText("未能生成结论");details.setText(e.getMessage()+"\n\n提示：部分比赛公开页可能暂时没有12BET数据。可稍后刷新或输入另一场Match ID。");}); }
    }

    private String analyze(Kerry k,String asian,String ou){
        double sh=(k.ho-k.hc)*1.6 + (k.kho-k.khc)*3.0;
        double sd=(k.do_-k.dc)*1.6 + (k.kdo-k.kdc)*3.0;
        double sa=(k.ao-k.ac)*1.6 + (k.kao-k.kac)*3.0;
        boolean drawAxis=Math.abs(k.dc-k.do_)<0.12 && Math.abs(k.kdc-k.kdo)<0.04 && Math.abs(k.hc-k.ho)>0.10 && Math.abs(k.ac-k.ao)>0.10;
        if(drawAxis) sd+=0.45;
        double[] s={sh,sd,sa}; String[] lab={"胜","平","负"}; Integer[] idx={0,1,2}; Arrays.sort(idx,(a,b)->Double.compare(s[b],s[a]));
        String first=lab[idx[0]], second=lab[idx[1]], third=lab[idx[2]];
        String hedge=(idx[0]==0?"主队不败":idx[0]==2?"客队不败":"平局优先，次防"+second);
        String score="1:1 / 0:0"; if(first.equals("胜"))score="1:0 / 2:1 / 2:0"; if(first.equals("负"))score="0:1 / 1:2 / 1:1";
        return "冻结结论："+first+" ＞ "+second+" ＞ "+third+"\n双选："+first+" / "+second+"\n方向："+hedge+"\n比分参考："+score;
    }

    private String explain(Kerry k){
        List<String> a=new ArrayList<>();
        if(k.hc>k.ho && k.khc>k.kho) a.add("• 主赔上升且主K上升：主胜同步弱化");
        if(k.hc<k.ho && k.khc<k.kho) a.add("• 主赔下降且主K下降：主胜同步强化");
        if(k.ac>k.ao && k.kac>k.kao) a.add("• 客赔上升且客K上升：客胜同步弱化");
        if(k.ac<k.ao && k.kac<k.kao) a.add("• 客赔下降且客K下降：客胜同步强化");
        if(Math.abs(k.dc-k.do_)<0.12 && Math.abs(k.kdc-k.kdo)<0.04) a.add("• 平赔与平K相对稳定：存在‘平局中轴’特征");
        if(k.rc-k.ro>1.0) a.add("• 返还率明显上升：临场定价层级变化较大，优先看相对路径而非绝对K值");
        a.add("• 不使用‘最低凯利=必出’的机械规则；赔率方向、K方向和亚盘需同参");
        return String.join("\n",a);
    }

    private Kerry parse12BetKerry(String html)throws Exception{
        String row=findRow(html,"12Bet"); if(row==null) row=findRow(html,"12BET"); if(row==null) throw new Exception("12BET凯利行未找到");
        String text=strip(row); Matcher m=Pattern.compile("\\d+(?:\\.\\d+)?").matcher(text); List<Double> n=new ArrayList<>(); while(m.find()) n.add(Double.parseDouble(m.group()));
        if(n.size()<14) throw new Exception("12BET凯利字段不足："+text);
        Kerry k=new Kerry(); int z=n.size()-14;
        k.ho=n.get(z); k.hc=n.get(z+1); k.do_=n.get(z+2); k.dc=n.get(z+3); k.ao=n.get(z+4); k.ac=n.get(z+5); k.ro=n.get(z+6); k.rc=n.get(z+7); k.kho=n.get(z+8); k.khc=n.get(z+9); k.kdo=n.get(z+10); k.kdc=n.get(z+11); k.kao=n.get(z+12); k.kac=n.get(z+13); return k;
    }

    private String parse12BetRow(String html,String from,String to){
        int a=html.indexOf(from), b=to==null?-1:html.indexOf(to,a+1); String seg=(a>=0)?html.substring(a,b>a?b:html.length()):html;
        String row=findRow(seg,"12Bet"); if(row==null)row=findRow(seg,"12BET"); return row==null?"未抓到":strip(row).replaceAll("\\s+"," ").trim();
    }

    private String findRow(String html,String key){
        Matcher tr=Pattern.compile("(?is)<tr[^>]*>(.*?)</tr>").matcher(html); while(tr.find()){String r=tr.group(1); if(strip(r).toLowerCase(Locale.ROOT).contains(key.toLowerCase(Locale.ROOT)))return r;} return null;
    }

    private List<MatchItem> parseMatches(String html){
        LinkedHashMap<String,String> map=new LinkedHashMap<>();
        Matcher m=Pattern.compile("(?is)<a[^>]+href=\"/en/match/(\\d+)[^\"]*\"[^>]*>(.*?)</a>").matcher(html);
        while(m.find()){String id=m.group(1), name=strip(m.group(2)).replaceAll("\\s+"," ").trim(); if(name.length()>5 && (name.toUpperCase(Locale.ROOT).contains("VS")||name.contains(" vs "))) map.put(id,name);}
        List<MatchItem> out=new ArrayList<>(); for(Map.Entry<String,String> e:map.entrySet()){out.add(new MatchItem(e.getKey(),e.getValue())); if(out.size()>=40)break;} return out;
    }

    private String parseTitle(String html){Matcher m=Pattern.compile("(?is)<h[12][^>]*>(.*?)</h[12]>").matcher(html); while(m.find()){String s=strip(m.group(1)); if(s.toLowerCase(Locale.ROOT).contains("vs")) return s.replace("-Kerry Index and Return","").trim();} return "";}
    private String strip(String s){return s.replaceAll("(?is)<script.*?</script>"," ").replaceAll("(?is)<style.*?</style>"," ").replaceAll("(?is)<[^>]+>"," ").replace("&nbsp;"," ").replace("&amp;","&").replace("&#39;","'").replace("&quot;","\"").trim();}
    private String get(String u)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(16000);c.setRequestProperty("User-Agent","Mozilla/5.0 (Android) OddsFlow/0.1");c.setRequestProperty("Accept-Language","zh-CN,zh;q=0.9,en;q=0.8");int code=c.getResponseCode();if(code<200||code>=400)throw new IOException("HTTP "+code);try(InputStream in=c.getInputStream(); ByteArrayOutputStream o=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)o.write(b,0,n);return o.toString(StandardCharsets.UTF_8.name());}}
}
