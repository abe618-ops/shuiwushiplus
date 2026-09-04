package com.abe.oddsboard;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String LIVE_URL = "https://m.titan007.com/";
    private static final String FIXTURE_URL = "https://m.titan007.com/fixture/entrance.htm";
    private static final long AUTO_REFRESH_MS = 60_000L;

    private WebView web;
    private ProgressBar progress;
    private TextView liveTab, scheduleTab, pageTitle, statusText;
    private boolean liveMode = true;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable autoRefresh = new Runnable() {
        @Override public void run() {
            if (liveMode && web != null && !isFinishing()) {
                statusText.setText("自动刷新中");
                web.reload();
                handler.postDelayed(this, AUTO_REFRESH_MS);
            }
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        configureWebView();
        loadLive();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(8), dp(12), dp(4));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        pageTitle = new TextView(this);
        pageTitle.setText("百家赔率");
        pageTitle.setTextSize(24);
        pageTitle.setTextColor(Color.rgb(18,18,18));
        pageTitle.setTypeface(null, 1);
        statusText = new TextView(this);
        statusText.setText("实时比分 · 点击比赛查看赔率");
        statusText.setTextSize(11);
        statusText.setTextColor(Color.rgb(125,125,125));
        titleBox.addView(pageTitle);
        titleBox.addView(statusText);
        header.addView(titleBox, new LinearLayout.LayoutParams(0, dp(58), 1));

        TextView refresh = action("刷新");
        refresh.setOnClickListener(v -> {
            statusText.setText("手动刷新中");
            web.reload();
        });
        header.addView(refresh, new LinearLayout.LayoutParams(dp(64), dp(48)));
        root.addView(header);

        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(240,240,240));
        root.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(dp(40), 0, dp(40), 0);
        liveTab = tab("即时");
        scheduleTab = tab("赛程");
        tabs.addView(liveTab);
        tabs.addView(scheduleTab);
        liveTab.setOnClickListener(v -> loadLive());
        scheduleTab.setOnClickListener(v -> loadSchedule());
        root.addView(tabs);
        selectTab(true);

        FrameLayout content = new FrameLayout(this);
        web = new WebView(this);
        web.setBackgroundColor(Color.WHITE);
        content.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3));
        pp.gravity = Gravity.TOP;
        content.addView(progress, pp);
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }

    private TextView tab(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(17);
        v.setGravity(Gravity.CENTER);
        v.setPadding(8, 8, 8, 8);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, dp(44), 1));
        return v;
    }

    private TextView action(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(14);
        v.setGravity(Gravity.CENTER);
        v.setTextColor(Color.rgb(27,126,202));
        return v;
    }

    private void selectTab(boolean live) {
        liveMode = live;
        liveTab.setTextColor(live ? Color.rgb(26,126,204) : Color.rgb(105,105,105));
        scheduleTab.setTextColor(live ? Color.rgb(105,105,105) : Color.rgb(26,126,204));
        liveTab.setTypeface(null, live ? 1 : 0);
        scheduleTab.setTypeface(null, live ? 0 : 1);
    }

    private void loadLive() {
        selectTab(true);
        pageTitle.setText("百家赔率");
        statusText.setText("实时比分 · 点击比赛查看赔率");
        web.loadUrl(LIVE_URL);
        handler.removeCallbacks(autoRefresh);
        handler.postDelayed(autoRefresh, AUTO_REFRESH_MS);
    }

    private void loadSchedule() {
        selectTab(false);
        pageTitle.setText("今日赛程");
        statusText.setText("按开赛时间浏览 · 点击比赛查看赔率");
        handler.removeCallbacks(autoRefresh);
        web.loadUrl(FIXTURE_URL);
    }

    private void configureWebView() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setUserAgentString(s.getUserAgentString() + " OddsBoardLite/0.2");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
        });

        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String u = request.getUrl().toString();
                if (isAllowed(u)) {
                    view.loadUrl(u);
                    return true;
                }
                Toast.makeText(MainActivity.this, "已拦截无关外部页面", Toast.LENGTH_SHORT).show();
                return true;
            }

            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                statusText.setText(isDetailUrl(url) ? "赔率详情 · 初赔 / 即时 / 让球 / 大小 / 凯利" :
                        (liveMode ? "实时比分 · 60秒自动刷新" : "今日赛程 · 点击比赛查看赔率"));
                handler.postDelayed(() -> simplifyPage(view), 350);
            }
        });
    }

    private boolean isAllowed(String u) {
        return u != null && (u.contains("titan007.com") || u.contains("nowodds.com"));
    }

    private boolean isDetailUrl(String u) {
        if (u == null) return false;
        String x = u.toLowerCase();
        return x.contains("analysis") || x.contains("odds") || x.contains("asian") || x.contains("overdown") || x.contains("match");
    }

    private void simplifyPage(WebView v) {
        String js = "(function(){"+
                "var st=document.getElementById('oddsboard-css');"+
                "if(!st){st=document.createElement('style');st.id='oddsboard-css';document.head.appendChild(st);}"+
                "st.innerHTML='html,body{background:#fff!important;color:#202020!important;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif!important;}header,footer,.footer,.bottom,.bottom-nav,.download,.download-app,.ad,.ads,.advertisement,.app-download,[class*=\\\"banner\\\"],[class*=\\\"popup\\\"]{display:none!important;}a{text-decoration:none!important;}table{width:100%!important;}td,th{font-size:14px!important;}body{padding-bottom:0!important;margin-bottom:0!important;}';"+
                "var bad=['社区','V计划','资讯','我的','AI预测','计算器','会员'];"+
                "document.querySelectorAll('a,button,li,div').forEach(function(e){var t=(e.innerText||'').trim();if(t.length<10&&bad.indexOf(t)>=0){e.style.display='none';}});"+
                "document.querySelectorAll('img').forEach(function(i){var a=(i.alt||'')+(i.title||'');if(/广告|下载|app/i.test(a)){i.style.display='none';}});"+
                "})();";
        v.evaluateJavascript(js, null);
    }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(autoRefresh);
        if (web != null) web.destroy();
        super.onDestroy();
    }

    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }
}
