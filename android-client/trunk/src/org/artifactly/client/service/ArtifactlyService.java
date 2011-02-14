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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.artifactly.client.ApplicationConstants;
import org.artifactly.client.Artifactly;
import org.artifactly.client.R;
import org.artifactly.client.content.DbAdapter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;


public class ArtifactlyService extends Service implements OnSharedPreferenceChangeListener, ApplicationConstants {

	// Logging
	private static final String LOG_TAG = "** A.S. **";

	// Preferences
	private static final String PREFS_NAME = "ArtifactlyPrefsFile";
	private SharedPreferences settings;

	// Location constants
	private static final int LOCATION_MIN_TIME = 5000;
	private static final int LOCATION_MIN_DISTANCE = 100;
	private static final String DISTANCE = "dist";
	
	// Location radius
	private static int radius = 100;

	// Notification constants
	private static final int NOTIFICATION_ID = 95691;
	
	// Context Resources
	private static String NOTIFICATION_TICKER_TEXT;
	private static String NOTIFICATION_CONTENT_TITLE;
	
	// Managers
	private LocationManager locationManager;
	private NotificationManager notificationManager;
	
	// Location provider
	private String locationProviderName;
	
	// Location LocationListener
	LocationListener locationListener;

	// DB adapter
	private DbAdapter dbAdapter;

	// Keeping track of current location
	private Location currentLocation;

	// Binder access to service API
	private IBinder localServiceBinder;
	
	private Gson gson = new Gson();
	
	@Override
	public IBinder onBind(Intent intent) {
		localServiceBinder = new LocalServiceImpl(this);
		return localServiceBinder;
	}

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
		locationProviderName = locationManager.getBestProvider(getLocationCriteria(), true);
		locationListener = getLocationListener();
		locationManager.requestLocationUpdates(locationProviderName, LOCATION_MIN_TIME, LOCATION_MIN_DISTANCE, locationListener);

		// Getting the initial location
		currentLocation = locationManager.getLastKnownLocation(locationProviderName);
	}


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
		
			locationManager.requestLocationUpdates(locationProviderName, LOCATION_MIN_TIME, LOCATION_MIN_DISTANCE, locationListener);
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

			locationManager.removeUpdates(locationListener);
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
	 * Method that returns a location listener
	 */
	private LocationListener getLocationListener() {

		return new LocationListener() {

			public void onLocationChanged(Location location) {

				if(null != currentLocation) {

					Log.i(LOG_TAG, "Distance = " + currentLocation.distanceTo(location));
					Log.i(LOG_TAG, "Old location accuracy = " + currentLocation.getAccuracy());
					Log.i(LOG_TAG, "New location accuracy = " + location.getAccuracy());
				}

				currentLocation = location;
				String match = locationMatch();
				if(null != match) {
					sendNotification(match);
				}
			}

			public void onProviderDisabled(String arg0) {
				//Log.i(LOG_TAG, "LocationListener.onProviderDisabled()");
			}

			public void onProviderEnabled(String arg0) {
				//Log.i(LOG_TAG, "LocationListener.onProviderEnabled()");
			}

			public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
				//Log.i(LOG_TAG, "LocationListener.onStatusChanged() provider = " + arg0 + " status = " + arg1);
			}
		};
	}

	/*
	 * Method that returns a location criteria
	 */
	private Criteria getLocationCriteria() {

		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setCostAllowed(false);
		criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
		criteria.setSpeedRequired(false);

		return criteria;

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
	
	private String locationMatch() {

		// Getting all the locations
		Cursor cursor = dbAdapter.select();
		cursor.moveToFirst();

		// Determine the table column indexes 
		int longitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE]);
		int latitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE]);
		int nameColumnIndex = cursor.getColumnIndex(DbAdapter.ART_FIELDS[DbAdapter.ART_NAME]);
		int dataColumnIndex = cursor.getColumnIndex(DbAdapter.ART_FIELDS[DbAdapter.ART_DATA]);

		List<Map<String, String>> items = new ArrayList<Map<String, String>>();

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

				Map<String, String> item = new HashMap<String, String>();
				item.put(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE], storedLatitude);
				item.put(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE], storedLongitude);
				item.put(DbAdapter.ART_FIELDS[DbAdapter.ART_NAME], cursor.getString(nameColumnIndex));
				item.put(DbAdapter.ART_FIELDS[DbAdapter.ART_DATA], cursor.getString(dataColumnIndex));
				item.put(DISTANCE, Float.toString(distanceResult[0]));
				items.add(item);
			}
		}

		cursor.close();

		if(items.isEmpty()) {
			return null;
		}
		else {

			return gson.toJson(items);
		}
	}
}
