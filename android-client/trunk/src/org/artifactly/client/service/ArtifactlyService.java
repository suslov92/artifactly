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
import org.json.JSONArray;
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
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;


public class ArtifactlyService extends Service implements OnSharedPreferenceChangeListener, ApplicationConstants {

	// Logging
	private static final String LOG_TAG = "Artifactly Service";

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

	// Content / DB
	DbAdapter dbAdapter;

	// Keeping track of current location
	private Location currentLocation;


	@Override
	public IBinder onBind(Intent arg0) {
		return null;
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

		// FIXME: Remove this after testing
		initTestData();

		// Setting up the notification manager
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// Setting up the location manager
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		String provider = locationManager.getBestProvider(getLocationCriteria(), true);
		locationManager.requestLocationUpdates(provider, LOCATION_MIN_TIME, LOCATION_MIN_DISTANCE, getLocationListener());

		// Getting the initial location
		currentLocation = locationManager.getLastKnownLocation(provider);
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

		int newRadius = sharedPreferences.getInt(key, 100);

		// TODO: Determine what the appropriate minimum radius is.
		if(0 < newRadius) {

			radius = newRadius;
		}
		else {

			Log.w(LOG_TAG, "Radius update was ignored because new radisu < 1");
		}
	}

	/*
	 * Method that returns a location listener
	 */
	private LocationListener getLocationListener() {

		return new LocationListener() {

			public void onLocationChanged(Location location) {

				if(null != currentLocation) {

					double distanceDifference = getDistanceDifference(currentLocation.getLatitude(), currentLocation.getLongitude(), location.getLatitude(), location.getLongitude());
					Log.i(LOG_TAG, "Distance (meters) = " + distanceDifference);
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

	/*
	 * Calculate distance between two geographic locations
	 * Giving credit for the distance formula: http://www.movable-type.co.uk/scripts/latlong.html
	 */
	private double getDistanceDifference(double lat1, double lng1, double lat2, double lng2) {

		// Earth's radius = 6371km
		double earthRadius = 6371;
		double dLat = Math.toRadians(lat2-lat1);
		double dLng = Math.toRadians(lng2-lng1);
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng/2) * Math.sin(dLng/2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		double dist = earthRadius * c;

		double meterConversion = 1000;

		return dist * meterConversion;
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

		List<JSONObject> items = new ArrayList<JSONObject>();

		int rowCount = cursor.getCount();
		Log.i(LOG_TAG, "row count = " + rowCount);
		
		// If there are no results, we return
		if(0 == rowCount) {
			
			cursor.close();
			return null;
		}

		/*
		 *  Iterating over all result and calculate the distance between
		 *  the current location and the stored location. If the distance lies within the defined
		 *  radius, we notify the user that there is an artifact.
		 */
		for(;cursor.isAfterLast() == false; cursor.moveToNext()) {

			// Getting latitude and longitude
			String storedLatitude = cursor.getString(latitudeColumnIndex).trim();
			String storedLongitude = cursor.getString(longitudeColumnIndex).trim();

			double distanceDifference = getDistanceDifference(currentLocation.getLatitude(),
					currentLocation.getLongitude(),
					Double.parseDouble(storedLatitude),
					Double.parseDouble(storedLongitude));

			Log.i(LOG_TAG, "distanceDifference = " + distanceDifference);

			if(distanceDifference <= radius) {

				Map<String, String> item = new HashMap<String, String>();
				item.put(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE], storedLatitude);
				item.put(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE], storedLongitude);
				item.put(DbAdapter.ART_FIELDS[DbAdapter.ART_NAME], cursor.getString(nameColumnIndex));
				item.put(DbAdapter.ART_FIELDS[DbAdapter.ART_DATA], cursor.getString(dataColumnIndex));
				item.put(DISTANCE, Double.toString(distanceDifference));
				items.add(new JSONObject(item));
			}
		}

		cursor.close();

		if(items.isEmpty()) {
			return null;
		}
		else {
			return new JSONArray(items).toString();
		}
	}
	
	/*
	 * FIXME: Remove this when not needed anymore
	 * Test date
	 */
	private void initTestData() {

		// TODO: add test to check if test data exists
		dbAdapter.insert("38.540013", "-121.57983", "artifact name 1", "artifact data 1");
		dbAdapter.insert("38.535298", "-121.578745", "artifact name 2", "artifact data 2");
	}
}
