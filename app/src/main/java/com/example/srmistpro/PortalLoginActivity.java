package com.example.srmistpro;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class PortalLoginActivity extends AppCompatActivity {

    private WebView webView;
    private View loadingLayout;
    private String csrfToken = "";
    private boolean isFetching = false;

    // URLs from your config.py
    private static final String LOGIN_URL = "https://sp.srmist.edu.in/srmiststudentportal/students/loginManager/youLogin.jsp";
    private static final String REPORT_URL = "https://sp.srmist.edu.in/srmiststudentportal/students/report/studentAttendanceDetails.jsp";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portal_login);

        loadingLayout = findViewById(R.id.loadingLayout);
        webView = findViewById(R.id.webView);

        // Setup WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // 1. On Login Page: Grab CSRF Token (just like python script finds hdnCSRF)
                if (url.contains("youLogin.jsp")) {
                    view.loadUrl("javascript:window.HTMLOUT.saveToken(document.getElementById('hdnCSRF').value);");
                }

                // 2. Detect Dashboard (Login Success)
                if ((url.contains("HRDSystem.jsp") || url.contains("UserHomePage.jsp")) && !isFetching) {
                    isFetching = true;
                    fetchAttendance();
                }
            }
        });

        Toast.makeText(this, "Please Login to Sync", Toast.LENGTH_LONG).show();
        webView.loadUrl(LOGIN_URL);
    }

    private void fetchAttendance() {
        runOnUiThread(() -> loadingLayout.setVisibility(View.VISIBLE));

        // 3. POST Request using WebView (Cookies are handled automatically)
        // Python Logic: data = {'iden': '9', 'filter': '', 'csrfPreventionSalt': token}
        String postData = "iden=9&filter=&csrfPreventionSalt=" + csrfToken;
        webView.postUrl(REPORT_URL, postData.getBytes(StandardCharsets.UTF_8));

        // 4. Extract HTML after load
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.contains("studentAttendanceDetails.jsp")) {
                    view.loadUrl("javascript:window.HTMLOUT.processHTML(document.documentElement.outerHTML);");
                }
            }
        });
    }

    // Bridge to handle JS calls
    class MyJavaScriptInterface {
        @JavascriptInterface
        public void saveToken(String token) {
            if (token != null && !token.isEmpty()) {
                csrfToken = token;
            }
        }

        @JavascriptInterface
        public void processHTML(String html) {
            List<AppData.Subject> data = HtmlParser.parseAttendance(html);

            if (data.isEmpty()) {
                runOnUiThread(() -> {
                    Toast.makeText(PortalLoginActivity.this, "Failed to parse. Try again.", Toast.LENGTH_SHORT).show();
                    loadingLayout.setVisibility(View.GONE);
                    isFetching = false;
                    webView.loadUrl(LOGIN_URL);
                });
            } else {
                // --- SECURITY FEATURE: SAVE DATA, NOT CREDENTIALS ---
                String json = new Gson().toJson(data);
// NEW CODE (Encrypted)
                com.example.srmistpro.SecureStore.saveData(PortalLoginActivity.this, "LAST_ATTENDANCE", json);

                Intent intent = new Intent(PortalLoginActivity.this, MainActivity.class);
                intent.putExtra("data", json);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        }
    }
}