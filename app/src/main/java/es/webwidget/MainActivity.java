package es.webwidget;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.SharedPreferences;
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
 Button modeButton, colorButton, refreshButton;
 Spinner widgetSelector;
 int widgetId=AppWidgetManager.INVALID_APPWIDGET_ID;
 boolean selecting=false;
 SharedPreferences settings(){return WidgetSettings.forId(this,widgetId);}
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  int[] initialIds=WidgetSettings.ids(this);
  if(initialIds.length==0){new AlertDialog.Builder(this).setMessage("Añade primero un widget a la pantalla de inicio y vuelve aquí para configurarlo.").setPositiveButton("Entendido",(d,w)->finish()).show();return;}
  widgetId=initialIds[0];
  WidgetSettings.ensure(this,widgetId);
  currentUrl=settings().getString("url","https://example.com");
  zoomPercent=settings().getInt("zoom",100);
  desktopMode=getPreferences(0).getBoolean("desktop",false);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.WHITE);
  widgetSelector=new Spinner(this);
  root.addView(widgetSelector,new LinearLayout.LayoutParams(-1,-2));
  address=new TextView(this);address.setText(currentUrl);address.setTextColor(Color.BLACK);address.setSingleLine(true);address.setEllipsize(TextUtils.TruncateAt.MIDDLE);address.setPadding(16,14,16,14);
  root.addView(address,new LinearLayout.LayoutParams(-1,-2));
  LinearLayout buttons=new LinearLayout(this);
  Button change=new Button(this);change.setText("Cambiar URL");
  Button capture=new Button(this);capture.setText("Actualizar widget");
  buttons.addView(change,new LinearLayout.LayoutParams(0,-2,1));buttons.addView(capture,new LinearLayout.LayoutParams(0,-2,1));root.addView(buttons);
  modeButton=new Button(this);
  modeButton.setText(desktopMode?"Vista: escritorio":"Vista: móvil");
  root.addView(modeButton,new LinearLayout.LayoutParams(-1,-2));
  colorButton=new Button(this);colorButton.setText("Fondo del widget");
  root.addView(colorButton,new LinearLayout.LayoutParams(-1,-2));
  colorButton.setOnClickListener(v->showBackgroundDialog());
  refreshButton=new Button(this);refreshButton.setText("Actualización automática");
  root.addView(refreshButton,new LinearLayout.LayoutParams(-1,-2));
  refreshButton.setOnClickListener(v->showRefreshDialog());
  RefreshWorker.schedule(this,widgetId);
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
  web.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView v,String s){applyViewport();handler.postDelayed(()->{applyZoom();snapshot();},1800);}});
  root.addView(web,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
  change.setOnClickListener(v->showUrlDialog());
  address.setOnClickListener(v->showUrlDialog());
  capture.setOnClickListener(v->snapshot());
  modeButton.setOnClickListener(v->{
   desktopMode=!desktopMode;
   getPreferences(0).edit().putBoolean("desktop",desktopMode).apply();
   modeButton.setText(desktopMode?"Vista: escritorio":"Vista: móvil");
   configureMode();
   web.loadUrl(currentUrl);
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
  int[] ids=WidgetSettings.ids(this);
  String[] names=new String[ids.length];
  for(int i=0;i<ids.length;i++){WidgetSettings.ensure(this,ids[i]);names[i]="Widget "+(i+1)+" — "+WidgetSettings.forId(this,ids[i]).getString("url","https://example.com");}
  widgetSelector.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));
  widgetSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
   public void onNothingSelected(AdapterView<?> parent){}
   public void onItemSelected(AdapterView<?> parent,android.view.View view,int position,long rowId){
    if(widgetId==ids[position])return;
    widgetId=ids[position];
    currentUrl=settings().getString("url","https://example.com");
    zoomPercent=settings().getInt("zoom",100);
    desktopMode=settings().getBoolean("desktop",false);
    address.setText(currentUrl);
    modeButton.setText(desktopMode?"Vista: escritorio":"Vista: móvil");
    zoomBar.setProgress(Math.max(0,Math.min(170,zoomPercent-30)));
    configureMode();
    web.loadUrl(currentUrl);
   }
  });
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
 void applyViewport(){
  String script=desktopMode
   ? "(function(){var m=document.querySelector('meta[name=viewport]');if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}m.setAttribute('content','width=1280, initial-scale=1, minimum-scale=0.1, maximum-scale=5, user-scalable=yes');})();"
   : "(function(){var m=document.querySelector('meta[name=viewport]');if(m){m.setAttribute('content','width=device-width, initial-scale=1');}})();";
  web.evaluateJavascript(script,null);
 }
 void showRefreshDialog(){
  final String[] options={"Desactivada","Cada 15 minutos","Cada 30 minutos","Cada 60 minutos","Cada 2 horas"};
  final int[] values={0,15,30,60,120};
  int selected=getPreferences(0).getInt("interval",30);
  int checked=2;
  for(int i=0;i<values.length;i++)if(values[i]==selected)checked=i;
  new AlertDialog.Builder(this).setTitle("Actualización automática")
   .setSingleChoiceItems(options,checked,(dialog,which)->{
    getPreferences(0).edit().putInt("interval",values[which]).apply();
    RefreshWorker.schedule(this,widgetId);
    Toast.makeText(this,"Frecuencia: "+options[which],Toast.LENGTH_LONG).show();
    dialog.dismiss();
   }).setNegativeButton("Cancelar",null).show();
 }
 void showBackgroundDialog(){
  final String[] labels={"Negro","Blanco","Gris oscuro","Gris claro","Azul oscuro","Transparente"};
  final int[] colors={Color.BLACK,Color.WHITE,Color.rgb(36,39,45),Color.rgb(225,228,232),Color.rgb(18,34,59),Color.TRANSPARENT};
  new AlertDialog.Builder(this).setTitle("Color del fondo del widget").setItems(labels,(d,which)->{
   getPreferences(0).edit().putInt("background",colors[which]).apply();
   WidgetProvider.refresh(this);
   Toast.makeText(this,"Fondo: "+labels[which],Toast.LENGTH_SHORT).show();
  }).show();
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
  try(FileOutputStream stream=openFileOutput(WidgetSettings.snapshotName(widgetId),MODE_PRIVATE)){
   bitmap.compress(Bitmap.CompressFormat.PNG,85,stream);
   WidgetProvider.refresh(this);
   Toast.makeText(this,"Widget actualizado",Toast.LENGTH_SHORT).show();
  }catch(Exception e){Toast.makeText(this,"Error al guardar captura",Toast.LENGTH_LONG).show();}
  bitmap.recycle();
 }
 @Override protected void onDestroy(){web.destroy();super.onDestroy();}
}