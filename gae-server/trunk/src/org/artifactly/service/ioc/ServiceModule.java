package org.artifactly.service.ioc;

import org.artifactly.service.dao.ArtifactService;
import org.artifactly.service.dao.ArtifactServiceImpl;
import org.artifactly.service.dao.LocationService;
import org.artifactly.service.dao.LocationServiceImpl;
import org.artifactly.service.dao.Manager;
import org.artifactly.service.dao.ManagerImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class ServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		
		bind(Manager.class).to(ManagerImpl.class).in(Scopes.SINGLETON);
		bind(ArtifactService.class).to(ArtifactServiceImpl.class).in(Scopes.SINGLETON);
		bind(LocationService.class).to(LocationServiceImpl.class).in(Scopes.SINGLETON);
	}
}
