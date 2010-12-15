package org.artifactly.service.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;

import org.artifactly.service.ioc.ServiceModule;
import org.artifactly.service.pojo.Location;
import org.datanucleus.store.appengine.query.JDOCursorHelper;

import com.google.appengine.api.datastore.Cursor;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class LocationServiceImpl implements LocationService {

	private PersistenceManagerFactory persistenceManagerFactory = null;
	private Injector injector = Guice.createInjector(new ServiceModule());
	private static final String ZERO_OFFSET = "0";
	private static final int ZERO = 0;

	@Inject
	public LocationServiceImpl() {

		persistenceManagerFactory = injector.getInstance(Manager.class).getFactory();
	}

	public List<Location> getAll() {

		PersistenceManager persistenceManager = persistenceManagerFactory.getPersistenceManager();
		
		Query query = persistenceManager.newQuery(Location.class);
		
		List<Location> locations = null;
		
		try {
			
			locations = (List<Location>) query.execute();
			persistenceManager.makeTransientAll(locations);
			
		} finally {
			
			persistenceManager.close();
		}
		
		return locations;
	}

	@Override
	public PageableResults<Location> get(String offset, long limit) {

		PersistenceManager persistenceManager = persistenceManagerFactory.getPersistenceManager();

		PageableResults<Location> pageableResults = new PageableResults<Location>();
		
		Query query = persistenceManager.newQuery(Location.class);
		
		if(null != offset && !"".equals(offset) && !ZERO_OFFSET.equals(offset)) {
			
			Cursor cursor = Cursor.fromWebSafeString(offset);
	        Map<String, Object> extensionMap = new HashMap<String, Object>();
	        extensionMap.put(JDOCursorHelper.CURSOR_EXTENSION, cursor);
	        query.setExtensions(extensionMap);
		}
		
		
		query.setRange(ZERO, limit);

		List<Location> locations = null;

		try {

			locations = (List<Location>) query.execute();
			Cursor cursor = JDOCursorHelper.getCursor(locations);
			pageableResults.setOffset(cursor.toWebSafeString());
			persistenceManager.makeTransientAll(locations);

		} finally {

			persistenceManager.close();
		}
		pageableResults.setResults(locations);
		return pageableResults;
	}

	@Override
	public Location create(Location location) {

		PersistenceManager persistenceManager = persistenceManagerFactory.getPersistenceManager();
		Location persistedLocation = null;

		try {

			persistedLocation = persistenceManager.makePersistent(location);
			persistenceManager.makeTransient(persistedLocation);

		} finally {

			persistenceManager.close();
		}

		return persistedLocation;
	}

	@Override
	public Location get(double latitude, double longitude) {

		PersistenceManager persistenceManager = persistenceManagerFactory.getPersistenceManager();
		Query query = persistenceManager.newQuery(Location.class);
		query.setFilter("latitude == latitudeParam && longitude == longitudeParam");
		query.declareParameters("double latitudeParam, double longitudeParam");

		try {

			List<Location> persistedLocations = (List<Location>) query.execute(latitude, longitude);

			if(persistedLocations.size() > 0) {
				
				persistenceManager.makeTransientAll(persistedLocations);
				return persistedLocations.get(0);
			}
		}
		finally {

			persistenceManager.close();
		}

		return null;
	}
}
