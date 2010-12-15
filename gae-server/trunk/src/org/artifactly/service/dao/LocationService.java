package org.artifactly.service.dao;

import java.util.List;
import org.artifactly.service.pojo.Location;

public interface LocationService {

	public List<Location> getAll();
	
	public PageableResults<Location> get(String offset, long limit);
	
	public Location get(double latitude, double longitude);
	
	public Location create(Location location);
}
