/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.impl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.predicate.PredicateFinalStep;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactory;
import org.hibernate.search.engine.search.dsl.projection.ProjectionFinalStep;
import org.hibernate.search.engine.search.dsl.query.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.dsl.query.SearchQueryPredicateStep;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractSearchQueryHitTypeStep;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

public final class DefaultSearchQueryHitTypeStep<R, E, C>
		extends AbstractSearchQueryHitTypeStep<
						SearchQueryOptionsStep<?, E, ?>,
						R,
						E,
						SearchProjectionFactory<R, E>,
						SearchPredicateFactory,
						C
				> {

	private final IndexScope<C> indexScope;
	private final SessionContextImplementor sessionContext;
	private final LoadingContextBuilder<R, E> loadingContextBuilder;

	public DefaultSearchQueryHitTypeStep(IndexScope<C> indexScope,
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, E> loadingContextBuilder) {
		this.indexScope = indexScope;
		this.sessionContext = sessionContext;
		this.loadingContextBuilder = loadingContextBuilder;
	}

	@Override
	public SearchQueryPredicateStep<?, E, ?> asEntity() {
		SearchQueryBuilder<E, C> builder = indexScope.getSearchQueryBuilderFactory()
				.asEntity( sessionContext, loadingContextBuilder );
		return new DefaultSearchQueryOptionsStep<>( indexScope, builder );
	}

	@Override
	public SearchQueryPredicateStep<?, R, ?> asEntityReference() {
		SearchQueryBuilder<R, C> builder = indexScope.getSearchQueryBuilderFactory()
				.asReference( sessionContext, loadingContextBuilder );
		return new DefaultSearchQueryOptionsStep<>( indexScope, builder );
	}

	@Override
	public <P> SearchQueryPredicateStep<?, P, ?> asProjection(
			Function<? super SearchProjectionFactory<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor) {
		SearchProjectionFactory<R, E> factory = createDefaultProjectionFactory();
		SearchProjection<P> projection = projectionContributor.apply( factory ).toProjection();
		return asProjection( projection );
	}

	@Override
	public <P> SearchQueryPredicateStep<?, P, ?> asProjection(SearchProjection<P> projection) {
		SearchQueryBuilder<P, C> builder = indexScope.getSearchQueryBuilderFactory()
				.asProjection( sessionContext, loadingContextBuilder, projection );
		return new DefaultSearchQueryOptionsStep<>( indexScope, builder );
	}

	@Override
	public SearchQueryPredicateStep<?, List<?>, ?> asProjections(SearchProjection<?>... projections) {
		SearchQueryBuilder<List<?>, C> builder = indexScope.getSearchQueryBuilderFactory()
				.asProjections( sessionContext, loadingContextBuilder, projections );
		return new DefaultSearchQueryOptionsStep<>( indexScope, builder );
	}

	@Override
	public SearchQueryOptionsStep<?, E, ?> predicate(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor) {
		return asEntity().predicate( predicateContributor );
	}

	@Override
	public SearchQueryOptionsStep<?, E, ?> predicate(SearchPredicate predicate) {
		return asEntity().predicate( predicate );
	}

	@Override
	protected IndexScope<C> getIndexScope() {
		return indexScope;
	}

	@Override
	protected SessionContextImplementor getSessionContext() {
		return sessionContext;
	}

	@Override
	protected LoadingContextBuilder<R, E> getLoadingContextBuilder() {
		return loadingContextBuilder;
	}
}
