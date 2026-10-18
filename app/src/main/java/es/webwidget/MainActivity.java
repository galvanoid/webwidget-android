package es.webwidget;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.*;
import java.io.FileOutputStream;

public class MainActivity extends Activity {
 WebView web;
 EditText url;
 Handler handler=new Handler(Looper.getMainLooper());
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.WHITE);
  url=new EditText(this);url.setSingleLine(true);url.setHint("https://ejemplo.com");url.setText(getPreferences(0).getString("url","https://example.com"));
  root.addView(url,new LinearLayout.LayoutParams(-1,-2));
  LinearLayout buttons=new LinearLayout(this);
  Button load=new Button(this);load.setText("Abrir URL");
  Button capture=new Button(this);capture.setText("Actualizar widget");
  buttons.addView(load,new LinearLayout.LayoutParams(0,-2,1));buttons.addView(capture,new LinearLayout.LayoutParams(0,-2,1));root.addView(buttons);
  web=new WebView(this);web.setBackgroundColor(Color.WHITE);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);
  web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  web.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView v,String s){handler.postDelayed(()->snapshot(),2500);}});
  root.addView(web,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
  load.setOnClickListener(v->navigate());
  capture.setOnClickListener(v->snapshot());
  navigate();
 }
 void navigate(){
  String u=url.getText().toString().trim();
  if(!u.startsWith("https://")){Toast.makeText(this,"Introduce una URL HTTPS",Toast.LENGTH_LONG).show();return;}
  getPreferences(0).edit().putString("url",u).apply();web.loadUrl(u);
 }
 void snapshot(){
  if(web.getWidth()<1||web.getHeight()<1)return;
  // Limit bitmap size to keep RemoteViews within Android Binder transaction limits.
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