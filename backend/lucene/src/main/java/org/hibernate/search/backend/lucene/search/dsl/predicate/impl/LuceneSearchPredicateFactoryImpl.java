/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.predicate.impl;

import org.hibernate.search.backend.lucene.search.dsl.predicate.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.dsl.predicate.PredicateFinalStep;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.predicate.spi.DelegatingSearchPredicateFactory;

import org.apache.lucene.search.Query;


public class LuceneSearchPredicateFactoryImpl
		extends DelegatingSearchPredicateFactory
		implements LuceneSearchPredicateFactory {

	private final LuceneSearchPredicateBuilderFactory factory;

	public LuceneSearchPredicateFactoryImpl(SearchPredicateFactory delegate,
			LuceneSearchPredicateBuilderFactory factory) {
		super( delegate );
		this.factory = factory;
	}

	@Override
	public PredicateFinalStep fromLuceneQuery(Query luceneQuery) {
		return new LuceneQueryPredicateFinalStep( factory, luceneQuery );
	}
}
