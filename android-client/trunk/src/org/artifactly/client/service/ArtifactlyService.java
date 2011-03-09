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
import android.widget.Toast;

public class ArtifactlyService extends Service implements OnSharedPreferenceChangeListener, ApplicationConstants {

	// Logging
	private static final String LOG_TAG = "** A.S. **";

	// Preferences
	private static final String PREFS_NAME = "ArtifactlyPrefsFile";
	private SharedPreferences settings;

	// Location constants
	private static final int LOCATION_MIN_TIME = 5000; // 300000 : 5 min
	private static final int LOCATION_MIN_DISTANCE = 100; // 100 m
	private static final String DISTANCE = "dist";
	private static final int SIGNIFICANT_LOCATION_DELTA = 200;

	// Location radius
	private int radius = 100;
	
	// Location experiation delta
	private static final long LOCATION_TIME_EXPIRATION_DELTA = 300000; // 5 min

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
	private LocationListener gpsLocationListener = getGpsLocationListener();
	private LocationListener networkLocationListener = getNetworkLocationListener();
	
	// Location state
	private boolean startedGpsListener = false;
	
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
		localServiceBinder = new LocalServiceImpl(this);
		return localServiceBinder;
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();

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

		mainLocationProviderName = getLocatinProvider();
		
		locationManager.requestLocationUpdates(mainLocationProviderName, LOCATION_MIN_TIME, LOCATION_MIN_DISTANCE, getLocationListener());

		// Getting the initial location
		currentLocation = getLastKnownLocation();
	}


	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();

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
	protected boolean startLocationTracking() {

		try {

			locationManager.requestLocationUpdates(mainLocationProviderName, LOCATION_MIN_TIME, LOCATION_MIN_DISTANCE, getLocationListener());
		}
		catch(IllegalArgumentException iae) {

			return false;
		}

		return true;
	}

	/*
	 * Dispatch method for local service
	 */
	protected boolean stopLocationTracking() {

		try {

			//locationManager.removeUpdates(getLocationListener());
		}
		catch(IllegalArgumentException iae) {

			return false;
		}

		return true;
	}

	/*
	 * Dispatch method for local service
	 */
	protected Location getLocation() {

		return currentLocation;
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
	 * Get location lister
	 * 
	 */
	private LocationListener getLocationListener() {
		
		if(null != mainLocationProviderName && mainLocationProviderName.equals(LocationManager.NETWORK_PROVIDER)) {
			
			return networkLocationListener;
		}
		else if(null != mainLocationProviderName && mainLocationProviderName.equals(LocationManager.GPS_PROVIDER)) {
			
			return gpsLocationListener;
		}
		
		return null;
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

		boolean isNewer = false;
		boolean isMoreAccurate = false;
		boolean isSignificantlyLessAccurate = false;
		boolean hasCurrentLocationTimeExpired = false;
		
		// Get current system time
		long expirationTime = System.currentTimeMillis() - LOCATION_TIME_EXPIRATION_DELTA;
		if(currentLocation.getTime() < expirationTime) {
			
			hasCurrentLocationTimeExpired = true;
			Log.i(LOG_TAG, "Current location time(" + currentLocation.getTime() + ") has expired. Expiration time = " + expirationTime);
		}
		
		// Check time difference
		long locationTimeDelta = newLocation.getTime() - currentLocation.getTime();
		isNewer = locationTimeDelta > 0;

		// Check accuracy difference
		if(newLocation.hasAccuracy() && currentLocation.hasAccuracy()) {
			
			Log.i(LOG_TAG, "isMoreAccurate() both location have accuracy information");
			float locationAccuracyDelta = newLocation.getAccuracy() - currentLocation.getAccuracy();
			isMoreAccurate = locationAccuracyDelta < 0;
			isSignificantlyLessAccurate = locationAccuracyDelta > SIGNIFICANT_LOCATION_DELTA;
		}
		
		if(isMoreAccurate) {
			
			return true;
		}
		else if(isNewer && !isSignificantlyLessAccurate) {
			
			return true;
		}
		else if(isNewer && hasCurrentLocationTimeExpired) {
			
			return true;
		}
		
		return false;
	}
	
	
	/*
	 * Method that returns a location provider string:
	 * 1. Network provider
	 * 2. GPS provider
	 * 
	 */
	private String getLocatinProvider() {
		
		if(null == locationManager) {
			return null;
		}
		
		// Use network provided location if available
		if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			
			return LocationManager.NETWORK_PROVIDER;
		}
		else {
			
			Log.i(LOG_TAG, "LOCATION NETWORK PROVIDER is disabled");
		}
		
		if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			
			return LocationManager.GPS_PROVIDER;
		}
		else {
			
			Log.i(LOG_TAG, "LOCATION GPS PROVIDER is disabled");
		}
		
		return null;	
	}

	/*
	 * Method that gets the Last Known Location. Trying to get location from:
	 * 1. network provider
	 * 2. gps provider 
	 * 
	 */
	private Location getLastKnownLocation() {
		
		if(null == locationManager) {
			return null;
		}
		
		// Use network provided location if available
		if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			
			return locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}
		else {
			
			Log.i(LOG_TAG, "LOCATION NETWORK PROVIDER is disabled");
		}
		
		if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			
			return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);	
		}
		else {
			
			Log.i(LOG_TAG, "LOCATION GPS PROVIDER is disabled");
		}
		
		return null;	
	}
	
	/*
	 * GPS Location Listener
	 */
	private LocationListener getGpsLocationListener() {
		
		return new LocationListener() {

			public void onLocationChanged(Location location) {
			
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
	 * Network Location Listener
	 */
	private LocationListener getNetworkLocationListener() {
		
		return new LocationListener() {

			public void onLocationChanged(Location location) {
			
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
			
			Log.i(LOG_TAG, "Location Provider = " + location.getProvider());
			Log.i(LOG_TAG, "Distance = " + currentLocation.distanceTo(location));
			Log.i(LOG_TAG, "Old location accuracy = " + currentLocation.getAccuracy());
			Log.i(LOG_TAG, "New location accuracy = " + location.getAccuracy());
			Log.i(LOG_TAG, "startedGpsListener flag = " + startedGpsListener);
		}

		// First we check if the new location is more accurate
		if(isMoreAccurate(location)) {
			
			// FIXME: remove Toast
			Toast toast = Toast.makeText(getApplicationContext(), "New location is more accurate (" + location.getAccuracy() + ")", Toast.LENGTH_SHORT);
			toast.show();
			Log.i(LOG_TAG, "New location is more accureate (" + location.getAccuracy() + "). Setting current location to new location");
			
			currentLocation = location;
			String match = getArtifactsForCurrentLocation();
			if(null != match) {
				sendNotification(match);
			}
		}
		else {
			
			// FIXME: remove Toast
			Toast toast = Toast.makeText(getApplicationContext(), "New location is less accureate (" + location.getAccuracy() + ")", Toast.LENGTH_SHORT);
			toast.show();
			Log.i(LOG_TAG, "New location is less accureate (" + location.getAccuracy() + ")");
		}
		
		// Handling the case where the location's accuracy is greater than the search radius, in
		// which case we activate the GPS location listener
		int locationAccuracy = (currentLocation.hasAccuracy()) ? (int)currentLocation.getAccuracy() : -1;
		if(location != null && radius < locationAccuracy && location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
			
			Log.i(LOG_TAG, "Location accuracy(" + locationAccuracy + ") is less than the defined search radius(" + radius + ")");
			
			// If we are using the network location provider, try to get new location updates from the GPS provider
			if(null != mainLocationProviderName && mainLocationProviderName.equals(LocationManager.NETWORK_PROVIDER)) {
				
				// Make sure the provider is enabled and we are not already using the GPS provider
				if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
				   !mainLocationProviderName.equals(LocationManager.GPS_PROVIDER)) {
					
					// Setting flag so the consecutive network listener updates don't try to add the GPS listener as well
					if(!startedGpsListener) {
						
						startedGpsListener = true;
						
						locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_MIN_TIME, LOCATION_MIN_DISTANCE, gpsLocationListener);
						
						Log.i(LOG_TAG, "## Activating GPS location listener ##");
					}
				}
			}
		}
		
		// Once the accuracy is better than the defined radius, and we are using the GPS location listener, we can turn it off
		if(null != location && radius >= locationAccuracy && location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
			
			// We only disable the GPS listener if the GPS location time is current
			long expirationTime = System.currentTimeMillis() - LOCATION_TIME_EXPIRATION_DELTA;
			if(location.getTime() > expirationTime) {
				
				Log.i(LOG_TAG, "Location time = " + new Date(location.getTime()));
				// In case the GPS location listener is active, we remove it
				try {

					locationManager.removeUpdates(gpsLocationListener);
				}
				catch(IllegalArgumentException iae) {

					Log.w(LOG_TAG, "locationManager.removeUpdates(...)");
				}

				startedGpsListener = false;

				Log.i(LOG_TAG, "## Removing GPS location lister ##");
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
