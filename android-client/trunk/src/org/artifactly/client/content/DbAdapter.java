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
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

public class DbAdapter {

	private static final String TAG = "Artifactly Db Adapter";

	private DatabaseHelper mDatabaseHelper;
	private SQLiteDatabase mSQLiteDatabase;

	private static final String DATABASE_NAME = "ArtifactlyData";
	private static final String DB_TABLE_LOCATION = "Location";
	private static final String DB_TABLE_ARTIFACT = "Artifact";
	private static final String DB_TABLE_LOC_TO_ART = "LocToArt";
	private static final int DATABASE_VERSION = 3;

	public static final String [] LOC_FIELDS = {"_id", "lat", "long"};
	public static final String [] ART_FIELDS = {"_id", "name", "data", "creationDate"};
	public static final String [] LOC_ART_FIELDS = {"artId", "locId" };

	public static final int LOC_ID = 0;
	public static final int LOC_LATITUDE = 1;
	public static final int LOC_LONGITUDE = 2;

	public static final int ART_ID = 0;
	public static final int ART_NAME = 1;
	public static final int ART_DATA = 2;
	public static final int ART_CREATION_DATE = 3;

	public static final int FK_ART_ID = 0;
	public static final int FK_LOC_ID = 1;

	private static final String CREATE_LOCATION_TABLE =
		"create table " + DB_TABLE_LOCATION + " (" + LOC_FIELDS[LOC_ID] + " INTEGER primary key autoincrement, "
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
	public long[] insert(String latitude, String longitude, String name, String data) {

		long locationRowID = -1;
		long artifactRowId = -1;
		long locToArtRowId = -1;
		ContentValues contentValues = null;

		// Test if location already exists
		long locationId = getLocation(latitude, longitude);

		if(0 <= locationId) {

			// Location exists so we keep track of its rowId
			locationRowID = locationId;
		}
		else {

			// Location doesn't exist so we create a new db record for it
			contentValues = new ContentValues();
			contentValues.put(LOC_FIELDS[LOC_LATITUDE], latitude);
			contentValues.put(LOC_FIELDS[LOC_LONGITUDE], longitude);
			locationRowID = mSQLiteDatabase.insert(DB_TABLE_LOCATION, null, contentValues);
		}

		// Test if artifact already exists
		long artifactId = getArtifact(name, data);

		if(0 <= artifactId) {

			// Artifact exists so we keep track of its rowID
			artifactRowId = artifactId;
		}
		else {

			// Artifact doesn't exist so we create a new db record for it 
			contentValues = new ContentValues();
			contentValues.put(ART_FIELDS[ART_NAME], name);
			contentValues.put(ART_FIELDS[ART_DATA], data);
			artifactRowId = mSQLiteDatabase.insert(DB_TABLE_ARTIFACT, null, contentValues);
		}

		// Test if location/artifact relationship already exists
		if(-1 == locationId || -1 == artifactId) {

			// The location and artifact relationship doesn't exist so we create a new db record for it
			contentValues = new ContentValues();
			contentValues.put(LOC_ART_FIELDS[FK_ART_ID], artifactRowId);
			contentValues.put(LOC_ART_FIELDS[FK_LOC_ID], locationRowID);
			locToArtRowId = mSQLiteDatabase.insert(DB_TABLE_LOC_TO_ART, null, contentValues);
		}

		// Returning the relevant rowIds
		return  new long[] {locationRowID, artifactRowId, locToArtRowId};
	}

	/*
	 * Deleting the location and artifact relationship. We only delete the location and or artifact if they
	 * are not referenced in any other relationship.
	 */
	public void delete(long artifactRowId, long locationRowId) {

		// Delete from location/artifact relationship
		mSQLiteDatabase.delete(DB_TABLE_LOC_TO_ART,
				LOC_ART_FIELDS[FK_ART_ID] + "=? AND " + LOC_ART_FIELDS[FK_LOC_ID] + "=?",
				new String[] {Long.toString(artifactRowId), Long.toString(locationRowId)});


		// Only delete location if it's not referenced by any other artifact
		if(!hasArtifactInLocToArtTable(artifactRowId)) {

			mSQLiteDatabase.delete(DB_TABLE_ARTIFACT,
					ART_FIELDS[ART_ID] + "=?",
					new String[] {Long.toString(artifactRowId)});
		}

		// Only delete artifact if it's not referenced by a location
		if(!hasLocationInLocToArtTable(locationRowId)) {

			mSQLiteDatabase.delete(DB_TABLE_LOCATION,
					LOC_FIELDS[LOC_ID] + "=?",
					new String[] {Long.toString(locationRowId)});
		}
	}
	/*
	 * Select all the location and artifact relationships
	 * NOTE: Caller must call cursor.close();
	 */
	public Cursor select() {

		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables("LocToArt JOIN Artifact ON (LocToArt.artId=Artifact._id) JOIN Location ON (LocToArt.locId=Location._id)");
		queryBuilder.setDistinct(true);
		return queryBuilder.query(mSQLiteDatabase,
				new String[] {"Artifact._id AS artifactid",
							  "Location._id AS locationid",
							  "Artifact.name AS name",
							  "Artifact.data AS data",
							  "Location.lat AS lat",
							  "Location.long AS long"},
				null, null, null, null, null);
	}


	/*
	 * Helper method that checks if the provided artifactRowId is part of an existing location and artifact relationship
	 */
	private boolean hasArtifactInLocToArtTable(long artifactRowId) {

		Cursor cursor = mSQLiteDatabase.query(true, DB_TABLE_LOC_TO_ART, LOC_ART_FIELDS, LOC_ART_FIELDS[FK_ART_ID] + "=" + Long.toString(artifactRowId), null, null, null, null, null);

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
	private boolean hasLocationInLocToArtTable(long locationRowId) {

		Cursor cursor = mSQLiteDatabase.query(true, DB_TABLE_LOC_TO_ART, LOC_ART_FIELDS, LOC_ART_FIELDS[FK_LOC_ID] + "=" + Long.toString(locationRowId), null, null, null, null, null);

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
	 * Helper method that searches by latitude and longitude for an existing location
	 */
	private long getLocation(String latitude, String longitude) {

		Cursor cursor = mSQLiteDatabase.query(true,
				DB_TABLE_LOCATION, LOC_FIELDS, LOC_FIELDS[LOC_LATITUDE] + "=? AND " + LOC_FIELDS[LOC_LONGITUDE] + "=?",
				new String [] {latitude, longitude},
				null,
				null,
				null,
				null);

		if((null != cursor) && (0 < cursor.getCount())) {
			cursor.moveToFirst();
			int idColumnIndex = cursor.getColumnIndex("_id");
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
			int idColumnIndex = cursor.getColumnIndex("_id");
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
			
			Log.w(TAG, "Upgrading database from verison " + oldVersion + " to " + newVersion + ", which will destroy all existing data");
		}
	}
}
