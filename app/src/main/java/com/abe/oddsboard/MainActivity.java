package com.abe.oddsboard;

import android.app.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.*;
import android.view.*;
import android.widget.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Native-only OddsBoard UI. No WebView is used anywhere in this app. */
public class MainActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private LinearLayout listBox, tabBar;
    private TextView title, subtitle, liveTab, scheduleTab;
    private ProgressBar spinner;
    private boolean liveMode = true;
    private final int BLUE = Color.rgb(25,118,210), RED = Color.rgb(220,55,55), GREEN = Color.rgb(26,150,74);

    @Override public void onCreate(Bundle b) { super.onCreate(b); buildUi(); loadMatches(true); }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.WHITE);
        LinearLayout header = row(); header.setPadding(dp(16), dp(10), dp(12), dp(6)); header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout tb = col(); title = text("百家赔率",24,Color.BLACK,true); subtitle = text("原生数据版 · 无网页套壳",11,Color.GRAY,false); tb.addView(title); tb.addView(subtitle);
        header.addView(tb,new LinearLayout.LayoutParams(0,dp(58),1));
        TextView refresh = text("刷新",14,BLUE,true); refresh.setGravity(Gravity.CENTER); refresh.setOnClickListener(v -> loadMatches(liveMode)); header.addView(refresh,new LinearLayout.LayoutParams(dp(64),dp(48)));
        root.addView(header); root.addView(line());

        tabBar = row(); tabBar.setPadding(dp(26),0,dp(26),0);
        liveTab = tab("即时"); scheduleTab = tab("赛程"); tabBar.addView(liveTab); tabBar.addView(scheduleTab);
        liveTab.setOnClickListener(v -> loadMatches(true)); scheduleTab.setOnClickListener(v -> loadMatches(false)); root.addView(tabBar);

        spinner = new ProgressBar(this); spinner.setVisibility(View.GONE); root.addView(spinner,new LinearLayout.LayoutParams(-1,dp(3)));
        ScrollView scroll = new ScrollView(this); listBox = col(); listBox.setPadding(0,dp(2),0,dp(24)); scroll.addView(listBox); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root); selectTab(true);
    }

    private void loadMatches(boolean live) {
        liveMode = live; selectTab(live); listBox.removeAllViews(); spinner.setVisibility(View.VISIBLE);
        title.setText(live ? "百家赔率" : "今日赛程"); subtitle.setText("正在获取实时赛事数据…");
        io.execute(() -> {
            List<Match> ms = new ArrayList<>(); String err = null;
            try { ms = TitanSource.loadMatches(); } catch (Throwable e) { err = e.getMessage(); }
            final List<Match> out = ms; final String msg = err;
            runOnUiThread(() -> { spinner.setVisibility(View.GONE); renderMatches(out, msg); });
        });
    }

    private void renderMatches(List<Match> ms, String err) {
        if (ms.isEmpty()) {
            subtitle.setText("实时源暂不可用");
            TextView e = text("没有取得赛事数据。\n\n本版已完全移除 WebView；数据源异常时不会跳转或显示第三方网页。\n点击“刷新”可重试。" + (err==null?"":"\n\n"+err),15,Color.DKGRAY,false);
            e.setPadding(dp(22),dp(34),dp(22),dp(20)); listBox.addView(e); return;
        }
        Collections.sort(ms,(a,b)->Integer.compare(rank(a.status),rank(b.status))!=0?Integer.compare(rank(a.status),rank(b.status)):a.time.compareTo(b.time));
        subtitle.setText("共 "+ms.size()+" 场 · 点击比赛查看赔率");
        for (Match m: ms) {
            if (!liveMode && ("完".equals(m.status)||"进行中".equals(m.status))) continue;
            LinearLayout card = col(); card.setPadding(dp(16),dp(10),dp(16),dp(10)); card.setBackgroundColor(Color.WHITE);
            LinearLayout meta = row(); TextView league = text(m.league,13,leagueColor(m.league),true); TextView time = text("  "+m.time,13,Color.DKGRAY,false); TextView state = text(m.status,13,"完".equals(m.status)?RED:BLUE,true); state.setGravity(Gravity.RIGHT);
            meta.addView(league); meta.addView(time); meta.addView(state,new LinearLayout.LayoutParams(0,-2,1)); card.addView(meta);
            LinearLayout teams = row(); teams.setGravity(Gravity.CENTER_VERTICAL); TextView home=text(m.home,17,Color.rgb(30,30,30),true); home.setGravity(Gravity.RIGHT); TextView score=text(m.score,20,RED,true); score.setGravity(Gravity.CENTER); TextView away=text(m.away,17,Color.rgb(30,30,30),true);
            teams.addView(home,new LinearLayout.LayoutParams(0,dp(42),1)); teams.addView(score,new LinearLayout.LayoutParams(dp(82),dp(42))); teams.addView(away,new LinearLayout.LayoutParams(0,dp(42),1)); card.addView(teams);
            if (!m.half.isEmpty()) { TextView half=text("半场 "+m.half,11,Color.GRAY,false); half.setGravity(Gravity.CENTER); card.addView(half); }
            card.setOnClickListener(v -> showOdds(m)); listBox.addView(card); listBox.addView(line());
        }
    }

    private void showOdds(Match m) {
        listBox.removeAllViews(); tabBar.setVisibility(View.GONE); title.setText(m.home+"  "+m.score+"  "+m.away); subtitle.setText(m.league+" · "+m.time+" · 正在读取百家赔率"); spinner.setVisibility(View.VISIBLE);
        TextView back=text("‹ 返回比赛",15,BLUE,true); back.setPadding(dp(16),dp(10),dp(16),dp(10)); back.setOnClickListener(v->{tabBar.setVisibility(View.VISIBLE);loadMatches(liveMode);}); listBox.addView(back); listBox.addView(line());
        io.execute(() -> {
            OddsPack p; String err=null; try { p=TitanSource.loadOdds(m.id); } catch(Throwable e){p=new OddsPack();err=e.getMessage();}
            final OddsPack out=p; final String ee=err; runOnUiThread(()->{spinner.setVisibility(View.GONE);renderOdds(m,out,ee);});
        });
    }

    private void renderOdds(Match m, OddsPack p, String err) {
        subtitle.setText(m.league+" · "+m.time+" · 原生赔率详情");
        LinearLayout tabs=row(); String[] labels={"胜平负","让球","大小球","凯利"};
        for(String s:labels){TextView t=text(s,15,"胜平负".equals(s)?BLUE:Color.GRAY,true);t.setGravity(Gravity.CENTER);tabs.addView(t,new LinearLayout.LayoutParams(0,dp(46),1));}
        listBox.addView(tabs); listBox.addView(line());
        if(p.euro.isEmpty()) {
            TextView e=text("当前比赛没有解析到百家欧赔数据。\n\n界面已经完全原生化，没有任何网页内容。后续只需继续增强数据适配器，不需要再改 UI。"+(err==null?"":"\n\n"+err),14,Color.DKGRAY,false); e.setPadding(dp(18),dp(24),dp(18),dp(24)); listBox.addView(e); return;
        }
        addEuroHeader(); addStatsRows(p.euro);
        for(OddRow r:p.euro) addOddRow(r);
    }

    private void addEuroHeader(){ LinearLayout h=row();h.setPadding(dp(8),dp(10),dp(8),dp(10));h.setBackgroundColor(Color.rgb(248,249,250));
        String[] a={"公司","初 主","初 平","初 客","即 主","即 平","即 客"}; int[] ws={96,54,54,54,54,54,54}; for(int i=0;i<a.length;i++){TextView t=text(a[i],12,Color.DKGRAY,true);t.setGravity(Gravity.CENTER);h.addView(t,new LinearLayout.LayoutParams(dp(ws[i]),dp(36)));} listBox.addView(h); }

    private void addStatsRows(List<OddRow> rows){ double[] min={99,99,99},max={0,0,0},avg={0,0,0};int n=0;for(OddRow r:rows){double[] x={r.nh,r.nd,r.na};if(x[0]<=0)continue;n++;for(int i=0;i<3;i++){min[i]=Math.min(min[i],x[i]);max[i]=Math.max(max[i],x[i]);avg[i]+=x[i];}}if(n==0)return;for(int i=0;i<3;i++)avg[i]/=n; addStat("最大值",max);addStat("最小值",min);addStat(n+"家平均",avg); }
    private void addStat(String name,double[] x){LinearLayout r=row();r.setPadding(dp(8),dp(5),dp(8),dp(5));TextView c=text(name,13,Color.DKGRAY,true);c.setGravity(Gravity.CENTER);r.addView(c,new LinearLayout.LayoutParams(dp(96),dp(34)));for(int i=0;i<3;i++){r.addView(cell("-",Color.GRAY));}for(double v:x)r.addView(cell(f(v),Color.BLACK));listBox.addView(r);}
    private void addOddRow(OddRow o){LinearLayout r=row();r.setPadding(dp(8),dp(5),dp(8),dp(5));TextView c=text(o.company,12,Color.DKGRAY,true);c.setGravity(Gravity.CENTER);r.addView(c,new LinearLayout.LayoutParams(dp(96),dp(38)));double[] x={o.oh,o.od,o.oa,o.nh,o.nd,o.na};for(int i=0;i<x.length;i++){int color=Color.BLACK;if(i>=3){double old=x[i-3];color=x[i]>old?RED:(x[i]<old?GREEN:Color.BLACK);}r.addView(cell(f(x[i]),color));}listBox.addView(r);listBox.addView(line());}
    private TextView cell(String s,int color){TextView t=text(s,13,color,true);t.setGravity(Gravity.CENTER);return t;}

    private static int rank(String s){ if("进行中".equals(s))return 0;if("完".equals(s))return 1;return 2; }
    private int leagueColor(String l){ if(l.contains("英")||l.contains("意"))return Color.rgb(80,70,150); if(l.contains("瑞")||l.contains("丹"))return Color.rgb(40,145,130); return Color.rgb(205,135,20); }
    private String f(double v){return v<=0?"-":String.format(Locale.US,"%.2f",v);} private View line(){View v=new View(this);v.setBackgroundColor(Color.rgb(238,238,238));v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return v;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);return l;} private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private TextView tab(String s){TextView t=text(s,17,Color.GRAY,true);t.setGravity(Gravity.CENTER);t.setLayoutParams(new LinearLayout.LayoutParams(0,dp(46),1));return t;} private void selectTab(boolean l){liveTab.setTextColor(l?BLUE:Color.GRAY);scheduleTab.setTextColor(l?Color.GRAY:BLUE);}
    private TextView text(String s,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;} private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    @Override public void onBackPressed(){ if(tabBar.getVisibility()==View.GONE){tabBar.setVisibility(View.VISIBLE);loadMatches(liveMode);}else super.onBackPressed(); }

    static class Match { String id="",league="",time="",status="未开始",home="",away="",score="-",half=""; }
    static class OddRow { String company=""; double oh,od,oa,nh,nd,na; }
    static class OddsPack { List<OddRow> euro=new ArrayList<>(); }

    static class TitanSource {
        static final String UA="Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/140 Mobile Safari/537.36";
        static List<Match> loadMatches() throws IOException {
            Document d=Jsoup.connect("https://m.titan007.com/").userAgent(UA).timeout(12000).get();
            LinkedHashMap<String,Match> map=new LinkedHashMap<>(); Pattern idp=Pattern.compile("(?:analysis/|match/|id=)(\\d{5,})",Pattern.CASE_INSENSITIVE);
            for(Element a:d.select("a[href]")){String href=a.attr("abs:href");Matcher mm=idp.matcher(href);if(!mm.find())continue;String id=mm.group(1);String raw=a.parent()!=null?a.parent().text():a.text();raw=raw.replaceAll("\\s+"," ").trim();if(raw.length()<5)continue;Match m=map.get(id);if(m==null){m=parseMatch(id,raw);if(m.home.length()>0&&m.away.length()>0)map.put(id,m);} }
            if(map.isEmpty()) {
                for(Element e:d.select("[id],[onclick]")){String raw=e.text().replaceAll("\\s+"," ").trim();String src=e.id()+" "+e.attr("onclick");Matcher mm=Pattern.compile("(\\d{6,})").matcher(src);if(mm.find()){Match m=parseMatch(mm.group(1),raw);if(m.home.length()>0&&m.away.length()>0)map.put(m.id,m);} }
            }
            return new ArrayList<>(map.values());
        }
        static Match parseMatch(String id,String raw){ Match m=new Match();m.id=id; Matcher tm=Pattern.compile("(\\d{1,2}:\\d{2})").matcher(raw);m.time=tm.find()?tm.group(1):"--:--"; Matcher sm=Pattern.compile("(\\d{1,2})\\s*[:：-]\\s*(\\d{1,2})").matcher(raw);if(sm.find()){m.score=sm.group(1)+":"+sm.group(2);m.status="完";} if(raw.matches(".*(上半|下半|中场|进行).*"))m.status="进行中";
            String clean=raw.replace(m.time," ").replace(m.score," ").replaceAll("\\([^)]*\\)"," ").replaceAll("\\b\\d+\\b"," ").replaceAll("\\s+"," ").trim(); String[] p=clean.split(" "); List<String> words=new ArrayList<>();for(String x:p)if(x.length()>1&&!x.matches("完|未|进行中|动画|析|亚|欧"))words.add(x); if(words.size()>=3){m.league=words.get(0);m.home=words.get(words.size()-2);m.away=words.get(words.size()-1);} else if(words.size()>=2){m.league="足球";m.home=words.get(0);m.away=words.get(1);} return m; }
        static OddsPack loadOdds(String id) throws IOException {
            OddsPack p=new OddsPack(); Document d=Jsoup.connect("https://1x2.titan007.com/oddslist/"+id+".htm").userAgent(UA).referrer("https://zq.titan007.com/").timeout(12000).get();
            Set<String> seen=new HashSet<>(); for(Element tr:d.select("tr")){Elements td=tr.select("td");if(td.size()<7)continue;String company=td.get(0).text().trim();if(company.length()==0||company.length()>25||seen.contains(company))continue;List<Double> nums=new ArrayList<>();for(Element cell:td){Matcher mm=Pattern.compile("(?<!\\d)(\\d+\\.\\d{2})(?!\\d)").matcher(cell.text());while(mm.find())try{nums.add(Double.parseDouble(mm.group(1)));}catch(Exception ignored){}}if(nums.size()>=6){OddRow o=new OddRow();o.company=company;o.oh=nums.get(0);o.od=nums.get(1);o.oa=nums.get(2);o.nh=nums.get(nums.size()-3);o.nd=nums.get(nums.size()-2);o.na=nums.get(nums.size()-1);if(o.oh>1&&o.od>1&&o.oa>1){p.euro.add(o);seen.add(company);}}if(p.euro.size()>=60)break; }
            return p;
        }
    }
}
