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
	$('#options').bind('pageshow', function(){
		
		$('#radius-input').val(window.android.getRadius());
	});
		
	$('#map').bind('pageshow', function() {
		
		// First we check if we have Internet access
		var canAccessInternet = window.android.canAccessInternet();
		if(!canAccessInternet) {
			
			return;
		}
		
		var data = JSON.parse(window.android.getLocation());
		var latlng = new google.maps.LatLng(data[0], data[1]);
        
        var myOptions = {
              zoom: 15,
              center: latlng,
              mapTypeId: google.maps.MapTypeId.ROADMAP
            };
        
        var map = new google.maps.Map(document.getElementById("map-canvas"), myOptions);
        map.panTo(latlng);
       
        var marker = new google.maps.Marker();
        marker.setPosition(latlng);
        marker.setMap(map);
        marker.setAnimation(google.maps.Animation.DROP);
        
        var content = "Latitude = " + (data[0]).toFixed(4) + "<br />Longitude = " + (data[1]).toFixed(4) +"<br />Accuracy = " + data[2] + " m";
        var infowindow = new google.maps.InfoWindow({
            content: content
        });
        
        google.maps.event.addListener(marker, 'click', function() {
            infowindow.open(map,marker);
        });    
	});

	$('#close-and-home').click(function() {
		$.mobile.changePage("#main", "fade", false, false);
	});
	
	/*
	 * Set radius button click handler
	 */
	$('#set-radius').click(function() {
		
		var radius = $('#radius-input').val();
		window.android.setRadius(+radius);
	});

	/*
	 * Get radius button click handler
	 */
	$('#get-radius').click(function() {
		
		var radius = window.android.getRadius();
		$('#radius-output').text("Radius: " + radius);
	});
	
	/*
	 * Create artifact click handler
	 */
	$('#create-artifact').click(function() {
		
		window.android.createArtifact();
	});
	
	/*
	 * Get location click handler
	 */
	$('#get-location').click(function() {
	
		var data = JSON.parse(window.android.getLocation());
		$('#log-latitude').text("Latitude: " + data[0].toFixed(4));
		$('#log-longitude').text("Longitude: " + data[1].toFixed(4));
		$('#log-accuracy').text("Accuracy: " + data[2]);
		$('#log-time').text("Time: " + data[3]);
	});
	
	/*
	 * Show map page
	 */
	$('#show-map').click(function() {
		
		$.mobile.changePage("#map", "fade", false, false);
	});
	
	/*
	 * Create artifact cancel button
	 */
	$('#cancel-artifact-button').click(function() {
		
		$.mobile.changePage("#main", "fade", false, false);
	});
	
	/*
	 * Create artifact add button
	 */
	$('#add-artifact-button').click(function() {
		
		var name = $('#artifact-name').val();
		var data = $('#artifact-data').val();
		window.android.createArtifact(name, data);
		
		$('#artifact-name').val('');
		$('#artifact-data').val('');
	});
	
	/*
	 * Log artifacts
	 */
	$('#log-artifacts').click(function() {
		
		var artifacts = JSON.parse(window.android.logArtifacts());
		
		$('#artifactly-list li').remove();
		$('#artifactly-list ul').listview('refresh');
		
		$.each(artifacts, function(i, val) {
			$('#artifactly-list ul').append('<li><a href="#location-result">' + val.name + '</a></li>');
		});
		$('#artifactly-list ul').listview('refresh');
		
	});
	
});

function showServiceResult(data) {
	
	$.each(data, function(i, val) {
		$('#artifactly-list ul').append('<li><a href="#location-result">' + val.name + '</a></li>');
	});
	$('#artifactly-list ul').listview('refresh');
}

function initWebView() {
	
	$('#artifactly-list li').remove();
	$('#artifactly-list ul').listview('refresh');
}


