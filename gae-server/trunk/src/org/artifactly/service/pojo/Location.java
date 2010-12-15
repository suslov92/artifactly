package org.artifactly.service.pojo;

import java.util.HashSet;
import java.util.Set;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

@PersistenceCapable(detachable = "true")
public class Location {

	@PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;
	
	@Persistent
	private double latitude;
	
	@Persistent
	private double longitude;
	
	@Persistent
	private Set<Key> artifacts = new HashSet<Key>();
	
	public Location(double latitude, double longitude) {
		
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public Key getKey() {
		return key;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public Set<Key> getArtifacts() {
		return artifacts;
	}

	public void setArtifacts(Set<Key> artifacts) {
		this.artifacts = artifacts;
	}
}
