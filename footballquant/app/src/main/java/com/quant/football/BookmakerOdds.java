package com.quant.football;

public class BookmakerOdds {
    public String company="";
    public double openHome=0,openDraw=0,openAway=0;
    public double nowHome=0,nowDraw=0,nowAway=0;
    public boolean validOpen(){return valid(openHome,openDraw,openAway);}
    public boolean validNow(){return valid(nowHome,nowDraw,nowAway);}
    private static boolean valid(double h,double d,double a){return h>1.01&&d>1.01&&a>1.01;}
    public double[] devigNow(){return devig(nowHome,nowDraw,nowAway);}
    public double[] devigOpen(){return devig(openHome,openDraw,openAway);}
    private static double[] devig(double h,double d,double a){
        if(!valid(h,d,a))return new double[]{0,0,0};
        double x=1.0/h,y=1.0/d,z=1.0/a,s=x+y+z;
        return new double[]{x/s,y/s,z/s};
    }
}
