package com.kanimc.tablet;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "KANIMC_Tablet";
    private static final String VERSION_CHECK_URL = "https://raw.githubusercontent.com/Nivash005/KANIMC-WEB-APP/main/version.json";
    private static final String FALLBACK_APK_URL = "https://github.com/Nivash005/KANIMC-WEB-APP/releases/download/v1.0.0-tablet/kanimc-tablet.apk";

    private WebView webView;
    private File pendingInstallApk = null;

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

        // Perform silent background update check after 3 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> checkOnlineUpdate(true), 3500);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pendingInstallApk != null && pendingInstallApk.exists()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (getPackageManager().canRequestPackageInstalls()) {
                    File apk = pendingInstallApk;
                    pendingInstallApk = null;
                    launchInstallIntent(apk);
                }
            }
        }
    }

    // =========================================================================
    // ================= ONLINE APK AUTO-UPDATE SYSTEM =========================
    // =========================================================================

    public void checkOnlineUpdate(final boolean silent) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int localCode = getAppVersionCode();
                String localName = getAppVersionName();

                HttpURLConnection conn = getRedirectedConnection(VERSION_CHECK_URL);
                if (conn.getResponseCode() != 200) {
                    if (!silent) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Could not check for updates (server unreachable)", Toast.LENGTH_SHORT).show());
                    }
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                conn.disconnect();

                JSONObject json = new JSONObject(sb.toString());
                int remoteCode = json.optInt("versionCode", 1);
                final String remoteName = json.optString("versionName", "1.0.0");
                final String apkUrl = json.optString("apkUrl", FALLBACK_APK_URL);
                final String notes = json.optString("releaseNotes", "New performance enhancements and bug fixes.");

                if (remoteCode > localCode) {
                    // Update available! Show Dialog
                    runOnUiThread(() -> {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Update Available (v" + remoteName + ")")
                                .setMessage("A new version of KANIMC Tablet is available!\n\n"
                                        + "What's New:\n" + notes + "\n\n"
                                        + "Current: v" + localName + "  ➜  Latest: v" + remoteName)
                                .setPositiveButton("Update Now", (dialog, which) -> downloadAndInstallApk(apkUrl, remoteName))
                                .setNegativeButton("Later", null)
                                .setCancelable(false)
                                .create()
                                .show();
                    });
                } else {
                    if (!silent) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "You are using the latest version (v" + localName + ")", Toast.LENGTH_SHORT).show());
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Update check failed", e);
                if (!silent) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Update check failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    public void downloadAndInstallApk(final String apkUrl, final String versionName) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Downloading KANIMC Update (v" + versionName + ")");
        progressDialog.setMessage("Please wait while the update is downloading...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();

        Executors.newSingleThreadExecutor().execute(() -> {
            File outputFile = null;
            try {
                HttpURLConnection conn = getRedirectedConnection(apkUrl);
                int fileLength = conn.getContentLength();

                File cacheDir = getExternalCacheDir() != null ? getExternalCacheDir() : getCacheDir();
                outputFile = new File(cacheDir, "kanimc-tablet-update.apk");
                if (outputFile.exists()) outputFile.delete();

                InputStream input = conn.getInputStream();
                FileOutputStream output = new FileOutputStream(outputFile);

                byte[] buffer = new byte[8192];
                long total = 0;
                int count;
                int lastPercent = 0;

                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    output.write(buffer, 0, count);
                    if (fileLength > 0) {
                        int percent = (int) (total * 100 / fileLength);
                        if (percent != lastPercent) {
                            lastPercent = percent;
                            final int current = percent;
                            runOnUiThread(() -> progressDialog.setProgress(current));
                        }
                    }
                }

                output.flush();
                output.close();
                input.close();
                conn.disconnect();

                final File finalFile = outputFile;
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    installApk(finalFile);
                });

            } catch (Exception e) {
                Log.e(TAG, "Download APK failed", e);
                final String err = e.getMessage();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Update Download Failed")
                            .setMessage("Error downloading update: " + err + "\n\nYou can also download directly from GitHub Releases.")
                            .setPositiveButton("Open Browser", (d, w) -> {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(FALLBACK_APK_URL));
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            }
        });
    }

    private void installApk(File apkFile) {
        if (apkFile == null || !apkFile.exists()) {
            Toast.makeText(this, "Update package not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // On Android 8.0+ (Oreo), verify unknown sources install permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                pendingInstallApk = apkFile;
                new AlertDialog.Builder(this)
                        .setTitle("Permission Needed")
                        .setMessage("To install the update, please enable 'Allow from this source' for KANIMC Tablet.")
                        .setPositiveButton("Settings", (dialog, which) -> {
                            Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                            settingsIntent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(settingsIntent);
                        })
                        .setNegativeButton("Cancel", null)
                        .setCancelable(false)
                        .show();
                return;
            }
        }

        launchInstallIntent(apkFile);
    }

    private void launchInstallIntent(File apkFile) {
        try {
            if (apkFile != null) {
                apkFile.setReadable(true, false);
            }

            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apkFile);
            } else {
                apkUri = Uri.fromFile(apkFile);
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);

            List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                grantUriPermission(packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Launch install failed", e);
            Toast.makeText(this, "Could not start installer: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private HttpURLConnection getRedirectedConnection(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(20000);
        conn.setRequestProperty("User-Agent", "KANIMC-Tablet-App");
        int status = conn.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
            String newUrl = conn.getHeaderField("Location");
            conn.disconnect();
            return getRedirectedConnection(newUrl);
        }
        return conn;
    }

    private int getAppVersionCode() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return (int) pInfo.getLongVersionCode();
            } else {
                return pInfo.versionCode;
            }
        } catch (Exception e) {
            return 1;
        }
    }

    private String getAppVersionName() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName != null ? pInfo.versionName : "1.0.0";
        } catch (Exception e) {
            return "1.0.0";
        }
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
        public void checkForUpdates(final boolean silent) {
            runOnUiThread(() -> checkOnlineUpdate(silent));
        }

        @JavascriptInterface
        public void triggerUpdate(final String url, final String versionName) {
            runOnUiThread(() -> downloadAndInstallApk(url != null ? url : FALLBACK_APK_URL, versionName != null ? versionName : "Latest"));
        }

        @JavascriptInterface
        public String getAppVersion() {
            return getAppVersionName() + " (Build " + getAppVersionCode() + ")";
        }

        @JavascriptInterface
        public boolean isTabletApp() {
            return true;
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
