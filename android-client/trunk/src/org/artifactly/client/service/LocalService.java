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

public interface LocalService {

	/**
	 * Creates an Artifact for the provided location
	 * 
	 * @param artifactName Artifact name
	 * @param artifactData Artifact data
	 * @param locationName Artifact's location name
	 * @param latitude Artifact latitude
	 * @param longitude Artifact longitude
	 * @return -1 on error, 0 on cannot create artifact because of the provided location name, 1 on success
	 */
	public int createArtifact(String artifactName, String artifactData, String locationName, String latitude, String longitude);
	
	/**
	 * Creates an Artifact at the current location
	 * 
	 * @param artifactName Artifact name
	 * @param artifactData Artifact data
	 * @param locationName Location name
	 * @return -1 on error, 0 on cannot create artifact because of the provided location name, 1 on success
	 */
	public int createArtifact(String artifactName, String artifactData, String locationName);
	
	/**
	 * Update an Artifact
	 * 
	 * @param artifactId
	 * @param artifactName
	 * @param artifactData
	 * @param locationId
	 * @param locationName
	 * @return false on error, otherwise true
	 */
	public boolean updateArtifact(String artifactId, String artifactName, String artifactData, String locationId, String locationName);
	
	/**
	 * Starts the location updates
	 * 
	 * @return
	 */
	public boolean startLocationTracking();
	
	/**
	 * Stops the location updates
	 * 
	 * @return
	 */
	public boolean stopLocationTracking();
	

	/**
	 * Get the current location, latitude, longitude, accuracy
	 * 
	 * @return JSON string containing latitude, longitude, accuracy
	 */
	public String getLocation();
	
	/**
	 * Get all artifacts
	 * 
	 * @return JSON all artifacts
	 */
	public String getArtifacts();
	
	/**
	 * Get artifacts for current location
	 * 
	 * @return JSON current location artifacts
	 */
	public String getArtifactsForCurrentLocation();
	
	/**
	 * Check if Internet access is available 
	 * 
	 * @return false if no Internet access is available, otherwise true
	 */
	public boolean canAccessInternet();
	
	
	/**
	 * Delete an artifact
	 * 
	 * @param artifactId
	 * @param locationId
	 * @return -1 on error, 0 on cannot delete because artifact has associated locations, 1 on success
	 */
	public int deleteArtifact(String artifactId, String locationId);
	
	/**
	 * 
	 * @param locationId
	 * @return -1 on error, 0 on cannot delete because location has associated artifacts, 1 on success
	 */
	public int deleteLocation(String locationId);
	
	/**
	 * Get an artifact that matches the provided id
	 * 
	 * @param id Artifact DB row id
	 * @return JSON artifact that matches the provided id
	 */
	public String getAtrifact(Long id);
	
	/**
	 * Get all locations
	 * 
	 * @return JSON containing all locations
	 */
	public String getLocations();

}
