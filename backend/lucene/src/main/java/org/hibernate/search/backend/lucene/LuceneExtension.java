/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.backend.lucene.search.dsl.predicate.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.dsl.predicate.impl.LuceneSearchPredicateFactoryImpl;
import org.hibernate.search.backend.lucene.search.dsl.projection.LuceneSearchProjectionFactory;
import org.hibernate.search.backend.lucene.search.dsl.projection.impl.LuceneSearchProjectionFactoryImpl;
import org.hibernate.search.backend.lucene.search.dsl.query.LuceneSearchQueryHitTypeStep;
import org.hibernate.search.backend.lucene.search.dsl.query.impl.LuceneSearchQueryHitTypeStepImpl;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.scope.impl.LuceneIndexScope;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryExtension;
import org.hibernate.search.backend.lucene.types.dsl.LuceneIndexFieldTypeFactory;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.dsl.sort.LuceneSearchSortFactory;
import org.hibernate.search.backend.lucene.search.dsl.sort.impl.LuceneSearchSortFactoryImpl;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilderFactory;
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
 * An extension for the Lucene backend, giving access to Lucene-specific features.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called directly by users.
 * In short, users are only expected to get instances of this type from an API and pass it to another API.
 *
 * @param <H> The type of query hits.
 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
 * {@code .extension( LuceneExtension.get() }.
 * @param <R> The entity reference type for projections.
 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
 * {@code .extension( LuceneExtension.get() }.
 * @param <E> entity type for projections.
 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
 * {@code .extension( LuceneExtension.get() }.
 *
 * @see #get()
 */
public final class LuceneExtension<H, R, E>
		implements SearchQueryDslExtension<LuceneSearchQueryHitTypeStep<R, E>, R, E>,
		SearchQueryExtension<LuceneSearchQuery<H>, H>,
		SearchPredicateFactoryExtension<LuceneSearchPredicateFactory>,
		SearchSortFactoryExtension<LuceneSearchSortFactory>,
		SearchProjectionFactoryExtension<LuceneSearchProjectionFactory<R, E>, R, E>,
		IndexFieldTypeFactoryExtension<LuceneIndexFieldTypeFactory> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final LuceneExtension<Object, Object, Object> INSTANCE = new LuceneExtension<>();

	/**
	 * Get the extension with generic parameters automatically set as appropriate for the context in which it's used.
	 *
	 * @param <H> The type of query hits.
	 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
	 * {@code .extension( LuceneExtension.get() }.
	 * @param <R> The entity reference type for projections.
	 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
	 * {@code .extension( LuceneExtension.get() }.
	 * @param <E> entity type for projections.
	 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
	 * {@code .extension( LuceneExtension.get() }.
	 * @return The extension.
	 */
	@SuppressWarnings("unchecked") // The instance works for any H, R and E
	public static <H, R, E> LuceneExtension<H, R, E> get() {
		return (LuceneExtension<H, R, E>) INSTANCE;
	}

	private LuceneExtension() {
		// Private constructor, use get() instead.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<LuceneSearchQueryHitTypeStep<R, E>> extendOptional(
			SearchQueryHitTypeStep<?, R, E, ?, ?> original,
			IndexScope<?> indexScope,
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, E> loadingContextBuilder) {
		if ( indexScope instanceof LuceneIndexScope ) {
			return Optional.of( new LuceneSearchQueryHitTypeStepImpl<>(
					(LuceneIndexScope) indexScope, sessionContext, loadingContextBuilder
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
	public Optional<LuceneSearchQuery<H>> extendOptional(SearchQuery<H> original,
			LoadingContext<?, ?> loadingContext) {
		if ( original instanceof LuceneSearchQuery ) {
			return Optional.of( (LuceneSearchQuery<H>) original );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <C, B> Optional<LuceneSearchPredicateFactory> extendOptional(
			SearchPredicateFactory original, SearchPredicateBuilderFactory<C, B> factory) {
		if ( factory instanceof LuceneSearchPredicateBuilderFactory ) {
			return Optional.of( new LuceneSearchPredicateFactoryImpl(
					original, (LuceneSearchPredicateBuilderFactory) factory
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
	@SuppressWarnings("unchecked") // If the factory is an instance of LuceneSearchSortBuilderFactory, the cast is safe
	public Optional<LuceneSearchSortFactory> extendOptional(
			SearchSortFactory original, SearchSortDslContext<?, ?> dslContext) {
		if ( dslContext.getBuilderFactory() instanceof LuceneSearchSortBuilderFactory ) {
			return Optional.of( new LuceneSearchSortFactoryImpl(
					original,
					(SearchSortDslContext<LuceneSearchSortBuilderFactory, LuceneSearchSortBuilder>) dslContext
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
	public Optional<LuceneSearchProjectionFactory<R, E>> extendOptional(
			SearchProjectionFactory<R, E> original, SearchProjectionBuilderFactory factory) {
		if ( factory instanceof LuceneSearchProjectionBuilderFactory ) {
			return Optional.of( new LuceneSearchProjectionFactoryImpl<>(
					original, (LuceneSearchProjectionBuilderFactory) factory
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
	public LuceneIndexFieldTypeFactory extendOrFail(IndexFieldTypeFactory original) {
		if ( original instanceof LuceneIndexFieldTypeFactory ) {
			return (LuceneIndexFieldTypeFactory) original;
		}
		else {
			throw log.luceneExtensionOnUnknownType( original );
		}
	}
}
