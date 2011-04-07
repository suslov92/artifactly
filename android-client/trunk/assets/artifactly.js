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
	

	$('#main').bind("pageshow", function() {
		
		var artifacts = JSON.parse(window.android.getArtifactsForCurrentLocation());
		
		$('#artifactly-list li').remove();
		$('#artifactly-list ul').listview('refresh');
		
		if(artifacts.length < 1) {
			$('#artifactly-message').text("There are no Artifacts close by");
		}
		else {
		
			$.each(artifacts, function(i, val) {
				$('#artifactly-list ul').append('<li><a href="#location-result" data-transition="none">' + val.name + '</a></li>');
			});
			$('#artifactly-list ul').listview('refresh');
		}
	});
	
	$('#options').bind('pageshow', function(){
		
		$('#radius-input').val(window.android.getRadius());
		$('#radius-input').slider('refresh');	
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
		$.mobile.changePage("#main", "none");
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
		
		window.android.showRadius();
	});
	
	/*
	 * Create artifact cancel button
	 */
	$('#cancel-artifact-button').click(function() {
		
		$.mobile.changePage("#main", "none");
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
	 * Debug select menu
	 */
	$('#select-debug').change(function() {
		
		var selected = $('#select-debug option:selected');
		
		if(selected.val() == "get-location") {
			
			var data = JSON.parse(window.android.getLocation());
			$('#log-latitude').text("Latitude: " + data[0].toFixed(4));
			$('#log-longitude').text("Longitude: " + data[1].toFixed(4));
			$('#log-accuracy').text("Accuracy: " + data[2]);
			$('#log-time').text("Time: " + data[3]);
			$('#log-time-latest').text("Last: " + data[4]);
			
		}
		else if(selected.val() == "show-map") {
			
			$.mobile.changePage("#map", "none");
		}
		else if(selected.val() == "get-artifacts") {
			
			$.mobile.changePage("#debug-result", "none");
			
			var artifacts = JSON.parse(window.android.logArtifacts());
			
			$('#artifactly-list-debug li').remove();
			$('#artifactly-list-debug ul').listview('refresh');
			
			if(artifacts.length < 1) {
				$('#artifactly-message-debug').text("There are no Artifacts");
			}
			else {

				$.each(artifacts, function(i, val) {
					$('#artifactly-list-debug ul').append('<li><a href="#location-result" data-transition="none">' + val.name + '</a></li>');
				});
				$('#artifactly-list-debug ul').listview('refresh');
			}
		}
	});
	
});

function showServiceResult(data) {
	
	$('#artifactly-list li').remove();
	$.each(data, function(i, val) {
		$('#artifactly-list ul').append('<li><a href="#location-result" data-transition="none">' + val.name + '</a></li>');
	});
	$('#artifactly-list ul').listview('refresh');
}

function initWebView() {
	
	$('#artifactly-list li').remove();
	$('#artifactly-list ul').listview('refresh');
}


