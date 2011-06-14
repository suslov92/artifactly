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
import org.artifactly.client.service.LocalService;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class Artifactly extends Activity implements ApplicationConstants {

	private static final String ARTIFACTLY_URL = "file:///android_asset/artifactly.html";

	private static final String LOG_TAG = " ** A.A. **";

	// Preferences
	private static final String PREFS_NAME = "ArtifactlyPrefsFile";

	// Constants
	private static final String EMPTY_STRING = "";

	// JavaScript function constants
	private static final String JAVASCRIPT_PREFIX = "javascript:";
	private static final String JAVASCRIPT_FUNCTION_OPEN_PARENTHESIS = "(";
	private static final String JAVASCRIPT_FUNCTION_CLOSE_PARENTHESIS = ")";
	private static final String JAVASCRIPT_BRIDGE_PREFIX = "android";

	// JavaScript functions
	private static final String SHOW_SERVICE_RESULT = "showServiceResult";
	private static final String GET_ARTIFACTS_CALLBACK = "getArtifactsCallback";
	private static final String GET_ARTIFACT_CALLBACK = "getArtifactCallback";
	private static final String GET_ARTIFACTS_FOR_CURRENT_LOCATION_CALLBACK = "getArtifactsForCurrentLocationCallback";
	private static final String GET_LOCATIONS_OPTIONS_CALLBACK = "getLocationsOptionsCallback";
	private static final String GET_LOCATIONS_LIST_CALLBACK =  "getLocationsListCallback";
	private static final String SET_BACKGROUND_COLOR = "setBackgroundColor";
	private static final String RESET_WEBVIEW = "resetWebView";

	private WebView webView = null;

	private Handler mHandler = new Handler();

	// Access to service API
	private ServiceConnection serviceConnection = getServiceConnection();
	private LocalService localService = null;
	private boolean isBound = false;

	IntentFilter intentFilter = new IntentFilter(LOCATION_UPDATE_INTENT);
	BroadcastReceiver broadcastReceiver = null;
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.i(LOG_TAG, "onCreate()");

		setContentView(R.layout.main);
		
		// Setting up the WebView
		webView = (WebView) findViewById(R.id.webview);
		webView.getSettings().setJavaScriptEnabled(true);
		// Enable the following if we need JavaScript localStorage 
		//webView.getSettings().setDomStorageEnabled(true);

		// Disable the vertical scroll bar
		webView.setVerticalScrollBarEnabled(false);

		webView.addJavascriptInterface(new JavaScriptInterface(), JAVASCRIPT_BRIDGE_PREFIX);

		webView.setWebChromeClient(new WebChromeClient() {
			
			public boolean onConsoleMessage(ConsoleMessage cm) {
			
				Log.d("** A.A - JS **", cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId() );
				return true;
			}
		});
		
		webView.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageFinished (WebView view, String url) {

				/*
				 * Since we use jQueryMobile, this is fired on each page change. So
				 * we can use it to set the background color to what the user has selected
				 */
				try {
					
					callJavaScriptFunction(SET_BACKGROUND_COLOR, getBackgroundColor());
				}
				catch(Exception e) {

					Log.e(LOG_TAG, "ERROR: callJavaScriptFunction : SET_BACKGROUND_COLOR", e);
				}
			}
		});

		webView.loadUrl(ARTIFACTLY_URL);
		
		/*
		 * Calling startService so that the service keeps running. e.g. After application installation
		 * The start of the service at boot is handled via a BroadcastReceiver and the BOOT_COMPLETED action
		 */
		startService(new Intent(this, ArtifactlyService.class));

		// Bind to the service
		bindService(new Intent(this, ArtifactlyService.class), serviceConnection, BIND_AUTO_CREATE);
		isBound = true;

		// Instantiate the broadcast receiver
		broadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				
				Log.i(LOG_TAG, "Broadcast --> onReceive()");
				new GetArtifactsForCurrentLocation().execute();
			}
		};
	}

	/*
	 * Handle back button clicks in webview
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
	    if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
	    	webView.goBack();
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	public void onStart() {
		super.onStart();

		Log.i(LOG_TAG, "onStart()");

		if(!isBound) {
			
			// Connect to the local service API
			bindService(new Intent(this, ArtifactlyService.class), serviceConnection, BIND_AUTO_CREATE);
			isBound = true;
			Log.i(LOG_TAG, "onStart Binding service done");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public void onResume() {
		super.onResume();

		Log.i(LOG_TAG, "onResume()");

		if(!isBound) {
			// Connect to the local service API
			bindService(new Intent(this, ArtifactlyService.class), serviceConnection, BIND_AUTO_CREATE);
			isBound = true;
			Log.i(LOG_TAG, "onResume Binding service done");
		}

		// Register broadcast receiver
		registerReceiver(broadcastReceiver, intentFilter);
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	public void onPause() {
		super.onPause();

		Log.i(LOG_TAG, "onPause()");

		if(isBound) {

			isBound = false;
			try {
				
				unbindService(serviceConnection);
			}
			catch(IllegalArgumentException e) {
				
				Log.w(LOG_TAG, "onPause() -> unbindService() caused an IllegalArgumentException");
			}
		}
		
		// Unregister broadcast receiver
		unregisterReceiver(broadcastReceiver);
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onStop() {
		super.onStop();

		Log.i(LOG_TAG, "onStop()");

		// Reset the WebView to show the main page
		callJavaScriptFunction(RESET_WEBVIEW, "");
		
		if(isBound) {

			isBound = false;
			try {
				
				unbindService(serviceConnection);
			}
			catch(IllegalArgumentException e) {
				
				Log.w(LOG_TAG, "onStop() -> unbindService() caused an IllegalArgumentException");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();

		Log.i(LOG_TAG, "onDestroy()");

		if(isBound) {

			isBound = false;
			try {
			
				unbindService(serviceConnection);
			}
			catch(IllegalArgumentException e) {
				
				Log.w(LOG_TAG, "onDestroy() -> unbindService() caused an IllegalArgumentException");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	public void onNewIntent(Intent intent) {

		Log.i(LOG_TAG, "onNewIntent()");

		Bundle extras = intent.getExtras();

		if(null != extras && extras.containsKey(NOTIFICATION_INTENT_KEY)) {

			String data = extras.getString(NOTIFICATION_INTENT_KEY);
			callJavaScriptFunction(SHOW_SERVICE_RESULT, data);
		}
	}

	/*
	 * Helper method that gets the user defined background color and returns it
	 * as a JSONObject that can be sent to the WebView
	 */
	private String getBackgroundColor() {
		
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		JSONObject jsonObject = new JSONObject();

		try {
			
			jsonObject.put("bgc",  settings.getString(PREFERENCE_BACKGROUND_COLOR, PREFERENCE_BACKGROUND_COLOR_DEFAULT));
		}
		catch(JSONException e) {

			Log.e(LOG_TAG, "ERROR: json.put()", e);
		}
		
		return jsonObject.toString();
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
				webView.loadUrl(stringBuilder.toString());
			}
		});
	}

	// Define methods that are called from JavaScript
	public class JavaScriptInterface {
		
		public void setBackgroundColor(String color) {
			
			Log.i(LOG_TAG, "JS --> setBackground");
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(PREFERENCE_BACKGROUND_COLOR, color);
			editor.commit();
		}
		
		public void setRadius(int radius) {

			Log.i(LOG_TAG, "JS --> setRadius");

			if(PREFERENCE_RADIUS_DEFAULT < radius) {

				String message = String.format(getResources().getString(R.string.set_location_radius), radius);
				Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();

				SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
				SharedPreferences.Editor editor = settings.edit();
				editor.putInt(PREFERENCE_RADIUS, radius);
				editor.commit();
				
				// Refreshing the artifacts list
				new GetArtifactsForCurrentLocation().execute();
			}
		}

		public int getRadius() {

			Log.i(LOG_TAG, "JS --> getRadius");
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
			return settings.getInt(PREFERENCE_RADIUS, PREFERENCE_RADIUS_DEFAULT);
		}

		public void deleteArtifact(long id) {

			Log.i(LOG_TAG, "JS --> deleteArtifact");
			
			boolean isSuccess = localService.deleteArtifact(id);

			if(isSuccess) {

				Toast.makeText(getApplicationContext(), R.string.delete_artifact_success, Toast.LENGTH_SHORT).show();
				
				// Refreshing the artifacts list
				new GetArtifactsForCurrentLocation().execute();
			}
			else {

				Toast.makeText(getApplicationContext(), R.string.delete_artifact_failure, Toast.LENGTH_SHORT).show();
			}
		}


		public void createArtifact(String artifactName, String artifactData, String locationName, String locationLat, String locationLng) {

			Log.i(LOG_TAG, "JS --> createArtifact");
			
			if(null == artifactName || EMPTY_STRING.equals(artifactName)) {

				Toast.makeText(getApplicationContext(), R.string.create_artifact_name_error, Toast.LENGTH_SHORT).show();
				return;
			}
			
			if(null == locationName || EMPTY_STRING.equals(locationName)) {

				Toast.makeText(getApplicationContext(), R.string.create_artifact_location_name_error, Toast.LENGTH_SHORT).show();
				return;
			}
			
			boolean isSuccess = false;
			
			// If latitude and longitude are provided we use them, otherwise we use the current location
			// TODO: Add check if provided latitude and longitude are valid geo points
			
			// First we check if the user selected current location
			if(null != locationLat &&
			   !EMPTY_STRING.equals(locationLat) &&
			   null != locationLng &&
			   !EMPTY_STRING.equals(locationLng) &&
			   locationLat.equals("0") &&
			   locationLng.equals("0")) {
				
				isSuccess = localService.createArtifact(artifactName, artifactData, locationName);
				
			}
			else if(null != locationLat &&
					!EMPTY_STRING.equals(locationLat) &&
					null != locationLng &&
					!EMPTY_STRING.equals(locationLng) &&
					isDouble(locationLat) &&
					isDouble(locationLng)) {
				
				isSuccess = localService.createArtifact(artifactName, artifactData, locationName, locationLat, locationLng);
			}
			else {
				
				isSuccess = localService.createArtifact(artifactName, artifactData, locationName);
			}
			
			if(isSuccess) {

				Toast.makeText(getApplicationContext(), R.string.create_artifact_success, Toast.LENGTH_SHORT).show();
				
				// Refreshing the artifacts list
				new GetArtifactsForCurrentLocation().execute();
			}
			else {

				Toast.makeText(getApplicationContext(), R.string.create_artifact_failure, Toast.LENGTH_SHORT).show();
			}
		}

		public void showRadius() {
			
			Log.i(LOG_TAG, "JS --> showRadius");
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
			int radius = settings.getInt(PREFERENCE_RADIUS, PREFERENCE_RADIUS_DEFAULT);
			String message = String.format(getResources().getString(R.string.set_location_radius), radius);
			Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
		}

		public String getLocation() {

			Log.i(LOG_TAG, "JS --> getLocation");
			return localService.getLocation();
		}

		public void getArtifact(long id) {

			Log.i(LOG_TAG, "JS --> getArtifact");
			new GetArtifactTask().execute(new Long(id));
		}

		public void getArtifacts() {

			Log.i(LOG_TAG, "JS --> getArtifacts");
			new GetArtifactsTask().execute();
		}

		public void getArtifactsForCurrentLocation() {

			Log.i(LOG_TAG, "JS --> getArtifactsForCurrentLocation");
			new GetArtifactsForCurrentLocation().execute();
		}

		public boolean canAccessInternet() {

			Log.i(LOG_TAG, "JS --> canAccessInternet");
			boolean canAccessInternet = localService.canAccessInternet();

			if(!canAccessInternet) {
				Toast.makeText(getApplicationContext(), R.string.can_access_internet_error, Toast.LENGTH_LONG).show();
			}
			return canAccessInternet;
		}
		
		public String getGoogleSearchApiKey() {
			
			Log.i(LOG_TAG, "JS --> getGoogleSearchApiKey");
			return getResources().getString(R.string.google_search_api_key);
		}
		
		public void getLocations(String callback) {
			
			Log.i(LOG_TAG, "JS --> getLocations");
			new GetLocationsTask().execute(callback);
		}
		
		public void updateArtifact(String artId, String artName, String artData, String locId, String locName) {
			
			Log.i(LOG_TAG, "JS --> updateArtifact");
			new UpdateArtifactTask().execute(artId, artName, artData, locId, locName);
		}
	} 

	// Method that returns a service connection
	private ServiceConnection getServiceConnection() {

		return new ServiceConnection() {

			public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

				localService = (LocalService)iBinder;
				isBound = true;
				Log.i(LOG_TAG, "onServiceConnected called");
				
				// When application starts, we load the artifacts for the current location
				new GetArtifactsForCurrentLocation().execute();
			}

			public void onServiceDisconnected(ComponentName componentName) {

				localService = null;
				isBound = false;
				Log.i(LOG_TAG, "onServiceDisconnected called");
			}
		};
	}

	/*
	 * Helper method that checks if a string is a valid Double
	 */
	private boolean isDouble(String number) {
		
		try {
			
			Double.parseDouble(number);
		}
		catch (NumberFormatException nfe) {
			
			return false;
		}
		
		return true;
	}
	
	private class GetArtifactsForCurrentLocation extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {

			if(null == localService) {
				
				Log.e(LOG_TAG, "LocalService instance is null : getArtifactsForCurrentLocation()");
				callJavaScriptFunction(GET_ARTIFACTS_FOR_CURRENT_LOCATION_CALLBACK, "[]");
			}
			else {

				String result = localService.getArtifactsForCurrentLocation();
				callJavaScriptFunction(GET_ARTIFACTS_FOR_CURRENT_LOCATION_CALLBACK, result);
			}

			return null;
		}

	}

	private class GetArtifactsTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {

			if(null == localService) {

				Log.e(LOG_TAG, "LocalService instance is null : getArtifacts()");
				callJavaScriptFunction(GET_ARTIFACTS_CALLBACK, "[]");
			}
			else {

				String result = localService.getArtifacts();
				callJavaScriptFunction(GET_ARTIFACTS_CALLBACK, result);
			}
			
			return null;
		}	
	}
	
	private class GetLocationsTask extends AsyncTask<String, Void, Void> {
			
		@Override
		protected Void doInBackground(String... args) {

			if(null == localService) {

				Log.e(LOG_TAG, "LocalService instance is null : getLocations()");
				

				if("options".equals(args[0])) {
					
					callJavaScriptFunction(GET_LOCATIONS_OPTIONS_CALLBACK, "[]");
					
				}
				else if("list".equals(args[0])) {
					
					callJavaScriptFunction(GET_LOCATIONS_LIST_CALLBACK, "[]");
				}
			}
			else {

				String result = localService.getLocations();
				
				if("options".equals(args[0])) {
					
					callJavaScriptFunction(GET_LOCATIONS_OPTIONS_CALLBACK, result);
					
				}
				else if("list".equals(args[0])) {
					
					callJavaScriptFunction(GET_LOCATIONS_LIST_CALLBACK, result);
				}
				
			}
			
			return null;
		}	
	}

	private class GetArtifactTask extends AsyncTask<Long, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Long... ids) {

			if(null == localService) {
				
				Log.e(LOG_TAG, "LocalService instance is null : getAtrifact()");
				callJavaScriptFunction(GET_ARTIFACT_CALLBACK, "[]");
				return Boolean.FALSE;
			}
			else {
				
				String result = localService.getAtrifact(ids[0]);
				callJavaScriptFunction(GET_ARTIFACT_CALLBACK, result);
			}

			return Boolean.TRUE;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
		
			/*
			 * Show Toast in here because onPostExecute executes in UI thread
			 */
			if(!result.booleanValue()) {
				
				Toast.makeText(getApplicationContext(), R.string.get_artifact_failure, Toast.LENGTH_LONG).show();
			}
		}
	}
	
	private class UpdateArtifactTask extends AsyncTask<String, Void, Boolean> {
		
		@Override
		protected Boolean doInBackground(String... args) {

			if(null == localService) {
				
				Log.e(LOG_TAG, "LocalService instance is null : getAtrifact()");
				return Boolean.FALSE;
			}
			else if(null == args[0] ||
					null == args[1] ||
					null == args[3] ||
					null == args[4] ||
					"".equals(args[0]) ||
					"".equals(args[1]) ||
					"".equals(args[3]) ||
					"".equals(args[4])) {
			
				// Above, we check all the fields except args[2], which is the location data. It can be null/empty
				return Boolean.FALSE;
			}			
			else {
				
				return Boolean.valueOf(localService.updateArtifact(args[0], args[1], args[2], args[3], args[4]));
				
			}
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
		
			/*
			 * Show Toast in here because onPostExecute executes in UI thread
			 */
			if(result.booleanValue()) {
				
				new GetArtifactsForCurrentLocation().execute();
				Toast.makeText(getApplicationContext(), R.string.update_artifact_success, Toast.LENGTH_SHORT).show();
			}
			else {
				
				Toast.makeText(getApplicationContext(), R.string.update_artifact_failure, Toast.LENGTH_SHORT).show();
			}
		}
	}
}