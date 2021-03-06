/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.util.Collection;
import java.util.function.Supplier;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.search.SearchScope;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.session.SearchSessionWritePlan;

/**
 * A lazily initializing {@link SearchSession}.
 * <p>
 * This implementation allows to call {@link org.hibernate.search.mapper.orm.Search#session(Session)}
 * before Hibernate Search is fully initialized, which can be useful in CDI/Spring environments.
 */
public class LazyInitSearchSession implements SearchSession {

	private final Supplier<? extends HibernateOrmSearchSessionMappingContext> mappingContextProvider;
	private final SessionImplementor sessionImplementor;
	private HibernateOrmSearchSession delegate;

	public LazyInitSearchSession(Supplier<? extends HibernateOrmSearchSessionMappingContext> mappingContextProvider,
			SessionImplementor sessionImplementor) {
		this.mappingContextProvider = mappingContextProvider;
		this.sessionImplementor = sessionImplementor;
	}

	@Override
	public EntityManager toEntityManager() {
		return getDelegate().toEntityManager();
	}

	@Override
	public Session toOrmSession() {
		return getDelegate().toOrmSession();
	}

	@Override
	public <T> SearchScope<T> scope(Collection<? extends Class<? extends T>> types) {
		return getDelegate().scope( types );
	}

	@Override
	public SearchSessionWritePlan writePlan() {
		return getDelegate().writePlan();
	}

	@Override
	public void setAutomaticIndexingSynchronizationStrategy(
			AutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
		getDelegate().setAutomaticIndexingSynchronizationStrategy( synchronizationStrategy );
	}

	private HibernateOrmSearchSession getDelegate() {
		if ( delegate == null ) {
			delegate = HibernateOrmSearchSession.get( mappingContextProvider.get(), sessionImplementor );
		}
		return delegate;
	}
}
