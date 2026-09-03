package com.quant.football;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class Predictor {
    private static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
    public Prediction predict(MatchInfo m){return predict(m,null);}

    public Prediction predict(MatchInfo m,MarketSnapshot snap){
        double structuralH=.445,structuralD=.285,structuralA=.270;
        if(m.homeRank>0&&m.awayRank>0){double adj=clamp((m.awayRank-m.homeRank)*.006,-.105,.105);structuralH+=adj;structuralA-=adj;}
        boolean cup=containsAny(m.league,"杯","欧冠","欧罗巴","欧联","欧协","资格","附加赛");
        if(cup){structuralD+=.012;structuralH-=.006;structuralA-=.006;}
        if(m.secondLeg&&m.aggregateLeadHome!=0){if(m.aggregateLeadHome>0){structuralH-=.010;structuralD+=.010;}else{structuralA-=.010;structuralD+=.010;}}
        Holder st=normalize(structuralH,structuralD,structuralA);structuralH=st.h;structuralD=st.d;structuralA=st.a;

        boolean hasOdds=validOdds(m.oddsHome,m.oddsDraw,m.oddsAway);double h,d,a;
        if(hasOdds){double[] q=devig(m.oddsHome,m.oddsDraw,m.oddsAway);h=.80*q[0]+.20*structuralH;d=.80*q[1]+.20*structuralD;a=.80*q[2]+.20*structuralA;}
        else{h=structuralH;d=structuralD;a=structuralA;}
        Holder n=normalize(h,d,a);h=n.h;d=n.d;a=n.a;

        Prediction p=new Prediction();p.marketBacked=hasOdds;
        if(snap!=null){
            p.bookmakerCount=snap.independentCount();
            double[] mc=snap.consensusNow();
            if(p.bookmakerCount>=2&&mc[0]>0){
                double delta=maxAbs(h-mc[0],d-mc[1],a-mc[2]);boolean topDiff=argmax(new double[]{h,d,a})!=argmax(mc);
                if(topDiff||delta>=.10){p.disagreementAlert=true;p.disagreement="基线/百家分歧 "+String.format(Locale.US,"%.0fpp",delta*100);}
                h=.65*h+.35*mc[0];d=.65*d+.35*mc[1];a=.65*a+.35*mc[2];n=normalize(h,d,a);h=n.h;d=n.d;a=n.a;
            }
            if(snap.external!=null&&snap.external.validProb()){
                ExternalModelData x=snap.external;double delta=maxAbs(h-x.home,d-x.draw,a-x.away);boolean topDiff=argmax(new double[]{h,d,a})!=argmax(new double[]{x.home,x.draw,x.away});
                if(topDiff||delta>=.10){p.disagreementAlert=true;p.disagreement=(p.disagreementAlert&&!"无明显分歧".equals(p.disagreement)?p.disagreement+"；":"")+"外部模型分歧 "+String.format(Locale.US,"%.0fpp",delta*100);}
                h=.85*h+.15*x.home;d=.85*d+.15*x.draw;a=.85*a+.15*x.away;n=normalize(h,d,a);h=n.h;d=n.d;a=n.a;
            }
        }
        p.home=h;p.draw=d;p.away=a;

        double totalMean=2.52;if(cup)totalMean-=.08;if(Math.max(h,a)>.62)totalMean+=.22;if(Math.max(h,a)>.72)totalMean+=.14;
        if(m.secondLeg&&m.aggregateLeadHome!=0){int lead=Math.abs(m.aggregateLeadHome);totalMean+=(lead>=2?-.08:.03);p.cupState="次回合领先"+lead+"球：Game-State Challenger低权重修正";}
        if(containsAny(m.league,"巴西杯","阿甲","阿根廷"))totalMean-=.18;
        if(snap!=null&&snap.totalLine>0)totalMean=.82*totalMean+.18*clamp(snap.totalLine+.02,1.5,4.2);
        if(snap!=null&&snap.external!=null&&snap.external.validXg())totalMean=.84*totalMean+.16*(snap.external.xgHome+snap.external.xgAway);
        totalMean=clamp(totalMean,1.55,3.65);

        double lh=totalMean*clamp(.5+(h-a)*.46,.28,.74),la=totalMean-lh;
        p.lambdaHome=lh;p.lambdaAway=la;p.expectedGoals=totalMean;p.homeZero=Math.exp(-lh);p.awayZero=Math.exp(-la);p.anyZero=1-(1-p.homeZero)*(1-p.awayZero);p.bttsYes=(1-p.homeZero)*(1-p.awayZero);

        List<ScoreP> scorePs=new ArrayList<>();double over=0,homeWin1=0,homeWin2=0,homeWin3=0,awayWin1=0,awayWin2=0,awayWin3=0,mass=0;
        for(int i=0;i<=10;i++)for(int j=0;j<=10;j++){
            double prob=poisson(i,lh)*poisson(j,la)*dcTau(i,j,lh,la,-.08);if(prob<0)prob=0;mass+=prob;if(i+j>=3)over+=prob;int diff=i-j;
            if(diff==1)homeWin1+=prob;else if(diff==2)homeWin2+=prob;else if(diff>=3)homeWin3+=prob;else if(diff==-1)awayWin1+=prob;else if(diff==-2)awayWin2+=prob;else if(diff<=-3)awayWin3+=prob;
            if(i<=6&&j<=6)scorePs.add(new ScoreP(i+":"+j,prob));
        }
        if(mass>0){over/=mass;homeWin1/=mass;homeWin2/=mass;homeWin3/=mass;awayWin1/=mass;awayWin2/=mass;awayWin3/=mass;}
        p.over25=clamp(over,0,1);Collections.sort(scorePs,Comparator.comparingDouble((ScoreP x)->x.p).reversed());
        p.favoriteSide=h>=a?"主队":"客队";if(h>=a){p.favWin1=homeWin1;p.favWin2=homeWin2;p.favWin3Plus=homeWin3;}else{p.favWin1=awayWin1;p.favWin2=awayWin2;p.favWin3Plus=awayWin3;}

        String[] labels={"主胜","平","客胜"};double[] probs={h,d,a};int first=argmax(probs),second=secondMax(probs,first);double firstP=probs[first],diff=firstP-probs[second];p.primary=labels[first];p.secondary=labels[second];
        boolean structuralConflict=false;if(m.homeRank>0&&m.awayRank>0&&hasOdds){boolean marketHome=h>a,rankHome=m.homeRank<m.awayRank;structuralConflict=marketHome!=rankHome&&Math.abs(m.homeRank-m.awayRank)>=5;}
        if(structuralConflict)p.distribution="逆分布";else if(diff<.065)p.distribution="均衡分布";else if(firstP>=.63)p.distribution="强顺分布";else if(first==2&&firstP>=.43)p.distribution="客向隐强";else p.distribution="顺分布";
        if((hasOdds||p.bookmakerCount>=2)&&firstP>=.62&&diff>=.20)p.grade="A";else if(firstP>=.50&&diff>=.10)p.grade="B";else p.grade="C";
        if(structuralConflict||p.disagreementAlert)p.grade=downgrade(p.grade);
        if(p.over25>=.57)p.totalPick="大2.5";else if(p.over25<=.44)p.totalPick="小2.5";else p.totalPick="2-3球区间";
        int topN=Math.min(4,scorePs.size());p.scores=new String[topN];for(int i=0;i<topN;i++)p.scores[i]=scorePs.get(i).score;
        if(p.bookmakerCount>=3&&hasOdds)p.dataQuality="ACCEPTED";else if(hasOdds||p.bookmakerCount>=2)p.dataQuality="CONDITIONAL";else p.dataQuality="REJECTED/LOW DATA";

        StringBuilder c=new StringBuilder();if(hasOdds)c.append("当前SP去水作为市场先验；");else c.append("完整SP缺失；");
        if(p.bookmakerCount>0)c.append("百家公司独立样本").append(p.bookmakerCount).append("家；");if(m.homeRank>0&&m.awayRank>0)c.append("排名差校正；");if(cup)c.append("杯赛低比分校正；");
        if(m.secondLeg&&m.aggregateLeadHome!=0)c.append("Game-State仅低权重；");if(structuralConflict)c.append("市场/排名冲突；");if(p.disagreementAlert)c.append("ModelDisagreement触发并降级；");
        c.append("ZeroGoal、BTTS、净胜1/2/3+由同一DC比分矩阵导出；深盘不以胜率直接替代穿盘率。");p.comment=c.toString();return p;
    }

    private static String downgrade(String g){if("A".equals(g))return"B";if("B".equals(g))return"C";return g;}
    private static double[] devig(double h,double d,double a){double x=1/h,y=1/d,z=1/a,s=x+y+z;return new double[]{x/s,y/s,z/s};}
    private static boolean validOdds(double h,double d,double a){return h>1.01&&d>1.01&&a>1.01;}
    private static int argmax(double[]x){int k=0;for(int i=1;i<x.length;i++)if(x[i]>x[k])k=i;return k;}
    private static int secondMax(double[]x,int first){int k=first==0?1:0;for(int i=0;i<x.length;i++)if(i!=first&&x[i]>x[k])k=i;return k;}
    private static double maxAbs(double...x){double m=0;for(double v:x)m=Math.max(m,Math.abs(v));return m;}
    private static boolean containsAny(String s,String...keys){if(s==null)return false;for(String k:keys)if(s.contains(k))return true;return false;}
    private static double poisson(int k,double l){double f=1;for(int i=2;i<=k;i++)f*=i;return Math.exp(-l)*Math.pow(l,k)/f;}
    private static double dcTau(int h,int a,double lh,double la,double rho){if(h==0&&a==0)return 1-lh*la*rho;if(h==0&&a==1)return 1+lh*rho;if(h==1&&a==0)return 1+la*rho;if(h==1&&a==1)return 1-rho;return 1;}
    private static Holder normalize(double h,double d,double a){double s=h+d+a;return new Holder(h/s,d/s,a/s);}
    private static class Holder{final double h,d,a;Holder(double h,double d,double a){this.h=h;this.d=d;this.a=a;}}
    private static class ScoreP{final String score;final double p;ScoreP(String s,double p){score=s;this.p=p;}}
    public static String pct(double x){return String.format(Locale.US,"%.0f%%",x*100.0);}
}
