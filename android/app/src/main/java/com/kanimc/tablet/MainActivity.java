package com.kanimc.tablet;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "KANIMC_Tablet";
    private WebView webView;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on during accounting work on tablet
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);

        // Tablet-optimized viewing
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // Offline-first caching
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // Bridge for native Android tablet features
        webView.addJavascriptInterface(new TabletAppBridge(this), "AndroidTablet");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("KANIMC")
                        .setMessage(message)
                        .setPositiveButton("OK", (dialog, which) -> result.confirm())
                        .setCancelable(false)
                        .create()
                        .show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("KANIMC")
                        .setMessage(message)
                        .setPositiveButton("Yes", (dialog, which) -> result.confirm())
                        .setNegativeButton("No", (dialog, which) -> result.cancel())
                        .setCancelable(false)
                        .create()
                        .show();
                return true;
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d(TAG, "[WebView Console] " + consoleMessage.message() + " -- From line "
                        + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false; // let WebView handle sync/API endpoints
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.i(TAG, "Loaded page: " + url);
            }
        });

        // Handle CSV / Excel downloads directly on Tablet
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                try {
                    if (url.startsWith("data:")) {
                        // Data URI base64 download
                        String[] parts = url.split(",");
                        if (parts.length > 1) {
                            byte[] data = Base64.decode(parts[1], Base64.DEFAULT);
                            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                            String fileName = "KANIMC_Export_" + System.currentTimeMillis() + ".csv";
                            File targetFile = new File(downloadDir, fileName);
                            FileOutputStream fos = new FileOutputStream(targetFile);
                            fos.write(data);
                            fos.close();
                            Toast.makeText(MainActivity.this, "Exported: " + fileName + " in Downloads", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Download error", e);
                    Toast.makeText(MainActivity.this, "Export complete", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Load offline-first local bundle
        webView.loadUrl("file:///android_asset/www/index.html");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Check if any modal is currently visible in the Web application
            webView.evaluateJavascript("(function(){ const m = document.querySelector('.modal-overlay[style*=\"display: flex\"], .modal-overlay[style*=\"display: block\"], .modal-overlay:not([style*=\"display: none\"]):not(#loginOverlay)'); if(m && m.style.display !== 'none'){ closeAll(); return 'closed'; } return 'none'; })()", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    if (value == null || !value.contains("closed")) {
                        if (webView.canGoBack()) {
                            webView.goBack();
                        } else {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("KANIMC")
                                    .setMessage("Do you want to exit the application?")
                                    .setPositiveButton("Exit", (dialog, which) -> finish())
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        }
                    }
                }
            });
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public class TabletAppBridge {
        private final Context context;

        public TabletAppBridge(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void printDocument(String jobName) {
            runOnUiThread(() -> {
                try {
                    PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                    if (printManager != null) {
                        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName != null ? jobName : "KANIMC");
                        printManager.print(jobName != null ? jobName : "KANIMC", printAdapter, new PrintAttributes.Builder().build());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Print error", e);
                    Toast.makeText(context, "Printing not available on this device", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void showToast(String message) {
            runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
        }
    }
}
