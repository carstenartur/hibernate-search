/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerStartContext;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkExecutor;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.work.execution.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.DetachedSessionContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl.StubDocumentElement;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

public class StubIndexManager implements IndexManagerImplementor<StubDocumentElement>, IndexManager {

	public static final StaticCounters.Key INSTANCE_COUNTER_KEY = StaticCounters.createKey();
	public static final StaticCounters.Key CLOSE_COUNTER_KEY = StaticCounters.createKey();

	private final StubBackend backend;
	private final String name;
	private final StubIndexSchemaNode rootSchemaNode;

	private boolean closed = false;

	StubIndexManager(StubBackend backend, String name, StubIndexSchemaNode rootSchemaNode) {
		StaticCounters.get().increment( INSTANCE_COUNTER_KEY );
		this.backend = backend;
		this.name = name;
		this.rootSchemaNode = rootSchemaNode;
		backend.getBehavior().pushSchema( name, rootSchemaNode );
	}

	@Override
	public void close() {

		/*
		 * This is important so that multiple calls to close on a single index manager
		 * won't be interpreted as closing multiple objects in test assertions.
		 */
		if ( closed ) {
			return;
		}
		StaticCounters.get().increment( CLOSE_COUNTER_KEY );
		closed = true;
	}

	@Override
	public String toString() {
		return StubIndexManager.class.getSimpleName() + "[" + name + "]";
	}

	@Override
	public void start(IndexManagerStartContext context) {
		// Nothing to do
	}

	@Override
	public IndexWorkPlan<StubDocumentElement> createWorkPlan(SessionContextImplementor context,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return new StubIndexWorkPlan( this, context, commitStrategy, refreshStrategy );
	}

	@Override
	public IndexDocumentWorkExecutor<StubDocumentElement> createDocumentWorkExecutor(SessionContextImplementor context,
			DocumentCommitStrategy commitStrategy) {
		return new StubIndexDocumentWorkExecutor( this, context, commitStrategy );
	}

	@Override
	public IndexWorkExecutor createWorkExecutor(DetachedSessionContextImplementor sessionContext) {
		return new StubIndexWorkExecutor( name, backend.getBehavior(), sessionContext );
	}

	@Override
	public IndexScopeBuilder createScopeBuilder(MappingContextImplementor mappingContext) {
		return new StubIndexScope.Builder( backend, mappingContext, name, rootSchemaNode );
	}

	@Override
	public void addTo(IndexScopeBuilder builder) {
		((StubIndexScope.Builder) builder ).add( backend, name, rootSchemaNode );
	}

	@Override
	public IndexManager toAPI() {
		return this;
	}

	@Override
	@SuppressWarnings("unchecked") // Checked using reflection
	public <T> T unwrap(Class<T> clazz) {
		if ( clazz.isAssignableFrom( IndexManager.class ) ) {
			return (T) this;
		}
		throw new SearchException( "Cannot unwrap " + this + " to " + clazz );
	}

	void prepare(List<StubDocumentWork> works) {
		backend.getBehavior().prepareDocumentWorks( name, works );
	}

	CompletableFuture<?> execute(List<StubDocumentWork> works) {
		return backend.getBehavior().executeDocumentWorks( name, works );
	}

	CompletableFuture<?> prepareAndExecuteWork(StubDocumentWork work) {
		return backend.getBehavior().prepareAndExecuteDocumentWork( name, work );
	}

	public void discard(List<StubDocumentWork> works) {
		backend.getBehavior().discardDocumentWorks( name, works );
	}
}
