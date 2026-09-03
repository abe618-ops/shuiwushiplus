package com.quant.football;

public class ExternalModelData {
    public double home=0,draw=0,away=0;
    public double over25=0,btts=0;
    public double eloHome=0,eloAway=0,xgHome=0,xgAway=0;
    public String model="",note="";
    public boolean validProb(){return home>0&&draw>0&&away>0&&Math.abs(home+draw+away-1.0)<0.10;}
    public boolean validOver(){return over25>0.02&&over25<0.98;}
    public boolean validXg(){return xgHome>0.05&&xgAway>0.05&&xgHome+xgAway<8.0;}
}
