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
 * Keeping track of the search center point
 */
var searchCenterPoinLatLng;

$(document).ready(function() {

	/*
	 * Clicking on the a list item, stores the item's id in localStorage
	 */
	$('#artifactly-list').delegate('li', 'click', function(event) {  

		localStorage['artifactId'] = $(this).attr('title');
	});

	/*
	 * Clicking on the artifact deletion dialog yes button
	 */
	$('#delete-artifact-yes').click(function(event) {

		var id = localStorage['deleteArtifactId'];
		window.android.deleteArtifact(+id);
		$('#artifactly-list li').remove('[title="' + id + '"]');
	});

	/*
	 * Clicking on the a list item, stores the item's id in localStorage
	 */
	$('#artifactly-list-debug').delegate('li', 'click', function(event) {  

		localStorage['artifactId'] = $(this).attr('title');
	});

	/*
	 * Show the result after clicking on a list item
	 */
	$('#selection-result').bind('pageshow', function() {

		var id = localStorage.getItem('artifactId');
		window.android.getArtifact(+id);
	});

	/*
	 * Initialize the option's page
	 */
	$('#options').bind('pageshow', function() {

		$('#radius-input').val(window.android.getRadius());
		$('#radius-input').slider('refresh');	
	});

	/*
	 * Loading the map and marker on the map page
	 */
	$('#map').bind('pageshow', function() {

		loadMapApi('loadMap');
	});
	
	/*
	 * Initialize Google's JSAPI when entering the create artifact page
	 */
	$('#new-location').bind('pageshow', function() {
		
		$('#new-location-center-point').html("Loading ...");
		$('#search-result').html('');
		$('#entered-search-term').html('');
		$('#google-search-branding').html('');
		
		loadMapApi('getSearchCenterPoint');
		
		var canAccessInternet = window.android.canAccessInternet();
		if(canAccessInternet && typeof(google) == "undefined") {
			
			// Can access the Internet, thus we can load the Google JSAPI
			var apiKey = window.android.getGoogleSearchApiKey();
			var script = document.createElement("script");
			script.src = "https://www.google.com/jsapi?key=" + apiKey + "&callback=loadSearchApi";
			script.type = "text/javascript";
			document.getElementsByTagName("head")[0].appendChild(script);
		}
		else if(canAccessInternet && typeof(google.search) == "undefined") {
			
			// Can access the Internet, thus we can load the Google JSAPI
			var apiKey = window.android.getGoogleSearchApiKey();
			var script = document.createElement("script");
			script.src = "https://www.google.com/jsapi?key=" + apiKey + "&callback=loadSearchApi";
			script.type = "text/javascript";
			document.getElementsByTagName("head")[0].appendChild(script);
		}
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

		var artName = $('#artifact-name').val();
		var artData = $('#artifact-data').val();
		var locName = $('#artifact-location').val();
		
		window.android.createArtifact(artName, artData, locName);

		$('#artifact-name').val('');
		$('#artifact-data').val('');
		$('#artifact-location').val('');
	});
	
	/*
	 * Search location button
	 */
	$('#search-location-button').click(function() {
		
		var search = $('#search-entry').val();
		
		if(!search || "" === search) {
			return;
		}
		
		$('#entered-search-term').html("<b>Search result for:</b>&nbsp;" + search);
		$('#search-entry').val('');
		
		if(google.search) {
			
			// Create a LocalSearch instance.
			var localSearch = new google.search.LocalSearch();

			// Set the Local Search center point
			if(searchCenterPoinLatLng) {

				localSearch.setCenterPoint(searchCenterPoinLatLng);

				// Set searchComplete as the callback function when a search is complete. The
				// localSearch object will have results in it.
				var result = new Array();
				result[0] = localSearch;
				localSearch.setSearchCompleteCallback(this, searchComplete, result);

				// Specify search query
				localSearch.execute(search);

				// Include the required Google branding.
				// Note that getBranding is called on google.search.Search
				google.search.Search.getBranding('google-search-branding');
			}
			else {
				console.log("ERROR: Search center point is not defined");
			}
		}
	});

	/*
	 * Debug select menu
	 */
	$('#select-debug').change(function() {

		var selected = $('#select-debug option:selected');

		if(selected.val() == "get-location") {

			var data = JSON.parse(window.android.getLocation());
			$('#log-latitude').text("Latitude: " + data[0].toFixed(6));
			$('#log-longitude').text("Longitude: " + data[1].toFixed(6));
			$('#log-accuracy').text("Accuracy: " + data[2]);
			$('#log-time').text("Time: " + data[3]);
			$('#log-time-latest').text("Last: " + data[4]);
		}
		else if(selected.val() == "show-map") {

			$.mobile.changePage("#map", "none");
		}
		else if(selected.val() == "get-artifacts") {

			$.mobile.changePage("#debug-result", "none");

			window.android.getArtifacts();			
		}
	});
});

/*
 * Load Google search API
 */
function loadSearchApi() {

	var canAccessInternet = window.android.canAccessInternet();
	if(canAccessInternet) {

		google.load("search", "1", {"callback" : onLoadSearchApi});
	}
}

/*
 * Google search api callback
 */
function onLoadSearchApi() {
	
	if(typeof(google.search) == "undefined") {
		
		console.log("ERROR: Google Search API did not load");
	}
}

function searchComplete(localSearch) {
	
	$(document).ready(function() {
	
		if (localSearch.results && localSearch.results.length > 0) {

			$('#search-result').html("<br />").append("Result: ").append(localSearch.results.length).append("<br />");
			
			for (var i = 0; i < localSearch.results.length; i++) {
				
				$('#search-result').append(localSearch.results[i].title + " : ").append("LAT = " + localSearch.results[i].lat + " : ").append("LNG = " + localSearch.results[i].lng).append("<br />");
			}
		}
	});
}

/*
 * Map page callback
 */
function loadMap() {

	$(document).ready(function() {
		
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

		var content = "Latitude = " + (data[0]).toFixed(6) + "<br />Longitude = " + (data[1]).toFixed(6) +"<br />Accuracy = " + data[2] + " m";
		var infowindow = new google.maps.InfoWindow({
			content: content
		});

		google.maps.event.addListener(marker, 'click', function() {
			infowindow.open(map,marker);
		});    
	});
}

function getArtifactsCallback(artifacts) {

	$(document).ready(function() {

		$('#artifactly-list-debug li').remove();
		$('#artifactly-list-debug ul').listview('refresh');

		if(artifacts.length < 1) {

			$('#artifactly-message-debug').text("There are no Artifacts");
		}
		else {

			$('#artifactly-message-debug').text("");
			$.each(artifacts, function(i, val) {

				$('#artifactly-list-debug ul').append('<li title="' + val.artId + '"><a href="#selection-result" data-transition="none">' + val.artName + '</a></li>');
			});

			$('#artifactly-list-debug ul').listview('refresh');
		}
	});
}

function getArtifactCallback(data) {

	if(data.length != 1) {

		// In case of an error, we go back to the main page
		$.mobile.changePage("#main", "none");
	}
	else {

		$(document).ready(function() {
			$('#selection-result-art-name').val(data[0].artName);
			$('#selection-result-art-data').val(data[0].artData);
			$('#selection-result-loc-name').val(data[0].locName);
			$('#selection-result-lat').val((+data[0].lat).toFixed(6));
			$('#selection-result-lng').val((+data[0].lng).toFixed(6));
		});
	}
}

function getArtifactsForCurrentLocationCallback(artifacts) {

	$(document).ready(function() {

		$('#artifactly-list li').remove();
		$('#artifactly-list ul').listview('refresh');
		
		if(artifacts.length < 1) {

			$('#artifactly-message').text("There are no Artifacts close by");
		}
		else {

			$('#artifactly-message').text("");
			$.each(artifacts, function(i, val) {

				$('#artifactly-list ul').append('<li title="' + val.artId + '"><a href="#selection-result" data-transition="none">' + val.artName + '</a></li>');
			});

			$('#artifactly-list ul').listview('refresh');
		}

		$('#artifactly-list li').each(function (idx) {

			$(this).bind('swiperight', function(event,ui) {

				// Showing dialog. Data is removed if the user clicks on the dialog's yes button
				$.mobile.changePage("#dialog", "none");
				localStorage['deleteArtifactId'] = $(this).attr('title');
			});
		});
	});
}

function showServiceResult(data) {

	$(document).ready(function() {

		$('#artifactly-list li').remove();

		$.each(data, function(i, val) {

			$('#artifactly-list ul').append('<li><a href="#selection-result" data-transition="none">' + val.name + '</a></li>');
		});

		$('#artifactly-list ul').listview('refresh');
	});
}

function loadMapApi(callback) {
	
	/*
	 * First we check if we have Internet access. The Activity will show a 
	 * message if we don't have Internet access
	 */
	var canAccessInternet = window.android.canAccessInternet();
	if(canAccessInternet && typeof(google) == "undefined") {
		
		// Can access the Internet, thus we can load the Google maps API and map
		$.getScript('http://maps.google.com/maps/api/js?sensor=true&callback=' + callback);
	}
	else if(canAccessInternet && typeof(google.maps) == "undefined") {
		
		// Can access the Internet, thus we can load the Google maps API and map
		$.getScript('http://maps.google.com/maps/api/js?sensor=true&callback=' + callback);
	}
	else {
		
		/*
		 * Since the maps api is loaded via "show map" or "new location" we have
		 * to make sure that if the api is loaded, that we still execute the callback
		 */
		eval(callback + "()");
	}
}

function getSearchCenterPoint() {
	
	$(document).ready(function() {

		var data = JSON.parse(window.android.getLocation());
		var latlng = new google.maps.LatLng(data[0], data[1]);

		searchCenterPoinLatLng = latlng;
		geocoder = new google.maps.Geocoder(); 

		geocoder.geocode({'latLng':latlng}, function(results, status) {

			if (status == google.maps.GeocoderStatus.OK) {
				
				$('#new-location-center-point').html(results[0].formatted_address);
				
			} else {
				
				console.log("ERROR: Geocode was not successful for the following reason: " + status);
			}
		});
	});
}

