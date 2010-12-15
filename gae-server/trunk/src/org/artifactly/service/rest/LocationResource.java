package org.artifactly.service.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.artifactly.service.dao.LocationService;
import org.artifactly.service.dao.PageableResults;
import org.artifactly.service.ioc.ServiceModule;
import org.artifactly.service.pojo.Location;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sun.jersey.spi.resource.Singleton;

@Singleton
@Path("/location/")
public class LocationResource {

	private Injector injector = Guice.createInjector(new ServiceModule());
	private LocationService locationService = injector.getInstance(LocationService.class);
	
	@GET
	@Path("/all/")
	@Produces("application/json")
	public List<Location> getLocations() {
		
		return locationService.getAll();
	}
	
	@GET
	@Path("/page/{offset}/{limit}/")
	@Produces("application/json")
	public PageableResults<Location> getLocations(@PathParam("offset") String offset,
												  @PathParam("limit")  long limit) {

		return locationService.get(offset, limit);
	}
	
	@GET
	@Path("/{latitude}/{longitude}")
	@Produces("application/json")
	public Location getLocations(@PathParam("latitude") double latitude,
								 @PathParam("longitude")  double longitude) {

		return locationService.get(latitude, longitude);
	}
	
	@PUT
	@Path("/{latitude}/{longitude}")
	@Produces("application/json")
	public Location createLocation(@PathParam("latitude") double latitude,
								   @PathParam("longitude") double longitude) {

		Location location = new Location(latitude, longitude);
		return locationService.create(location);
	}
}
