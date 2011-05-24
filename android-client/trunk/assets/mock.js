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

		var data = '[{"data":"prius 20 k","artId":1,"long":"-121.4020047","lat":"38.6300699","name":"car service"},{"data":"","artId":2,"long":"-121.5799187","lat":"38.5400857","name":"home "},{"data":"art","artId":3,"long":"-121.4763178","lat":"38.5745222","name":"art beast "},{"data":"2","artId":4,"long":"-121.55368518","lat":"38.586812609999996","name":"bed"}]';
	
		getArtifactsForCurrentLocation(JSON.parse(data));
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

		var data = '[{"data":"prius 20 k","artId":1,"long":"-121.4020047","lat":"38.6300699","name":"car service"},{"data":"","artId":2,"long":"-121.5799187","lat":"38.5400857","name":"home "},{"data":"art","artId":3,"long":"-121.4763178","lat":"38.5745222","name":"art beast "},{"data":"2","artId":4,"long":"-121.55368518","lat":"38.586812609999996","name":"bed"}]';
	
		getArtifactsCallback(JSON.parse(data));
	}
	
	window.android.getArtifact = function(id) {
		
		var data = '[{"data":"clean","artId":9,"long":"-121.57963725","lat":"38.5399477","name":"room"}]';

		getArtifactCallback(JSON.parse(data));
	}
	
	window.android.getGoogleSearchApiKey = function() {
		
		return "YOUR-API-KEY";
	}
}