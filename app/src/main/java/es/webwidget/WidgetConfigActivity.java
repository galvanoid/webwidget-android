package es.webwidget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

/** Launcher configuration: finish only after user returns from settings. */
public class WidgetConfigActivity extends MainActivity {
 private int widgetId=AppWidgetManager.INVALID_APPWIDGET_ID;

 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  setResult(RESULT_CANCELED);
  widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
  if(widgetId==AppWidgetManager.INVALID_APPWIDGET_ID){
   finish();
  }
 }

 @Override public void finish() {
  if(widgetId!=AppWidgetManager.INVALID_APPWIDGET_ID){
   WidgetProvider.refresh(this);
   Intent result=new Intent();
   result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);
   setResult(RESULT_OK,result);
  }
  super.finish();
 }
}
