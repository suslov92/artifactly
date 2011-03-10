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

package org.artifactly.client.service;

import java.util.Date;

import org.artifactly.client.ApplicationConstants;
import org.artifactly.client.Artifactly;
import org.artifactly.client.R;
import org.artifactly.client.content.DbAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class ArtifactlyService extends Service implements OnSharedPreferenceChangeListener, ApplicationConstants {

	// Logging
	private static final String LOG_TAG = "** A.S. **";

	// Preferences
	private static final String PREFS_NAME = "ArtifactlyPrefsFile";
	private SharedPreferences settings;

	// Location constants
	private static final int LOCATION_MIN_TIME = 300000; // 5 min
	private static final int LOCATION_MIN_DISTANCE = 100; // 100 m
	private static final String DISTANCE = "dist";

	// Location radius
	private int radius = 100;
	
	// Location expiration delta is used to determine if the current location
	// is current enough. If it's not, we enable the GPS listener if available 
	private static final long LOCATION_TIME_EXPIRATION_DELTA = 300000; // 5 min
	
	// The new location can only be older than the current location plus this delta
	// in order for it to be considered slightly inaccurate 
	private static final long LOCATION_TIME_ALLOWED_DELTA = 300000; // 5 min

	// Notification constants
	private static final int NOTIFICATION_ID = 95691;

	// Context Resources
	private String NOTIFICATION_TICKER_TEXT;
	private String NOTIFICATION_CONTENT_TITLE;

	// Managers
	private LocationManager locationManager;
	private NotificationManager notificationManager;

	// Location provider
	private String mainLocationProviderName;
	
	// Location listeners
	private LocationListener gpsLocationListener = getNewLocationListener();
	private LocationListener networkLocationListener = getNewLocationListener();
	
	// Location state
	private boolean isGpsListenerEnabled = false;
	
	// DB adapter
	private DbAdapter dbAdapter;

	// Keeping track of current location
	private Location currentLocation;

	// Binder access to service API
	private IBinder localServiceBinder;

	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		
		Log.i(LOG_TAG, "Service onBind called");
		localServiceBinder = new LocalServiceImpl(this);
		return localServiceBinder;
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onUnbind(android.content.Intent)
	 */
	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(LOG_TAG, "Service onUnbind called");
		return super.onUnbind(intent);
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onRebind(android.content.Intent)
	 */
	@Override
	public void onRebind(Intent intent) {
		Log.i(LOG_TAG, "Service onRebind called");
		super.onRebind(intent);
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();

		Log.i(LOG_TAG, "Service onCreate() called");
		
		// Getting the constants from resources
		NOTIFICATION_TICKER_TEXT = getResources().getString(R.string.notification_ticker_text);
		NOTIFICATION_CONTENT_TITLE = getResources().getString(R.string.notification_content_title);

		// Getting shared preferences such as search radius, etc.
		settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		settings.registerOnSharedPreferenceChangeListener(this);
		radius = settings.getInt(PREFERENCE_RADIUS, PREFERENCE_RADIUS_DEFAULT);
		Log.i(LOG_TAG, "Preferences radius = " + radius);

		// Setting up the database
		dbAdapter = new DbAdapter(this);

		// Setting up the notification manager
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// Setting up the location manager
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// Register location listener and getting last known location
		registerLocationListener();
	}


	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		Log.i(LOG_TAG, "Service onDestroy called");
		settings.unregisterOnSharedPreferenceChangeListener(this);
	}

	/*
	 * (non-Javadoc)
	 * @see android.content.SharedPreferences.OnSharedPreferenceChangeListener#onSharedPreferenceChanged(android.content.SharedPreferences, java.lang.String)
	 */
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

		int newRadius = sharedPreferences.getInt(key, LOCATION_MIN_DISTANCE);

		// TODO: Determine what the appropriate minimum radius is.
		if(0 < newRadius) {

			Log.i(LOG_TAG, "Radius was set to " + newRadius);
			radius = newRadius;
		}
		else {

			Log.w(LOG_TAG, "Radius update was ignored because new radisu < 1");
		}
	}

	/*
	 * DbAdater getter method
	 */
	protected DbAdapter getDbAdapter() {
		
		return dbAdapter;
	}

	/*
	 * Dispatch method for local service
	 */
	protected void startLocationTracking() {

		registerLocationListener();
	}

	/*
	 * Dispatch method for local service
	 */
	protected void stopLocationTracking() {

		unregisterLocationListeners();
	}

	/*
	 * Dispatch method for local service
	 */
	protected Location getLocation() {

		return currentLocation;
	}
	
	/*
	 * Register location listener
	 * 1. network
	 * 2. gps
	 * 
	 * Also setting currentLocation via last known location
	 */
	private void registerLocationListener() {
		
		// If location manager is null, we just return
		if(null == locationManager) {
			Log.e(LOG_TAG, "LocationManager instance is null");
			return;
		}

		try {

			// First, use network provided location if available
			if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

				mainLocationProviderName = LocationManager.NETWORK_PROVIDER;
				locationManager.requestLocationUpdates(mainLocationProviderName, LOCATION_MIN_TIME, LOCATION_MIN_DISTANCE, networkLocationListener);
				currentLocation = locationManager.getLastKnownLocation(mainLocationProviderName);
				Log.i(LOG_TAG, "registerLocationListener() -> NETWORK_PROVIDER");
			}
			else if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

				mainLocationProviderName = LocationManager.GPS_PROVIDER;
				locationManager.requestLocationUpdates(mainLocationProviderName, LOCATION_MIN_TIME, LOCATION_MIN_DISTANCE, gpsLocationListener);
				currentLocation = locationManager.getLastKnownLocation(mainLocationProviderName);
				Log.i(LOG_TAG, "registerLocationListener() -> GPS_PROVIDER");
			}
			else {

				Log.w(LOG_TAG, "All available location providers [network, gps] are not available");
			}
		}
		catch(IllegalArgumentException iae) {
			
			Log.w(LOG_TAG, "registerLocationListener() IllegalArgumentException");
		}
		catch(SecurityException se) {
			
			Log.w(LOG_TAG, "registerLocationListener() SecurityException");

		}
		catch(RuntimeException re) {
			
			Log.w(LOG_TAG, "registerLocationListener() RuntimeException");
		}
	}
	
	/*
	 * Unregister location listeners
	 * 
	 */
	private void unregisterLocationListeners() {
		
		if(null == locationManager) {
			Log.e(LOG_TAG, "LocationManager instance is null");
			return;
		}

		try {
		
			locationManager.removeUpdates(networkLocationListener);
			locationManager.removeUpdates(gpsLocationListener);
		}
		catch(IllegalArgumentException iae) {
			
			Log.w(LOG_TAG, "IllegalArgumentException thrown while removing location listener updates");
		}
	}

	/*
	 * This method sends a message using Android's notification notification manager.
	 * It set up an intent so that the UI can be launched from within the notification message.
	 */
	private void sendNotification(String message) {

		Intent notificationIntent = new Intent(this, Artifactly.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		notificationIntent.putExtra(NOTIFICATION_INTENT_KEY, message);
		// Setting FLAG_UPDATE_CURRENT, so that the extra content is updated for each notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		Notification notification = new Notification(R.drawable.icon, NOTIFICATION_TICKER_TEXT, System.currentTimeMillis());
		notification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL | Notification.DEFAULT_SOUND;
		notification.setLatestEventInfo(this, NOTIFICATION_CONTENT_TITLE, message, contentIntent);
		notificationManager.notify(NOTIFICATION_ID, notification);
	}

	/*
	 * Method that retrieves all artifacts from the database that match the current location
	 * 
	 * @return All the artifacts in JSON format
	 */
	private String getArtifactsForCurrentLocation() {

		// Getting all the locations
		Cursor cursor = dbAdapter.select();
		cursor.moveToFirst();

		// Determine the table column indexes 
		int longitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE]);
		int latitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE]);
		int nameColumnIndex = cursor.getColumnIndex(DbAdapter.ART_FIELDS[DbAdapter.ART_NAME]);
		int dataColumnIndex = cursor.getColumnIndex(DbAdapter.ART_FIELDS[DbAdapter.ART_DATA]);

		// JSON array that holds the result
		JSONArray items = new JSONArray();
		
		int rowCount = cursor.getCount();
		Log.i(LOG_TAG, "row count = " + rowCount);

		// If there are no results, we return
		if(0 == rowCount) {

			cursor.close();
			return null;
		}

		/*
		 *  Iterating over all result and calculate the distance between
		 *  radius, we notify the user that there is an artifact.
		 */
		for(;cursor.isAfterLast() == false; cursor.moveToNext()) {

			// Getting latitude and longitude
			String storedLatitude = cursor.getString(latitudeColumnIndex).trim();
			String storedLongitude = cursor.getString(longitudeColumnIndex).trim();

			float[] distanceResult = new float[1];
			Location.distanceBetween(currentLocation.getLatitude(),
					currentLocation.getLongitude(),
					Double.parseDouble(storedLatitude),
					Double.parseDouble(storedLongitude), distanceResult);

			Log.i(LOG_TAG, "distanceDifference = " + distanceResult[0]);

			if(distanceResult[0] <= radius) {

				JSONObject item = new JSONObject();

				try {
					
					item.put(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE], storedLatitude);
					item.put(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE], storedLongitude);
					item.put(DbAdapter.ART_FIELDS[DbAdapter.ART_NAME], cursor.getString(nameColumnIndex));
					item.put(DbAdapter.ART_FIELDS[DbAdapter.ART_DATA], cursor.getString(dataColumnIndex));
					item.put(DISTANCE, Float.toString(distanceResult[0]));
				}
				catch (JSONException e) {
					Log.w(LOG_TAG, "Error while populating JSONObject");
				}
				
				items.put(item);
			}
		}

		cursor.close();

		if(items.length() == 0) {
			
			return null;
		}
		else {

			return items.toString();
		}
	}

	/*
	 * Method that determines if a new location is more accurate the the currently saved location
	 */
	private boolean isMoreAccurate(Location newLocation) {

		if(null == currentLocation && null != newLocation) {
			return true;
		}

		if(null != currentLocation && null == newLocation) {
			return false;
		}
		
		// Check if the new location's accuracy lies within the defined search radius
		if(newLocation.hasAccuracy() && newLocation.getAccuracy() > radius) {
			Log.i(LOG_TAG, "The new location accuracy is less accurate then the defined radius");
			return false;
		}
		
		boolean isMoreAccurate = false;
		boolean isMoreCurrent = false;
		boolean isSlightlyLessCurrent = false;
		
		// Check if the new location is more accurate 
		if(currentLocation.hasAccuracy() && newLocation.hasAccuracy()) {

			float accuracyDelta = currentLocation.getAccuracy() - newLocation.getAccuracy();
			isMoreAccurate = accuracyDelta >= 0;
		}
		
		// Check if the new location is more current in terms of location fix time
		long locationTimeDelta = currentLocation.getTime() - newLocation.getTime();
		isMoreCurrent = locationTimeDelta <= 0;
		
		isSlightlyLessCurrent = (locationTimeDelta > 0 && locationTimeDelta < LOCATION_TIME_ALLOWED_DELTA);
		
		if(isMoreAccurate && isMoreCurrent) {
			
			Log.i(LOG_TAG, "New location is more accurate and more current");
			return true;
		}
		else if(isMoreAccurate && isSlightlyLessCurrent) {
			
			Log.i(LOG_TAG, "New location is more accurate and slightly less current");
			return true;
		}
		
		return false;
	}
	
	
	/*
	 * Create a location listener
	 */
	private LocationListener getNewLocationListener() {
		
		return new LocationListener() {

			public void onLocationChanged(final Location location) {
				
				locationChanged(location);
			}

			public void onProviderDisabled(String provider) {
				
				locationProviderDisabled(provider);
				
			}

			public void onProviderEnabled(String provider) {
				
				locationProviderEnabled(provider);
				
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {
				
				locationStatusChanged(provider, status, extras);
			}
		};
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onLocationChanged(android.location.Location)
	 */
	private void locationChanged(Location location) {

		if(null != currentLocation) {
			
			Log.i(LOG_TAG, "===============================================");
			Log.i(LOG_TAG, "Location Provider: " + location.getProvider());
			Log.i(LOG_TAG, "Distance:          " + currentLocation.distanceTo(location));
			Log.i(LOG_TAG, "CL accuracy:       " + currentLocation.getAccuracy());
			Log.i(LOG_TAG, "NL accuracy:       " + location.getAccuracy());
			Log.i(LOG_TAG, "CL time:           " + new Date(currentLocation.getTime()));
			Log.i(LOG_TAG, "NL time:           " + new Date(location.getTime()));
			Log.i(LOG_TAG, "isGpsListenerEnabled flag: " + isGpsListenerEnabled);
			Log.i(LOG_TAG, "===============================================");
		}

		// First we check if the new location is more accurate
		if(isMoreAccurate(location)) {
			
			Log.i(LOG_TAG, "New location is better, thus setting currentLocation to newLocation");
			
			// Update the current location with the new one
			currentLocation = location;
			
			// Since we are getting a more accurate location, we should check if the 
			// GPS listener is still enabled. If it is enabled we can turn it off
			if(isGpsListenerEnabled) {
				
				try {
					
					locationManager.removeUpdates(gpsLocationListener);
					isGpsListenerEnabled = false;
					Log.i(LOG_TAG, "Removing GPS listener updates");
				}
				catch(IllegalArgumentException iae) {
					
					isGpsListenerEnabled = false;
					Log.w(LOG_TAG, "Was not able to remove GPS listener updates");
				}
			}
			
			// FIXME: This is just experimental and will change. The correct way of doing this is to 
			// notify the activity to get the location artifacts from the service
			
			// Getting all the artifact location matches and send it to the Activity via a notification
			String match = getArtifactsForCurrentLocation();
			if(null != match) {
				sendNotification(match);
			}
		}
		else {
			
			// Check if the currentLocation has been updated recently
			Log.i(LOG_TAG, "The currentLocation time = " + new Date(currentLocation.getTime()));
			
			long expirationTime = System.currentTimeMillis() - currentLocation.getTime() - LOCATION_TIME_EXPIRATION_DELTA;
			if(expirationTime > 0) {
				
				Log.i(LOG_TAG, "The current location's fix time is too old.");
				
				// The current location's fix time is too old. Check if GPS provider is 
				// available and try to get a better fix from it
				if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !isGpsListenerEnabled) {

					try {
						
						isGpsListenerEnabled = true;
						locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_MIN_TIME, LOCATION_MIN_DISTANCE, gpsLocationListener);
						
						Log.i(LOG_TAG, "Enabling GPS listener updates");
					}
					catch(Exception e) {
						
						isGpsListenerEnabled = false;
						Log.w(LOG_TAG, "Was not able to start GPS listener");
					}
					
				}
			}
		}	
	}

	/*
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onProviderDisabled(java.lang.String)
	 */
	private void locationProviderDisabled(String provider) {

		Log.i(LOG_TAG, "LocationListener.onProviderDisabled() provider = " + provider);

	}

	/*
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onProviderEnabled(java.lang.String)
	 */
	private void locationProviderEnabled(String provider) {

		Log.i(LOG_TAG, "LocationListener.onProviderEnabled() provider = " + provider);	
	}

	/*
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onStatusChanged(java.lang.String, int, android.os.Bundle)
	 */
	private void locationStatusChanged(String provider, int status, Bundle extras) {

		Log.i(LOG_TAG, "LocationListener.onStatusChanged()");
		Log.i(LOG_TAG, "provider = " + provider);
		Log.i(LOG_TAG, "status = " + status);
	}
}
