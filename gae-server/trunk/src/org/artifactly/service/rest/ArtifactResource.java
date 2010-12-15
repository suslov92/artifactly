package org.artifactly.service.rest;

import java.util.Date;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.artifactly.service.dao.ArtifactService;
import org.artifactly.service.dao.PageableResults;
import org.artifactly.service.ioc.ServiceModule;
import org.artifactly.service.pojo.Artifact;
import org.artifactly.service.pojo.Location;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sun.jersey.spi.resource.Singleton;

@Singleton
@Path("/artifact/")
public class ArtifactResource {

	private Injector injector = Guice.createInjector(new ServiceModule());
	private ArtifactService artifactService = injector.getInstance(ArtifactService.class);
	
	@GET
	@Path("/all/")
	@Produces("application/json")
	public List<Artifact> getArtifacts() {

		return artifactService.getAll();
	}
	
	@GET
	@Path("/page/{offset}/{limit}/")
	@Produces("application/json")
	public PageableResults<Artifact> getArtifacts(@PathParam("offset") String offset,
												  @PathParam("limit")  long limit) {

		return artifactService.get(offset, limit);
	}

	@GET
	@Path("/{latitude}/{longitude}")
	@Produces("application/json")
	public List<Artifact> getLocations(@PathParam("latitude") double latitude,
									   @PathParam("longitude")  double longitude) {

		return artifactService.get(latitude, longitude);
	}

	@PUT
	@Path("/{latitude}/{longitude}/{name}/{isPublic}")
	@Produces("application/json")
	public Artifact createArtifact(@PathParam("latitude") double latitude,
								   @PathParam("longitude") double longitude,
								   @PathParam("name") String name,
								   @PathParam("isPublic") Boolean isPublic) {

		
		UserService userService = UserServiceFactory.getUserService();
		
		if(userService.isUserLoggedIn()) {
			
			User creator = userService.getCurrentUser();
			Artifact artifact = new Artifact(creator, name, new Date(), isPublic);
			Location location = new Location(latitude, longitude);
			return artifactService.create(artifact, location);
		}

		return null;
	}
}
