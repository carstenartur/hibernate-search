/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search;

import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackend;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.impl.StubScopeModel;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionContext;

public class StubSearchQueryBuilder<H> implements SearchQueryBuilder<H, StubQueryElementCollector> {

	private final StubBackend backend;
	private final StubScopeModel scopeModel;
	private final StubSearchWork.Builder workBuilder;
	private final StubSearchProjectionContext projectionContext;
	private final LoadingContextBuilder<?, ?> loadingContextBuilder;
	private final StubSearchProjection<H> rootProjection;

	public StubSearchQueryBuilder(StubBackend backend, StubScopeModel scopeModel,
			StubSearchWork.ResultType resultType,
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<?, ?> loadingContextBuilder, StubSearchProjection<H> rootProjection) {
		this.backend = backend;
		this.scopeModel = scopeModel;
		this.workBuilder = StubSearchWork.builder( resultType );
		this.projectionContext = new StubSearchProjectionContext( sessionContext );
		this.loadingContextBuilder = loadingContextBuilder;
		this.rootProjection = rootProjection;
	}

	@Override
	public StubQueryElementCollector getQueryElementCollector() {
		return StubQueryElementCollector.get();
	}

	@Override
	public void addRoutingKey(String routingKey) {
		workBuilder.routingKey( routingKey );
	}

	@Override
	public SearchQuery<H> build() {
		return new StubSearchQuery<>(
				backend, scopeModel.getIndexNames(), workBuilder, projectionContext,
				loadingContextBuilder.build(), rootProjection
		);
	}
}
