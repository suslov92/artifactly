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

/*
 * Boolean flag that is used to determine if we need to rebuild the location selection options.
 * When the new artifact's page is shown, we get all the currently stored locations. We don't want to 
 * do that if we just added a new location option via the 'new location' page.
 */
var refreshLocations = true;


$(document).ready(function() {

	/*
	 * Clicking on an artifact list item
	 */
	$('#artifactly-list').delegate('li', 'click', function(event) {  

		var id = $(this).data("artId");
		window.android.getArtifact(+id);
	});
	
	/*
	 * Swiping right on an artifact list item, starts the delete dialog
	 */
	$('#artifactly-list').delegate('li', 'swiperight', function(event) {
		
		$.mobile.changePage("#artifact-dialog", "none");
		$('#delete-artifact-name').html($(this).data("artName"));
		$('#delete-artifact-name').data({ artId : $(this).data("artId"), locId : $(this).data("locId") });
		console.log("xlxlxlxlxllx locId = " + $(this).data("locId"));
	});
	
	/*
	 * Swiping right on a location list item, starts the delete dialog
	 */
	$('#manage-locations-list').delegate('li', 'swiperight', function(event) {

		$.mobile.changePage("#location-dialog", "none");
		$('#delete-location-name').html($(this).data("locName"));
		$('#delete-location-name').data({ locId : $(this).data("locId") });
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

		var artId = $('#delete-artifact-name').data("artId");
		var locId = $('#delete-artifact-name').data("locId");
		window.android.deleteArtifact(artId, locId);
		$('.ui-dialog').dialog('close');
	});
	
	/*
	 * Clicking on the location deletion dialog yes button
	 */
	$('#delete-location-yes').click(function(event) {

		var locId = $('#delete-location-name').data("locId");
		window.android.deleteLocation(locId);
		$('.ui-dialog').dialog('close');
	});
	
	/*
	 * Clicking on the artifact deletion dialog cancel button
	 */
	$('#delete-artifact-cancel').click(function(event) {

		$('.ui-dialog').dialog('close');
	});
	
	/*
	 * Clicking on the location deletion dialog cancel button
	 */
	$('#delete-location-cancel').click(function(event) {

		$('.ui-dialog').dialog('close');
	});
	
	/*
	 * Clicking on the manage locations button
	 */
	$('#manage-locations-button').click(function(event) {
		
		console.log("DEBUG: manage-locations-button clicked");
		
		$.mobile.changePage("#manage-locations", "none");
	});
	
	/*
	 * Initialize the manage locations page
	 */
	$('#manage-locations').bind('pageshow', function() {
		
		window.android.getLocations("list");
	});
	
	/*
	 * Initialize the new artifact page
	 */
	$('#new-artifact').bind('pageshow', function() {
		
		$('#artifact-location-name-div').hide();
		
		if(refreshLocations) {
		
			window.android.getLocations("options");
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
		
		// Showing the loading animation
		$.mobile.pageLoading();

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
		
		$('#search-result-list li').remove();
		$('#search-result-list ul').listview('refresh');
		
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
	 * Update the artifact
	 */
	$('#update-artifact').click(function() {
		
		var artId = $('#view-artifact-art-name').data("artId");
		var locId = $('#view-artifact-art-name').data("locId");
		var artName = $('#view-artifact-art-name').val();
		var artData = $('#view-artifact-art-data').val();
		var locName = $('#view-artifact-loc-name').val();
		
		window.android.updateArtifact(artId, artName, artData, locId, locName);
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
		
		if(selectedLocation.locName == "Current Location") {
		
			var locationName = $('#artifact-location-name').val();
			window.android.createArtifact(artName, artData, locationName, selectedLocation.locLat, selectedLocation.locLng);
			$('#artifact-location-name').val('');
		}
		else {
		
			window.android.createArtifact(artName, artData, selectedLocation.locName, selectedLocation.locLat, selectedLocation.locLng);
		}
		
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
	$('#option-select-back-ground-color').change(function() {

		var selected = $('#option-select-back-ground-color option:selected');
		var color = '#ADDFFF';
		
		if(selected.val() == "blue") {
			
			$('.ui-page').css('background', '#ADDFFF');
			color = "#ADDFFF";
		}
		else if(selected.val() == "pink") {
			
			$('.ui-page').css('background', '#FFCECE');
			color = "#FFCECE";
		}
		else if(selected.val() == "green") {
			
			$('.ui-page').css('background', '#D2FFC4');
			color = "#D2FFC4";
		}
		else if(selected.val() == "white") {
			
			$('.ui-page').css('background', '#FFFFFF');
			color = "#FFFFFF";
		}
		
		window.android.setBackgroundColor(color);
	});
	
	/*
	 * Locations' select menu
	 */
	$('#artifact-location-selection').change(function() {
		
		var selectedLocation = $('#artifact-location-selection option:selected').data();
		
		// If the selection is Current Location, then we need to allow the user to enter a location name
		if(selectedLocation.locName == "Current Location") {
			
			$('#artifact-location-name').val('');
			$('#artifact-location-name-div').show();
			
			var location = JSON.parse(window.android.getLocation());
			$('#artifact-current-location-map img').attr('src', getMapImage(location[0], location[1], "15", "250", "200"));

		}
		else if(selectedLocation.locName == "New Location") {

			$.mobile.changePage("#new-location", "none");
		}
		else {

			$('#artifact-location-name-div').hide();
		}
	});
}); // END jQuery main block

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
								
				$('<li/>', { html : '<img src="' + getMapImage(localSearch.results[i].lat, localSearch.results[i].lng, "13", "78", "78") + '"/>'+
									'<h3>' + localSearch.results[i].title + '</h3>' +
									'<p>' + localSearch.results[i].addressLines[0] + '</p>' +
									'<p>' + localSearch.results[i].city + '</p>' })        
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
		
		// Hide the maps loading animation
		$.mobile.pageLoading(true);
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

	$(document).ready(function() {
		
		if(data.length != 1) {

			// In case of an error, we go back to the main page
			$.mobile.changePage("#main", "none");
		}
		else {

			/*
			 * Attach the artifact id to the artifact name field so that
			 * we can use it to update the artifact values
			 */
			$('#view-artifact-art-name').data({ artId : data[0].artId, locId : data[0].locId });
			$('#view-artifact-art-name').val(data[0].artName);
			$('#view-artifact-art-data').val(data[0].artData);
			$('#view-artifact-loc-name').val(data[0].locName);
			$('#view-artifact-lat').val((+data[0].lat).toFixed(6));
			$('#view-artifact-lng').val((+data[0].lng).toFixed(6));
			$('#view-artifact-map img').attr('src', getMapImage(data[0].lat, data[0].lng, "14", "250", "200"));
		}
	});
}

function getArtifactsForCurrentLocationCallback(locations) {

	$(document).ready(function() {

		$('#artifactly-list li').remove();
		$('#artifactly-list ul').listview('refresh');
		
		if(!locations || locations.length < 1) {

			$('#artifactly-message').text("There are no Artifacts close by");
		}
		else {

			$('#artifactly-message').text("");
			$.each(locations, function(i, location) {

				$('<li/>', { html : location.locName })
				.attr('data-role', 'list-divider')
				.appendTo($('#artifactly-list ul'));

				$.each(location.artifacts, function(j, artifact) {
					$('<li/>', { html : '<a href="#view-artifact" data-transition="none">' + artifact.artName + '</a>' })
					.data({ artId : artifact.artId, artName : artifact.artName, locId : location.locId })
					.appendTo($('#artifactly-list ul'))
				});
			});

			$('#artifactly-list ul').listview('refresh');
		}
	});
}

function getLocationsOptionsCallback(locations) {
	
	$(document).ready(function() {
	
		// Remove all existing location options
		$('#artifact-location-selection option').remove();
		
		// Adding Choose one
		$('<option/>', { text :  'Choose one ...' })
		.attr('data-placeholder', 'true')
		.data({
			locId : '',
			locName : '',
			locLat : '',
			locLng : ''
		})
		.appendTo($('#artifact-location-selection'));
		
		// Adding new location option
		$('<option/>', { text :  'New Location' })
		.data({
			locId : '',
			locName : 'New Location',
			locLat : '',
			locLng : ''
		})
		.appendTo($('#artifact-location-selection'));
		
		// Adding current locaiton
		$('<option/>', { text :  'Current Location' })
		.data({
			locId : '',
			locName : 'Current Location',
			locLat : '0',
			locLng : '0'
		})
		.appendTo($('#artifact-location-selection'));
		
		// Adding all stored locations
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

function getLocationsListCallback(locations) {
	
	
	// Reset the list
	$('#manage-locations-list li').remove();
	$('#manage-locations-list ul').listview('refresh');
	
	if(!locations || locations.length < 1) {

		$('#manage-locations-list-message').text("There are no locations yet");
	}
	else {
		
		$('#manage-locations-list-message').text("");
		$.each(locations, function(i, location) {
			
			$('<li/>', { html : '<h3>' + location.locName + '</h3>' })
			.data({
				locId : location.locId,
				locName : location.locName,
				locLat : location.locLat,
				locLng : location.locLng
			})
			.appendTo($('#manage-locations-list ul'));
		});
	}
	
	$('#manage-locations-list ul').listview('refresh');
}


/*
 * Method that resets the the view to the main page
 */
function resetWebView() {

	$(document).ready(function() {
		
		$.mobile.changePage("#main", "none");
	});
}

function setBackgroundColor(color) {
	
	$(document).ready(function() {
		
		$('.ui-page').css('background', color.bgc);
	});
}

function showServiceResult(data) {

	$(document).ready(function() {

		$('#artifactly-list li').remove();

		$.each(data, function(i, val) {

			$('#artifactly-list ul').append('<li><a href="#view-artifact" data-transition="none">' + val.name + '</a></li>');
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
 * Android menu option: Options
 */
function showOptionsPage() {
	
	$.mobile.changePage("#options", "none");
}

/*
 * Android menu option: Map
 */
function showMapPage() {
	
	$.mobile.changePage("#map", "none");
}

/*
 * Android menu option: Info
 */
function showAppInfoPage() {
	
	$.mobile.changePage("#app-info", "none");
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

/*
 * Helper method that creates an URL that returns a Google Maps image
 */
function getMapImage(lat, lng, zoom, width, height) {
	
	var canAccessInternet = window.android.canAccessInternet();
	
	if(canAccessInternet) {

		return "http://maps.google.com/maps/api/staticmap?center=" + lat + "," + lng + "&zoom=" + zoom + "&size=" + width + "x" + height + "&markers=color:red%7Csize:small%7C" + lat + "," + lng + "&sensor=false";
	}
	else {
		
		return "";
	}
}