package org.artifactly.service.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;

import org.artifactly.service.ioc.ServiceModule;
import org.artifactly.service.pojo.Artifact;
import org.artifactly.service.pojo.Location;
import org.datanucleus.store.appengine.query.JDOCursorHelper;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.Key;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class ArtifactServiceImpl implements ArtifactService {

	private PersistenceManagerFactory persistenceManagerFactory = null;
	private LocationService locationService = null;
	private Injector injector = Guice.createInjector(new ServiceModule());
	private static final String ZERO_OFFSET = "0";
	private static final int ZERO = 0;
	

	@Inject
	public ArtifactServiceImpl() {

		persistenceManagerFactory = injector.getInstance(Manager.class).getFactory();
		locationService = injector.getInstance(LocationService.class);
	}

	public Artifact create(Artifact artifact, Location location) {

		// First we check if the location already exists
		Location existingLocation = locationService.get(location.getLatitude(), location.getLongitude());
		
		PersistenceManager persistenceManager = persistenceManagerFactory.getPersistenceManager();

		try {

			if(null == existingLocation) {
			
				persistenceManager.makePersistentAll(artifact, location);
			}
			else {
				
				persistenceManager.makePersistent(artifact);
			}

		} finally {

			persistenceManager.close();
		}
		
		if(null == existingLocation) {
		
			return setRelations(artifact, location);
		}
		else {
		
			return setRelations(artifact, existingLocation.getKey());
		}
	}

	public List<Artifact> getAll() {

		PersistenceManager persistenceManager = persistenceManagerFactory.getPersistenceManager();
		Query query = persistenceManager.newQuery(Artifact.class);

		List<Artifact> artifacts = null;

		try {
			
			artifacts = (List<Artifact>) query.execute();
			persistenceManager.makeTransientAll(artifacts);

		} finally {

			persistenceManager.close();
		}

		return artifacts;
	}

	public PageableResults<Artifact> get(String offset, long limit) {

		PersistenceManager persistenceManager = persistenceManagerFactory.getPersistenceManager();

		PageableResults<Artifact> pageableResults = new PageableResults<Artifact>();
		
		Query query = persistenceManager.newQuery(Artifact.class);
		
		if(null != offset && !"".equals(offset) && !ZERO_OFFSET.equals(offset)) {
		
			Cursor cursor = Cursor.fromWebSafeString(offset);
	        Map<String, Object> extensionMap = new HashMap<String, Object>();
	        extensionMap.put(JDOCursorHelper.CURSOR_EXTENSION, cursor);
	        query.setExtensions(extensionMap);
		}
		
		query.setRange(ZERO, limit);

		List<Artifact> artifacts = null;
		
		try {
			
			artifacts = (List<Artifact>) query.execute();
			Cursor cursor = JDOCursorHelper.getCursor(artifacts);
			pageableResults.setOffset(cursor.toWebSafeString());
			persistenceManager.makeTransientAll(artifacts);
			
		} finally {
			
			persistenceManager.close();
		}
		
		pageableResults.setResults(artifacts);
		return pageableResults;
	}

	@Override
	public List<Artifact> get(double latitude, double longitude) {

		Location location = locationService.get(latitude, longitude);
		
		// TODO: Add error handling in case location is null
		
		PersistenceManager persistenceManager = persistenceManagerFactory.getPersistenceManager();
		Query query = persistenceManager.newQuery(Artifact.class);
		query.setFilter("locations.contains(locationKey)");
		query.declareParameters("com.google.appengine.api.datastore.Key locationKey");
		
		List<Artifact> artifacts = null;
		
		try {
		
			artifacts = (List<Artifact>) query.execute(location.getKey());
			persistenceManager.makeTransientAll(artifacts);
			
		} finally {
			
			persistenceManager.close();
		}

		return artifacts;
	}
	
	private Artifact setRelations(Artifact artifact, Location location) {

		PersistenceManager persistenceManager = persistenceManagerFactory.getPersistenceManager();

		Artifact persistedArtifact = null;
		Location persistedLocation = null;

		try {

			persistedArtifact = persistenceManager.getObjectById(Artifact.class, artifact.getKey());
			persistedLocation = persistenceManager.getObjectById(Location.class, location.getKey());
			persistedLocation.getArtifacts().add(persistedArtifact.getKey());
			persistedArtifact.getLocations().add(persistedLocation.getKey());
			
			persistenceManager.makeTransient(persistedArtifact);

		} finally {

			persistenceManager.close();
		}

		return persistedArtifact;
	}
	
	
	private Artifact setRelations(Artifact artifact, Key locationKey) {

		PersistenceManager persistenceManager = persistenceManagerFactory.getPersistenceManager();

		Artifact persistedArtifact = null;
		Location persistedLocation = null;

		try {

			persistedArtifact = persistenceManager.getObjectById(Artifact.class, artifact.getKey());
			persistedLocation = persistenceManager.getObjectById(Location.class, locationKey);
			persistedLocation.getArtifacts().add(persistedArtifact.getKey());
			persistedArtifact.getLocations().add(persistedLocation.getKey());
			
			persistenceManager.makeTransient(persistedArtifact);

		} finally {

			persistenceManager.close();
		}

		return persistedArtifact;
	}
}
