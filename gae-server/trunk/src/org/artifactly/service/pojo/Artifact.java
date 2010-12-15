package org.artifactly.service.pojo;


import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.users.User;

@PersistenceCapable(detachable = "true")
public class Artifact {
	
	@PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;
	
	@Persistent
	private String name;
	
	@Persistent
	private User creator;
	
	@Persistent
	private Date creationDate;
	
	@Persistent
	private Date expirationDate;
	
	@Persistent
	private Boolean isPublic;
	
	@Persistent
	private Set<Key> locations = new HashSet<Key>();

	public Artifact(User creator, String name, Date creationDate, Boolean isPublic) {
		
		this.creator = creator;
		this.name = name;
		this.creationDate = creationDate;
		this.isPublic = isPublic;
	}
	
	public Key getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Key> getLocations() {
		return locations;
	}

	public void setLocations(Set<Key> locations) {
		this.locations = locations;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Date getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	public User getCreator() {
		return creator;
	}

	public void setCreator(User creator) {
		this.creator = creator;
	}

	public Boolean getIsPublic() {
		return isPublic;
	}

	public void setIsPublic(Boolean isPublic) {
		this.isPublic = isPublic;
	}
}
