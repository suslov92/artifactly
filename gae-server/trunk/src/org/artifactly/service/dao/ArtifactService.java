package org.artifactly.service.dao;

import java.util.List;

import org.artifactly.service.pojo.Artifact;
import org.artifactly.service.pojo.Location;

public interface ArtifactService {

	public Artifact create(Artifact artifact, Location location);
	
	public List<Artifact> getAll();
	
	public PageableResults<Artifact> get(String offset, long limit);
	
	public List<Artifact> get(double latitude, double longitude);
}
