package com.quant.football;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Reproducible number features for the V4 shadow layer.
 * Traditional tables are kept separate from the modern football mapping.
 */
public final class TaiXuanNumberEngine {
    private TaiXuanNumberEngine() {}

    private static final String[] STEMS={"甲","乙","丙","丁","戊","己","庚","辛","壬","癸"};
    private static final String[] BRANCHES={"子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥"};
    private static final int[] STEM_NUM={9,8,7,6,5,9,8,7,6,5};
    private static final int[] BRANCH_NUM={9,8,7,6,5,4,9,8,7,6,5,4};
    private static final String[] LUE={"黄钟","大吕","太簇","夹钟","姑洗","仲吕","蕤宾","林钟","夷则","南吕","无射","应钟"};

    public static final class Feature {
        public final String number;
        public final int taiXuanState;
        public final int[] trits;
        public final String fiveElements;
        public final double fiveElementBias;
        public final double taiXuanBias;
        public final String dayStemBranch;
        public final int stemNumber;
        public final int branchNumber;
        public final String zhongLu;
        public final double stemBranchBias;
        public final boolean zeroUsesExperimentalEarth;

        Feature(String number,int state,int[] trits,String fiveElements,double fiveElementBias,
                double taiXuanBias,String dayStemBranch,int stemNumber,int branchNumber,String zhongLu,
                double stemBranchBias,boolean zeroUsesExperimentalEarth){
            this.number=number;this.taiXuanState=state;this.trits=trits;this.fiveElements=fiveElements;
            this.fiveElementBias=fiveElementBias;this.taiXuanBias=taiXuanBias;this.dayStemBranch=dayStemBranch;
            this.stemNumber=stemNumber;this.branchNumber=branchNumber;this.zhongLu=zhongLu;
            this.stemBranchBias=stemBranchBias;this.zeroUsesExperimentalEarth=zeroUsesExperimentalEarth;
        }

        public String summary(){
            return number+"｜太玄"+taiXuanState+"("+trits[0]+trits[1]+trits[2]+trits[3]+")｜五行"+fiveElements+
                    "｜日"+dayStemBranch+" "+stemNumber+"/"+branchNumber+"｜律"+zhongLu+
                    (zeroUsesExperimentalEarth?"｜0按土*实验":"");
        }
    }

    public static Feature analyze(String raw,String day){
        String number=DualNumberFreezeEngine.normalizeManualNumber(raw);
        int value=Integer.parseInt(number);
        int state=(value%81)+1;
        int x=state-1;
        int[] trits=new int[4];
        for(int i=3;i>=0;i--){trits[i]=(x%3)+1;x/=3;}
        double tx=0;
        for(int t:trits)tx+=(t-2);
        tx/=4.0;

        StringBuilder els=new StringBuilder();double eb=0;boolean zero=false;
        for(int i=0;i<3;i++){
            int d=number.charAt(i)-'0';
            if(i>0)els.append('-');
            String e=element(d);els.append(e);
            eb+=elementBias(d);
            if(d==0)zero=true;
        }
        eb/=3.0;

        DayFeature df=dayFeature(day);
        double sb=(df.stemNumber==0||df.branchNumber==0)?0:((df.stemNumber+df.branchNumber)-13.0)/5.0;
        if(sb>1)sb=1;if(sb<-1)sb=-1;
        return new Feature(number,state,trits,els.toString(),eb,tx,df.stem+df.branch,df.stemNumber,df.branchNumber,df.lu,sb,zero);
    }

    private static String element(int d){
        if(d==1||d==6)return "水";
        if(d==2||d==7)return "火";
        if(d==3||d==8)return "木";
        if(d==4||d==9)return "金";
        if(d==5)return "土";
        return "土*"; // zero has no classical entry in the 1-9 table; this is an explicit experiment.
    }

    private static double elementBias(int d){
        if(d==2||d==7)return .60;   // fire: release/open experiment
        if(d==3||d==8)return .30;   // wood: penetration experiment
        if(d==4||d==9)return .08;   // metal: near neutral
        if(d==1||d==6)return -.50;  // water: inhibition experiment
        return -.35;                // earth/0*: containment experiment
    }

    private static final class DayFeature{
        final String stem,branch,lu; final int stemNumber,branchNumber;
        DayFeature(String stem,String branch,int stemNumber,int branchNumber,String lu){
            this.stem=stem;this.branch=branch;this.stemNumber=stemNumber;this.branchNumber=branchNumber;this.lu=lu;
        }
    }

    private static DayFeature dayFeature(String day){
        try{
            SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setLenient(false);f.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d=f.parse(day);Date epoch=f.parse("2000-01-07"); // verified Jia-Zi day anchor
            long diff=(d.getTime()-epoch.getTime())/86400000L;
            int cycle=mod(diff,60);int si=cycle%10,bi=cycle%12;
            return new DayFeature(STEMS[si],BRANCHES[bi],STEM_NUM[si],BRANCH_NUM[bi],LUE[bi]);
        }catch(Exception e){
            return new DayFeature("?","?",0,0,"未解析");
        }
    }

    private static int mod(long x,int m){long r=x%m;return (int)(r<0?r+m:r);}
}
