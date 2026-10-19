package es.webwidget;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;

public final class WidgetSettings {
 private WidgetSettings(){}
 public static SharedPreferences forId(Context c,int id){return c.getSharedPreferences("widget_"+id,Context.MODE_PRIVATE);}
 public static String snapshotName(int id){return "snapshot_"+id+".png";}
 public static int[] ids(Context c){return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,WidgetProvider.class));}
 public static void ensure(Context c,int id){
  SharedPreferences p=forId(c,id);
  if(p.contains("initialized"))return;
  SharedPreferences old=c.getSharedPreferences("MainActivity",Context.MODE_PRIVATE);
  SharedPreferences.Editor e=p.edit();
  e.putString("url",old.getString("url","https://example.com"));
  e.putInt("zoom",old.getInt("zoom",100));
  e.putBoolean("desktop",old.getBoolean("desktop",false));
  e.putInt("background",old.getInt("background",-1));
  e.putInt("interval",old.getInt("interval",30));
  e.putBoolean("initialized",true).apply();
  File legacy=new File(c.getFilesDir(),"snapshot.png");
  File target=new File(c.getFilesDir(),snapshotName(id));
  if(legacy.exists()&&!target.exists()){
   try(java.io.FileInputStream in=new java.io.FileInputStream(legacy);java.io.FileOutputStream out=new java.io.FileOutputStream(target)){
    byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
   }catch(Exception ignored){}
  }
 }
 public static void delete(Context c,int id){
  forId(c,id).edit().clear().apply();
  new File(c.getFilesDir(),snapshotName(id)).delete();
 }
}
