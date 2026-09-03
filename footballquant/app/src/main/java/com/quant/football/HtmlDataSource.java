package com.quant.football;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlDataSource {
    public static final String JC_URL="https://trade.500.com/jczq/?date=%s";
    public static final String BJ_URL="https://trade.500.com/bjdc/";

    public String get(String url,String referer) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(10000);c.setReadTimeout(12000);
        c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 16) FootballQuant/7.1");
        c.setRequestProperty("Accept","text/html,application/json;q=0.9,*/*;q=0.8");
        if(referer!=null)c.setRequestProperty("Referer",referer);
        InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream();
        byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);in.close();
        byte[] raw=out.toByteArray();
        Charset cs=detectCharset(c.getContentType(),raw,url);
        String s=new String(raw,cs);
        // 500 系页面历史上存在 GBK/GB2312/GB18030；若服务器未声明或声明错误，UTF-8 解码会出现 �。
        if(s.indexOf('\uFFFD')>=0 && url.contains("500.com")){
            try{s=new String(raw,Charset.forName("GB18030"));}catch(Exception ignored){}
        }
        return s;
    }

    private static Charset detectCharset(String contentType,byte[] raw,String url){
        String declared=charsetToken(contentType);
        if(declared!=null){try{return Charset.forName(declared);}catch(Exception ignored){}}
        // HTML meta 标签本身由 ASCII 字符组成，可以先按 ISO-8859-1 扫描前 8KB。
        int len=Math.min(raw.length,8192);
        String head=new String(raw,0,len,StandardCharsets.ISO_8859_1);
        Matcher m=Pattern.compile("(?i)charset\\s*=\\s*[\\\"']?([a-zA-Z0-9._-]+)").matcher(head);
        if(m.find()){try{return Charset.forName(m.group(1));}catch(Exception ignored){}}
        // 500.com 的旧页面通常是 GBK 系；体彩 JSON/API 默认 UTF-8。
        if(url.contains("500.com")){try{return Charset.forName("GB18030");}catch(Exception ignored){}}
        return StandardCharsets.UTF_8;
    }
    private static String charsetToken(String contentType){
        if(contentType==null)return null;
        Matcher m=Pattern.compile("(?i)charset\\s*=\\s*[\\\"']?([^;\\s\\\"']+)").matcher(contentType);
        return m.find()?m.group(1):null;
    }

    public List<MatchInfo> fetchJc(String override,String day) throws Exception {String u=(override==null||override.trim().isEmpty())?String.format(JC_URL,day):(override.contains("{date}")?override.replace("{date}",day):override);List<MatchInfo> x=parse500(get(u,"https://trade.500.com/"),"JC",day);enrichSporttery(x);return x;}
    public List<MatchInfo> fetchBj(String override,String day) throws Exception {String u=(override==null||override.trim().isEmpty())?BJ_URL:(override.contains("{date}")?override.replace("{date}",day):override);return parse500(get(u,"https://trade.500.com/"),"BJ",day);}
    public List<MatchInfo> fetchResults(boolean bj,String day) throws Exception {return bj?fetchBj("",day):fetchJc("",day);}
    private List<MatchInfo> parse500(String h,String src,String day){List<MatchInfo> out=new ArrayList<>();Pattern row=Pattern.compile("(?is)<tr[^>]*(?:data-fixtureid|data-id|fid)[^>]*>(.*?)</tr>");Matcher rm=row.matcher(h);int n=0;while(rm.find()&&n<200){String r=rm.group(1),t=text(r);String[] teams=teams(r);if(teams[0].isEmpty()||teams[1].isEmpty())continue;MatchInfo m=new MatchInfo();m.source=src;m.code=find(r,"(?i)(?:data-fixtureid|data-id|fid)=[^0-9]*([0-9]+)");m.matchId=m.code;m.matchDate=day;m.kickoff=find(t,"(\\d{1,2}:\\d{2})");m.league=findText(r,"(?:league|league-name)[^>]*>([^<]+)");m.home=teams[0];m.away=teams[1];double[] o=odds(t);if(o.length>=3){m.oddsHome=o[0];m.oddsDraw=o[1];m.oddsAway=o[2];}out.add(m);n++;}return out;}
    private void enrichSporttery(List<MatchInfo> ms){try{String raw=get("https://webapi.sporttery.cn/gateway/jc/football/getMatchCalculatorV1.qry?poolCode=had,hhad,crs,ttg,hafu&channel=c","https://www.sporttery.cn/");JSONObject root=new JSONObject(raw);JSONArray list=findArray(root);if(list==null)return;for(int i=0;i<list.length();i++){JSONObject x=list.optJSONObject(i);if(x==null)continue;String home=x.optString("homeTeamName"),away=x.optString("awayTeamName");for(MatchInfo m:ms)if(sim(m.home,home)&&sim(m.away,away)){JSONArray sp=x.optJSONArray("ttg");if(sp!=null)for(int k=0;k<Math.min(8,sp.length());k++)m.ttgSp[k]=sp.optDouble(k,0);break;}}}catch(Exception ignored){}}
    private static JSONArray findArray(JSONObject o){for(String k:new String[]{"value","matchInfoList","matchList","data"}){Object v=o.opt(k);if(v instanceof JSONArray)return(JSONArray)v;if(v instanceof JSONObject){JSONArray a=findArray((JSONObject)v);if(a!=null)return a;}}return null;}
    private static String[] teams(String r){List<String>x=new ArrayList<>();Matcher m=Pattern.compile("(?is)<a[^>]*>([^<]{2,40})</a>").matcher(r);while(m.find()){String s=text(m.group(1));if(!s.isEmpty()&&!s.matches(".*\\d.*")&&!x.contains(s))x.add(s);}return new String[]{x.size()>0?x.get(Math.max(0,x.size()-2)):"",x.size()>1?x.get(x.size()-1):""};}
    private static double[] odds(String t){Matcher m=Pattern.compile("(?<!\\d)([1-9]\\d?\\.\\d{2})(?!\\d)").matcher(t);double[] a=new double[3];int n=0;while(m.find()&&n<3)try{a[n++]=Double.parseDouble(m.group(1));}catch(Exception ignored){}if(n<3)return new double[0];return a;}
    private static String find(String s,String p){Matcher m=Pattern.compile(p).matcher(s);return m.find()?m.group(1):"";}private static String findText(String s,String p){Matcher m=Pattern.compile("(?is)"+p).matcher(s);return m.find()?text(m.group(1)):"";}
    private static String text(String s){return s==null?"":s.replaceAll("(?is)<[^>]+>"," ").replace("&nbsp;"," ").replace("&amp;","&").replaceAll("\\s+"," ").trim();}
    private static boolean sim(String a,String b){if(a==null||b==null)return false;String x=a.replaceAll("\\s+",""),y=b.replaceAll("\\s+","");return !x.isEmpty()&&!y.isEmpty()&&(x.contains(y)||y.contains(x));}
}
