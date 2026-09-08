package com.mingyang.webtovideo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

public class MainActivity extends ComponentActivity {
    private WebView webView;
    private LinearLayout controls;
    private EditText urlBox;
    private MediaProjectionManager projectionManager;

    private final ActivityResultLauncher<String> audioPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> requestProjection());

    private final ActivityResultLauncher<Intent> projectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                    Toast.makeText(this, "未获得录屏授权", Toast.LENGTH_SHORT).show();
                    return;
                }
                controls.setVisibility(View.GONE);
                enterImmersive();
                Intent service = new Intent(this, RecordService.class);
                service.putExtra("resultCode", result.getResultCode());
                service.putExtra("resultData", result.getData());
                ContextCompat.startForegroundService(this, service);
                Toast.makeText(this, "正在录制；下拉通知栏可停止并保存 MP4", Toast.LENGTH_LONG).show();
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xff101010);

        controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(18, 14, 18, 14);

        urlBox = new EditText(this);
        urlBox.setSingleLine(true);
        urlBox.setText("https://scrimba.com/explain/guide045kj2cvc?claim=889bn6863aq60brh&fullscreen=1");
        urlBox.setHint("粘贴 Scrimba 或普通网页链接");
        controls.addView(urlBox, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        Button open = new Button(this);
        open.setText("打开网页");
        open.setOnClickListener(v -> loadUrl());
        Button record = new Button(this);
        record.setText("开始录制 MP4");
        record.setOnClickListener(v -> beginRecording());
        row.addView(open, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(record, new LinearLayout.LayoutParams(0, -2, 1));
        controls.addView(row, new LinearLayout.LayoutParams(-1, -2));
        root.addView(controls, new LinearLayout.LayoutParams(-1, -2));

        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        loadUrl();
    }

    private void loadUrl() {
        String u = urlBox.getText().toString().trim();
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://" + u;
        webView.loadUrl(u);
    }

    private void beginRecording() {
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 41);
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            audioPermission.launch(Manifest.permission.RECORD_AUDIO);
        } else requestProjection();
    }

    private void requestProjection() {
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent());
    }

    private void enterImmersive() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
