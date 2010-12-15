package org.artifactly.service.dao;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

import com.google.inject.Inject;

public class ManagerImpl implements Manager {

	private static final PersistenceManagerFactory persistenceManagerFactory = JDOHelper.getPersistenceManagerFactory("transactions-optional");

	@Inject
	public ManagerImpl() {
		
	}

	public PersistenceManagerFactory getFactory() {
		
		return persistenceManagerFactory;
	}
}
