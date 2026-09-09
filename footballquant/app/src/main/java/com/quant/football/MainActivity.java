package com.quant.football;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService io=Executors.newFixedThreadPool(3);
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final HtmlDataSource html=new HtmlDataSource();
    private final GlobalSportsDataSource global=new GlobalSportsDataSource(html);
    private final MarketDataSource market=new MarketDataSource(html);
    private final Predictor football=new Predictor();
    private final BasketballPredictor basketball=new BasketballPredictor();
    private final GoalEnsemble goals=new GoalEnsemble();
    private final DivinationEngine divination=new DivinationEngine();
    private final MultiConsensusEngine multiConsensus=new MultiConsensusEngine();
    private final HalfFullEngine halfFull=new HalfFullEngine();
    private final BasketballDivinationEngine basketballDivination=new BasketballDivinationEngine();
    private HistoryStore history; private SharedPreferences prefs; private LinearLayout content; private TextView status;
    private String mode="GF",day=HistoryStore.today(); private Button jc,bj,gf,bb;

    @Override public void onCreate(Bundle b){super.onCreate(b);history=new HistoryStore(this);prefs=getSharedPreferences("footballquant_v711",MODE_PRIVATE);build();load("GF");}
    @Override protected void onDestroy(){super.onDestroy();io.shutdownNow();}

    private void build(){
        Window w=getWindow();if(android.os.Build.VERSION.SDK_INT>=35)w.setStatusBarColor(Color.rgb(244,247,248));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(12),dp(8),dp(12),dp(8));root.setBackgroundColor(Color.rgb(244,247,248));
        if(android.os.Build.VERSION.SDK_INT>=23){root.setOnApplyWindowInsetsListener((v,insets)->{int top=0;if(android.os.Build.VERSION.SDK_INT>=30)top=insets.getInsets(WindowInsets.Type.statusBars()).top;else top=insets.getSystemWindowInsetTop();v.setPadding(dp(12),top+dp(6),dp(12),dp(8));return insets;});}
        TextView title=tv("FootballQuant V8.0 · Fusion V4",20,true);title.setTextColor(Color.rgb(28,64,75));root.addView(title);root.addView(tv("Market/Quant正式层 · 太玄81态/五行/干支钟律/双三位数Shadow · Half/Full · 大小单双四象 · Random-Control",10,false));
        LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);jc=btn("竞彩");bj=btn("北单");gf=btn("全球足球");bb=btn("篮球");tabs.addView(jc,wgt());tabs.addView(bj,wgt());tabs.addView(gf,wgt());tabs.addView(bb,wgt());root.addView(tabs);
        LinearLayout tools=new LinearLayout(this);tools.setOrientation(LinearLayout.HORIZONTAL);Button refresh=btn("刷新当天"),skill=btn("V4内核"),prev=btn("前一天"),today=btn("今天");tools.addView(refresh,wgt());tools.addView(skill,wgt());tools.addView(prev,wgt());tools.addView(today,wgt());root.addView(tools);
        status=tv("准备加载",11,true);status.setPadding(0,dp(4),0,dp(4));root.addView(status);
        ScrollView sv=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);sv.addView(content);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        jc.setOnClickListener(v->load("JC"));bj.setOnClickListener(v->load("BJ"));gf.setOnClickListener(v->load("GF"));bb.setOnClickListener(v->load("BB"));refresh.setOnClickListener(v->load(mode));skill.setOnClickListener(v->skillInfo());prev.setOnClickListener(v->{day=shift(day,-1);load(mode);});today.setOnClickListener(v->{day=HistoryStore.today();load(mode);});
    }

    private void load(String m){mode=m;setActive();content.removeAllViews();status.setText("正在获取 "+day+" · "+modeName(m)+" …");io.execute(()->{try{if("BB".equals(m)){List<BasketballMatch>x=global.fetchBasketball(day);ui.post(()->renderBasketball(x));return;}List<MatchInfo>x;if("JC".equals(m))x=html.fetchJc(prefs.getString("jc_url",""),day);else if("BJ".equals(m))x=html.fetchBj(prefs.getString("bj_url",""),day);else x=global.fetchFootball(day);history.saveBase(m,day,x,football,goals,divination);ui.post(()->renderFootball(x));}catch(Exception e){ui.post(()->error("获取失败："+e.getMessage()));}});}
    private void renderFootball(List<MatchInfo> ms){content.removeAllViews();status.setText(day+" · "+modeName(mode)+" · "+ms.size()+"场 · BASE冻结 + V4多重合参");if(ms.isEmpty()){error("当前源没有解析到比赛。全球足球为公开补充源；竞彩官方SP与第三方赔率严格分离。");return;}for(MatchInfo m:ms)content.addView(footballCard(m));}

    private LinearLayout footballCard(MatchInfo m){
        Prediction p=football.predict(m);String seed=history.getOrCreateDivinationSeed(mode,day,m);String manual=prefs.getString(manualKey(m),"");
        DivinationEngine.Bundle d=divination.run(m,day,seed);MultiConsensusEngine.Result mx=multiConsensus.run(m,day,seed,manual);GoalEnsemble.Result g=goals.predict(m,p,null,d,mx);HalfFullEngine.Result hf=halfFull.predict(p);history.saveV4Shadow(mode,day,m,mx,hf,"BASE");
        LinearLayout box=cardBox();box.addView(tv(clean(m.league)+" · "+m.kickoff+" · "+m.status,10,false));box.addView(tv(clean(m.home)+"  vs  "+clean(m.away)+(m.score.isEmpty()?"":"   "+m.score),15,true));
        box.addView(tv("["+p.grade+"] 1X2 主"+pct(p.home)+" / 平"+pct(p.draw)+" / 客"+pct(p.away)+" · "+p.primary+" 防"+p.secondary,11,true));
        box.addView(tv("总进球 "+g.pick+" · O2.5 "+pct(g.over25)+" · "+g.range+" · 单双 "+g.oddPick+" · 四象 "+g.quadTag,11,true));
        box.addView(tv("比分 Top4 "+join(p.scores)+" · 半全场 "+hf.topPick+" "+pct(hf.topProb)+" · Top3 "+hf.top3,10,false));
        box.addView(tv("ZeroGoal 主0 "+pct(p.homeZero)+" / 客0 "+pct(p.awayZero)+" / 任一0 "+pct(p.anyZero)+" · BTTS "+pct(p.bttsYes),10,false));
        box.addView(tv("Margin "+p.favoriteSide+"净胜1球 "+pct(p.favWin1)+" / 2球 "+pct(p.favWin2)+" / 3+ "+pct(p.favWin3Plus),10,false));
        TextView q=tv("数据质量 "+p.dataQuality+(p.disagreementAlert?" · ⚠ "+p.disagreement:" · 分歧未触发"),10,true);q.setTextColor(p.disagreementAlert?Color.rgb(190,65,45):Color.rgb(45,105,72));box.addView(q);
        box.addView(tv(mx.summary(),9,false));
        Button number=btn(manual.isEmpty()?"输入并冻结手动三位数":"手动三位数 "+manual+" · 已冻结");box.addView(number);number.setOnClickListener(v->{if(manual.isEmpty())inputManualNumber(m);else new AlertDialog.Builder(this).setTitle("三位数已冻结").setMessage("本场手动数为 "+manual+"。为保证盲测可复演，同一比赛不在赛前版本中重抽或覆盖。").setPositiveButton("关闭",null).show();});
        Button deep=btn("联网深度：百家盘口 / 凯利返还率 / AI / xG");box.addView(deep);deep.setOnClickListener(v->{deep.setEnabled(false);deep.setText("正在获取…");io.execute(()->{try{MarketSnapshot s=market.fetch(m,prefs.getString("model_api",""),day);Prediction pp=football.predict(m,s);DivinationEngine.Bundle dd=divination.run(m,day,seed);String mm=prefs.getString(manualKey(m),"");MultiConsensusEngine.Result mmx=multiConsensus.run(m,day,seed,mm);GoalEnsemble.Result gg=goals.predict(m,pp,s,dd,mmx);HalfFullEngine.Result hh=halfFull.predict(pp);history.update(mode,day,m,pp,gg);history.saveV4Shadow(mode,day,m,mmx,hh,"DEEP");ui.post(()->{deep.setEnabled(true);deep.setText("深度完成："+pp.grade+" · "+pp.primary+" · "+gg.pick+" · "+gg.quadTag+" · 半全场"+hh.topPick+" · 公司"+pp.bookmakerCount+"家"+(pp.disagreementAlert?" ⚠分歧":""));});}catch(Exception e){ui.post(()->{deep.setEnabled(true);deep.setText("深度失败："+e.getMessage());});}});});return box;
    }

    private void inputManualNumber(MatchInfo m){
        EditText in=new EditText(this);in.setHint("000-999");in.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this).setTitle("输入手动三位数").setMessage("保存后本场冻结，不因结果或刷新而重抽。").setView(in).setNegativeButton("取消",null).setPositiveButton("冻结",(d,w)->{try{String n=DualNumberFreezeEngine.normalizeManualNumber(in.getText().toString());prefs.edit().putString(manualKey(m),n).apply();load(mode);}catch(Exception e){status.setText("三位数无效：请输入000-999");}}).show();
    }
    private String manualKey(MatchInfo m){return "v4_manual_"+day+"_"+(clean(m.matchId)+clean(m.home)+clean(m.away)).hashCode();}

    private void renderBasketball(List<BasketballMatch> ms){content.removeAllViews();status.setText(day+" · 篮球 · "+ms.size()+"场 · Moneyline/Margin/Spread/Total + Shadow");if(ms.isEmpty()){error("当前公开篮球补充源未覆盖该日赛事。CBA/NBL/MPBL等继续接入可授权或公开稳定源，缺失时不伪造。");return;}for(BasketballMatch m:ms){BasketballPrediction p=basketball.predict(m);BasketballDivinationEngine.Bundle d=basketballDivination.run(m,day,lockedSeed(m));LinearLayout box=cardBox();box.addView(tv(clean(m.league)+" · "+m.kickoff+" · "+m.status,10,false));box.addView(tv(clean(m.home)+"  vs  "+clean(m.away)+(m.score.isEmpty()?"":"   "+m.score),15,true));box.addView(tv("["+p.grade+"] "+p.winner+" · 主"+pct(p.homeWin)+" / 客"+pct(p.awayWin)+" · Expected Margin "+signed(p.expectedMargin),11,true));box.addView(tv("Spread "+(Double.isNaN(m.marketSpread)?"缺失":signed(m.marketSpread))+" → "+p.spreadPick+" · Total "+(Double.isNaN(m.marketTotal)?"缺失":f1(m.marketTotal))+" → "+p.totalPick,10,false));box.addView(tv("Expected Score "+f1(p.homePoints)+" : "+f1(p.awayPoints)+" · Margin 1-5 "+pct(p.margin1to5)+" / 6-10 "+pct(p.margin6to10)+" / 11-15 "+pct(p.margin11to15)+" / 16+ "+pct(p.margin16plus),10,false));TextView q=tv("数据质量 "+p.dataQuality+(p.disagreementAlert?" · ⚠ "+p.disagreement:" · 分歧未触发"),10,true);q.setTextColor(p.disagreementAlert?Color.rgb(190,65,45):Color.rgb(45,105,72));box.addView(q);box.addView(tv(BasketballDivinationEngine.summary(d)+"（Shadow，不覆盖BasketballQuant）",10,false));content.addView(box);}}

    private String lockedSeed(BasketballMatch m){String k="bb_seed_"+day+"_"+(m.matchId+m.home+m.away).hashCode();String s=prefs.getString(k,"");if(s.isEmpty()){s=Long.toHexString(new SecureRandom().nextLong());prefs.edit().putString(k,s).apply();}return s;}
    private void skillInfo(){new AlertDialog.Builder(this).setTitle("FootballQuant V8.0 / Fusion V4").setMessage("正式层：欧赔去水、Poisson、Dixon-Coles、ZeroGoal、BTTS、Margin、Top比分、12BET/百家盘口、模型分歧。\n\nV4多重合参Shadow：手动三位数+冻结系统三位数、梅花V1、太玄81态、五行数、太玄干支数、十二律状态；Random-Control独立保存。传统层正式概率权重=0，必须经前瞻验证才能晋级。\n\n大小单双：由正式大小球与统一比分矩阵单双共同生成，避免内部矛盾。\n\n半全场：独立时间联合模型，上半场进球占比暂冻结45%，后续按联赛校准。\n\n审计：同场手动数/系统种子冻结；BASE不可覆盖；深度结果写DEEP；第三方赔率不冒充官方竞彩SP。30/50场只看结构，100场才讨论权重，500-1000场再做稳定性判断。概率研究，不保证收益。").setPositiveButton("关闭",null).show();}
    private LinearLayout cardBox(){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(10),dp(7),dp(10),dp(7));b.setBackgroundColor(Color.WHITE);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(6));b.setLayoutParams(lp);return b;}
    private static String clean(String s){if(s==null)return "";return s.replace("�","").replaceAll("\\s+"," ").trim();}
    private void error(String s){status.setText(s);content.addView(tv(s,12,false));}private void setActive(){jc.setEnabled(!"JC".equals(mode));bj.setEnabled(!"BJ".equals(mode));gf.setEnabled(!"GF".equals(mode));bb.setEnabled(!"BB".equals(mode));}private static String modeName(String m){return "JC".equals(m)?"竞彩足球":"BJ".equals(m)?"北京单场":"GF".equals(m)?"全球足球":"篮球";}private static String shift(String d,int n){try{java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US);java.util.Calendar c=java.util.Calendar.getInstance();c.setTime(f.parse(d));c.add(java.util.Calendar.DAY_OF_MONTH,n);return f.format(c.getTime());}catch(Exception e){return HistoryStore.today();}}private TextView tv(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.rgb(42,47,49));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(1),0,dp(1));return t;}private Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextSize(10);b.setAllCaps(false);b.setMinHeight(dp(42));return b;}private LinearLayout.LayoutParams wgt(){return new LinearLayout.LayoutParams(0,-2,1);}private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}private static String pct(double x){return String.format(Locale.US,"%.0f%%",x*100);}private static String f1(double x){return String.format(Locale.US,"%.1f",x);}private static String signed(double x){return String.format(Locale.US,"%+.1f",x);}private static String join(String[]a){StringBuilder b=new StringBuilder();for(int i=0;i<a.length;i++){if(i>0)b.append(" / ");b.append(a[i]);}return b.toString();}
}
