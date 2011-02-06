
function getRadius() {
	
	var radius = window.android.getRadius();
	
	document.getElementById("radiusOutput").innerHTML = "Radius: " + radius;
}

function startLocationTracking() {

	// Make Android interface call
	window.android.startLocationTracking();
}

function stopLocationTracking() {
	
	// Make Android interface call
	window.android.stopLocationTracking();
}

function createArtifact() {
	
	// Make Android interface call
	window.android.createArtifact();
}

function showServiceResult(data) {
	
	// Show result
	document.getElementById("latitude").innerHTML = "Latitude: " + data[0].lat;
	document.getElementById("longitude").innerHTML = "Longitude: " + data[0].long;
	document.getElementById("artifactName").innerHTML = "Name: " + data[0].name;
	document.getElementById("artifactData").innerHTML = "Data: " + data[0].data;
	document.getElementById("distance").innerHTML = "Distance: " + data[0].dist;
}

function setRadius() {
	
	// Get radius from UI
	var radius = document.getElementById("radiusInput").value;
	document.getElementById("radiusInput").value = "";
	
	// Make Android interface call
	window.android.setRadius(radius);
}