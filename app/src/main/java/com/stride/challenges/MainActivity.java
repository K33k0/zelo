package com.stride.challenges;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.JavascriptInterface;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private WebView web;
    private String pendingBackup;
    private boolean ready;
    private String deferredResult;
    private boolean resumed;
    private String lastNativeDay="";
    private final android.os.Handler dayHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable dayRefresh=()->refreshClock();
    private final android.content.BroadcastReceiver clockReceiver=new android.content.BroadcastReceiver(){
        @Override public void onReceive(android.content.Context context,Intent intent){if(resumed)refreshClock();}
    };
    private void refreshClock(){
        if(!resumed)return;
        String day=java.time.LocalDate.now()+"/"+java.time.ZoneId.systemDefault();
        if(!day.equals(lastNativeDay)){lastNativeDay=day;ZeloWidget.updateAll(this);ReminderReceiver.refreshNotifications(this);ReminderReceiver.scheduleAll(this);}
        if(ready)web.evaluateJavascript("window.onAppResume ? window.onAppResume() : (window.refreshNative && window.refreshNative())",null);
        dayHandler.removeCallbacks(dayRefresh);
        long delay=java.time.Duration.between(java.time.ZonedDateTime.now(),java.time.LocalDate.now().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault())).toMillis()+100;
        dayHandler.postDelayed(dayRefresh,Math.max(100,delay));
    }
    private final java.util.concurrent.ExecutorService fileIo=java.util.concurrent.Executors.newSingleThreadExecutor();
    private void openExternal(String address){
        try{
            Uri uri=Uri.parse(address);String scheme=uri.getScheme();
            if(address.length()>2048||!("https".equalsIgnoreCase(scheme)||"http".equalsIgnoreCase(scheme))||uri.getHost()==null||uri.getUserInfo()!=null)return;
            runOnUiThread(()->{try{startActivity(new Intent(Intent.ACTION_VIEW,uri).addCategory(Intent.CATEGORY_BROWSABLE));}catch(android.content.ActivityNotFoundException e){android.widget.Toast.makeText(this,"No browser is available to open this page.",android.widget.Toast.LENGTH_LONG).show();}});
        }catch(Exception e){android.util.Log.w("Zelo","Could not open linked page",e);}
    }
    private void resultToWeb(String script){runOnUiThread(()->{if(isFinishing())return;if(ready)web.evaluateJavascript(script,null);else deferredResult=script;});}
    private static java.lang.ref.WeakReference<MainActivity> active=new java.lang.ref.WeakReference<>(null);
    static void notifyDataChanged(){MainActivity activity=active.get();if(activity!=null)activity.runOnUiThread(()->{if(activity.ready)activity.web.evaluateJavascript("window.refreshNative && window.refreshNative()",null);});}
    @android.annotation.SuppressLint("SetJavaScriptEnabled") // Only bundled, escaped UI content is loaded; external navigation and file access are blocked.
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(15,17,35));
        getWindow().setNavigationBarColor(Color.rgb(15,17,35));
        getWindow().getDecorView().setSystemUiVisibility(0);
        ReminderReceiver.channel(this);
        active=new java.lang.ref.WeakReference<>(this);
        android.content.IntentFilter clockFilter=new android.content.IntentFilter();
        clockFilter.addAction(Intent.ACTION_DATE_CHANGED);clockFilter.addAction(Intent.ACTION_TIME_CHANGED);clockFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);clockFilter.addAction(Intent.ACTION_SCREEN_ON);clockFilter.addAction(Intent.ACTION_USER_PRESENT);
        if(android.os.Build.VERSION.SDK_INT>=33)registerReceiver(clockReceiver,clockFilter,RECEIVER_NOT_EXPORTED);else registerReceiver(clockReceiver,clockFilter);
        // Keep content clear of system bars with Android 15 edge-to-edge enforcement.
        android.widget.FrameLayout frame = new android.widget.FrameLayout(this);
        frame.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        web = new WebView(this);
        frame.addView(web);
        setContentView(frame);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.addJavascriptInterface(new BackupBridge(), "StrideAndroid");
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) { ready=true;if(deferredResult!=null){web.evaluateJavascript(deferredResult,null);deferredResult=null;}refreshClock(); }
            @Override public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (!"app.stride.local".equals(uri.getHost())) return new android.webkit.WebResourceResponse("text/plain", "UTF-8", null);
                String path = uri.getPath();
                if (path == null || path.equals("/")) path = "/index.html";
                if (path.contains("..")) return new android.webkit.WebResourceResponse("text/plain", "UTF-8", null);
                String mime = path.endsWith(".css") ? "text/css" : path.endsWith(".js") ? "text/javascript" : path.endsWith(".ttf") ? "font/ttf" : "text/html";
                try { return new android.webkit.WebResourceResponse(mime, "UTF-8", getAssets().open("www" + path)); }
                catch (Exception ex) { return new android.webkit.WebResourceResponse("text/plain", "UTF-8", null); }
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return !"app.stride.local".equals(request.getUrl().getHost()); }
        });
        web.loadUrl("https://app.stride.local/index.html");
    }
    private class BackupBridge {
        @JavascriptInterface public void openExternalLink(String address){openExternal(address);}
        @JavascriptInterface public String getData(){return ProgressStore.read(MainActivity.this);}
        @JavascriptInterface public int saveData(String data,int expectedRevision){
            int revision=ProgressStore.save(MainActivity.this,data,expectedRevision);
            if(revision>=0)runOnUiThread(()->{ZeloWidget.updateAll(MainActivity.this);ReminderReceiver.scheduleAll(MainActivity.this);ReminderReceiver.refreshNotifications(MainActivity.this);BackupService.schedule(MainActivity.this,false);});
            return revision;
        }
        @JavascriptInterface public int restoreData(String data,String expectedRaw){
            int revision=ProgressStore.restore(MainActivity.this,data,expectedRaw);
            if(revision>=0)runOnUiThread(()->ProgressStore.changed(MainActivity.this));
            return revision;
        }
        @JavascriptInterface public void requestNotifications(){runOnUiThread(()->{
            if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},20);
        });}
        @JavascriptInterface public void notificationSettings(){runOnUiThread(()->startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName())));}
        @JavascriptInterface public String backupStatus(){return BackupService.status(MainActivity.this);}
        @JavascriptInterface public void backupNow(){BackupService.schedule(MainActivity.this,true);}
        @JavascriptInterface public void stopBackups(){BackupService.stop(MainActivity.this);}
        @JavascriptInterface public void connectBackup(){runOnUiThread(()->startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION).putExtra(Intent.EXTRA_TITLE,"zelo-backup-history.json"),12));}
        @JavascriptInterface public void pinWidget(){runOnUiThread(()->{
            android.appwidget.AppWidgetManager manager=getSystemService(android.appwidget.AppWidgetManager.class);
            if(manager.isRequestPinAppWidgetSupported())manager.requestPinAppWidget(new android.content.ComponentName(MainActivity.this,ZeloWidget.class),null,null);
            else android.widget.Toast.makeText(MainActivity.this,"Long-press your home screen, choose Widgets, then Zelo.",android.widget.Toast.LENGTH_LONG).show();
        });}
        @JavascriptInterface public void exportData(String data) {
            if (data.length() > 5_000_000) return;
            pendingBackup = data;
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, "zelo-backup.json");
                startActivityForResult(intent, 10);
            });
        }
        @JavascriptInterface public void importData() {
            runOnUiThread(() -> startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE), 11));
        }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        final String exportSnapshot=pendingBackup!=null?pendingBackup:ProgressStore.read(this);
        fileIo.execute(()->{
        try {
            if (request == 10 && exportSnapshot != null) {
                try (OutputStream stream = getContentResolver().openOutputStream(data.getData())) { if(stream==null)throw new java.io.IOException("No export stream");stream.write(exportSnapshot.getBytes(StandardCharsets.UTF_8)); }
                if(pendingBackup==exportSnapshot)pendingBackup = null;
                resultToWeb("window.backupSaved()");
            } else if (request == 11) {
                try (InputStream stream = getContentResolver().openInputStream(data.getData()); java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096]; int count;
                    while ((count = stream.read(buffer)) != -1) { out.write(buffer, 0, count); if (out.size() > 5_000_000) throw new Exception("Backup too large"); }
                    String json = out.toString("UTF-8");
                    resultToWeb("window.restoreBackup(" + org.json.JSONObject.quote(json) + ")");
                }
            } else if(request==12){
                Uri uri=data.getData();
                getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                String name="zelo-backup-history.json";
                try(android.database.Cursor cursor=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);}
                BackupService.connect(this,uri,name);resultToWeb("window.backupConnected && window.backupConnected()");
            }
        } catch (Exception ex) { resultToWeb(request==12?"window.backupConnectionFailed && window.backupConnectionFailed()":"window.backupFailed()"); }
        });
    }
    @Override public void onBackPressed() { web.evaluateJavascript("window.handleBack()", handled->{if("false".equals(handled))finish();}); }
    @Override protected void onResume(){super.onResume();resumed=true;if(web!=null)web.onResume();BackupService.schedule(this,false);refreshClock();}
    @Override protected void onPause(){resumed=false;dayHandler.removeCallbacks(dayRefresh);if(web!=null)web.onPause();super.onPause();}
    @Override public void onWindowFocusChanged(boolean hasFocus){super.onWindowFocusChanged(hasFocus);if(hasFocus)refreshClock();}
    @Override protected void onDestroy(){ready=false;dayHandler.removeCallbacks(dayRefresh);unregisterReceiver(clockReceiver);fileIo.shutdownNow();if(active.get()==this)active.clear();if(web!=null)web.destroy();super.onDestroy();}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==20){ReminderReceiver.scheduleAll(this);if(results.length>0&&results[0]!=android.content.pm.PackageManager.PERMISSION_GRANTED)web.evaluateJavascript("window.backupSaved && document.getElementById('toast').replaceChildren('Notifications are off. Enable them in Android settings.'); document.getElementById('toast').classList.add('visible'); setTimeout(()=>document.getElementById('toast').classList.remove('visible'),4000)",null);}}
}
