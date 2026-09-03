package com.quant.football;

public class DivinationResult {
    public String name="",mode="",goalSignal="",paritySignal="",detail="";
    public double over25=0.50,oddProb=0.50,confidence=0.45;
    public boolean core=false;

    public DivinationResult(String name,String mode,double over25,double oddProb,double confidence,String signal,String paritySignal,String detail,boolean core){
        this.name=name;this.mode=mode;
        this.over25=Math.max(.15,Math.min(.85,over25));
        this.oddProb=Math.max(.25,Math.min(.75,oddProb));
        this.confidence=Math.max(.35,Math.min(.70,confidence));
        this.goalSignal=signal;this.paritySignal=paritySignal;this.detail=detail;this.core=core;
    }
}
