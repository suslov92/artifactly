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

/*
 * JavaScript to Java calls
 */
function getRadius() {
	
	var radius = window.android.getRadius();
	
	document.getElementById("radiusOutput").innerHTML = "Radius: " + radius;
}

function startLocationTracking() {

	// Make Android interface call
	window.android.startLocationTracking();
}

function stopLocationTracking() {
	
	// Make Android interface call
	window.android.stopLocationTracking();
}

function createArtifact() {
	
	// Make Android interface call
	window.android.createArtifact();
}

/*
 * Java to JavaScript calls
 */

function showServiceResult(data) {
	
	// Show result
	document.getElementById("latitude").innerHTML = "Latitude: " + data[0].lat;
	document.getElementById("longitude").innerHTML = "Longitude: " + data[0].long;
	document.getElementById("artifactName").innerHTML = "Name: " + data[0].name;
	document.getElementById("artifactData").innerHTML = "Data: " + data[0].data;
	document.getElementById("distance").innerHTML = "Distance: " + data[0].dist;
}

function setRadius() {
	
	// Get radius from UI
	var radius = document.getElementById("radiusInput").value;
	document.getElementById("radiusInput").value = "";
	
	// TODO: Either check user input if it's numeric and within bounds or use number slider component
	// Make Android interface call, casting radius into a number using the unary '+' operator
	window.android.setRadius(+radius);
}