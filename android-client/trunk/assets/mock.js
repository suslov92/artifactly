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

		return "[]";
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


	window.android.logArtifacts = function() {

		return "[]";
	}
}