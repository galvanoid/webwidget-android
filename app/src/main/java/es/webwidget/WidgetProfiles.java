package es.webwidget;

import android.webkit.WebView;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

/** Assign a persistent, isolated cookie/storage profile before using the WebView. */
public final class WidgetProfiles {
 private WidgetProfiles(){}
 public static boolean supported(){return WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE);}
 public static boolean apply(WebView web,int widgetId){
  if(!supported())return false;
  WebViewCompat.setProfile(web,"webwidget_"+widgetId);
  return true;
 }
}
