/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The final step in an "nested" predicate definition, where optional parameters can be set.
 */
public interface NestedPredicateOptionsStep extends PredicateFinalStep {

	// TODO HSEARCH-3090 add tuning methods, like the "score_mode" in Elasticsearch (avg, min, ...)

}
