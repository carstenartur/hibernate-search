/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.dsl.predicate.WildcardPredicateFieldStep;
import org.hibernate.search.engine.search.dsl.predicate.WildcardPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


class WildcardPredicateFieldStepImpl<B> implements WildcardPredicateFieldStep {

	private final WildcardPredicateFieldMoreStepImpl.CommonState<B> commonState;

	WildcardPredicateFieldStepImpl(SearchPredicateBuilderFactory<?, B> builderFactory) {
		this.commonState = new WildcardPredicateFieldMoreStepImpl.CommonState<>( builderFactory );
	}

	@Override
	public WildcardPredicateFieldMoreStep onFields(String ... absoluteFieldPaths) {
		return new WildcardPredicateFieldMoreStepImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}
}
