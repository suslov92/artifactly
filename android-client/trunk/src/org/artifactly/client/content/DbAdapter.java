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

package org.artifactly.client.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

public class DbAdapter {

	private static final String PROD_LOG_TAG = "** A.S.DB **";

	private DatabaseHelper mDatabaseHelper;
	private SQLiteDatabase mSQLiteDatabase;

	private static final String DATABASE_NAME = "ArtifactlyData";
	private static final String DB_TABLE_LOCATION = "Location";
	private static final String DB_TABLE_ARTIFACT = "Artifact";
	private static final String DB_TABLE_LOC_TO_ART = "LocToArt";
	private static final int DATABASE_VERSION = 3;

	public static final String [] LOC_FIELDS = {"_id", "locName", "lat", "lng"};
	public static final String [] LOC_FIELDS_AS = {"locId", "locName", "locLat", "locLng"};
	public static final String [] ART_FIELDS = {"_id", "artName", "artData", "artCreationDate"};
	public static final String [] LOC_ART_FIELDS = {"artId", "locId" };

	public static final int LOC_ID = 0;
	public static final int LOC_NAME = 1;
	public static final int LOC_LATITUDE = 2;
	public static final int LOC_LONGITUDE = 3;

	public static final int ART_ID = 0;
	public static final int ART_NAME = 1;
	public static final int ART_DATA = 2;
	public static final int ART_CREATION_DATE = 3;

	public static final int FK_ART_ID = 0;
	public static final int FK_LOC_ID = 1;

	private static final String CREATE_LOCATION_TABLE =
		"create table " + DB_TABLE_LOCATION + " (" + LOC_FIELDS[LOC_ID] + " INTEGER primary key autoincrement, "
		+ LOC_FIELDS[LOC_NAME] + " TEXT not null, "
		+ LOC_FIELDS[LOC_LATITUDE] + " TEXT not null, "
		+ LOC_FIELDS[LOC_LONGITUDE] + " TEXT not null);";

	private static final String CREATE_ARTIFACT_TABLE =
		"create table " + DB_TABLE_ARTIFACT + "(" + ART_FIELDS[ART_ID] + " INTEGER primary key autoincrement, "
		+ ART_FIELDS[ART_NAME] + " TEXT not null, "
		+ ART_FIELDS[ART_DATA] + " TEXT, "
		+ ART_FIELDS[ART_CREATION_DATE] + " DATETIME default current_timestamp);";

	private static final String CREATE_LOC_TO_ART_TABLE =
		"create table " + DB_TABLE_LOC_TO_ART + "(" + LOC_ART_FIELDS[FK_ART_ID] + " INTEGER REFERENCES " + DB_TABLE_ARTIFACT + "(" + ART_FIELDS[ART_ID] + "), "
		+ LOC_ART_FIELDS[FK_LOC_ID] + " INTEGER REFERENCES " + DB_TABLE_LOCATION + "(" + LOC_FIELDS[LOC_ID] + "),"
		+ "PRIMARY KEY (" + LOC_ART_FIELDS[FK_ART_ID] + ", " + LOC_ART_FIELDS[FK_LOC_ID] + "))";


	// Constructor that initializes the database
	public DbAdapter(Context context) {

		mDatabaseHelper = new DatabaseHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
		mSQLiteDatabase = mDatabaseHelper.getWritableDatabase();
	}

	public void close() {

		mDatabaseHelper.close();
	}

	/*
	 * The insert checks first if the entity exists, if the entity exists, nothing is inserted.
	 * If the entity doesn't exist, a new DB record is created for it.
	 */
	public int insert(String locationName, String latitude, String longitude, String artifactName, String artifactData) {

		long locationRowID = -1;
		long artifactRowId = -1;
		ContentValues contentValues = null;

		try {

			/*
			 * Get location by name and see if the result matches the provided Lat/Lng coordinates,
			 * if the coordinates don't match, we return an error
			 */
			if(!isValidLocation(locationName, latitude, longitude)) {
				
				return 0;
			}
			
			// Test if location already exists
			long locationId = getLocation(locationName, latitude, longitude);

			if(0 <= locationId) {

				// Location exists so we keep track of its rowId
				locationRowID = locationId;
			}
			else {

				// Location doesn't exist so we create a new DB record for it
				contentValues = new ContentValues();
				contentValues.put(LOC_FIELDS[LOC_NAME], locationName);
				contentValues.put(LOC_FIELDS[LOC_LATITUDE], latitude);
				contentValues.put(LOC_FIELDS[LOC_LONGITUDE], longitude);
				locationRowID = mSQLiteDatabase.insert(DB_TABLE_LOCATION, null, contentValues);
			}

			// Test if artifact already exists
			long artifactId = getArtifact(artifactName, artifactData);

			if(0 <= artifactId) {

				// Artifact exists so we keep track of its rowID
				artifactRowId = artifactId;
			}
			else {

				// Artifact doesn't exist so we create a new db record for it 
				contentValues = new ContentValues();
				contentValues.put(ART_FIELDS[ART_NAME], artifactName);
				contentValues.put(ART_FIELDS[ART_DATA], artifactData);
				artifactRowId = mSQLiteDatabase.insert(DB_TABLE_ARTIFACT, null, contentValues);
			}

			// Test if location/artifact relationship already exists
			if(-1 == locationId || -1 == artifactId) {

				// The location and artifact relationship doesn't exist so we create a new db record for it
				contentValues = new ContentValues();
				contentValues.put(LOC_ART_FIELDS[FK_ART_ID], artifactRowId);
				contentValues.put(LOC_ART_FIELDS[FK_LOC_ID], locationRowID);
				mSQLiteDatabase.insert(DB_TABLE_LOC_TO_ART, null, contentValues);
			}

		}
		catch(SQLiteException e) {
			
			Log.e(PROD_LOG_TAG, "SQLiteException insert()", e);
			return -1;
		}
		
		return 1;
	}

	/*
	 * Deleting artifact and its artifact to location mapping
	 */
	public int deleteArtifact(String artifactId, String locationId) {
		
		try {
			
			// Delete from location/artifact mapping
			int numRowsAffected = mSQLiteDatabase.delete(DB_TABLE_LOC_TO_ART, LOC_ART_FIELDS[FK_ART_ID] + "=? AND " + LOC_ART_FIELDS[FK_LOC_ID] + "=?", new String[] {artifactId, locationId});

			if(numRowsAffected != 1) {
				
				return -1;
			}

			// Only delete artifact if it's not referenced by a location
			if(!hasArtifactInLocToArtTable(artifactId)) {

				numRowsAffected = mSQLiteDatabase.delete(DB_TABLE_ARTIFACT, ART_FIELDS[ART_ID] + "=?", new String[] {artifactId});
				
				if(numRowsAffected != 1) {
					
					return -1;
				}
			}
			else {
				
				return 0;
			}

		}
		catch(SQLiteException e) {
			
			Log.e(PROD_LOG_TAG, "SQLiteException: deleteArtifact()", e);
			return -1;
		}

		return 1;
	}
	
	/*
	 * Select one artifact 
	 */
	public Cursor select(String artifactId) {
		
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables("LocToArt JOIN Artifact ON (LocToArt.artId=Artifact._id) JOIN Location ON (LocToArt.locId=Location._id)");
		queryBuilder.setDistinct(true);
		return queryBuilder.query(mSQLiteDatabase,
				new String[] {"Artifact._id AS artId",
							  "Location._id AS locId",
							  "Artifact.artName AS artName",
							  "Artifact.artData AS artData",
							  "Location.locName AS locName",
							  "Location.lat AS lat",
							  "Location.lng AS lng"},
							  LOC_ART_FIELDS[FK_ART_ID] + "=?", new String[] {artifactId}, null, null, null);
	}
	
	/*
	 * Select all locations
	 * NOTE: Caller must call cursor.close()
	 */
	public Cursor getLocations() {

		return mSQLiteDatabase.query(true,
				DB_TABLE_LOCATION,
				LOC_FIELDS,
				null, null, null, null, "Location.locName ASC", null);
	}
	
	/*
	 * Update an Artifact
	 */
	public boolean updateArtifact(String artifactId, String artifactName, String artifactData, String locationId, String locationName) {
		
		ContentValues artContentValues = new ContentValues();
		artContentValues.put(ART_FIELDS[ART_NAME], artifactName);
		artContentValues.put(ART_FIELDS[ART_DATA], artifactData);
		int numberArtRowsAffected = mSQLiteDatabase.update(DB_TABLE_ARTIFACT, artContentValues, ART_FIELDS[ART_ID] + "=?", new String[] {artifactId});
		
		ContentValues locContentValues = new ContentValues();
		locContentValues.put(LOC_FIELDS[LOC_NAME], locationName);
		int numberLocRowsAffected = mSQLiteDatabase.update(DB_TABLE_LOCATION, locContentValues, LOC_FIELDS[LOC_ID] + "=?", new String[] {locationId});
		
		return ((numberArtRowsAffected == 1 && numberLocRowsAffected ==  1) ? true : false);
	}
	
	/*
	 * Update a Location
	 */
	public boolean updateLocation(String locationId, String locationName, String locationLat, String locationLng) {
		
		// TODO: Allow user to update the Lat/Lng via moving the marker on the map
		ContentValues locContentValues = new ContentValues();
		locContentValues.put(LOC_FIELDS[LOC_NAME], locationName);
		int numberLocRowsAffected = mSQLiteDatabase.update(DB_TABLE_LOCATION, locContentValues, LOC_FIELDS[LOC_ID] + "=?", new String[] {locationId});
		
		return ((numberLocRowsAffected ==  1) ? true : false);
	}
	
	/*
	 * Select all the location and artifact relationships
	 * NOTE: Caller must call cursor.close()
	 */
	public Cursor select() {

		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables("LocToArt JOIN Artifact ON (LocToArt.artId=Artifact._id) JOIN Location ON (LocToArt.locId=Location._id)");
		queryBuilder.setDistinct(true);
		return queryBuilder.query(mSQLiteDatabase,
				new String[] {"Artifact._id AS artId",
							  "Location._id AS locId",
							  "Artifact.artName AS artName",
							  "Artifact.artData AS artData",
							  "Location.locName AS locName",
							  "Location.lat AS lat",
							  "Location.lng AS lng"},
				null, null, null, null, "Location.locName ASC, Artifact.artName ASC");
	}

	/*
	 * Delete location if it doesn't have any artifact mappings
	 */
	public int deleteLocation(String locationId) {
		
		try {
			
			// Only delete location if it's not referenced by an artifact
			if(!hasLocationInLocToArtTable(locationId)) {

				int numAffectedRows = mSQLiteDatabase.delete(DB_TABLE_LOCATION, LOC_FIELDS[LOC_ID] + "=?", new String[] {locationId});
				
				if(numAffectedRows != 1) {
					
					return -1;
				}
			}
			else {
				
				return 0;
			}

		}
		catch(SQLiteException e) {
			
			Log.e(PROD_LOG_TAG, "SQLiteException: deleteLocation()", e);
			return -1;
		}

		return 1;
	}
	
	
	/*
	 * Helper method that checks if the provided artifactRowId is part of an existing location and artifact relationship
	 */
	private boolean hasArtifactInLocToArtTable(String artifactId) {

		Cursor cursor = mSQLiteDatabase.query(true, DB_TABLE_LOC_TO_ART, LOC_ART_FIELDS, LOC_ART_FIELDS[FK_ART_ID] + "=?", new String [] {artifactId}, null, null, null, null);

		if((null != cursor) && (0 < cursor.getCount())) {

			cursor.close();
			return true;
		}
		else {
			
			if(null != cursor) {
			
				cursor.close();
			}
			
			return false;
		}
	}

	/*
	 * Helper method that checks if the provided locationRowId is part of an existing location and artifact relationship
	 */
	private boolean hasLocationInLocToArtTable(String locationId) {

		Cursor cursor = mSQLiteDatabase.query(true, DB_TABLE_LOC_TO_ART, LOC_ART_FIELDS, LOC_ART_FIELDS[FK_LOC_ID] + "=?", new String [] {locationId}, null, null, null, null);

		if((null != cursor) && (0 < cursor.getCount())) {
			
			cursor.close();
			return true;
		}
		else {
			
			if(null != cursor) {

				cursor.close();
			}
			
			return false;
		}
	}

	/*
	 * Helper method that searches by name, latitude, and longitude for an existing location
	 */
	private long getLocation(String name, String latitude, String longitude) {

		Cursor cursor = mSQLiteDatabase.query(true,
				DB_TABLE_LOCATION,
				LOC_FIELDS,
				LOC_FIELDS[LOC_NAME] + "=? AND " + LOC_FIELDS[LOC_LATITUDE] + "=? AND " + LOC_FIELDS[LOC_LONGITUDE] + "=?",
				new String [] {name, latitude, longitude},
				null,
				null,
				null,
				null);

		if((null != cursor) && (0 < cursor.getCount())) {
			
			cursor.moveToFirst();
			int idColumnIndex = cursor.getColumnIndex(LOC_FIELDS[LOC_ID]);
			long rowId = cursor.getLong(idColumnIndex);
			cursor.close();
			return rowId;
		}
		else {
			
			if(null != cursor) {
			
				cursor.close();
			}
			
			return -1;
		}
	}
	
	/*
	 * Helper method that queries locations for the provided name. The result should contain zero or only
	 * one location. If the location doesn't matches the provided Lat/Lng then we signal a failure
	 * so that the user can choose another name.
	 */
	private boolean isValidLocation(String name, String latitude, String longitude) {

		Cursor cursor = null;
		
		try {
			
			cursor = mSQLiteDatabase.query(true,
					DB_TABLE_LOCATION,
					LOC_FIELDS,
					LOC_FIELDS[LOC_NAME] + "=?",
					new String [] {name},
					null,
					null,
					null,
					null);

			if((null != cursor) && (1 == cursor.getCount())) {

				cursor.moveToFirst();

				int latColumnIndex = cursor.getColumnIndex(LOC_FIELDS[LOC_LATITUDE]);
				int lngColumnIndex = cursor.getColumnIndex(LOC_FIELDS[LOC_LONGITUDE]);
				String lat = cursor.getString(latColumnIndex);
				String lng = cursor .getString(lngColumnIndex);

				if(null != lat && null != lng && lat.equals(latitude) && lng.equals(longitude)) {
					
					cursor.close();
					return true;
				}
				else {
					
					cursor.close();
					return false;
				}
			}
			else if((null != cursor) && (0 == cursor.getCount())) {
				
				cursor.close();
				return true;
			}
			else {

				if(null != cursor) {
				
					cursor.close();
				}
				return false;
			}
		}
		catch(SQLiteException e) {

			if(null != cursor) {
				
				cursor.close();
				return false;
			}
		}
		
		return false;
	}

	/*
	 * Helper method that searches by name and data for an existing artifact
	 */
	private long getArtifact(String name, String data) {

		Cursor cursor = mSQLiteDatabase.query(true,
				DB_TABLE_ARTIFACT, ART_FIELDS, ART_FIELDS[ART_NAME] + "=? AND " + ART_FIELDS[ART_DATA] + "=?",
				new String [] {name, data},
				null,
				null,
				null,
				null);

		if((null != cursor) && (0 < cursor.getCount())) {
			
			cursor.moveToFirst();
			int idColumnIndex = cursor.getColumnIndex(ART_FIELDS[ART_ID]);
			long rowId = cursor.getLong(idColumnIndex);
			cursor.close();
			return rowId;
		}
		else {
			
			if(null != cursor) {
				
				cursor.close();
			}
			return -1;
		}
	}


	private static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context, String name, CursorFactory factory, int version) {
			
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			db.execSQL(CREATE_LOCATION_TABLE);
			db.execSQL(CREATE_ARTIFACT_TABLE);
			db.execSQL(CREATE_LOC_TO_ART_TABLE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase arg0, int oldVersion, int newVersion) {
			
		}
	}
}
