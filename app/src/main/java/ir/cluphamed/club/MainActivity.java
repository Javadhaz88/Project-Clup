package ir.cluphamed.club;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String BASE = "https://cluphamed.ir/";
    private static final String HOST = "cluphamed.ir";
    private static final int FILE_CODE = 9201;
    private static final int BG = Color.rgb(12,11,9), PANEL = Color.rgb(25,23,19);
    private static final int GOLD = Color.rgb(215,181,109), TEXT = Color.rgb(247,242,230);
    private static final int MUTED = Color.rgb(174,164,145);
    private WebView browser;
    private ProgressBar progress;
    private TextView connection, offline;
    private ValueCallback<Uri[]> fileChoice;
    private final TextView[] navLabels = new TextView[4];

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow(); w.setStatusBarColor(BG); w.setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 29) w.setNavigationBarContrastEnforced(false);
        setContentView(buildUi()); configureWebView();
        if (state != null && browser.restoreState(state) != null) connection.setText("آماده");
        else browser.loadUrl(BASE);
    }
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private LinearLayout.LayoutParams lp(int w,int h) { return new LinearLayout.LayoutParams(w,h); }
    private GradientDrawable shape(int color,int radius,int stroke) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke); return d;
    }
    private TextView text(String s,float size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private View buildUi() {
        LinearLayout root=new LinearLayout(this); root.setOrientation(1);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); root.setBackgroundColor(BG);
        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16),0,dp(16),0); root.addView(header,lp(-1,dp(66)));
        header.addView(text("✦",27,GOLD,true),lp(dp(42),-1));
        LinearLayout brand=new LinearLayout(this); brand.setOrientation(1); brand.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(brand,new LinearLayout.LayoutParams(0,-1,1));
        TextView title=text("کلوپ حامد",19,TEXT,true); title.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        brand.addView(title,lp(-1,dp(29)));
        TextView sub=text("دنیای فیلم و سریال",11,GOLD,false); sub.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        brand.addView(sub,lp(-1,dp(20)));
        connection=text("در حال اتصال",10,GOLD,false); connection.setPadding(dp(11),dp(6),dp(11),dp(6));
        connection.setBackground(shape(PANEL,20,Color.rgb(66,56,37))); header.addView(connection,lp(-2,dp(32)));
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); progress.setMax(100);
        if(Build.VERSION.SDK_INT>=21)progress.setProgressTintList(android.content.res.ColorStateList.valueOf(GOLD));
        root.addView(progress,lp(-1,dp(2)));
        FrameLayout frame=new FrameLayout(this); root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
        browser=new WebView(this); browser.setBackgroundColor(BG); frame.addView(browser,new FrameLayout.LayoutParams(-1,-1));
        offline=text("اتصال به کلوپ حامد برقرار نشد\nاینترنت را بررسی کن و دوباره تلاش کن",15,TEXT,false);
        offline.setLineSpacing(dp(7),1f); offline.setPadding(dp(24),dp(24),dp(24),dp(24));
        offline.setBackgroundColor(BG); offline.setVisibility(View.GONE);
        offline.setOnClickListener(v->{offline.setVisibility(View.GONE);browser.reload();});
        frame.addView(offline,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout nav=new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setPadding(dp(6),dp(6),dp(6),dp(6));
        nav.setBackground(shape(PANEL,20,Color.rgb(65,53,33)));
        LinearLayout.LayoutParams np=lp(-1,dp(68)); np.setMargins(dp(10),dp(7),dp(10),dp(10)); root.addView(nav,np);
        addNav(nav,0,"⌂","خانه",()->browser.loadUrl(BASE));
        addNav(nav,1,"▦","آرشیو",()->openSection("آرشیو"));
        addNav(nav,2,"🛒","سبد سفارش",()->openSection("سبد"));
        addNav(nav,3,"☎","تماس",()->openSection("تماس"));
        selectNav(0); return root;
    }
    private void addNav(LinearLayout nav,int i,String icon,String caption,Runnable action) {
        LinearLayout item=new LinearLayout(this); item.setOrientation(1); item.setGravity(Gravity.CENTER);
        item.setClickable(true); item.setFocusable(true);
        item.addView(text(icon,21,GOLD,true),lp(-1,dp(29)));
        navLabels[i]=text(caption,10,MUTED,false); item.addView(navLabels[i],lp(-1,dp(20)));
        item.setOnClickListener(v->{selectNav(i);action.run();});
        nav.addView(item,new LinearLayout.LayoutParams(0,-1,1));
    }
    private void selectNav(int selected) {
        for(int i=0;i<navLabels.length;i++)if(navLabels[i]!=null)navLabels[i].setTextColor(i==selected?GOLD:MUTED);
    }
    // Locate the matching real section or link on the live site; no fake cart is created.
    private void openSection(String keyword) {
        String js="(function(){var k='"+keyword+"';var a=[...document.querySelectorAll('a,button,[role=button],h1,h2,h3,h4,section,article,div')];"
          +"var e=a.find(x=>{var t=(x.innerText||x.textContent||'').trim();if(!t||t.length>90||!t.includes(k))return false;"
          +"var r=x.getBoundingClientRect();return r.width>0&&r.height>0;});"
          +"if(!e)return 'missing';e.scrollIntoView({behavior:'smooth',block:'start'});"
          +"if(['A','BUTTON'].includes(e.tagName)||e.getAttribute('role')==='button')e.click();return 'ok';})()";
        browser.evaluateJavascript(js,result->{if(result==null||result.contains("missing"))
            Toast.makeText(this,"این بخش در صفحه فعلی پیدا نشد؛ از منوی خود سایت استفاده کن",Toast.LENGTH_SHORT).show();});
    }
    private void configureWebView() {
        WebSettings s=browser.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false); s.setAllowContentAccess(true); s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false); s.setUseWideViewPort(true); s.setLoadWithOverviewMode(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        if(Build.VERSION.SDK_INT>=26)s.setSafeBrowsingEnabled(true);
        CookieManager c=CookieManager.getInstance(); c.setAcceptCookie(true); c.setAcceptThirdPartyCookies(browser,false);
        browser.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return handleLink(r.getUrl());}
            @Override public boolean shouldOverrideUrlLoading(WebView v,String u){return handleLink(Uri.parse(u));}
            @Override public void onPageStarted(WebView v,String u,android.graphics.Bitmap b){offline.setVisibility(View.GONE);connection.setText("در حال بارگذاری");}
            @Override public void onPageFinished(WebView v,String u){CookieManager.getInstance().flush();connection.setText("متصل");}
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){
                if(r.isForMainFrame()){offline.setVisibility(View.VISIBLE);connection.setText("بدون اتصال");}
            }
        });
        browser.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView v,int p){progress.setProgress(p);progress.setVisibility(p>=100?View.INVISIBLE:View.VISIBLE);}
            @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> cb,FileChooserParams p){
                if(fileChoice!=null)fileChoice.onReceiveValue(null);fileChoice=cb;
                try{startActivityForResult(p.createIntent(),FILE_CODE);return true;}
                catch(ActivityNotFoundException e){fileChoice=null;Toast.makeText(MainActivity.this,"انتخاب فایل در دسترس نیست",Toast.LENGTH_SHORT).show();return false;}
            }
        });
    }
    private boolean isOurs(Uri u) {
        if(u==null||!"https".equalsIgnoreCase(u.getScheme()))return false;
        String h=u.getHost(); return h!=null&&(h.equalsIgnoreCase(HOST)||h.toLowerCase(Locale.ROOT).endsWith("."+HOST));
    }
    private boolean handleLink(Uri u) {
        if(isOurs(u))return false; if(u==null||u.getScheme()==null)return true;
        String s=u.getScheme().toLowerCase(Locale.ROOT);
        if(s.equals("https")||s.equals("http")||s.equals("tel")||s.equals("mailto")||s.equals("sms"))
            try{startActivity(new Intent(Intent.ACTION_VIEW,u));}
            catch(ActivityNotFoundException e){Toast.makeText(this,"برنامه‌ای برای باز کردن پیوند پیدا نشد",Toast.LENGTH_SHORT).show();}
        return true;
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==FILE_CODE&&fileChoice!=null){fileChoice.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));fileChoice=null;}
    }
    @Override public void onBackPressed() {
        if(browser!=null&&browser.canGoBack())browser.goBack();
        else new AlertDialog.Builder(this).setMessage("از برنامه خارج می‌شوی؟")
          .setPositiveButton("خروج",(d,w)->finish()).setNegativeButton("ماندن",null).show();
    }
    @Override protected void onSaveInstanceState(Bundle out){if(browser!=null)browser.saveState(out);super.onSaveInstanceState(out);}
    @Override protected void onDestroy() {
        if(browser!=null){browser.stopLoading();browser.setWebViewClient(null);browser.setWebChromeClient(null);browser.destroy();browser=null;}
        if(fileChoice!=null){fileChoice.onReceiveValue(null);fileChoice=null;} super.onDestroy();
    }
}