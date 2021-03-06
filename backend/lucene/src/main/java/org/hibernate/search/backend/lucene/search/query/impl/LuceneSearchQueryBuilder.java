/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneReadWorkOrchestrator;
import org.hibernate.search.backend.lucene.search.extraction.impl.ReusableDocumentStoredFieldVisitor;
import org.hibernate.search.backend.lucene.search.impl.LuceneQueries;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

public class LuceneSearchQueryBuilder<H> implements SearchQueryBuilder<H, LuceneSearchQueryElementCollector> {

	private final LuceneWorkFactory workFactory;
	private final LuceneReadWorkOrchestrator queryOrchestrator;

	private final LuceneSearchContext searchContext;
	private final SessionContextImplementor sessionContext;
	private final Set<String> routingKeys;

	private final ReusableDocumentStoredFieldVisitor storedFieldVisitor;
	private final LoadingContextBuilder<?, ?> loadingContextBuilder;
	private final LuceneSearchProjection<?, H> rootProjection;
	private final LuceneSearchQueryElementCollector elementCollector;

	public LuceneSearchQueryBuilder(
			LuceneWorkFactory workFactory,
			LuceneReadWorkOrchestrator queryOrchestrator,
			LuceneSearchContext searchContext,
			SessionContextImplementor sessionContext,
			ReusableDocumentStoredFieldVisitor storedFieldVisitor,
			LoadingContextBuilder<?, ?> loadingContextBuilder,
			LuceneSearchProjection<?, H> rootProjection) {
		this.workFactory = workFactory;
		this.queryOrchestrator = queryOrchestrator;

		this.searchContext = searchContext;
		this.sessionContext = sessionContext;
		this.routingKeys = new HashSet<>();

		this.elementCollector = new LuceneSearchQueryElementCollector();
		this.storedFieldVisitor = storedFieldVisitor;
		this.loadingContextBuilder = loadingContextBuilder;
		this.rootProjection = rootProjection;
	}

	@Override
	public LuceneSearchQueryElementCollector getQueryElementCollector() {
		return elementCollector;
	}

	@Override
	public void addRoutingKey(String routingKey) {
		this.routingKeys.add( routingKey );
	}

	@Override
	public LuceneSearchQuery<H> build() {
		LoadingContext<?, ?> loadingContext = loadingContextBuilder.build();

		LuceneSearchResultExtractor<H> searchResultExtractor = new LuceneSearchResultExtractorImpl<>(
				storedFieldVisitor, rootProjection, loadingContext
		);

		BooleanQuery.Builder luceneQueryBuilder = new BooleanQuery.Builder();
		luceneQueryBuilder.add( elementCollector.toLuceneQueryPredicate(), Occur.MUST );
		luceneQueryBuilder.add( LuceneQueries.mainDocumentQuery(), Occur.FILTER );

		return new LuceneSearchQueryImpl<>(
				queryOrchestrator, workFactory,
				searchContext,
				sessionContext,
				loadingContext,
				routingKeys,
				searchContext.decorateLuceneQuery( luceneQueryBuilder.build(), sessionContext.getTenantIdentifier() ),
				elementCollector.toLuceneSort(),
				rootProjection, searchResultExtractor
		);
	}
}
