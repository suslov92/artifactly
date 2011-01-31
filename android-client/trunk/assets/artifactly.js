

function showServiceResult(data) {
	
	document.getElementById("latitude").innerHTML = "Latitude: " + data[0].lat;
	document.getElementById("longitude").innerHTML = "Longitude: " + data[0].long;
	document.getElementById("artifactName").innerHTML = "Name: " + data[0].name;
	document.getElementById("artifactData").innerHTML = "Data: " + data[0].data;
	document.getElementById("distance").innerHTML = "Distance: " + data[0].dist;
}

function setRadius() {
	
	var radius = document.getElementById("radius").value;
	
	window.android.setRadius(radius);
}