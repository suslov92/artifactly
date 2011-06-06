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
 * This is a mock implementation of the Artifactly.java JavaScriptInterface,
 * which allows for UI testing in a browser
 * 
 */


// Only define window.android and its methods if it's undefined
if(typeof window.android == "undefined") {
		
	window.android = {};

	window.android.getArtifactsForCurrentLocation = function () {

		var data = '[{"artData":"prius 20 k","artId":1,"lng":"-121.4020047","lat":"38.6300699","artName":"car service","locName":"Toyota Dealer"},{"artData":"fun","artId":2,"lng":"-121.5799187","lat":"38.5400857","artName":"home","locName":"Home"},{"artData":"art","artId":3,"lng":"-121.4763178","lat":"38.5745222","artName":"art beast","locName":"Art Beast"},{"artData":"2","artId":4,"lng":"-121.55368518","lat":"38.586812609999996","artName":"bed","locName":"Somewhere"}]';
	
		getArtifactsForCurrentLocationCallback(JSON.parse(data));
	};

	window.android.getRadius = function () {

		return 1000;
	}

	window.android.canAccessInternet = function () {

		return true;
	}

	window.android.getLocation = function() {

		return '[38.5400438, -121.5798584, 84, "' + new Date() + '", "' + new Date() + '"]';
	}

	window.android.setRadius = function(radius) {

		alert("Setting Radius = " + radius);
	}

	window.android.showRadius = function() {

		alert("Setting Radius = 1000");
	}

	window.android.createArtifact = function(name, data) {

		alert("Created Artifact: name = " + name + " : data = " + data);
	}


	window.android.getArtifacts = function() {

		var data = '[{"artData":"prius 20 k","artId":1,"lng":"-121.4020047","lat":"38.6300699","artName":"car service","locName":"Toyota Dealer"},{"artData":"fun","artId":2,"lng":"-121.5799187","lat":"38.5400857","artName":"home","locName":"Home"},{"artData":"art","artId":3,"lng":"-121.4763178","lat":"38.5745222","artName":"art beast","locName":"Art Beast"},{"artData":"2","artId":4,"lng":"-121.55368518","lat":"38.586812609999996","artName":"bed","locName":"Somewhere"}]';
		
		getArtifactsCallback(JSON.parse(data));
	}
	
	window.android.getArtifact = function(id) {
		
		var data =  new Array();
		data[0] = "[{}]";
		data[1] = '[{"artData":"prius 20 k","artId":1,"lng":"-121.4020047","lat":"38.6300699","artName":"car service","locName":"Toyota Dealer"}]';
		data[2] = '[{"artData":"fun","artId":2,"lng":"-121.5799187","lat":"38.5400857","artName":"home","locName":"Home"}]';
		data[3] = '[{"artData":"art","artId":3,"lng":"-121.4763178","lat":"38.5745222","artName":"art beast","locName":"Art Beast"}]';
		data[4] = '[{"artData":"2","artId":4,"lng":"-121.55368518","lat":"38.586812609999996","artName":"bed","locName":"Somewhere"}]';
		getArtifactCallback(JSON.parse(data[id]));
	}
	
	window.android.getLocations = function() {
		
		var locations = '[{"locLat":"38.53991935","locId":1,"locLng":"-121.57961713333333","locName":"home"},{"locLat":"38.540062000000006","locId":2,"locLng":"-121.5798648","locName":"home"},{"locLat":"38.754536","locId":3,"locLng":"-121.252692","locName":"carmax"},{"locLat":"38.540018360000005","locId":4,"locLng":"-121.57981720000001","locName":"library"},{"locLat":"38.53995125","locId":5,"locLng":"-121.57965679999998","locName":"home "},{"locLat":"38.551167","locId":6,"locLng":"-121.70006","locName":"psl"},{"locLat":"38.539981600000004","locId":7,"locLng":"-121.5797042","locName":"cvvbbb"},{"locLat":"38.54002381428571","locId":8,"locLng":"-121.57970111428573","locName":"Target Specialty Products"},{"locLat":"38.563057","locId":9,"locLng":"-121.498367","locName":"Target"},{"locLat":"38.540013","locId":10,"locLng":"-121.57983","locName":"Amsler Consulting"}]';
	
		getLocationsCallback(JSON.parse(locations));
	}
	
	window.android.getGoogleSearchApiKey = function() {

		return "YourKey";
	}

	// Since all the current artifact loading is done from within the Activity, we need to simulate that call
	setTimeout("window.android.getArtifactsForCurrentLocation()", 500);
}