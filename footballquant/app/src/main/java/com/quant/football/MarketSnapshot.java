package com.quant.football;

import java.util.ArrayList;
import java.util.List;

public class MarketSnapshot {
    public final List<BookmakerOdds> bookmakers=new ArrayList<>();
    public String asianSummary="",totalSummary="",sourceNote="";
    public double totalLine=0,asianAbsLine=0;
    public ExternalModelData external=null;

    public int independentCount(){int n=0;for(BookmakerOdds b:bookmakers)if(b.validNow())n++;return n;}
    public double[] consensusNow(){return consensus(false);}
    public double[] consensusOpen(){return consensus(true);}
    private double[] consensus(boolean open){
        double h=0,d=0,a=0;int n=0;
        for(BookmakerOdds b:bookmakers){boolean ok=open?b.validOpen():b.validNow();if(!ok)continue;double[]p=open?b.devigOpen():b.devigNow();h+=p[0];d+=p[1];a+=p[2];n++;}
        if(n==0)return new double[]{0,0,0};return new double[]{h/n,d/n,a/n};
    }
    public double[] movement(){double[]o=consensusOpen(),n=consensusNow();if(o[0]<=0||n[0]<=0)return new double[]{0,0,0};return new double[]{n[0]-o[0],n[1]-o[1],n[2]-o[2]};}
}
