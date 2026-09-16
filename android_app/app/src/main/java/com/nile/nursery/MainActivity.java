package com.nile.nursery;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    // 🌟 رابط موقع الحضانة المباشر على الاستضافة
    private static final String DEFAULT_URL = "https://nilenursery.great-site.net/";
    private static final String WHATSAPP_NUMBER = "201000000000";

    private WebView webView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private RelativeLayout offlineContainer;
    private ImageButton btnBack;
    private ImageButton btnRefresh;
    private Button btnRetryOffline;
    private Button btnWhatsappOffline;

    private ValueCallback<Uri[]> fileUploadCallback;
    private final static int FILE_CHOOSER_REQUEST_CODE = 1001;

    private boolean isOfflineState = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize Views
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        offlineContainer = findViewById(R.id.offlineContainer);
        btnBack = findViewById(R.id.btnBack);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnRetryOffline = findViewById(R.id.btnRetryOffline);
        btnWhatsappOffline = findViewById(R.id.btnWhatsappOffline);

        // 2. Setup Fixed Buttons Actions
        // زر الرجوع الثابت
        btnBack.setOnClickListener(v -> handleBackAction());

        // زر التحديث الثابت
        btnRefresh.setOnClickListener(v -> reloadCurrentPage());

        // زر إعادة المحاولة في شاشة الصيانة
        btnRetryOffline.setOnClickListener(v -> reloadCurrentPage());

        // زر التواصل عبر واتساب في شاشة الصيانة
        btnWhatsappOffline.setOnClickListener(v -> openWhatsApp());

        // سحب لأسفل للتحديث (Pull to Refresh)
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(this::reloadCurrentPage);

        // 3. Configure WebView & Cache & Cookies
        setupWebView();

        // 4. Initial Load
        loadTargetUrl(DEFAULT_URL);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);

        // Session & Cookie Persistence (حفظ جلسة ولي الأمر والمعلمات)
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // Cache Configuration
        if (isNetworkAvailable()) {
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        } else {
            settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }

        // Custom WebViewClient with Maintenance & Offline Interception
        webView.setWebViewClient(new CustomWebViewClient());

        // Custom WebChromeClient for Progress & File Uploads (Camera / Photos)
        webView.setWebChromeClient(new CustomWebChromeClient());
    }

    private void loadTargetUrl(String url) {
        if (!isNetworkAvailable()) {
            showOfflineScreen();
            return;
        }

        hideOfflineScreen();
        webView.loadUrl(url);
    }

    private void reloadCurrentPage() {
        if (!isNetworkAvailable()) {
            showOfflineScreen();
            swipeRefresh.setRefreshing(false);
            return;
        }

        hideOfflineScreen();
        if (webView.getUrl() != null && !isOfflineState) {
            webView.reload();
        } else {
            webView.loadUrl(DEFAULT_URL);
        }
    }

    private void showOfflineScreen() {
        isOfflineState = true;
        offlineContainer.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
    }

    private void hideOfflineScreen() {
        isOfflineState = false;
        offlineContainer.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
    }

    private void handleBackAction() {
        if (isOfflineState) {
            reloadCurrentPage();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }

    private void openWhatsApp() {
        try {
            String url = "https://api.whatsapp.com/send?phone=" + WHATSAPP_NUMBER +
                    "&text=" + Uri.encode("السلام عليكم.. أستفسر بخصوص حضانة النيل");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "تطبيق واتساب غير مثبت على جهازك", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────
    // Custom WebViewClient
    // ─────────────────────────────────────────────────────────────
    private class CustomWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(10);
            hideOfflineScreen();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
        }

        // Catch connection errors on modern Android (API 23+)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame()) {
                showOfflineScreen();
            }
        }

        // Legacy error handler
        @SuppressWarnings("deprecation")
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            showOfflineScreen();
        }

        // Catch HTTP 500, 502, 503, 504 server errors (تحت الصيانة)
        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
            if (request.isForMainFrame() && errorResponse != null) {
                int status = errorResponse.getStatusCode();
                if (status >= 500) {
                    showOfflineScreen();
                }
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            return handleExternalUrls(url);
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return handleExternalUrls(url);
        }

        private boolean handleExternalUrls(String url) {
            // Handle tel:, mailto:, whatsapp: links in external apps
            if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("whatsapp:") || url.startsWith("https://wa.me/")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Custom WebChromeClient
    // ─────────────────────────────────────────────────────────────
    private class CustomWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            progressBar.setProgress(newProgress);
            if (newProgress >= 100) {
                progressBar.setVisibility(View.GONE);
            }
        }

        // Support for File upload / Camera photos
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if (fileUploadCallback != null) {
                fileUploadCallback.onReceiveValue(null);
            }
            fileUploadCallback = filePathCallback;

            Intent intent = fileChooserParams.createIntent();
            try {
                startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                fileUploadCallback = null;
                Toast.makeText(MainActivity.this, "تعذر فتح المعرض أو الكاميرا", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (fileUploadCallback != null) {
                Uri[] results = null;
                if (resultCode == RESULT_OK && data != null) {
                    if (data.getData() != null) {
                        results = new Uri[]{data.getData()};
                    } else if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    }
                }
                fileUploadCallback.onReceiveValue(results);
                fileUploadCallback = null;
            }
        }
    }

    @Override
    public void onBackPressed() {
        handleBackAction();
    }
}
