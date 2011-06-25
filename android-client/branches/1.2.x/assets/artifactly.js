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
	 * Variable used for Google local search API to keep track of search center point
	 */
	$('body').data({ searchCenterPoinLatLng : null });
	
	/*
	 * Boolean flag that is used to determine if we need to rebuild the location selection options.
	 * When the new artifact's page is shown, we get all the currently stored locations. We don't want to 
	 * do that if we just added a new location option via the 'new location' page.
	 */
	$('body').data({ refreshLocations : true });

	/*
	 * Clicking on an artifact list item
	 */
	$('#artifactly-list').delegate('.artifactly-list-item', 'click', function(event) {  

		var artId = $(this).data("artId");
		window.android.getArtifact(artId);
	});
	
	/*
	 * Click on a location list item
	 */
	$('#artifactly-list').delegate('.artifactly-list-divider', 'click', function(event) {  

		var location = $(this).data();
		$('#view-location-loc-name').data(location);
		$('#view-location-loc-name').val(location.locName);
		$('#view-location-address').html(location.locAddress);
		
		// Remove an existing stale map before we add a new one
		$('#view-location-map img').remove();
		
		// Add new map
		if(window.android.canLoadStaticMap()) {
			
			$('<img/>')
			.attr('src', getMapImage(location.locLat, location.locLng, "15", "250", "200"))
			.appendTo($('#view-location-map'));
		}
		
		addLocationAddressToViewLocationPage(location.locLat, location.locLng);
		$('#delete-location-name').data({ navigateTo : '#main' });
		$.mobile.changePage($('#view-location'), "none");
	});
	
	/*
	 * Clicking on a location list item
	 */
	$('#manage-locations-list').delegate('li', 'click', function(event) {  

		var location = $(this).data();
		$('#view-location-loc-name').data(location);
		$('#view-location-loc-name').val(location.locName);
		$('#view-location-address').html(location.locAddress);
		
		// Remove stale map image
		$('#view-location-map img').remove();
		
		// Add new map
		if(window.android.canLoadStaticMap()) {
						
			$('<img/>')
			.attr('src', getMapImage(location.locLat, location.locLng, "15", "250", "200"))
			.appendTo($('#view-location-map'));
		}
		
		$('#delete-location-name').data({ navigateTo : '#manage-locations' });
		$.mobile.changePage($('#view-location'), "none");
	});
	
	/*
	 * Swiping right on an artifact list item, starts the delete dialog
	 */
	$('#artifactly-list').delegate('.artifactly-list-item', 'swiperight', function(event) {
		
		var artifact = $(this).data();
		$('#delete-artifact-name').html(artifact.artName);
		$('#delete-artifact-name').data(artifact);
		$.mobile.changePage($('#artifact-dialog'), "none");
	});
	
	/*
	 * Swiping right on a location list item, starts the delete dialog
	 */
	$('#manage-locations-list').delegate('li', 'swiperight', function(event) {

		$('#delete-location-name').html($(this).data("locName"));
		$('#delete-location-name').data({ locId : $(this).data("locId") });
		$('#delete-location-name').data({ navigateTo : '#manage-locations' });
		$.mobile.changePage($('#location-dialog'), "none");
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
		$('body').data({ refreshLocations : false });
		$.mobile.changePage($('#new-artifact'), "none");
	});

	/*
	 * Clicking on the artifact deletion dialog yes button
	 */
	$('#delete-artifact-yes').click(function(event) {

		var data = $('#delete-artifact-name').data();
		window.android.deleteArtifact(data.artId, data.locId);
		$('.ui-dialog').dialog('close');
		$.mobile.changePage($('#main'), "none");
	});
	
	/*
	 * Clicking on the location deletion dialog yes button
	 */
	$('#delete-location-yes').click(function(event) {

		var locId = $('#delete-location-name').data("locId");
		var navigateTo = $('#delete-location-name').data("navigateTo");
		window.android.deleteLocation(locId);
		$('.ui-dialog').dialog('close');
		$.mobile.changePage($(navigateTo), "none");
	});
	
	/*
	 * Clicking on the artifact deletion dialog cancel button
	 */
	$('#delete-artifact-cancel').click(function(event) {

		$('#artifact-dialog').dialog('close');
	});
	
	/*
	 * Clicking on the location deletion dialog cancel button
	 */
	$('#delete-location-cancel').click(function(event) {

		var navigateTo = $('#delete-location-name').data("navigateTo");
		$('#location-dialog').dialog('close');
		$.mobile.changePage($(navigateTo), "none");
	});
	
	/*
	 * Clicking on the manage locations button
	 */
	$('#manage-locations-button').click(function(event) {
		
		// NOTE: the getLocations callback will switch to the manage locations page
		window.android.getLocations("list");
	});
		
	/*
	 * Initialize the new artifact page
	 */
	$('#new-artifact').bind('pageshow', function() {
		
		$('#artifact-location-name-div').hide();
		
		if($('body').data("refreshLocations")) {
		
			window.android.getLocations("options");
		}
		
		$('body').data({ refreshLocations : true });
	});
	
	/*
	 * Initialize the option's page
	 */
	$('#options').bind('pageshow', function() {

		
		var preferences = JSON.parse(window.android.getPreferences());
		
		// Setting radius preference
		$('#radius-input').val(preferences.radius).slider('refresh');
		
		// Setting sound notification preference
		$('#sound-notification-option-checkbox-text .ui-btn-text').text("Sound Notification: " + (preferences.soundNotification ? "On" : "Off"));
		$('#sound-notification-option-checkbox').prop("checked", preferences.soundNotification).checkboxradio("refresh");

		// Setting load static map preference
		$('#load-static-map-option-checkbox-text .ui-btn-text').text("Load Maps: " + (preferences.loadStaticMap ? "On" : "Off"));
		$('#load-static-map-option-checkbox').prop("checked", preferences.loadStaticMap).checkboxradio("refresh");
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
		$('#search-result-message').html('');
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
	 * Update artifact
	 */
	$('#update-artifact').click(function() {
		
		var data = $('#view-artifact-art-name').data();
		var artName = $('#view-artifact-art-name').val();
		var artData = $('#view-artifact-art-data').val();
		var locName = $('#view-artifact-loc-name').val();
		
		/*
		 * Only update if the artifact name, or the artifact data, or the location name changed
		 */
		if(data.artName != artName || data.artData != artData || data.locName != locName) {
		
			window.android.updateArtifact(data.artId, artName, artData, data.locId, locName);
		}
	});
	
	/*
	 * Update location
	 */
	$('#update-location').click(function() {
	
		var location = $('#view-location-loc-name').data();
		var updatedLocName = $('#view-location-loc-name').val();
		
		/*
		 * Only update if the location name changed
		 * TODO: Allow the user to change lat/lng via moving the marker on a map
		 */
		if(location.locName != updatedLocName) {
			
			/*
			 * Setting a flag on the view-location so that we know not to navigate to
			 * a different page when the getLocationsListCallback() is executed
			 */
			$('#view-location').data({navigate:'no'});
			window.android.updateLocation(location.locId, updatedLocName, location.locLat, location.locLng);
		}
	});

	/*
	 * Delete artifact
	 */
	$('#delete-artifact').click(function() {
		
		var data = $('#view-artifact-art-name').data();
		$('#delete-artifact-name').html(data.artName);
		$('#delete-artifact-name').data(data);
		$.mobile.changePage($('#artifact-dialog'), "none");
	});
	
	/*
	 * Delete location
	 */
	$('#delete-location').click(function() {
	
		var location = $('#view-location-loc-name').data();
		$('#delete-location-name').html(location.locName);
		$('#delete-location-name').data(location);
		$.mobile.changePage($('#location-dialog'), "none");
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
		$.mobile.changePage($('#main'), "none");
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
		}
		else {
		
			window.android.createArtifact(artName, artData, selectedLocation.locName, selectedLocation.locLat, selectedLocation.locLng);
		}
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
			if($('body').data("searchCenterPoinLatLng")) {

				localSearch.setCenterPoint($('body').data("searchCenterPoinLatLng"));

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
				
				$('#search-result-message').html("<p>Searching ...</p>");
			}
			else {
				
				console.log("ERROR: Search center point is not defined");
			}
		}
	});

	/*
	 * Options' page select menu
	 */
	$('#option-select-background-color').change(function() {

		var selected = $('#option-select-background-color option:selected');
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
	 * Options' page sound notification on/off
	 */
	$('#sound-notification-option-checkbox').bind('change', function() {
		
		var checked = $('#sound-notification-option-checkbox').prop("checked");
		
		if(checked) {
			
			$('#sound-notification-option-checkbox-text .ui-btn-text').text("Sound Notification: On");
		}
		else {
			
			$('#sound-notification-option-checkbox-text .ui-btn-text').text("Sound Notification: Off");
		}
		
		window.android.setSoundNotificationPreference(checked);
	});

	/*
	 * Options' page load static map of/off
	 */
	$('#load-static-map-option-checkbox').bind('change', function() {
		
		var checked = $('#load-static-map-option-checkbox').prop("checked");
		
		if(checked) {
			
			$('#load-static-map-option-checkbox-text .ui-btn-text').text("Load Maps: On");
		}
		else {
			
			$('#load-static-map-option-checkbox-text .ui-btn-text').text("Load Maps: Off");
		}
		
		window.android.setLoadStaticMapPreference(checked);
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
			
			/*
			 * For now, we load the static map in this specific case even if the user sets the option to load maps to false
			 * TODO: Check how we can provide current location information if the user doesn't want to load map images
			 */
			if(window.android.canAccessInternet()) {
			
				$('#artifact-current-location-map img').attr('src', getMapImage(location.locLat, location.locLng, "15", "250", "200"));
			}
		}
		else if(selectedLocation.locName == "New Location") {

			$.mobile.changePage($('#new-location'), "none");
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
			
			$('#search-result-message').html('');
			
			// First we check if the user wants to load map images
			var canLoadStaticMap = window.android.canLoadStaticMap();
			
			// Iterate over the search result
			for (var i = 0; i < localSearch.results.length; i++) {
				
				var imgHtml = "";
				
				if(canLoadStaticMap) {
					
					imgHtml = '<img src="' + getMapImage(localSearch.results[i].lat, localSearch.results[i].lng, "13", "78", "78") + '"/>';
				}
								
				$('<li/>', { html : imgHtml +
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
		else {
			
			$('#search-result-message').html("<p>No Search results</p>");
		}
	});
}

/*
 * Map page callback
 */
function loadMap() {

	$(document).ready(function() {
		
		var location = JSON.parse(window.android.getLocation());
		var latlng = new google.maps.LatLng(location.locLat, location.locLng);

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

		var content = "Latitude = " + (location.locLat).toFixed(6) + "<br />Longitude = " + (location.locLng).toFixed(6) +"<br />Accuracy = " + (location.locAccuracy).toFixed(2) + " m";
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
 * Create artifact callback
 */
function createArtifactCallback(data) {
	
	// If the createArtifact call succeeded, we clear the form fields
	if(data.isSuccess) {

		$('#artifact-name').val('');
		$('#artifact-data').val('');
		$('#artifact-location-name').val('');
	}
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
function getArtifactCallback(artifact) {

	$(document).ready(function() {

		/*
		 * Attach the artifact id to the artifact name field so that
		 * we can use it to update the artifact values
		 */
		$('#view-artifact-art-name').data(artifact);
		$('#view-artifact-art-name').val(artifact.artName);
		$('#view-artifact-art-data').val(artifact.artData);
		$('#view-artifact-loc-name').val(artifact.locName);
		$('#view-artifact-lat').val((+artifact.lat).toFixed(6));
		$('#view-artifact-lng').val((+artifact.lng).toFixed(6));

		// Remove stale map image
		$('#view-artifact-map img').remove();

		// Add new map
		if(window.android.canLoadStaticMap()) {

			$('<img/>')
			.attr('src', getMapImage(artifact.lat, artifact.lng, "15", "250", "200"))
			.appendTo($('#view-artifact-map'));
		}

		$.mobile.changePage($('#view-artifact'), "none");
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

				$('<li/>', { html : '<p><img src="images/map-marker.png" /></p>' +
									'<h3>' + location.locName + '</h3>' +
									'<span class="ui-li-count">' + location.artifacts.length + '</span>'})
				.attr('data-role', 'list-divider')
				.data(location)
				.addClass('artifactly-list-divider')
				.appendTo($('#artifactly-list ul'));

				$.each(location.artifacts, function(j, artifact) {
					$('<li/>', { html : '<h3>' + artifact.artName + '</h3>' })
					.data({ artId : artifact.artId, artName : artifact.artName, locId : location.locId })
					.addClass('artifactly-list-item')
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
		.attr('disabled', 'disabled')
		.data({ locId : '', locName : '', locLat : '', locLng : '' })
		.appendTo($('#artifact-location-selection'));
		
		// Adding new location option
		$('<option/>', { text :  'New Location' })
		.data({ locId : '', locName : 'New Location', locLat : '', locLng : '' })
		.appendTo($('#artifact-location-selection'));
		
		// Adding current locaiton
		$('<option/>', { text :  'Current Location' })
		.data({ locId : '', locName : 'Current Location', locLat : '0', locLng : '0' })
		.appendTo($('#artifact-location-selection'));
		
		// Adding all stored locations
		if(locations.length > 0) {
			
			$.each(locations, function(i, location) {
				
				$('<option/>', { text : location.locName})
				.data(location)
				.appendTo($('#artifact-location-selection'));
			});
		}
		
		$('#artifact-location-selection').selectmenu('refresh');
	});
}

function getLocationsListCallback(locations) {
	
	$(document).ready(function() {
	
		/*
		 * In case this callback was triggered by a locaiton update, we don't navigate
		 * to another page
		 */
		if("no" != $('#view-location').data("navigate")) {
			
			// This call has to occur before the manage-locations-list is manipulated via remove, refresh, etc.
			$.mobile.changePage($('#manage-locations'), "none");
		}
		else {
			
			$('#view-location').data({navigate:'yes'});
		}

		// Reset the list
		$('#manage-locations-list li').remove();
		$('#manage-locations-list ul').listview('refresh');

		if(!locations || locations.length < 1) {

			$('#manage-locations-list-message').text("There are no locations");
		}
		else {

			var canAccessInternet = window.android.canAccessInternet();

			$('#manage-locations-list-message').text("");
			$.each(locations, function(i, location) {

				var element = $('<li/>', { html : '<h3>' + location.locName + '</h3>' })
				.data(location)
				.appendTo($('#manage-locations-list ul'));

				if(canAccessInternet) {

					/*
					 * The following method will append the address to the <li/> element
					 */
					appendLocationAddress(location.locLat, location.locLng, element);
				}
			});
		}

		$('#manage-locations-list ul').listview('refresh');
	});
}

/*
 * Helper method that appends the formatted address to the provided DOM element
 */
function appendLocationAddress(lat, lng , element) {
	
	$(document).ready(function() {
		
		$.ajax({
			type:'Get',
			url:'http://maps.googleapis.com/maps/api/geocode/json?address=' + lat + ',' + lng + '&sensor=true',
			success:function(data) {

				element.append('<p>' + data.results[0].formatted_address + '</p>');
				element.data({ locAddress : data.results[0].formatted_address });
			}
		});
	});
}

/*
 * Helper method that set the formatted address to the "view-location-address" div tag in the "view-location" page
 */
function addLocationAddressToViewLocationPage(lat, lng) {
	
	$(document).ready(function() {
	
		$.ajax({
			type:'Get',
			url:'http://maps.googleapis.com/maps/api/geocode/json?address=' + lat + ',' + lng + '&sensor=true',
			success:function(data) {
				
				$('#view-location-address').html(data.results[0].formatted_address);
			}
		});
	});
}

/*
 * Method that resets the the view to the main page
 */
function resetWebView() {

	$(document).ready(function() {
		
		$.mobile.changePage($('#main'), "none");
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
	
	$(document).ready(function() {

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
	});
}

function getSearchCenterPoint() {
	
	$(document).ready(function() {

		var location = JSON.parse(window.android.getLocation());
		var latlng = new google.maps.LatLng(location.locLat, location.locLng);

		$('body').data({ searchCenterPoinLatLng : latlng });
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

	$(document).ready(function() {

		$.mobile.changePage($('#options'), "none");
	});
}

/*
 * Android menu option: Map
 */
function showMapPage() {
	
	$(document).ready(function() {

		$.mobile.changePage($('#map'), "none");
	});
}

/*
 * Android menu option: Info
 */
function showAppInfoPage() {

	$(document).ready(function() {

		$.mobile.changePage($('#app-info'), "none");
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

/*
 * Helper method that creates an URL that returns a Google Maps image
 */
function getMapImage(lat, lng, zoom, width, height) {

	return "http://maps.google.com/maps/api/staticmap?center=" + lat + "," + lng + "&zoom=" + zoom + "&size=" + width + "x" + height + "&markers=color:red%7Csize:small%7C" + lat + "," + lng + "&sensor=false";
}

