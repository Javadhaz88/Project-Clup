package ir.cluphamed.club;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final String BASE = "https://cluphamed.ir/";
    private static final String HOST = "cluphamed.ir";
    private static final int FILE_CODE = 9201;
    private static final int BG = Color.rgb(12, 11, 9);
    private static final int PANEL = Color.rgb(25, 23, 19);
    private static final int GOLD = Color.rgb(215, 181, 109);
    private static final int TEXT = Color.rgb(247, 242, 230);

    private WebView browser;
    private ProgressBar progress;
    private TextView connection;
    private TextView offline;
    private ValueCallback<Uri[]> fileChoice;
    private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("club_hamed", MODE_PRIVATE);
        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 29) {
            window.setNavigationBarContrastEnforced(false);
        }
        setContentView(buildUi());
        configureWebView();
        if (state != null && browser.restoreState(state) != null) {
            // WebView state restored.
        } else {
            browser.loadUrl(BASE);
        }
    }

    private LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private GradientDrawable rounded(int color, int radiusDp, int strokeColor) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radiusDp));
        if (strokeColor != Color.TRANSPARENT) shape.setStroke(dp(1), strokeColor);
        return shape;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView label(String text, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(color);
        v.setTextSize(size);
        v.setGravity(Gravity.CENTER);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), 0, dp(16), 0);
        header.setBackgroundColor(BG);
        root.addView(header, lp(-1, dp(62)));

        TextView mark = label("✦", 25, GOLD, true);
        header.addView(mark, lp(dp(38), -1));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(brand, new LinearLayout.LayoutParams(0, -1, 1));

        TextView title = label("کلوپ حامد", 18, TEXT, true);
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        brand.addView(title, lp(-1, dp(28)));
        TextView subtitle = label("آرشیو فیلم و سریال", 11, GOLD, false);
        subtitle.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        brand.addView(subtitle, lp(-1, dp(20)));

        connection = label("در حال اتصال…", 10, GOLD, false);
        connection.setPadding(dp(10), dp(6), dp(10), dp(6));
        connection.setBackground(rounded(PANEL, 20, Color.rgb(66, 56, 37)));
        header.addView(connection, lp(-2, dp(32)));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        if (Build.VERSION.SDK_INT >= 21) progress.setProgressTintList(android.content.res.ColorStateList.valueOf(GOLD));
        root.addView(progress, lp(-1, dp(2)));

        FrameLayout webFrame = new FrameLayout(this);
        root.addView(webFrame, new LinearLayout.LayoutParams(-1, 0, 1));

        browser = new WebView(this);
        browser.setBackgroundColor(BG);
        webFrame.addView(browser, new FrameLayout.LayoutParams(-1, -1));

        offline = label("اتصال به سایت برقرار نشد\nاینترنت را بررسی کن و برای تلاش دوباره بزن", 15, TEXT, false);
        offline.setLineSpacing(dp(6), 1f);
        offline.setPadding(dp(24), dp(24), dp(24), dp(24));
        offline.setBackgroundColor(BG);
        offline.setVisibility(View.GONE);
        offline.setOnClickListener(v -> {
            offline.setVisibility(View.GONE);
            browser.reload();
        });
        webFrame.addView(offline, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(7), dp(7), dp(7), dp(7));
        nav.setBackground(rounded(PANEL, 18, Color.rgb(55, 47, 32)));
        LinearLayout.LayoutParams navParams = lp(-1, dp(68));
        navParams.setMargins(dp(10), dp(7), dp(10), dp(10));
        root.addView(nav, navParams);

        addNav(nav, "⌂", "خانه", () -> browser.loadUrl(BASE));
        addNav(nav, "☆", "نشانک", this::toggleFavorite);
        addNav(nav, "▤", "ذخیره‌ها", this::showFavorites);
        addNav(nav, "↗", "اشتراک", this::shareCurrent);
        addNav(nav, "☎", "تماس", () -> browser.loadUrl(BASE + "#contact"));

        return root;
    }

    private void addNav(LinearLayout nav, String icon, String text, Runnable action) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setClickable(true);
        item.setFocusable(true);
        item.setBackground(rounded(Color.TRANSPARENT, 12, Color.TRANSPARENT));

        TextView iconView = label(icon, 22, GOLD, true);
        item.addView(iconView, lp(-1, dp(29)));
        TextView textView = label(text, 10, TEXT, false);
        item.addView(textView, lp(-1, dp(20)));

        item.setOnClickListener(v -> action.run());
        nav.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
    }

    private void configureWebView() {
        WebSettings settings = browser.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        if (Build.VERSION.SDK_INT >= 26) settings.setSafeBrowsingEnabled(true);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(browser, false);

        browser.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleLink(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleLink(Uri.parse(url));
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                offline.setVisibility(View.GONE);
                connection.setText("در حال بارگذاری");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().flush();
                connection.setText("متصل");
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    offline.setVisibility(View.VISIBLE);
                    connection.setText("بدون اتصال");
                }
            }
        });

        browser.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int value) {
                progress.setProgress(value);
                progress.setVisibility(value >= 100 ? View.INVISIBLE : View.VISIBLE);
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileChoice != null) fileChoice.onReceiveValue(null);
                fileChoice = callback;
                try {
                    startActivityForResult(params.createIntent(), FILE_CODE);
                    return true;
                } catch (ActivityNotFoundException ex) {
                    fileChoice = null;
                    Toast.makeText(MainActivity.this, "انتخاب فایل در این دستگاه در دسترس نیست", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
    }

    private boolean isOurs(Uri uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())) return false;
        String host = uri.getHost();
        return host != null && (host.equalsIgnoreCase(HOST) || host.toLowerCase(Locale.ROOT).endsWith("." + HOST));
    }

    private boolean handleLink(Uri uri) {
        if (isOurs(uri)) return false;
        if (uri == null || uri.getScheme() == null) return true;
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (scheme.equals("https") || scheme.equals("http") || scheme.equals("tel") || scheme.equals("mailto") || scheme.equals("sms")) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(this, "برنامه‌ای برای باز کردن این پیوند پیدا نشد", Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }

    private JSONArray getFavorites() {
        try {
            return new JSONArray(prefs.getString("favorites", "[]"));
        } catch (JSONException ex) {
            return new JSONArray();
        }
    }

    private void toggleFavorite() {
        Uri uri = Uri.parse(browser.getUrl() == null ? "" : browser.getUrl());
        if (!isOurs(uri)) {
            Toast.makeText(this, "ابتدا صفحه‌ای از سایت را باز کن", Toast.LENGTH_SHORT).show();
            return;
        }
        String current = uri.toString();
        JSONArray old = getFavorites();
        JSONArray updated = new JSONArray();
        boolean removed = false;
        for (int i = 0; i < old.length(); i++) {
            JSONObject item = old.optJSONObject(i);
            if (item == null) continue;
            if (current.equals(item.optString("url"))) removed = true;
            else updated.put(item);
        }
        if (!removed) {
            JSONObject item = new JSONObject();
            try {
                item.put("url", current);
                item.put("title", browser.getTitle() == null ? "کلوپ حامد" : browser.getTitle());
                updated.put(item);
            } catch (JSONException ignored) { }
        }
        prefs.edit().putString("favorites", updated.toString()).apply();
        Toast.makeText(this, removed ? "از نشانک‌ها حذف شد" : "به نشانک‌ها اضافه شد", Toast.LENGTH_SHORT).show();
    }

    private void showFavorites() {
        JSONArray saved = getFavorites();
        if (saved.length() == 0) {
            new AlertDialog.Builder(this).setTitle("صفحه‌های ذخیره‌شده")
                    .setMessage("هنوز صفحه‌ای ذخیره نکرده‌ای.")
                    .setPositiveButton("باشه", null).show();
            return;
        }
        String[] titles = new String[saved.length()];
        for (int i = 0; i < saved.length(); i++) {
            JSONObject item = saved.optJSONObject(i);
            titles[i] = item == null ? "صفحه ذخیره‌شده" : item.optString("title", "صفحه ذخیره‌شده");
        }
        new AlertDialog.Builder(this).setTitle("صفحه‌های ذخیره‌شده")
                .setItems(titles, (dialog, which) -> {
                    JSONObject item = saved.optJSONObject(which);
                    if (item != null) {
                        Uri uri = Uri.parse(item.optString("url", ""));
                        if (isOurs(uri)) browser.loadUrl(uri.toString());
                    }
                })
                .setNeutralButton("پاک‌کردن همه", (dialog, which) -> {
                    new AlertDialog.Builder(this).setMessage("همه نشانک‌ها حذف شوند؟")
                            .setNegativeButton("انصراف", null)
                            .setPositiveButton("حذف", (d, w) -> {
                                prefs.edit().putString("favorites", "[]").apply();
                                Toast.makeText(this, "نشانک‌ها پاک شدند", Toast.LENGTH_SHORT).show();
                            }).show();
                })
                .setNegativeButton("بستن", null).show();
    }

    private void shareCurrent() {
        Uri uri = Uri.parse(browser.getUrl() == null ? BASE : browser.getUrl());
        String shareUrl = isOurs(uri) ? uri.toString() : BASE;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, "کلوپ حامد\n" + shareUrl);
        startActivity(Intent.createChooser(send, "اشتراک‌گذاری"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CODE && fileChoice != null) {
            fileChoice.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileChoice = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (browser != null && browser.canGoBack()) {
            browser.goBack();
        } else {
            new AlertDialog.Builder(this).setMessage("از برنامه خارج می‌شوی؟")
                    .setPositiveButton("خروج", (dialog, which) -> finish())
                    .setNegativeButton("ماندن", null).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (browser != null) browser.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (browser != null) {
            browser.stopLoading();
            browser.setWebViewClient(null);
            browser.setWebChromeClient(null);
            browser.destroy();
            browser = null;
        }
        if (fileChoice != null) {
            fileChoice.onReceiveValue(null);
            fileChoice = null;
        }
        super.onDestroy();
    }
}
