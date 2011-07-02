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
import android.content.res.Configuration;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

public class Artifactly extends Activity implements ApplicationConstants {

	private static final String ARTIFACTLY_URL = "file:///android_asset/artifactly.html#welcome";

	private static final String PROD_LOG_TAG = " ** A.A. **";
	//private static final String DEBUG_LOG_TAG = " ** DEBUG A.A. **";

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
	private static final String GET_LOCATIONS_LIST_CALLBACK = "getLocationsListCallback";
	private static final String CREATE_ARTIFACT_CALLBACK = "createArtifactCallback";
	private static final String RESET_WEBVIEW = "resetWebView";
	private static final String SHOW_OPTIONS_PAGE = "showOptionsPage";
	private static final String SHOW_MAP_PAGE = "showMapPage";
	private static final String SHOW_APP_INFO_PAGE = "showAppInfoPage";
	private static final String SHOW_WELCOME_PAGE = "showWelcomePage";
	private static final String SHOW_ORIENTATION_PORTRAIT_MODE = "showOrientationPortraitMode";
	private static final String SHOW_ORIENTATION_LANDSCAPE_MODE = "showOrientationLandscapeMode";
	

	private WebView webView = null;

	private Handler mHandler = new Handler();

	// Access to service API
	private ServiceConnection serviceConnection = getServiceConnection();
	private LocalService localService = null;
	private boolean isBound = false;

	IntentFilter locationUpdateIntentFilter = new IntentFilter(LOCATION_UPDATE_INTENT);
	IntentFilter connectivityIntentFilter = new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
	BroadcastReceiver locationUpdateBroadcastReceiver = null;
	BroadcastReceiver connectivityBroadcastReceiver = null;
	
	private boolean canAccessInternet = true;
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		
		// Setting up the WebView
		webView = (WebView) findViewById(R.id.webview);
		webView.getSettings().setJavaScriptEnabled(true);
		
		webView.addJavascriptInterface(new JavaScriptInterface(), JAVASCRIPT_BRIDGE_PREFIX);
		
		// Disable the vertical scroll bar
		webView.setVerticalScrollBarEnabled(false);

		webView.setWebChromeClient(new WebChromeClient() {
			
			public boolean onConsoleMessage(ConsoleMessage cm) {
			
				Log.d(PROD_LOG_TAG, cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId() );
				return true;
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
		locationUpdateBroadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				
				new GetArtifactsForCurrentLocationTask().execute();
			}
		};
		
		connectivityBroadcastReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				
				canAccessInternet = hasConnectivity();
			}
		};
		
		// Initialize connectivity flag
		canAccessInternet = hasConnectivity();
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.opitons, menu);
	    return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    
		// Handle item selection
	    switch (item.getItemId()) {
	    case R.id.options:
	        callJavaScriptFunction(SHOW_OPTIONS_PAGE);
	        return true;
	    case R.id.map:
	    	callJavaScriptFunction(SHOW_MAP_PAGE);
	        return true;
	    case R.id.info:
	    	callJavaScriptFunction(SHOW_APP_INFO_PAGE);
	    	return true;
	    case R.id.welcome:
	    	callJavaScriptFunction(SHOW_WELCOME_PAGE);
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
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

		if(!isBound) {
			
			// Connect to the local service API
			bindService(new Intent(this, ArtifactlyService.class), serviceConnection, BIND_AUTO_CREATE);
			isBound = true;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public void onResume() {

		super.onResume();

		if(!isBound) {

			// Connect to the local service API
			bindService(new Intent(this, ArtifactlyService.class), serviceConnection, BIND_AUTO_CREATE);
			isBound = true;
		}
		
		// Register broadcast receivers
		registerReceiver(locationUpdateBroadcastReceiver, locationUpdateIntentFilter);
		registerReceiver(connectivityBroadcastReceiver, connectivityIntentFilter);
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	public void onPause() {

		super.onPause();

		if(isBound) {

			isBound = false;

			try {
				
				unbindService(serviceConnection);
			}
			catch(IllegalArgumentException e) {
				
				Log.w(PROD_LOG_TAG, "onPause() -> unbindService() caused an IllegalArgumentException", e);
			}
		}
		
		// Unregister broadcast receivers
		unregisterReceiver(locationUpdateBroadcastReceiver);
		unregisterReceiver(connectivityBroadcastReceiver);
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onStop() {
		
		super.onStop();

		// Reset the WebView to show the main page
		callJavaScriptFunction(RESET_WEBVIEW);
		
		if(isBound) {

			isBound = false;

			try {
				
				unbindService(serviceConnection);
			}
			catch(IllegalArgumentException e) {
				
				Log.w(PROD_LOG_TAG, "onStop() -> unbindService() caused an IllegalArgumentException", e);
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

		if(isBound) {

			isBound = false;

			try {
			
				unbindService(serviceConnection);
			}
			catch(IllegalArgumentException e) {
				
				Log.w(PROD_LOG_TAG, "onDestroy() -> unbindService() caused an IllegalArgumentException", e);
			}
		}
	}

	@Override
	public void onConfigurationChanged (Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		switch(newConfig.orientation) {
		
			case Configuration.ORIENTATION_PORTRAIT:
				
				callJavaScriptFunction(SHOW_ORIENTATION_PORTRAIT_MODE);
			break;
			case Configuration.ORIENTATION_LANDSCAPE:
				
				callJavaScriptFunction(SHOW_ORIENTATION_LANDSCAPE_MODE);
			break;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	public void onNewIntent(Intent intent) {

		Bundle extras = intent.getExtras();

		if(null != extras && extras.containsKey(NOTIFICATION_INTENT_KEY)) {

			String data = extras.getString(NOTIFICATION_INTENT_KEY);
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
				
				try {
				
					webView.loadUrl(stringBuilder.toString());
				}
				catch(Exception e) {
					
					Log.e(PROD_LOG_TAG, "callJavaScriptFunction(...)", e);
				}
			}
		});
	}
	
	/*
	 * Helper method to call JavaScript methods without JSON data
	 */
	private void callJavaScriptFunction(final String functionName) {

		callJavaScriptFunction(functionName, "");
	}

	// Define methods that are called from JavaScript
	public class JavaScriptInterface {
		
		public void setRadius(int radius) {

			if(PREFERENCE_RADIUS_MIN <= radius) {

				SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
				SharedPreferences.Editor editor = settings.edit();
				editor.putInt(PREFERENCE_RADIUS, radius);
				editor.commit();
				
				String message = String.format(getResources().getString(R.string.set_location_radius), radius);
				Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
			}
		}

		public void setSoundNotificationPreference(boolean preference) {

			SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean(PREFERENCE_SOUND_NOTIFICATION, preference);
			editor.commit();
			
			if(preference) {
				
				Toast.makeText(getApplicationContext(), R.string.preference_sound_alert_on, Toast.LENGTH_SHORT).show();
			}
			else {
				
				Toast.makeText(getApplicationContext(), R.string.preference_sound_alert_off, Toast.LENGTH_SHORT).show();
			}
		}

		public void setLoadStaticMapPreference(boolean preference) {
			
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean(PREFERENCE_LOAD_STATIC_MAP, preference);
			editor.commit();
			
			if(preference) {
				
				Toast.makeText(getApplicationContext(), R.string.preference_load_maps_on, Toast.LENGTH_SHORT).show();
			}
			else {
				
				Toast.makeText(getApplicationContext(), R.string.preference_load_maps_off, Toast.LENGTH_SHORT).show();
			}
		}
		
		public String getPreferences() {

			SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
			
			JSONObject preferences = new JSONObject();
			
			try {
				
				// Sound notification preference
				preferences.put("soundNotification", settings.getBoolean(PREFERENCE_SOUND_NOTIFICATION, PREFERENCE_SOUND_NOTIFICATION_DEFAULT));

				// Radius preference
				preferences.put("radius", settings.getInt(PREFERENCE_RADIUS, PREFERENCE_RADIUS_DEFAULT));

				// Static map loading preference
				preferences.put("loadStaticMap", settings.getBoolean(PREFERENCE_LOAD_STATIC_MAP, PREFERENCE_LOAD_STATIC_MAP_DEFAULT));

			}
			catch(JSONException e) {

				Toast.makeText(getApplicationContext(), R.string.loading_preferences_error, Toast.LENGTH_SHORT).show();
			}

			return preferences.toString();
		}
		
		public void deleteArtifact(String artifactId, String locationId) {

			new DeleteArtifactTask().execute(artifactId, locationId);
		}
		
		public void deleteLocation(String locationId) {
			
			new DeleteLocationTask().execute(locationId);
		}

		public void createArtifact(String artifactName, String artifactData, String locationName, String locationLat, String locationLng) {
			
			new CreateArtifactTask().execute(artifactName, artifactData, locationName, locationLat, locationLng);
		}

		public void showRadius() {
			
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
			int radius = settings.getInt(PREFERENCE_RADIUS, PREFERENCE_RADIUS_DEFAULT);
			String message = String.format(getResources().getString(R.string.set_location_radius), radius);
			Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
		}

		public String getLocation() {

			Location location = localService.getLocation();
			return  locationToJSON(location);
			
		}

		public void getArtifact(String id) {

			new GetArtifactTask().execute(id);
		}

		public void getArtifacts() {

			new GetArtifactsTask().execute();
		}

		public void getArtifactsForCurrentLocation() {

			new GetArtifactsForCurrentLocationTask().execute();
		}

		public boolean canAccessInternet() {

			return canAccessInternet;
		}
		
		public boolean canLoadStaticMap() {
			
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
			boolean canLoadStaticMap = settings.getBoolean(PREFERENCE_LOAD_STATIC_MAP, PREFERENCE_LOAD_STATIC_MAP_DEFAULT);
			
			return canLoadStaticMap && canAccessInternet;
		}
		
		public String getGoogleSearchApiKey() {
			
			return getResources().getString(R.string.google_search_api_key);
		}
		
		public void getLocations(String callback) {
			
			new GetLocationsTask().execute(callback);
		}
		
		public void updateArtifact(String artId, String artName, String artData, String locId, String locName) {
			
			new UpdateArtifactTask().execute(artId, artName, artData, locId, locName);
		}
		
		public void updateLocation(String locId, String locName, String locLat, String locLng) {
			
			new UpdateLocationTask().execute(locId, locName, locLat, locLng);
		}
	} 

	// Method that returns a service connection
	private ServiceConnection getServiceConnection() {

		return new ServiceConnection() {

			public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

				localService = (LocalService)iBinder;
				isBound = true;
			}

			public void onServiceDisconnected(ComponentName componentName) {

				localService = null;
				isBound = false;
			}
		};
	}

	/*
	 * Helper method that turns a Location object into JSON
	 */
	private String locationToJSON(Location location) {
		
		JSONObject data = new JSONObject();
		
		if(null == location) {

			Toast.makeText(getApplicationContext(), R.string.get_location_error, Toast.LENGTH_LONG).show();
			
			try {
				
				data.put("locLat", 0.0d);
				data.put("locLng", 0.0d);
				data.put("locAccuracy", 0.0d);
			}
			catch (JSONException e) {

				Log.e(PROD_LOG_TAG, "Error while populating JSONArray", e);
			}
		}
		else {

			try {

				data.put("locLat", location.getLatitude());
				data.put("locLng", location.getLongitude());
				data.put("locAccuracy", location.getAccuracy());
			}
			catch (JSONException e) {

				Log.e(PROD_LOG_TAG, "Error while population JSONArray", e);
			}
		}
		
		return data.toString();
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
	
	/*
	 * Helper method to check if there is network connectivity
	 */
	private boolean hasConnectivity() {
		
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    
		if(null == connectivityManager) {
			
			return false;
		}
		
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		
		if(null != networkInfo && networkInfo.isAvailable() && networkInfo.isConnected()) {
			
			return true;
		}
		else {
			
			return false;
		}
	}
	
	
	private class CreateArtifactTask extends AsyncTask<String, Void, Byte> {

		/*
		 * args[0] = artifactName
		 * args[1] = artifactData
		 * args[2] = locationName
		 * args[3] = locationLat
		 * args[4] = locationLng
		 * 
		 */
		@Override
		protected Byte doInBackground(String... args) {
			
			if(null == args[0] || EMPTY_STRING.equals(args[0])) {

				return Byte.valueOf(ARTIFACT_NAME_ERROR);
			}
			
			if(null == args[2] || EMPTY_STRING.equals(args[2])) {

				return Byte.valueOf(LOCATION_NAME_ERROR);
			}
			
			byte state = 0;
			
			// If latitude and longitude are provided we use them, otherwise we use the current location
			// TODO: Add check if provided latitude and longitude are valid geo points
			
			// First we check if the user selected current location
			if(null != args[3] &&
			   !EMPTY_STRING.equals(args[3]) &&
			   null != args[4] &&
			   !EMPTY_STRING.equals(args[4]) &&
			   args[3].equals("0") &&
			   args[4].equals("0")) {
				
				state = localService.createArtifact(args[0], args[1], args[2]);
				
			}
			else if(null != args[3] &&
					!EMPTY_STRING.equals(args[3]) &&
					null != args[4] &&
					!EMPTY_STRING.equals(args[4]) &&
					isDouble(args[3]) &&
					isDouble(args[4])) {
				
				state = localService.createArtifact(args[0], args[1], args[2], args[3], args[4]);
			}
			else {
				
				state = localService.createArtifact(args[0], args[1], args[2]);
			}
			
			return Byte.valueOf(state);
		}
		
		@Override
		protected void onPostExecute(Byte result) {
		
			byte state = result.byteValue();
			boolean returnValue = false;
			
			if((state ^ CREATE_ARTIFACT_LOCATION_ERROR) == IS_MATCH) {
				
				Toast.makeText(getApplicationContext(), R.string.create_artifact_failure, Toast.LENGTH_LONG).show();
				returnValue = false;
			}
			else if((state ^ CHOOSE_DIFFERENT_LOC_NAME) == IS_MATCH) {
				
				Toast.makeText(getApplicationContext(), R.string.create_artifact_provide_different_location_name, Toast.LENGTH_LONG).show();
				returnValue = false;
			}
			else if((state ^ ARTIFACT_AND_LOCATION_EXIST) == IS_MATCH) {
				
				Toast.makeText(getApplicationContext(), R.string.create_artifact_already_exists, Toast.LENGTH_LONG).show();
				returnValue = false;
			}
			else if((state ^ ARTIFACT_NAME_ERROR) == IS_MATCH) {
				
				Toast.makeText(getApplicationContext(), R.string.create_artifact_name_error, Toast.LENGTH_SHORT).show();
				returnValue = false;
			}
			else if((state ^ LOCATION_NAME_ERROR) == IS_MATCH) {
				
				Toast.makeText(getApplicationContext(), R.string.create_artifact_location_name_error, Toast.LENGTH_SHORT).show();
				returnValue = false;
			}
			else {
				
				new GetArtifactsForCurrentLocationTask().execute();
				Toast.makeText(getApplicationContext(), R.string.create_artifact_success, Toast.LENGTH_SHORT).show();
				returnValue = true;
			}
			
			JSONObject data = new JSONObject();
			try {
				
				data.put("isSuccess", returnValue);
			}
			catch (JSONException e) {
				
				Log.e(PROD_LOG_TAG, "ERROR: json.put()", e);
			}
			
			callJavaScriptFunction(CREATE_ARTIFACT_CALLBACK, data.toString());
		}
	}
	private class GetArtifactsForCurrentLocationTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {

			/*
			 * Check if the localService is null, in which case we wait for some time and try again
			 */
			if(null == localService) {
				
				try {

					// Give enough time so that the localService can bind
					Thread.sleep(400);
				}
				catch(Exception e) {

					Log.w(PROD_LOG_TAG, "EXCEPTION: Thread.sleep(N)", e);
				}
			}
			
			if(null != localService) {

				String result = localService.getArtifactsForCurrentLocation();
				callJavaScriptFunction(GET_ARTIFACTS_FOR_CURRENT_LOCATION_CALLBACK, result);
				return Boolean.TRUE;
			}
			else {
				
				callJavaScriptFunction(GET_ARTIFACTS_FOR_CURRENT_LOCATION_CALLBACK, "[]");
				return Boolean.FALSE;
			}
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
		
			/*
			 * Show Toast in here because onPostExecute executes in UI thread
			 */
			if(!result.booleanValue()) {
				
				Toast.makeText(getApplicationContext(), R.string.get_artifact_for_current_location_error, Toast.LENGTH_LONG).show();
			}
		}
	}

	private class GetArtifactsTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {

			if(null == localService) {

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

	private class GetArtifactTask extends AsyncTask<String, Void, Boolean> {

		@Override
		protected Boolean doInBackground(String... args) {

			if(null == localService) {
				
				callJavaScriptFunction(GET_ARTIFACT_CALLBACK, "{}");
				return Boolean.FALSE;
			}
			else {
				
				String result = localService.getAtrifact(args[0]);
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
				
				new GetArtifactsForCurrentLocationTask().execute();
				Toast.makeText(getApplicationContext(), R.string.update_artifact_success, Toast.LENGTH_SHORT).show();
			}
			else {
				
				Toast.makeText(getApplicationContext(), R.string.update_artifact_failure, Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	private class UpdateLocationTask extends AsyncTask<String, Void, Boolean> {
		
		@Override
		protected Boolean doInBackground(String ... args) {
			
			if(null == localService) {
				
				return Boolean.FALSE;
			}
			else if(null == args[0] || null == args[1] || null == args[2] || null == args[3] ||
					"".equals(args[0]) || "".equals(args[1]) || "".equals(args[2]) || "".equals(args[3])) {
				
				// Above, we check all the fields. They cannot be null or empty
				return Boolean.FALSE;
			}
			else {
				
				return Boolean.valueOf(localService.updateLocation(args[0], args[1], args[2], args[3]));
			}
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
		
			/*
			 * Show Toast in here because onPostExecute executes in UI thread
			 */
			if(result.booleanValue()) {
				
				new GetArtifactsForCurrentLocationTask().execute();
				new GetLocationsTask().execute("list");
				Toast.makeText(getApplicationContext(), R.string.update_location_success, Toast.LENGTH_SHORT).show();
			}
			else {
				
				Toast.makeText(getApplicationContext(), R.string.update_location_failure, Toast.LENGTH_SHORT).show();
			}
		}
	}
	private class DeleteLocationTask extends AsyncTask<String, Void, Integer> {

		@Override
		protected Integer doInBackground(String... args) {

			if(null == localService || null == args[0] || "".equals(args[0])) {

				// Above, we check the parameter. It cannot be null or empty
				return Integer.valueOf(-2);
			}
			else {

				return Integer.valueOf(localService.deleteLocation(args[0]));
			}
		}

		@Override
		protected void onPostExecute(Integer result) {

			switch(result.intValue()) {
			case -2:
			case -1:
				Toast.makeText(getApplicationContext(), R.string.delete_location_failure, Toast.LENGTH_LONG).show();
				break;
			case 0:
				Toast.makeText(getApplicationContext(), R.string.delete_location_partial, Toast.LENGTH_LONG).show();
				break;
			case 1:
				new GetLocationsTask().execute("list");
				new GetArtifactsForCurrentLocationTask().execute();
				Toast.makeText(getApplicationContext(), R.string.delete_location_success, Toast.LENGTH_SHORT).show();
				break;
			default:
				Log.e(PROD_LOG_TAG, "ERROR: unexpected deleteLocation() status");
			}
		}
	}
	
	private class DeleteArtifactTask extends AsyncTask<String, Void, Integer> {

		@Override
		protected Integer doInBackground(String... args) {
			
			if(null == localService || null == args[0] || null == args[1] || "".equals(args[0]) || "".equals(args[1])) {
				
				// Above, we check the parameters. They cannot be null or empty
				return Integer.valueOf(-2);
			}
			else {
				
				return Integer.valueOf(localService.deleteArtifact(args[0], args[1]));
			}
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			
			switch(result.intValue()) {
			case -2:
			case -1:
				Toast.makeText(getApplicationContext(), R.string.delete_artifact_failure, Toast.LENGTH_LONG).show();
				break;
			case 0:
				Toast.makeText(getApplicationContext(), R.string.delete_artifact_partial, Toast.LENGTH_LONG).show();
				break;
			case 1:
				new GetArtifactsForCurrentLocationTask().execute();
				Toast.makeText(getApplicationContext(), R.string.delete_artifact_success, Toast.LENGTH_SHORT).show();
				break;
			default:
				Log.e(PROD_LOG_TAG, "ERROR: unexpected deleteArtifact() status");
			}
		}
	}
}