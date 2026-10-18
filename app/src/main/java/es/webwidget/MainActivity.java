package es.webwidget;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.InputType;
import android.text.TextUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.*;
import java.io.FileOutputStream;
import java.util.Locale;

public class MainActivity extends Activity {
 WebView web;
 TextView address, zoomLabel;
 SeekBar zoomBar;
 Handler handler=new Handler(Looper.getMainLooper());
 String currentUrl;
 int zoomPercent;
 boolean desktopMode;
 String mobileUserAgent;
 Button modeButton;
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  currentUrl=getPreferences(0).getString("url","https://example.com");
  zoomPercent=getPreferences(0).getInt("zoom",100);
  desktopMode=getPreferences(0).getBoolean("desktop",false);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.WHITE);
  address=new TextView(this);address.setText(currentUrl);address.setTextColor(Color.BLACK);address.setSingleLine(true);address.setEllipsize(TextUtils.TruncateAt.MIDDLE);address.setPadding(16,14,16,14);
  root.addView(address,new LinearLayout.LayoutParams(-1,-2));
  LinearLayout buttons=new LinearLayout(this);
  Button change=new Button(this);change.setText("Cambiar URL");
  Button capture=new Button(this);capture.setText("Actualizar widget");
  buttons.addView(change,new LinearLayout.LayoutParams(0,-2,1));buttons.addView(capture,new LinearLayout.LayoutParams(0,-2,1));root.addView(buttons);
  modeButton=new Button(this);
  modeButton.setText(desktopMode?"Vista: escritorio":"Vista: móvil");
  root.addView(modeButton,new LinearLayout.LayoutParams(-1,-2));
  LinearLayout zoomControls=new LinearLayout(this);zoomControls.setOrientation(LinearLayout.HORIZONTAL);
  zoomLabel=new TextView(this);zoomLabel.setTextColor(Color.BLACK);zoomLabel.setPadding(12,12,8,0);
  zoomBar=new SeekBar(this);zoomBar.setMax(170);zoomBar.setProgress(Math.max(0,Math.min(170,zoomPercent-30)));
  zoomControls.addView(zoomLabel,new LinearLayout.LayoutParams(-2,-2));
  zoomControls.addView(zoomBar,new LinearLayout.LayoutParams(0,-2,1));
  root.addView(zoomControls,new LinearLayout.LayoutParams(-1,-2));
  web=new WebView(this);web.setBackgroundColor(Color.WHITE);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);
  web.getSettings().setSupportZoom(true);web.getSettings().setBuiltInZoomControls(true);web.getSettings().setDisplayZoomControls(false);
  web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  mobileUserAgent=web.getSettings().getUserAgentString();
  configureMode();
  web.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView v,String s){applyZoom();handler.postDelayed(()->snapshot(),2500);}});
  root.addView(web,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
  change.setOnClickListener(v->showUrlDialog());
  address.setOnClickListener(v->showUrlDialog());
  capture.setOnClickListener(v->snapshot());
  modeButton.setOnClickListener(v->{
   desktopMode=!desktopMode;
   getPreferences(0).edit().putBoolean("desktop",desktopMode).apply();
   modeButton.setText(desktopMode?"Vista: escritorio":"Vista: móvil");
   configureMode();
   web.reload();
  });
  zoomBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar b,int progress,boolean user){
    zoomPercent=progress+30;zoomLabel.setText("Zoom "+zoomPercent+"%");
    if(user){getPreferences(0).edit().putInt("zoom",zoomPercent).apply();applyZoom();}
   }
   public void onStartTrackingTouch(SeekBar b){}
   public void onStopTrackingTouch(SeekBar b){snapshot();}
  });
  zoomLabel.setText("Zoom "+zoomPercent+"%");
  web.loadUrl(currentUrl);
 }
 void configureMode(){
  WebSettings settings=web.getSettings();
  settings.setUseWideViewPort(desktopMode);
  settings.setLoadWithOverviewMode(desktopMode);
  if(desktopMode){
   settings.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
  }else{
   settings.setUserAgentString(mobileUserAgent);
  }
 }
 void applyZoom(){
  if(web==null)return;
  // Text zoom scales CSS text without requiring page-specific JavaScript.
  // For the whole page, scale its visual viewport through CSS transform.
  final float scale=zoomPercent/100f;
  web.evaluateJavascript("(function(){var id='webwidget-zoom';var st=document.getElementById(id);if(!st){st=document.createElement('style');st.id=id;document.head.appendChild(st);}st.textContent='html{zoom:"+String.format(Locale.US,"%.2f",scale)+" !important;}';})();",null);
 }
 void showUrlDialog(){
  EditText input=new EditText(this);
  input.setSingleLine(true);input.setSelectAllOnFocus(true);
  input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
  input.setText(currentUrl);
  LinearLayout wrapper=new LinearLayout(this);wrapper.setPadding(28,12,28,0);wrapper.addView(input,new LinearLayout.LayoutParams(-1,-2));
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Dirección del widget").setView(wrapper)
   .setNegativeButton("Cancelar",(d,w)->{})
   .setPositiveButton("Guardar y abrir",(d,w)->{
    String u=input.getText().toString().trim();
    if(!(u.startsWith("https://")||u.startsWith("http://"))){Toast.makeText(this,"La URL debe empezar por http:// o https://",Toast.LENGTH_LONG).show();return;}
    currentUrl=u;getPreferences(0).edit().putString("url",u).apply();address.setText(u);web.loadUrl(u);
   }).create();
  dialog.show();input.requestFocus();input.selectAll();
 }
 void snapshot(){
  if(web.getWidth()<1||web.getHeight()<1)return;
  // Preserve the WebView's aspect ratio exactly, including on very wide or tall screens.
  int sourceW=web.getWidth(), sourceH=web.getHeight();
  float factor=Math.min(1f,Math.min(800f/sourceW,1000f/sourceH));
  int w=Math.max(1,Math.round(sourceW*factor));
  int h=Math.max(1,Math.round(sourceH*factor));
  Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.RGB_565);
  Canvas canvas=new Canvas(bitmap);canvas.scale(factor,factor);web.draw(canvas);
  try(FileOutputStream stream=openFileOutput("snapshot.png",MODE_PRIVATE)){
   bitmap.compress(Bitmap.CompressFormat.PNG,85,stream);
   WidgetProvider.refresh(this);
   Toast.makeText(this,"Widget actualizado",Toast.LENGTH_SHORT).show();
  }catch(Exception e){Toast.makeText(this,"Error al guardar captura",Toast.LENGTH_LONG).show();}
  bitmap.recycle();
 }
 @Override protected void onDestroy(){web.destroy();super.onDestroy();}
}