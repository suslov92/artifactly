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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.os.Binder;
import android.util.Log;

public class LocalServiceImpl extends Binder implements LocalService {

	// Logging
	private static final String LOG_TAG = "** A.L.S. **";
	
	// Artifact Filters
	private static final int ALL_ARTIFACTS_FILTER = 0;
	private static final int CURRENT_LOCATION_ARTIFACTS_FILTER = 1;
	
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
	public int createArtifact(String artifactName, String artifactData, String locationName, String latitude, String longitude) {
		
		Log.i(LOG_TAG, "LocalService : createArtifact called");
		
		if(null == dbAdapter) {
			Log.e(LOG_TAG, "LocalService : createArtifact : dbAdapter is null");
			return -1;
		}
		
		return dbAdapter.insert(locationName, latitude, longitude, artifactName, artifactData);
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
	public int createArtifact(String artifactName, String artifactData, String locationName) {
		
		Location currentLocation = artifactlyService.getLocation();
		
		if(null != currentLocation) {
		
			return createArtifact(artifactName, artifactData, locationName, Double.toString(currentLocation.getLatitude()), Double.toString(currentLocation.getLongitude()));
		}
		else {
			
			return -1;
		}
	}

	// API method
	public String getArtifacts() {
		
		return getFilteredArtifacts(ALL_ARTIFACTS_FILTER);
	}

	// API method
	public boolean canAccessInternet() {
		
		return artifactlyService.canAccessInternet();
	}

	// API method
	public String getArtifactsForCurrentLocation() {
		
		return getFilteredArtifacts(CURRENT_LOCATION_ARTIFACTS_FILTER);
	}

	// API method
	public int deleteArtifact(String artifactId, String locationId) {
		
		if(null == dbAdapter) {
			
			Log.e(LOG_TAG, "LocalService : deleteArtifact : dbAdapter is null");
			return -1;
		}
		
		return dbAdapter.deleteArtifact(artifactId, locationId);
	}
	
	public int deleteLocation(String locationId) {
		
		if(null == dbAdapter) {

			Log.e(LOG_TAG, "LocalService : deleteLocation : dbAdapter is null");
			return -1;
		}

		return dbAdapter.deleteLocation(locationId);
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
		int artNameColumnIndex = cursor.getColumnIndex(DbAdapter.ART_FIELDS[DbAdapter.ART_NAME]);
		int artDataColumnIndex = cursor.getColumnIndex(DbAdapter.ART_FIELDS[DbAdapter.ART_DATA]);
		int locIdColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_LOC_ID]);
		int locNameColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_NAME]);
		int longitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE]);
		int latitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE]);

		
		JSONObject item = new JSONObject();

		try {
			
			item.put(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_ART_ID], cursor.getInt(artIdColumnIndex));
			item.put(DbAdapter.ART_FIELDS[DbAdapter.ART_NAME], cursor.getString(artNameColumnIndex));
			item.put(DbAdapter.ART_FIELDS[DbAdapter.ART_DATA], cursor.getString(artDataColumnIndex));
			item.put(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_LOC_ID], cursor.getInt(locIdColumnIndex));
			item.put(DbAdapter.LOC_FIELDS[DbAdapter.LOC_NAME], cursor.getString(locNameColumnIndex));
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

	// API method
	public String getLocations() {
		
		// JSON array that holds the result
		JSONArray items = new JSONArray();
		
		if(null == dbAdapter) {
			
			Log.w(LOG_TAG, "DB Adapter is null");
			return items.toString();
		}
		
		// Getting all the locations
		Cursor cursor = dbAdapter.getLocations();
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
		int locIdColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_ID]);
		int locNameColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_NAME]);
		int locLatColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE]);
		int locLngColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE]);
		
		/*
		 *  Iterating over all result and calculate the distance between
		 *  radius, we notify the user that there is an artifact.
		 */
		for(;cursor.isAfterLast() == false; cursor.moveToNext()) {

			JSONObject item = new JSONObject();

			try {
				
				item.put(DbAdapter.LOC_FIELDS_AS[DbAdapter.LOC_ID], cursor.getInt(locIdColumnIndex));
				item.put(DbAdapter.LOC_FIELDS_AS[DbAdapter.LOC_NAME], cursor.getString(locNameColumnIndex));
				item.put(DbAdapter.LOC_FIELDS_AS[DbAdapter.LOC_LATITUDE], cursor.getString(locLatColumnIndex));
				item.put(DbAdapter.LOC_FIELDS_AS[DbAdapter.LOC_LONGITUDE], cursor.getString(locLngColumnIndex));
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
	
	// Helper method
	private String getFilteredArtifacts(int filter) {
		
		// JSON array that holds the result
		JSONArray locations = new JSONArray();
		
		if(null == dbAdapter) {
			
			Log.w(LOG_TAG, "DB Adapter is null");
			return locations.toString();
		}
		
		// Getting all the locations
		Cursor cursor = dbAdapter.select();
		if(null == cursor) {
			
			Log.w(LOG_TAG, "Cursor is null");
			return locations.toString();
		}
		
		// Checking if the cursor set has any items
		boolean hasItems = cursor.moveToFirst();
		
		if(!hasItems) {
			
			Log.i(LOG_TAG, "DB has not items");
			cursor.close();
			return locations.toString();
		}

		// Determine the table column indexes 
		int artIdColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_ART_ID]);
		int artNameColumnIndex = cursor.getColumnIndex(DbAdapter.ART_FIELDS[DbAdapter.ART_NAME]);
		int artDataColumnIndex = cursor.getColumnIndex(DbAdapter.ART_FIELDS[DbAdapter.ART_DATA]);
		int locIdColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_LOC_ID]);
		int locNameColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_NAME]);
		int longitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE]);
		int latitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE]);

		String currentLocationName = null;
		JSONObject location = null;
		
		for(;cursor.isAfterLast() == false; cursor.moveToNext()) {
			
			switch(filter) {
				case CURRENT_LOCATION_ARTIFACTS_FILTER:
					// If the current location is not nearby, we don't add it
					String lat = cursor.getString(latitudeColumnIndex).trim();
					String lng = cursor.getString(longitudeColumnIndex).trim();
					if(!artifactlyService.isNearbyCurrentLocation(lat, lng)) {
						continue;
					}
					break;
				case ALL_ARTIFACTS_FILTER:
					// Don't do anything
					break;
				default:
					// Don't do anything
			}
			
			// Get the location
			String locationName = cursor.getString(locNameColumnIndex);
			
			try {
				
				// Check if we need to setup an new location
				if(null == currentLocationName || !currentLocationName.equals(locationName)) {

					currentLocationName = locationName;
					location = new JSONObject();
					location.put(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_LOC_ID], cursor.getInt(locIdColumnIndex));
					location.put(DbAdapter.LOC_FIELDS[DbAdapter.LOC_NAME], cursor.getString(locNameColumnIndex));
					location.put(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE], cursor.getString(latitudeColumnIndex));
					location.put(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE], cursor.getString(longitudeColumnIndex));
					location.put("artifacts", new JSONArray());
					locations.put(location);
				}
				
				JSONObject artifact = new JSONObject();	
				artifact.put(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_ART_ID], cursor.getInt(artIdColumnIndex));
				artifact.put(DbAdapter.ART_FIELDS[DbAdapter.ART_NAME], cursor.getString(artNameColumnIndex));
				artifact.put(DbAdapter.ART_FIELDS[DbAdapter.ART_DATA], cursor.getString(artDataColumnIndex));
				
				location.getJSONArray("artifacts").put(artifact);
				
			}
			catch (JSONException e) {
				
				Log.w(LOG_TAG, "Error while populating JSONObject");
			}
		}

		cursor.close();

		if(locations.length() == 0) {

			return null;
		}
		else {
	
			return locations.toString();
		}
	}

	public boolean updateArtifact(String artifactId, String artifactName, String artifactData, String locationId, String locationName) {
		
		if(null == dbAdapter) {
			
			Log.w(LOG_TAG, "DB Adapter is null");
			return false;
		}
		
		return dbAdapter.updateArtifact(artifactId, artifactName, artifactData, locationId, locationName);
		
	}
}
