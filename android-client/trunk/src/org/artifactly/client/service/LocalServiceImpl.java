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

import org.artifactly.client.content.DbAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.os.Binder;
import android.util.Log;

public class LocalServiceImpl extends Binder implements LocalService {

	// Logging
	private static final String LOG_TAG = "** A.L.S. **";
	
	private ArtifactlyService artifactlyService;
	
	private DbAdapter dbAdapter;
	
	// Constructor
	public LocalServiceImpl(ArtifactlyService artifactlyService) { 
		this.artifactlyService = artifactlyService;
		
		if(null != artifactlyService) {
			this.dbAdapter = artifactlyService.getDbAdapter();
		}
	}
	
	// API method
	public boolean createArtifact(String name, String data, String latitude, String longitude) {
		
		Log.i(LOG_TAG, "LocalService : createArtifact called");
		
		if(null == dbAdapter) {
			Log.e(LOG_TAG, "LocalService : createArtifact : dbAdapter is null");
			return false;
		}
		
		try {
			
			dbAdapter.insert(latitude, longitude, name, data);
		}
		catch(SQLiteException e) {
			
			return false;
		}
		
		return true;
	}

	// API method
	public boolean startLocationTracking() {
		
		if(null != artifactlyService) {
			
			artifactlyService.startLocationTracking();
		}
		else {
			
			return false;
		}
		
		return true;
	}
	
	// API method
	public boolean stopLocationTracking() {

		if(null != artifactlyService) {

			artifactlyService.stopLocationTracking();
		}
		else {

			return false;
		}
		
		return true;
	}

	// API method
	public String getLocation() {
		
		Location location = artifactlyService.getLocation();
		
		if(null == location) {
			
			JSONArray data = new JSONArray();
			
			try {
			
				data.put(0.0d);
				data.put(0.0d);
				data.put(0.0d);
				data.put(0.0d);
				data.put(0.0d);
			}
			catch (JSONException e) {
			
				Log.w(LOG_TAG, "Error while populating JSONArray");
			}
			
			return data.toString();

		}
		else {
			
			JSONArray data = new JSONArray();
			
			try {
			
				data.put(location.getLatitude());
				data.put(location.getLongitude());
				data.put(location.getAccuracy());
				data.put(new Date(location.getTime()));
				data.put(new Date(artifactlyService.getLastLocationUpdateTime()));
			}
			catch (JSONException e) {
				
				Log.w(LOG_TAG, "Error while population JSONArray");
			}
			
			return data.toString();
		}
	}

	// API method
	public boolean createArtifact(String name, String data) {
		
		Location currentLocation = artifactlyService.getLocation();
		
		if(null != currentLocation) {
		
			return createArtifact(name, data, Double.toString(currentLocation.getLatitude()), Double.toString(currentLocation.getLongitude()));
		}
		else {
			
			return false;
		}
	}

	// API method
	public String getArtifacts() {
		
		// JSON array that holds the result
		JSONArray items = new JSONArray();
		
		if(null == dbAdapter) {
			
			Log.w(LOG_TAG, "DB Adapter is null");
			return items.toString();
		}
		
		// Getting all the locations
		Cursor cursor = dbAdapter.select();
		if(null == cursor) {
			
			Log.w(LOG_TAG, "Cursor is null");
			return items.toString();
		}
		
		// Checking if the cursor set has any items
		boolean hasItems = cursor.moveToFirst();
		
		if(!hasItems) {
			
			Log.i(LOG_TAG, "DB has not items");
			cursor.close();
			return items.toString();
		}

		// Determine the table column indexes 
		int artIdColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_ART_ID]);
		int nameColumnIndex = cursor.getColumnIndex(DbAdapter.ART_FIELDS[DbAdapter.ART_NAME]);
		int dataColumnIndex = cursor.getColumnIndex(DbAdapter.ART_FIELDS[DbAdapter.ART_DATA]);
		int longitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE]);
		int latitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE]);

		/*
		 *  Iterating over all result and calculate the distance between
		 *  radius, we notify the user that there is an artifact.
		 */
		for(;cursor.isAfterLast() == false; cursor.moveToNext()) {

			JSONObject item = new JSONObject();

			try {
				
				item.put(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_ART_ID], cursor.getInt(artIdColumnIndex));
				item.put(DbAdapter.ART_FIELDS[DbAdapter.ART_NAME], cursor.getString(nameColumnIndex));
				item.put(DbAdapter.ART_FIELDS[DbAdapter.ART_DATA], cursor.getString(dataColumnIndex));
				item.put(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE], cursor.getString(latitudeColumnIndex));
				item.put(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE], cursor.getString(longitudeColumnIndex));
			}
			catch (JSONException e) {
				Log.w(LOG_TAG, "Error while populating JSONObject");
			}

			items.put(item);

		}

		cursor.close();

		if(items.length() == 0) {

			return null;
		}
		else {
	
			return items.toString();
		}
	}

	// API method
	public boolean canAccessInternet() {
		
		return artifactlyService.canAccessInternet();
	}

	// API method
	public String getArtifactsForCurrentLocation() {
		
		Location currentLocation = artifactlyService.getLocation();
		int radius = artifactlyService.getRadius();
		
		// JSON array that holds the result
		JSONArray items = new JSONArray();
		
		if(null == currentLocation) {
			return items.toString();
		}
		
		if(null == dbAdapter) {
			
			Log.w(LOG_TAG, "DB Adapter is null");
			return items.toString();
		}
		
		// Getting all the locations
		Cursor cursor = dbAdapter.select();
		if(null == cursor) {
			
			Log.w(LOG_TAG, "Cursor is null");
			return items.toString();
		}
		
		// Checking if the cursor set has any items
		boolean hasItems = cursor.moveToFirst();
		
		if(!hasItems) {
			
			Log.i(LOG_TAG, "DB has not items");
			cursor.close();
			return items.toString();
		}

		// Determine the table column indexes 
		int artIdColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_ART_ID]);
		int nameColumnIndex = cursor.getColumnIndex(DbAdapter.ART_FIELDS[DbAdapter.ART_NAME]);
		int dataColumnIndex = cursor.getColumnIndex(DbAdapter.ART_FIELDS[DbAdapter.ART_DATA]);
		int longitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE]);
		int latitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE]);

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
					
					item.put(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_ART_ID], cursor.getInt(artIdColumnIndex));
					item.put(DbAdapter.ART_FIELDS[DbAdapter.ART_NAME], cursor.getString(nameColumnIndex));
					item.put(DbAdapter.ART_FIELDS[DbAdapter.ART_DATA], cursor.getString(dataColumnIndex));
					item.put(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE], storedLatitude);
					item.put(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE], storedLongitude);
					item.put(ArtifactlyService.DISTANCE, Float.toString(distanceResult[0]));
				}
				catch (JSONException e) {
					Log.w(LOG_TAG, "Error while populating JSONObject");
				}
				
				items.put(item);
			}
		}

		cursor.close();
		
		return items.toString();
	}

	// API method
	public boolean deleteArtifact(long id) {
		
		if(null == dbAdapter) {
			Log.e(LOG_TAG, "LocalService : deleteArtifact : dbAdapter is null");
			return false;
		}
		
		try {
			
			dbAdapter.delete(id);
		}
		catch(SQLiteException e) {
			return false;
		}
		
		return true;
	}
	
	// API method
	public String getAtrifact(Long id) {
	
		// JSON array that holds the result
		JSONArray items = new JSONArray();
		
		if(null == dbAdapter) {
			
			Log.w(LOG_TAG, "DB Adapter is null");
			return items.toString();
		}
		
		// Getting all the locations
		Cursor cursor = dbAdapter.select(id);
		if(null == cursor) {
			
			Log.w(LOG_TAG, "Cursor is null");
			return items.toString();
		}
		
		// Checking if the cursor set has any items
		boolean hasItems = cursor.moveToFirst();
		
		if(!hasItems) {
			
			Log.i(LOG_TAG, "DB has not items");
			cursor.close();
			return items.toString();
		}

		if(cursor.getCount() != 1) {
			
			Log.w(LOG_TAG, "Expetecd the cursor to have only one row");
		}
		
		// Determine the table column indexes 
		int artIdColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_ART_ID]);
		int nameColumnIndex = cursor.getColumnIndex(DbAdapter.ART_FIELDS[DbAdapter.ART_NAME]);
		int dataColumnIndex = cursor.getColumnIndex(DbAdapter.ART_FIELDS[DbAdapter.ART_DATA]);
		int longitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE]);
		int latitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE]);

		
		JSONObject item = new JSONObject();

		try {
			
			item.put(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_ART_ID], cursor.getInt(artIdColumnIndex));
			item.put(DbAdapter.ART_FIELDS[DbAdapter.ART_NAME], cursor.getString(nameColumnIndex));
			item.put(DbAdapter.ART_FIELDS[DbAdapter.ART_DATA], cursor.getString(dataColumnIndex));
			item.put(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE], cursor.getString(latitudeColumnIndex));
			item.put(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE], cursor.getString(longitudeColumnIndex));
		}
		catch (JSONException e) {
			
			Log.w(LOG_TAG, "Error while populating JSONObject");
		}

		items.put(item);

		cursor.close();
		
		return items.toString();
	}
}
