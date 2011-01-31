/*
 * Copyright 2011 Thomas Amsler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package org.artifactly.client;

import org.artifactly.client.service.ArtifactlyService;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class Artifactly extends Activity implements ApplicationConstants {
	
	private static final String ARTIFACTLY_URL = "file:///android_asset/artifactly.html";
	
	private static final String LOG_TAG = "Artifactly Activity";
	
	// Preferences
	private static final String PREFS_NAME = "ArtifactlyPrefsFile";
	
	// JavaScript functions
	private static final String JAVASCRIPT_PREFIX = "javascript:";
	private static final String JAVASCRIPT_FUNCTION_OPEN_PARENTHESIS = "(";
	private static final String JAVASCRIPT_FUNCTION_CLOSE_PARENTHESIS = ")";
	private static final String SHOW_SERVICE_RESULT = "showServiceResult";
	private static final String JAVASCRIPT_BRIDGE_PREFIX = "android";
	
	private WebView webView;
	
	private Handler mHandler = new Handler();
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        // Create the service intent and start the service
        final Intent artifactlyService = new Intent (this, ArtifactlyService.class);
        startService(artifactlyService);
        
        // Setting up the WebView
        webView = (WebView) findViewById(R.id.webview);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageFinished(WebView view, String url) {
				// Nothing to do
			}
		});
		
		webView.loadUrl(ARTIFACTLY_URL);
		
		webView.addJavascriptInterface(new JavaScriptInterface(), JAVASCRIPT_BRIDGE_PREFIX);
    }
    
    @Override
    public void onStart() {
    	super.onStart();

    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	
    }
    
    @Override
    public void onNewIntent(Intent intent) {
    
    	Bundle extras = intent.getExtras();
    	
    	if(null != extras && extras.containsKey(NOTIFICATION_INTENT_KEY)) {
    		String data = extras.getString(NOTIFICATION_INTENT_KEY);
    		//Log.i(LOG_TAG, "Notification data = " + data);
      		callJavaScriptFunction(SHOW_SERVICE_RESULT, data);
    	}
    }

    /*
     * Helper method to call JavaScript methods
     */
    private void callJavaScriptFunction(final String functionName, final String json) {

    	mHandler.post(new Runnable() {

    		public void run() {
    			
    			StringBuilder stringBuilder = new StringBuilder();
    			stringBuilder.append(JAVASCRIPT_PREFIX);
    			stringBuilder.append(functionName);
    			stringBuilder.append(JAVASCRIPT_FUNCTION_OPEN_PARENTHESIS);
    			stringBuilder.append(json);
    			stringBuilder.append(JAVASCRIPT_FUNCTION_CLOSE_PARENTHESIS);
    			Log.i(LOG_TAG, stringBuilder.toString());
    			webView.loadUrl(stringBuilder.toString());
    		}
    	});
    }

    // Define the methods that are called from JavaScript
    public class JavaScriptInterface {
        
    	public void setRadius(int radius) {
    		
    		SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    		SharedPreferences.Editor editor = settings.edit();
    	    editor.putInt(PREFERENCE_RADIUS, radius);
    	    editor.commit();
    	}
    }  
}