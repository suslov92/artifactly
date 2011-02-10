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

$(document).ready(function() {
	
	/*
	 * Initialize
	 */
	$('#preferences').bind('pageshow', function(){
		
		$('#radiusInput').val(window.android.getRadius());
	});

	
	/*
	 * Set radius button click handler
	 */
	$('#setRadius').click(function() {
		
		var radius = $('#radiusInput').val();
		window.android.setRadius(+radius);
	});

	/*
	 * Get radius button click handler
	 */
	$('#getRadius').click(function() {
		
		var radius = window.android.getRadius();
		$('#radiusOutput').text("Radius: " + radius);
	});
	
	/*
	 * Stop location tracking click handler
	 */
	$('#stopLocationTracking').click(function() {
		
		window.android.stopLocationTracking();
	});
	
	/*
	 * Start location tracking click handler
	 */
	$('#startLocationTracking').click(function() {
		
		window.android.startLocationTracking();
	});
	
	/*
	 * Create artifact click handler
	 */
	$('#createArtifact').click(function() {
		
		window.android.createArtifact();
	});
});

function showServiceResult(data) {
	
	// Show result
	$('#latitude').text("Latitude: " + data[0].lat);
	$('#longitude').text("Longitude: " + data[0].long);
	$('#artifactName').text("Name: " + data[0].name);
	$('#artifactData').text("Data: " + data[0].data);
	$('#distance').text("Distance: " + data[0].dist);
}


