package com.abe.oddsboard;

import android.app.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class MainActivity extends Activity {
    final ExecutorService io=Executors.newSingleThreadExecutor();
    final int BLUE=Color.rgb(25,118,210), RED=Color.rgb(220,55,55), GREEN=Color.rgb(26,150,74);
    LinearLayout list, tabs; TextView title, sub, liveTab, fixtureTab; ProgressBar bar; boolean live=true;

    @Override public void onCreate(Bundle b){super.onCreate(b); build(); load(true);}

    void build(){
        LinearLayout root=col(); root.setBackgroundColor(Color.WHITE);
        LinearLayout head=row(); head.setPadding(dp(16),dp(8),dp(12),dp(5)); head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout box=col(); title=txt("百家赔率",24,Color.BLACK,true); sub=txt("原生多源数据版",11,Color.GRAY,false); box.addView(title);box.addView(sub);
        head.addView(box,new LinearLayout.LayoutParams(0,dp(60),1)); TextView rf=txt("刷新",15,BLUE,true);rf.setGravity(Gravity.CENTER);rf.setOnClickListener(v->load(live));head.addView(rf,new LinearLayout.LayoutParams(dp(62),dp(48)));root.addView(head);root.addView(line());
        tabs=row();tabs.setPadding(dp(30),0,dp(30),0);liveTab=tab("即时");fixtureTab=tab("赛程");tabs.addView(liveTab);tabs.addView(fixtureTab);liveTab.setOnClickListener(v->load(true));fixtureTab.setOnClickListener(v->load(false));root.addView(tabs);
        bar=new ProgressBar(this);bar.setVisibility(View.GONE);root.addView(bar,new LinearLayout.LayoutParams(-1,dp(3)));
        ScrollView sv=new ScrollView(this);list=col();sv.addView(list);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);select(true);
    }

    void load(boolean isLive){
        live=isLive;select(isLive);tabs.setVisibility(View.VISIBLE);list.removeAllViews();bar.setVisibility(View.VISIBLE);title.setText(isLive?"百家赔率":"今日赛程");sub.setText("正在尝试多个实时数据源…");
        io.execute(()->{List<Match> m=new ArrayList<>();String e=null;try{m=Source.matches();}catch(Throwable x){e=x.getMessage();}final List<Match> mm=m;final String ee=e;runOnUiThread(()->{bar.setVisibility(View.GONE);renderMatches(mm,ee);});});
    }

    void renderMatches(List<Match> ms,String err){
        if(ms.isEmpty()){sub.setText("实时源暂不可用");TextView t=txt("没有取得赛事数据。\n\nv0.4 已改为多源读取：移动页 → 桌面即时页 → 静态页 → 动态比分 feed。\n仍然不会打开任何第三方网页。\n\n"+(err==null?"请稍后刷新重试。":err),15,Color.DKGRAY,false);t.setPadding(dp(22),dp(28),dp(22),dp(24));list.addView(t);return;}
        Collections.sort(ms,(a,b)->{int r=Integer.compare(rank(a.status),rank(b.status));return r!=0?r:a.time.compareTo(b.time);});sub.setText("共 "+ms.size()+" 场 · 点击比赛查看赔率");
        int shown=0;for(Match m:ms){if(!live&&!("未开始".equals(m.status)))continue;shown++;LinearLayout c=col();c.setPadding(dp(15),dp(9),dp(15),dp(9));LinearLayout meta=row();meta.addView(txt(m.league,13,Color.rgb(40,135,125),true));meta.addView(txt("  "+m.time,13,Color.DKGRAY,false));TextView st=txt(m.status,13,"完".equals(m.status)?RED:BLUE,true);st.setGravity(Gravity.RIGHT);meta.addView(st,new LinearLayout.LayoutParams(0,-2,1));c.addView(meta);LinearLayout team=row();team.setGravity(Gravity.CENTER_VERTICAL);TextView h=txt(m.home,17,Color.rgb(30,30,30),true);h.setGravity(Gravity.RIGHT);TextView sc=txt(m.score,20,RED,true);sc.setGravity(Gravity.CENTER);TextView a=txt(m.away,17,Color.rgb(30,30,30),true);team.addView(h,new LinearLayout.LayoutParams(0,dp(42),1));team.addView(sc,new LinearLayout.LayoutParams(dp(82),dp(42)));team.addView(a,new LinearLayout.LayoutParams(0,dp(42),1));c.addView(team);if(!m.half.isEmpty()){TextView ht=txt("半场 "+m.half,11,Color.GRAY,false);ht.setGravity(Gravity.CENTER);c.addView(ht);}c.setOnClickListener(v->odds(m));list.addView(c);list.addView(line());}
        if(shown==0){TextView t=txt("当前没有未开始赛事。",15,Color.DKGRAY,false);t.setPadding(dp(22),dp(30),dp(22),dp(20));list.addView(t);}
    }

    void odds(Match m){tabs.setVisibility(View.GONE);list.removeAllViews();title.setText(m.home+"  "+m.score+"  "+m.away);sub.setText(m.league+" · 正在获取百家赔率");TextView bk=txt("‹ 返回比赛",15,BLUE,true);bk.setPadding(dp(16),dp(10),dp(16),dp(10));bk.setOnClickListener(v->load(live));list.addView(bk);list.addView(line());bar.setVisibility(View.VISIBLE);io.execute(()->{OddsPack p=new OddsPack();String e=null;try{p=Source.odds(m.id);}catch(Throwable x){e=x.getMessage();}final OddsPack pp=p;final String ee=e;runOnUiThread(()->{bar.setVisibility(View.GONE);renderOdds(m,pp,ee);});});}

    void renderOdds(Match m,OddsPack p,String err){sub.setText(m.league+" · "+m.time+" · 原生赔率详情");LinearLayout nav=row();for(String s:new String[]{"胜平负","让球","大小球","凯利"}){TextView t=txt(s,15,"胜平负".equals(s)?BLUE:Color.GRAY,true);t.setGravity(Gravity.CENTER);nav.addView(t,new LinearLayout.LayoutParams(0,dp(46),1));}list.addView(nav);list.addView(line());if(p.euro.isEmpty()){TextView t=txt("当前比赛暂未取得欧赔数据。\n\n"+(err==null?"可返回后稍后刷新。":err),14,Color.DKGRAY,false);t.setPadding(dp(18),dp(24),dp(18),dp(24));list.addView(t);return;}header();stats(p.euro);for(OddRow r:p.euro)oddrow(r);}
    void header(){LinearLayout h=row();h.setPadding(dp(6),dp(8),dp(6),dp(8));h.setBackgroundColor(Color.rgb(248,249,250));String[] x={"公司","初主","初平","初客","即主","即平","即客"};int[] w={92,52,52,52,52,52,52};for(int i=0;i<x.length;i++){TextView t=txt(x[i],12,Color.DKGRAY,true);t.setGravity(Gravity.CENTER);h.addView(t,new LinearLayout.LayoutParams(dp(w[i]),dp(34)));}list.addView(h);}
    void stats(List<OddRow> rs){double[] mi={99,99,99},ma={0,0,0},av={0,0,0};int n=0;for(OddRow r:rs){double[] x={r.nh,r.nd,r.na};if(x[0]<=0)continue;n++;for(int i=0;i<3;i++){mi[i]=Math.min(mi[i],x[i]);ma[i]=Math.max(ma[i],x[i]);av[i]+=x[i];}}if(n==0)return;for(int i=0;i<3;i++)av[i]/=n;stat("最大值",ma);stat("最小值",mi);stat(n+"家平均",av);}
    void stat(String n,double[] x){LinearLayout r=row();r.addView(cell(n,Color.DKGRAY,92));for(int i=0;i<3;i++)r.addView(cell("-",Color.GRAY,52));for(double v:x)r.addView(cell(fmt(v),Color.BLACK,52));list.addView(r);}
    void oddrow(OddRow o){LinearLayout r=row();r.addView(cell(o.company,Color.DKGRAY,92));double[] x={o.oh,o.od,o.oa,o.nh,o.nd,o.na};for(int i=0;i<x.length;i++){int c=Color.BLACK;if(i>=3){double z=x[i-3];c=x[i]>z?RED:(x[i]<z?GREEN:Color.BLACK);}r.addView(cell(fmt(x[i]),c,52));}list.addView(r);list.addView(line());}
    TextView cell(String s,int c,int w){TextView t=txt(s,12,c,true);t.setGravity(Gravity.CENTER);t.setPadding(1,6,1,6);t.setLayoutParams(new LinearLayout.LayoutParams(dp(w),dp(36)));return t;}

    void select(boolean l){liveTab.setTextColor(l?BLUE:Color.GRAY);fixtureTab.setTextColor(l?Color.GRAY:BLUE);}TextView tab(String s){TextView t=txt(s,17,Color.GRAY,true);t.setGravity(Gravity.CENTER);t.setLayoutParams(new LinearLayout.LayoutParams(0,dp(46),1));return t;}LinearLayout row(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.HORIZONTAL);return x;}LinearLayout col(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);return x;}View line(){View v=new View(this);v.setBackgroundColor(Color.rgb(238,238,238));v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return v;}TextView txt(String s,float z,int c,boolean b){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);if(b)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}String fmt(double v){return v<=0?"-":String.format(Locale.US,"%.2f",v);}static int rank(String s){if("进行中".equals(s))return 0;if("完".equals(s))return 1;return 2;}
    @Override public void onBackPressed(){if(tabs.getVisibility()==View.GONE)load(live);else super.onBackPressed();}

    static class Match{String id="",league="足球",time="--:--",status="未开始",home="",away="",score="-",half="";}
    static class OddRow{String company="";double oh,od,oa,nh,nd,na;}
    static class OddsPack{List<OddRow> euro=new ArrayList<>();}

    static class Source{
        static final String UA="Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/140 Mobile Safari/537.36";
        static List<Match> matches() throws IOException{
            LinkedHashMap<String,Match> out=new LinkedHashMap<>();List<String> errors=new ArrayList<>();String[] html={"https://m.titan007.com/","https://live.titan007.com/","https://livestatic.titan007.com/"};
            for(String u:html){try{Document d=Jsoup.connect(u).userAgent(UA).referrer("https://www.titan007.com/").timeout(10000).get();fromHtml(d,out);if(!out.isEmpty())break;}catch(Throwable e){errors.add(shortName(u)+":"+e.getClass().getSimpleName());}}
            if(out.isEmpty()){String ts=String.valueOf(System.currentTimeMillis());String[] feed={"https://live.titan007.com/vbsxml/bfdata.js?r=007"+ts,"http://live.titan007.com/vbsxml/bfdata.js?r=007"+ts,"https://live.titan007.com/vbsxml/goalBf3.xml?r=007"+ts};for(String u:feed){try{String b=Jsoup.connect(u).ignoreContentType(true).userAgent(UA).referrer("https://live.titan007.com/").timeout(10000).execute().body();fromFeed(b,out);if(!out.isEmpty())break;}catch(Throwable e){errors.add(shortName(u)+":"+e.getClass().getSimpleName());}}}
            if(out.isEmpty())throw new IOException("多源读取失败："+String.join(" · ",errors));return new ArrayList<>(out.values());
        }
        static String shortName(String u){return u.replace("https://","").replace("http://","").split("/")[0];}
        static void fromHtml(Document d,LinkedHashMap<String,Match> out){Pattern p=Pattern.compile("(?:analysis/|match/|oddslist/|id=)(\\d{5,})",Pattern.CASE_INSENSITIVE);for(Element e:d.select("a[href],tr,[onclick],[data-id]")){String src=e.attr("abs:href")+" "+e.id()+" "+e.attr("onclick")+" "+e.attr("data-id");Matcher m=p.matcher(src);if(!m.find()){m=Pattern.compile("(\\d{6,})").matcher(src);if(!m.find())continue;}Match x=parse(m.group(1),e.text());if(valid(x))out.putIfAbsent(x.id,x);}}
        static void fromFeed(String b,LinkedHashMap<String,Match> out){if(b==null)return;String s=b.replace('\r','\n');for(String line:s.split("\n")){Matcher id=Pattern.compile("(\\d{6,})").matcher(line);if(!id.find())continue;Match x=parse(id.group(1),line.replaceAll("[\\[\\]{}\\\"']"," ").replaceAll("[,;|^]+"," "));if(valid(x))out.putIfAbsent(x.id,x);if(out.size()>350)break;}}
        static boolean valid(Match m){return m.home.length()>1&&m.away.length()>1&&!m.home.equals(m.away);}
        static Match parse(String id,String raw){Match m=new Match();m.id=id;String r=raw==null?"":raw.replaceAll("\\s+"," ").trim();Matcher tm=Pattern.compile("(\\d{1,2}:\\d{2})").matcher(r);if(tm.find())m.time=tm.group(1);Matcher sc=Pattern.compile("(?:^|\\s)(\\d{1,2})\\s*[:：-]\\s*(\\d{1,2})(?:\\s|$)").matcher(r);if(sc.find()){m.score=sc.group(1)+":"+sc.group(2);m.status="完";}if(r.matches(".*(上半|下半|中场|进行中|HT|1H|2H).*"))m.status="进行中";String c=r.replace(m.time," ").replace(m.score," ").replaceAll("\\([^)]*\\)"," ").replaceAll("\\b\\d+\\b"," ").replaceAll("\\s+"," ").trim();List<String>w=new ArrayList<>();for(String q:c.split(" "))if(q.length()>1&&!q.matches("完|未开始|进行中|动画|析|亚|欧|比分|指数|详情"))w.add(q);if(w.size()>=3){m.league=w.get(0);m.home=w.get(w.size()-2);m.away=w.get(w.size()-1);}else if(w.size()==2){m.home=w.get(0);m.away=w.get(1);}return m;}
        static OddsPack odds(String id)throws IOException{OddsPack p=new OddsPack();Document d=Jsoup.connect("https://1x2.titan007.com/oddslist/"+id+".htm").userAgent(UA).referrer("https://zq.titan007.com/").timeout(12000).get();Set<String>s=new HashSet<>();for(Element tr:d.select("tr")){Elements td=tr.select("td");if(td.size()<6)continue;String company=td.get(0).text().trim();if(company.isEmpty()||company.length()>28||s.contains(company))continue;List<Double> n=new ArrayList<>();for(Element c:td){Matcher z=Pattern.compile("(?<!\\d)(\\d+\\.\\d{2})(?!\\d)").matcher(c.text());while(z.find())try{n.add(Double.parseDouble(z.group(1)));}catch(Exception ignored){}}if(n.size()>=6){OddRow o=new OddRow();o.company=company;o.oh=n.get(0);o.od=n.get(1);o.oa=n.get(2);o.nh=n.get(n.size()-3);o.nd=n.get(n.size()-2);o.na=n.get(n.size()-1);if(o.oh>1&&o.od>1&&o.oa>1){p.euro.add(o);s.add(company);}}if(p.euro.size()>=80)break;}return p;}
    }
}
