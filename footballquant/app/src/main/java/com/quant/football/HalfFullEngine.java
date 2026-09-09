package com.quant.football;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

/**
 * Statistical half/full-time scenario model derived from the same expected-goal backbone.
 * V4.0 uses a frozen first-half goal share and labels this as an approximate time model.
 */
public final class HalfFullEngine {
    public static final class Result {
        public String topPick="";
        public double topProb=0;
        public String top3="";
        public String note="半全场时间模型：首半场占比固定45%，待联赛级校准";
    }

    private static final String[] LABELS={"胜/胜","胜/平","胜/负","平/胜","平/平","平/负","负/胜","负/平","负/负"};

    public Result predict(Prediction p){
        Result r=new Result();
        if(p==null||p.lambdaHome<0||p.lambdaAway<0){r.topPick="数据不足";return r;}
        double share=FusionV4Config.FIRST_HALF_GOAL_SHARE;
        double h1=p.lambdaHome*share,a1=p.lambdaAway*share;
        double h2=p.lambdaHome*(1-share),a2=p.lambdaAway*(1-share);
        double[] probs=new double[9];double mass=0;
        for(int ih=0;ih<=5;ih++)for(int ia=0;ia<=5;ia++){
            double ph=poisson(ih,h1)*poisson(ia,a1);
            int half=outcome(ih,ia);
            for(int jh=0;jh<=6;jh++)for(int ja=0;ja<=6;ja++){
                double q=ph*poisson(jh,h2)*poisson(ja,a2);
                int full=outcome(ih+jh,ia+ja);
                probs[index(half,full)]+=q;mass+=q;
            }
        }
        if(mass>0)for(int i=0;i<probs.length;i++)probs[i]/=mass;
        Integer[] idx=new Integer[9];for(int i=0;i<9;i++)idx[i]=i;
        Arrays.sort(idx, Comparator.comparingDouble((Integer i)->probs[i]).reversed());
        r.topPick=LABELS[idx[0]];r.topProb=probs[idx[0]];
        StringBuilder b=new StringBuilder();
        for(int k=0;k<3;k++){
            if(k>0)b.append(" / ");int i=idx[k];
            b.append(LABELS[i]).append(' ').append(String.format(Locale.US,"%.0f%%",probs[i]*100));
        }
        r.top3=b.toString();return r;
    }

    private static int outcome(int h,int a){return h>a?0:h==a?1:2;}
    private static int index(int half,int full){return half*3+full;}
    private static double poisson(int k,double l){double f=1;for(int i=2;i<=k;i++)f*=i;return Math.exp(-l)*Math.pow(l,k)/f;}
}
