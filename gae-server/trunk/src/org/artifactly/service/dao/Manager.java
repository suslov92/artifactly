package org.artifactly.service.dao;

import javax.jdo.PersistenceManagerFactory;

public interface Manager {

	public PersistenceManagerFactory getFactory();
}
