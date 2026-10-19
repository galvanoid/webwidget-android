package es.webwidget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

/** Configuration entry point used by the home-screen launcher's widget gear. */
public class WidgetConfigActivity extends MainActivity {
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  int widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
  if(widgetId!=AppWidgetManager.INVALID_APPWIDGET_ID) {
   Intent result=new Intent();
   result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);
   setResult(RESULT_OK,result);
  }
 }
 @Override protected void onStop() {
  super.onStop();
  WidgetProvider.refresh(this);
 }
}
