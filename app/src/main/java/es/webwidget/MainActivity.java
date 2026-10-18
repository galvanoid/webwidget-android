package es.webwidget;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.EditorInfo;
import android.text.InputType;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.*;
import java.io.FileOutputStream;

public class MainActivity extends Activity {
 WebView web;
 TextView address;
 Handler handler=new Handler(Looper.getMainLooper());
 String currentUrl;
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  currentUrl=getPreferences(0).getString("url","https://example.com");
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.WHITE);
  address=new TextView(this);address.setText(currentUrl);address.setTextColor(Color.BLACK);address.setSingleLine(true);address.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);address.setPadding(16,14,16,14);
  root.addView(address,new LinearLayout.LayoutParams(-1,-2));
  LinearLayout buttons=new LinearLayout(this);
  Button change=new Button(this);change.setText("Cambiar URL");
  Button capture=new Button(this);capture.setText("Actualizar widget");
  buttons.addView(change,new LinearLayout.LayoutParams(0,-2,1));buttons.addView(capture,new LinearLayout.LayoutParams(0,-2,1));root.addView(buttons);
  web=new WebView(this);web.setBackgroundColor(Color.WHITE);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);
  web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  web.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView v,String s){handler.postDelayed(()->snapshot(),2500);}});
  root.addView(web,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
  change.setOnClickListener(v->showUrlDialog());
  address.setOnClickListener(v->showUrlDialog());
  capture.setOnClickListener(v->snapshot());
  web.loadUrl(currentUrl);
 }
 void showUrlDialog(){
  EditText input=new EditText(this);
  input.setSingleLine(true);
  input.setSelectAllOnFocus(true);
  input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
  input.setImeOptions(EditorInfo.IME_ACTION_DONE);
  input.setText(currentUrl);
  LinearLayout wrapper=new LinearLayout(this);wrapper.setPadding(28,12,28,0);wrapper.addView(input,new LinearLayout.LayoutParams(-1,-2));
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Dirección del widget").setView(wrapper)
   .setNegativeButton("Cancelar",(d,w)->{})
   .setPositiveButton("Guardar y abrir",(d,w)->{
    String u=input.getText().toString().trim();
    if(!u.startsWith("https://")){Toast.makeText(this,"La URL debe empezar por https://",Toast.LENGTH_LONG).show();return;}
    currentUrl=u;
    getPreferences(0).edit().putString("url",u).apply();
    address.setText(u);
    web.loadUrl(u);
   }).create();
  dialog.getWindow();
  dialog.setOnShowListener(d->{input.requestFocus();input.selectAll();dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);});
  dialog.show();
  input.requestFocus();input.selectAll();
 }
 void snapshot(){
  if(web.getWidth()<1||web.getHeight()<1)return;
  int w=Math.min(800,web.getWidth());int h=Math.min(1000,web.getHeight());
  Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.RGB_565);
  Canvas canvas=new Canvas(bitmap);canvas.scale((float)w/web.getWidth(),(float)h/web.getHeight());web.draw(canvas);
  try(FileOutputStream stream=openFileOutput("snapshot.png",MODE_PRIVATE)){
   bitmap.compress(Bitmap.CompressFormat.PNG,85,stream);
   WidgetProvider.refresh(this);
   Toast.makeText(this,"Widget actualizado",Toast.LENGTH_SHORT).show();
  }catch(Exception e){Toast.makeText(this,"Error al guardar captura",Toast.LENGTH_LONG).show();}
  bitmap.recycle();
 }
 @Override protected void onDestroy(){web.destroy();super.onDestroy();}
}