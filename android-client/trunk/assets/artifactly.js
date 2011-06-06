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
var refreshLocations = true;

$(document).ready(function() {

	/*
	 * Clicking on an artifact list item, stores the item's id in localStorage
	 */
	$('#artifactly-list').delegate('li', 'click', function(event) {  

		var id = $(this).attr('data-artId');
		window.android.getArtifact(+id);
	});
	
	/*
	 * Swiping right on an artifact list item, starts the delete dialog
	 */
	$('#artifactly-list').delegate('li', 'swiperight', function(event) {
		
		$.mobile.changePage("#dialog", "none");
		localStorage['deleteArtifactId'] = $(this).attr("data-artId");
	});
	
	/*
	 * Clicking on a search result list item, populates the location options menu
	 */
	$('#search-result-list').delegate('li', 'click', function(event) {
		
		$('<option/>', { text : $(this).data("locName")})
		.data({
			locId : '',
			locName : $(this).data("locName"),
			locLat : $(this).data("locLat"),
			locLng : $(this).data("locLng")
		})
		.attr('selected', 'selected')
		.appendTo($('#artifact-location-selection'));
		
		$('#artifact-location-selection').selectmenu('refresh');
		
		refreshLocations = false;
		
		$.mobile.changePage("#new-artifact", "none");
	});

	/*
	 * Clicking on the artifact deletion dialog yes button
	 */
	$('#delete-artifact-yes').click(function(event) {

		var id = localStorage['deleteArtifactId'];
		window.android.deleteArtifact(+id);
		$('#artifactly-list li').remove('[data-artId="' + id + '"]');
	});
	
	/*
	 * Initialize the new artifact page
	 */
	$('#new-artifact').bind('pageshow', function() {
		
		if(refreshLocations) {
		
			window.android.getLocations();
		}
		
		refreshLocations = true;
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
	
	/*
	 * Close the selected artifact page and navigate to the home page
	 */
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

		$('#artifact-name').val('');
		$('#artifact-data').val('');
	});
	
	/*
	 * Create artifact close button
	 */
	$('#close-artifact-button').click(function() {

		$('#artifact-name').val('');
		$('#artifact-data').val('');
		$.mobile.changePage("#main", "none");
	});

	/*
	 * Create artifact add button
	 */
	$('#add-artifact-button').click(function() {

		var artName = $('#artifact-name').val();
		var artData = $('#artifact-data').val();
		
		var selectedLocation = $('#artifact-location-selection option:selected').data();
		
		window.android.createArtifact(artName, artData, selectedLocation.locName, selectedLocation.locLat, selectedLocation.locLng);

		$('#artifact-name').val('');
		$('#artifact-data').val('');
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
	 * Options' page select menu
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
 * Google search API callback
 */
function onLoadSearchApi() {
	
	if(typeof(google.search) == "undefined") {
		
		console.log("ERROR: Google Search API did not load");
	}
}

function searchComplete(localSearch) {
	
	$(document).ready(function() {
	
		// Reset the list
		$('#search-result-list li').remove();
		$('#search-result-list ul').listview('refresh');
		
		// Do we have any search results
		if (localSearch.results && localSearch.results.length > 0) {
			
			// Iterate over the search result
			for (var i = 0; i < localSearch.results.length; i++) {
				
				//$('<li/>', { html : '<a href="#new-artifact" data-transition="none">' + localSearch.results[i].title + '</a>' })
				$('<li/>', { html : localSearch.results[i].title })        
			      .data({
			    	locName : htmlDecode(stripHtml(localSearch.results[i].title)),
			    	locLat : localSearch.results[i].lat,
			    	locLng : localSearch.results[i].lng    
			      })        
			      .appendTo($('#search-result-list ul'));
			}
	
			// Refresh the list so that all the data is shown
			$('#search-result-list ul').listview('refresh');
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

/*
 * Get all artifacts callback
 */
function getArtifactsCallback(artifacts) {

	// Not needed yet
	console.log("ERROR: getArtifactsCallback() is not implemented.");
}

/*
 * Get artifact callback
 */
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
				
				$('<li/>', { html : '<a href="#selection-result" data-transition="none">' + val.artName + '</a>' })        
			      .attr('data-artId', val.artId)
			      .appendTo($('#artifactly-list ul'));
			});

			$('#artifactly-list ul').listview('refresh');
		}
	});
}

function getLocationsCallback(locations) {
	
	console.log("DEBUG: getLocationsCallback()");
	$(document).ready(function() {
		
		$('#artifact-location-selection option').remove();
		
		$('<option/>', { text :  'Choose one ...' })
		.attr('data-placeholder', 'true')
		.appendTo($('#artifact-location-selection'));
		
		if(locations.length > 0) {
			
			$.each(locations, function(i, val) {
				
				$('<option/>', { text : val.locName})
				.data({
					locId : val.locId,
					locName : val.locName,
					locLat : val.locLat,
					locLng : val.locLng
				})
				.appendTo($('#artifact-location-selection'));
			});
		}
		
		$('#artifact-location-selection').selectmenu('refresh');

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

/*
 * Helper method that strips HTML from a string
 */
function stripHtml(data) {
	
	return data.replace(/(<([^>]+)>)/ig,"");
}

/*
 * Helper method to HTML decode
 */
function htmlDecode(value) {
	
	return $('<div/>').html(value).text();
}

/*
 * Helper method to HTML encode
 */
function htmlEncode(value) {
	
	return $('<div/>').text(value).html();
}