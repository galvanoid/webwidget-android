package es.webwidget;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.RemoteViews;
import java.io.File;

public class WidgetProvider extends AppWidgetProvider {
 public static void refresh(Context c) {
  AppWidgetManager manager=AppWidgetManager.getInstance(c);
  int[] ids=manager.getAppWidgetIds(new ComponentName(c,WidgetProvider.class));
  for(int id:ids) render(c,manager,id);
 }
 static void render(Context c,AppWidgetManager manager,int id) {
  RemoteViews rv=new RemoteViews(c.getPackageName(),R.layout.widget);
  SharedPreferences prefs=WidgetSettings.forId(c,id);
  rv.setInt(R.id.widget_root,"setBackgroundColor",prefs.getInt("background",android.graphics.Color.WHITE));
  File file=new File(c.getFilesDir(),WidgetSettings.snapshotName(id));
  Bitmap bitmap=BitmapFactory.decodeFile(file.getAbsolutePath());
  if(bitmap!=null){rv.setImageViewBitmap(R.id.screenshot,bitmap);rv.setViewVisibility(R.id.empty,View.GONE);}
  else {rv.setViewVisibility(R.id.empty,View.VISIBLE);}
  String url=prefs.getString("url","https://example.com");
  Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse(url));
  intent.addCategory(Intent.CATEGORY_BROWSABLE);
  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  // A distinct request code prevents reuse of the older MainActivity PendingIntent.
  PendingIntent pending=PendingIntent.getActivity(c,100000+id,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  rv.setOnClickPendingIntent(R.id.widget_root,pending);
  rv.setOnClickPendingIntent(R.id.screenshot,pending);
  rv.setOnClickPendingIntent(R.id.empty,pending);
  manager.updateAppWidget(id,rv);
 }
 @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){for(int id:ids){WidgetSettings.ensure(c,id);render(c,manager,id);RefreshWorker.schedule(c,id);}}
 @Override public void onDeleted(Context c,int[] ids){for(int id:ids){RefreshWorker.cancel(c,id);WidgetSettings.delete(c,id);}}
}