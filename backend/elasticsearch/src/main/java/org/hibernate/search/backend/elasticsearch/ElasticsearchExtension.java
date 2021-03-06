/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.search.dsl.projection.ElasticsearchSearchProjectionFactory;
import org.hibernate.search.backend.elasticsearch.search.dsl.projection.impl.ElasticsearchSearchProjectionFactoryImpl;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryHitTypeStep;
import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.impl.ElasticsearchSearchQueryHitTypeStepImpl;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.scope.impl.ElasticsearchIndexScope;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryExtension;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchIndexFieldTypeFactory;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.impl.ElasticsearchSearchPredicateFactoryImpl;
import org.hibernate.search.backend.elasticsearch.search.dsl.sort.ElasticsearchSearchSortFactory;
import org.hibernate.search.backend.elasticsearch.search.dsl.sort.impl.ElasticsearchSearchSortFactoryImpl;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilderFactory;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryExtension;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactory;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryExtension;
import org.hibernate.search.engine.search.dsl.query.SearchQueryDslExtension;
import org.hibernate.search.engine.search.dsl.query.SearchQueryHitTypeStep;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactory;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryExtension;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchQueryExtension;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * An extension for the Elasticsearch backend, giving access to Lucene-specific features.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called directly by users.
 * In short, users are only expected to get instances of this type from an API and pass it to another API.
 *
 * @param <H> The type of query hits.
 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
 * {@code .extension( ElasticsearchExtension.get() }.
 * @param <R> The entity reference type for projections.
 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
 * {@code .extension( ElasticsearchExtension.get() }.
 * @param <E> The entity type for projections.
 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
 * {@code .extension( ElasticsearchExtension.get() }.
 *
 * @see #get()
 */
public final class ElasticsearchExtension<H, R, E>
		implements SearchQueryDslExtension<ElasticsearchSearchQueryHitTypeStep<R, E>, R, E>,
		SearchQueryExtension<ElasticsearchSearchQuery<H>, H>,
		SearchPredicateFactoryExtension<ElasticsearchSearchPredicateFactory>,
		SearchSortFactoryExtension<ElasticsearchSearchSortFactory>,
		SearchProjectionFactoryExtension<ElasticsearchSearchProjectionFactory<R, E>, R, E>,
		IndexFieldTypeFactoryExtension<ElasticsearchIndexFieldTypeFactory> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ElasticsearchExtension<Object, Object, Object> INSTANCE = new ElasticsearchExtension<>();

	/**
	 * Get the extension with generic parameters automatically set as appropriate for the context in which it's used.
	 *
	 * @param <H> The type of query hits.
	 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
	 * {@code .extension( ElasticsearchExtension.get() }.
	 * @param <R> The entity reference type for projections.
	 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
	 * {@code .extension( ElasticsearchExtension.get() }.
	 * @param <E> The entity type for projections.
	 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
	 * {@code .extension( ElasticsearchExtension.get() }.
	 * @return The extension.
	 */
	@SuppressWarnings("unchecked") // The instance works for any H, R and E
	public static <H, R, E> ElasticsearchExtension<H, R, E> get() {
		return (ElasticsearchExtension<H, R, E>) INSTANCE;
	}

	private ElasticsearchExtension() {
		// Private constructor, use get() instead.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<ElasticsearchSearchQueryHitTypeStep<R, E>> extendOptional(
			SearchQueryHitTypeStep<?, R, E, ?, ?> original,
			IndexScope<?> indexScope,
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, E> loadingContextBuilder) {
		if ( indexScope instanceof ElasticsearchIndexScope ) {
			return Optional.of( new ElasticsearchSearchQueryHitTypeStepImpl<>(
					(ElasticsearchIndexScope) indexScope, sessionContext, loadingContextBuilder
			) );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<ElasticsearchSearchQuery<H>> extendOptional(SearchQuery<H> original,
			LoadingContext<?, ?> loadingContext) {
		if ( original instanceof ElasticsearchSearchQuery ) {
			return Optional.of( (ElasticsearchSearchQuery<H>) original );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <C, B> Optional<ElasticsearchSearchPredicateFactory> extendOptional(
			SearchPredicateFactory original, SearchPredicateBuilderFactory<C, B> factory) {
		if ( factory instanceof ElasticsearchSearchPredicateBuilderFactory ) {
			return Optional.of( new ElasticsearchSearchPredicateFactoryImpl(
					original, (ElasticsearchSearchPredicateBuilderFactory) factory
			) );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked") // If the factory is an instance of ElasticsearchSearchSortBuilderFactory, the cast is safe
	public Optional<ElasticsearchSearchSortFactory> extendOptional(
			SearchSortFactory original, SearchSortDslContext<?, ?> dslContext) {
		if ( dslContext.getBuilderFactory() instanceof ElasticsearchSearchSortBuilderFactory ) {
			return Optional.of( new ElasticsearchSearchSortFactoryImpl(
					original,
					(SearchSortDslContext<ElasticsearchSearchSortBuilderFactory, ElasticsearchSearchSortBuilder>) dslContext
			) );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<ElasticsearchSearchProjectionFactory<R, E>> extendOptional(
			SearchProjectionFactory<R, E> original, SearchProjectionBuilderFactory factory) {
		if ( factory instanceof ElasticsearchSearchProjectionBuilderFactory ) {
			return Optional.of( new ElasticsearchSearchProjectionFactoryImpl<>(
					original, (ElasticsearchSearchProjectionBuilderFactory) factory
			) );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ElasticsearchIndexFieldTypeFactory extendOrFail(IndexFieldTypeFactory original) {
		if ( original instanceof ElasticsearchIndexFieldTypeFactory ) {
			return (ElasticsearchIndexFieldTypeFactory) original;
		}
		else {
			throw log.elasticsearchExtensionOnUnknownType( original );
		}
	}
}
