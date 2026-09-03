package com.quant.football;

import java.util.Locale;

public class BasketballPredictor {
    public BasketballPrediction predict(BasketballMatch m){
        BasketballPrediction p=new BasketballPrediction();double h=.54;
        if(m.homeWinPct>=0&&m.awayWinPct>=0)h=clamp(.52+(m.homeWinPct-m.awayWinPct)*.62,.12,.88);
        if(m.homeMoneyline!=0&&m.awayMoneyline!=0){double mh=americanProb(m.homeMoneyline),ma=americanProb(m.awayMoneyline),s=mh+ma;if(s>0){mh/=s;double delta=Math.abs(h-mh);if((h>=.5)!=(mh>=.5)||delta>=.10){p.disagreementAlert=true;p.disagreement="基础效率/市场胜率分歧 "+String.format(Locale.US,"%.0fpp",delta*100);}h=.65*h+.35*mh;}}
        p.homeWin=h;p.awayWin=1-h;p.winner=h>=.5?"主胜":"客胜";p.expectedMargin=(h-.5)*28.0;double baseTotal=leagueTotal(m.league);p.expectedTotal=baseTotal;p.homePoints=baseTotal/2+p.expectedMargin/2;p.awayPoints=baseTotal-p.homePoints;double sdMargin=12.5;
        if(!Double.isNaN(m.marketSpread)){p.coverHome=1-normalCdf((-m.marketSpread-p.expectedMargin)/sdMargin);p.spreadPick=p.coverHome>=.54?"主队让分方向":p.coverHome<=.46?"客队受让方向":"让分PASS";double implied=-m.marketSpread;if(Math.abs(p.expectedMargin-implied)>=6){p.disagreementAlert=true;p.disagreement=("无明显分歧".equals(p.disagreement)?"":p.disagreement+"；")+"模型净胜分/盘口差 "+String.format(Locale.US,"%.1f",Math.abs(p.expectedMargin-implied));}}else p.spreadPick=(h>=.5?"主队":"客队")+"胜负方向，盘口缺失";
        if(!Double.isNaN(m.marketTotal)){p.overMarket=1-normalCdf((m.marketTotal-p.expectedTotal)/15.0);p.totalPick=p.overMarket>=.55?"大"+f1(m.marketTotal):p.overMarket<=.45?"小"+f1(m.marketTotal):"总分PASS";}else p.totalPick="模型总分 "+f1(p.expectedTotal)+"（市场线缺失）";
        double abs=Math.abs(p.expectedMargin);p.margin1to5=band(abs,1,5,sdMargin);p.margin6to10=band(abs,6,10,sdMargin);p.margin11to15=band(abs,11,15,sdMargin);p.margin16plus=1-normalCdf((15.5-abs)/sdMargin);double top=Math.max(p.homeWin,p.awayWin);if(top>=.72)p.grade="A";else if(top>=.61)p.grade="B";else p.grade="C";if(p.disagreementAlert)p.grade=downgrade(p.grade);int evidence=0;if(m.homeWinPct>=0&&m.awayWinPct>=0)evidence++;if(!Double.isNaN(m.marketSpread))evidence++;if(!Double.isNaN(m.marketTotal))evidence++;if(m.homeMoneyline!=0&&m.awayMoneyline!=0)evidence++;p.dataQuality=evidence>=3?"ACCEPTED":evidence>=1?"CONDITIONAL":"REJECTED/LOW DATA";p.comment="BasketballQuant：胜率先验 + 战绩效率差 + 市场Moneyline去水（有则启用） + Margin正态近似 + Spread Cover + Expected Total/Team Total；季前/青年/杯赛数据不足时自动降级。";return p;
    }
    private static double leagueTotal(String s){String x=s==null?"":s.toLowerCase(Locale.US);if(x.contains("nba")&&!x.contains("wnba"))return 226;if(x.contains("wnba"))return 164;if(x.contains("nbl")||x.contains("australia"))return 176;if(x.contains("cba")||x.contains("china"))return 174;return 168;}
    private static double americanProb(double x){if(x==0)return 0;return x<0?(-x)/(-x+100.0):100.0/(x+100.0);}
    private static double band(double mu,double lo,double hi,double sd){return Math.max(0,normalCdf((hi+.5-mu)/sd)-normalCdf((lo-.5-mu)/sd));}
    private static double normalCdf(double x){double t=1.0/(1.0+0.2316419*Math.abs(x));double d=0.3989423*Math.exp(-x*x/2);double p=d*t*(0.3193815+t*(-0.3565638+t*(1.781478+t*(-1.821256+t*1.330274))));return x>0?1-p:p;}
    private static String downgrade(String g){return "A".equals(g)?"B":"B".equals(g)?"C":g;}private static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}private static String f1(double x){return String.format(Locale.US,"%.1f",x);}
}
