package es.webwidget;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.PeriodicWorkRequest;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.WorkManager;
import androidx.work.Data;
import android.appwidget.AppWidgetManager;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RefreshWorker extends Worker {
 static final String WORK_NAME="webwidget-periodic-capture-";
 static String workName(int id){return WORK_NAME+id;}
 public RefreshWorker(@NonNull Context c,@NonNull WorkerParameters p){super(c,p);}
 public static void schedule(Context context,int id){
  WidgetSettings.ensure(context,id);
  int minutes=WidgetSettings.forId(context,id).getInt("interval",30);
  if(minutes==0){WorkManager.getInstance(context).cancelUniqueWork(workName(id));return;}
  Data data=new Data.Builder().putInt("widget_id",id).build();
  PeriodicWorkRequest request=new PeriodicWorkRequest.Builder(RefreshWorker.class,Math.max(15,minutes),TimeUnit.MINUTES).setInputData(data).build();
  WorkManager.getInstance(context).enqueueUniquePeriodicWork(workName(id),ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,request);
 }
 public static void cancel(Context context,int id){WorkManager.getInstance(context).cancelUniqueWork(workName(id));}
 @NonNull @Override public Result doWork(){
  Context context=getApplicationContext();
  int id=getInputData().getInt("widget_id",AppWidgetManager.INVALID_APPWIDGET_ID);
  if(id==AppWidgetManager.INVALID_APPWIDGET_ID)return Result.failure();
  boolean exists=false;for(int active:WidgetSettings.ids(context))if(active==id){exists=true;break;}
  if(!exists)return Result.success();
  SharedPreferences prefs=WidgetSettings.forId(context,id);
  String url=prefs.getString("url","https://example.com");
  boolean desktop=prefs.getBoolean("desktop",false);
  int zoom=prefs.getInt("zoom",100);
  CountDownLatch latch=new CountDownLatch(1);
  Handler main=new Handler(Looper.getMainLooper());
  final WebView[] view=new WebView[1];
  final boolean[] saved={false};
  main.post(()->{
   try{
    WebView web=new WebView(context);view[0]=web;
    web.setBackgroundColor(Color.WHITE);
    WebSettings settings=web.getSettings();
    settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
    settings.setUseWideViewPort(desktop);settings.setLoadWithOverviewMode(desktop);
    if(desktop)settings.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
    final int width=Math.max(320,context.getResources().getDisplayMetrics().widthPixels);
    final int height=Math.max(400,(int)(context.getResources().getDisplayMetrics().heightPixels*0.70f));
    web.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
    web.layout(0,0,width,height);
    web.setWebViewClient(new WebViewClient(){
     @Override public void onPageFinished(WebView v,String page){
      String viewport=desktop?"width=1280, initial-scale=1":"width=device-width, initial-scale=1";
      String script="(function(){var m=document.querySelector('meta[name=viewport]');if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}m.setAttribute('content','"+viewport+"');var st=document.getElementById('webwidget-zoom');if(!st){st=document.createElement('style');st.id='webwidget-zoom';document.head.appendChild(st);}st.textContent='html{zoom:"+String.format(Locale.US,"%.2f",zoom/100f)+" !important;}';})();";
      v.evaluateJavascript(script,x->main.postDelayed(()->{
       try{
        int w=Math.min(800,width),h=Math.max(1,Math.round(height*(w/(float)width)));
        Bitmap bmp=Bitmap.createBitmap(w,h,Bitmap.Config.RGB_565);
        Canvas canvas=new Canvas(bmp);canvas.scale(w/(float)width,h/(float)height);v.draw(canvas);
        try(FileOutputStream out=context.openFileOutput(WidgetSettings.snapshotName(id),Context.MODE_PRIVATE)){
         saved[0]=bmp.compress(Bitmap.CompressFormat.PNG,100,out);
        }
        bmp.recycle();
        if(saved[0])WidgetProvider.refresh(context);
       }catch(Exception ignored){}finally{latch.countDown();}
      },3000));
     }
    });
    web.loadUrl(url);
   }catch(Exception e){latch.countDown();}
  });
  try{if(!latch.await(45,TimeUnit.SECONDS))return Result.retry();}
  catch(InterruptedException e){Thread.currentThread().interrupt();return Result.retry();}
  finally{main.post(()->{if(view[0]!=null)view[0].destroy();});}
  return saved[0]?Result.success():Result.retry();
 }
}