/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.scope;

import java.util.function.Function;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactory;
import org.hibernate.search.engine.search.dsl.query.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.dsl.query.SearchQueryPredicateStep;
import org.hibernate.search.engine.search.dsl.query.SearchQueryHitTypeStep;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactory;
import org.hibernate.search.mapper.javabean.common.EntityReference;

/**
 * Represents a set of types and the corresponding indexes,
 * allowing to build search-related objects (query, predicate, ...)
 * taking into account the relevant indexes and their metadata (underlying technology, field types, ...).
 */
public interface SearchScope {

	/**
	 * Initiate the building of a search query.
	 * <p>
	 * The query will target the indexes mapped to types in this scope, or to any of their sub-types.
	 *
	 * @return The initial step of a DSL where the search query can be defined.
	 * @see SearchQueryHitTypeStep
	 */
	SearchQueryHitTypeStep<?, EntityReference, ?, ?, ?> search();

	/**
	 * Initiate the building of a search predicate.
	 * <p>
	 * The predicate will only be valid for {@link #search() search queries} created using this scope
	 * or a wider scope.
	 * <p>
	 * Note this method is only necessary if you do not want to use lambda expressions,
	 * since you can {@link SearchQueryPredicateStep#predicate(Function) define predicates with lambdas}
	 * within the search query DSL,
	 * removing the need to create separate objects to represent the predicates.
	 *
	 * @return A predicate factory.
	 * @see SearchPredicateFactory
	 */
	SearchPredicateFactory predicate();

	/**
	 * Initiate the building of a search sort.
	 * <p>
	 * The sort will only be valid for {@link #search() search queries} created using this scope
	 * or a wider scope.
	 * <p>
	 * Note this method is only necessary if you do not want to use lambda expressions,
	 * since you can {@link SearchQueryOptionsStep#sort(Function) define sorts with lambdas}
	 * within the search query DSL,
	 * removing the need to create separate objects to represent the sorts.
	 *
	 * @return A sort factory.
	 * @see SearchSortFactory
	 */
	SearchSortFactory sort();

	/**
	 * Initiate the building of a search projection that will be valid for the indexes in this scope.
	 * <p>
	 * The projection will only be valid for {@link #search() search queries} created using this scope
	 * or a wider scope.
	 * <p>
	 * Note this method is only necessary if you do not want to use lambda expressions,
	 * since you can {@link SearchQueryHitTypeStep#asProjection(Function)} define projections with lambdas}
	 * within the search query DSL,
	 * removing the need to create separate objects to represent the projections.
	 *
	 * @return A projection factory.
	 * @see SearchProjectionFactory
	 */
	SearchProjectionFactory<EntityReference, ?> projection();

}
