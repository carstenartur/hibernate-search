/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchIndexNameNormalizer;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchResult;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.backend.elasticsearch.work.result.impl.ExplainResult;
import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.spi.AbstractSearchQuery;
import org.hibernate.search.engine.search.query.SearchQueryExtension;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;



public class ElasticsearchSearchQueryImpl<H> extends AbstractSearchQuery<H, ElasticsearchSearchResult<H>>
		implements ElasticsearchSearchQuery<H> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	/**
	 * ES default limit for (limit + offset); any search query beyond that limit will be rejected.
	 */
	private static final int MAX_RESULT_WINDOW_SIZE = 10000;

	private final ElasticsearchWorkBuilderFactory workFactory;
	private final ElasticsearchWorkOrchestrator queryOrchestrator;
	private final ElasticsearchSearchContext searchContext;
	private final SessionContextImplementor sessionContext;
	private final LoadingContext<?, ?> loadingContext;
	private final Set<String> routingKeys;
	private final JsonObject payload;
	private final ElasticsearchSearchResultExtractor<H> searchResultExtractor;

	ElasticsearchSearchQueryImpl(ElasticsearchWorkBuilderFactory workFactory,
			ElasticsearchWorkOrchestrator queryOrchestrator,
			ElasticsearchSearchContext searchContext,
			SessionContextImplementor sessionContext,
			LoadingContext<?, ?> loadingContext,
			Set<String> routingKeys,
			JsonObject payload, ElasticsearchSearchResultExtractor<H> searchResultExtractor) {
		this.workFactory = workFactory;
		this.queryOrchestrator = queryOrchestrator;
		this.searchContext = searchContext;
		this.sessionContext = sessionContext;
		this.loadingContext = loadingContext;
		this.routingKeys = routingKeys;
		this.payload = payload;
		this.searchResultExtractor = searchResultExtractor;
	}

	@Override
	public String getQueryString() {
		return payload.toString();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + getQueryString() + "]";
	}

	@Override
	public <Q> Q extension(SearchQueryExtension<Q, H> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, loadingContext )
		);
	}

	@Override
	public ElasticsearchSearchResult<H> fetch(Integer limit, Integer offset) {
		// TODO restore scrolling support. See HSEARCH-3323
		ElasticsearchWork<ElasticsearchLoadableSearchResult<H>> work = workFactory.search( payload, searchResultExtractor )
				.indexes( searchContext.getIndexNames() )
				.paging( defaultedLimit( limit, offset ), offset )
				.routingKeys( routingKeys )
				.build();

		return Futures.unwrappedExceptionJoin( queryOrchestrator.submit( work ) )
				/*
				 * WARNING: the following call must run in the user thread.
				 * If we introduce async query execution, we will have to add a loadAsync method here,
				 * as well as in ProjectionHitMapper and EntityLoader.
				 * This method may not be easy to implement for blocking mappers,
				 * so we may choose to throw exceptions for those.
				 */
				.loadBlocking( sessionContext );
	}

	@Override
	public long fetchTotalHitCount() {
		JsonObject filteredPayload = new JsonObject();
		Optional<JsonObject> querySubTree = JsonAccessor.root().property( "query" ).asObject().get( payload );
		if ( querySubTree.isPresent() ) {
			filteredPayload.add( "query", querySubTree.get() );
		}

		ElasticsearchWork<Long> work = workFactory.count( searchContext.getIndexNames() )
				.query( filteredPayload )
				.routingKeys( routingKeys )
				.build();
		return queryOrchestrator.submit( work ).join();
	}

	@Override
	public String explain(String id) {
		Contracts.assertNotNull( id, "id" );

		Set<URLEncodedString> targetedIndexNames = searchContext.getIndexNames();
		if ( targetedIndexNames.size() != 1 ) {
			throw log.explainRequiresIndexName( targetedIndexNames );
		}

		return doExplain( targetedIndexNames.iterator().next(), id );
	}

	@Override
	public String explain(String indexName, String id) {
		Contracts.assertNotNull( indexName, "indexName" );
		Contracts.assertNotNull( id, "id" );

		Set<URLEncodedString> targetedIndexNames = searchContext.getIndexNames();
		URLEncodedString encodedIndexName = URLEncodedString.fromString(
				ElasticsearchIndexNameNormalizer.normalize( indexName )
		);
		if ( !targetedIndexNames.contains( encodedIndexName ) ) {
			throw log.explainRequiresIndexTargetedByQuery( targetedIndexNames, encodedIndexName );
		}

		return doExplain( encodedIndexName, id );
	}

	private Integer defaultedLimit(Integer limit, Integer offset) {
		/*
		 * If the user has given a 'size' value, take it as is, let ES itself complain if it's too high;
		 * if no value is given, take as much as possible, as by default only 10 rows would be returned.
		 */
		if ( limit != null ) {
			return limit;
		}
		else {
			// Elasticsearch has a default limit of 10, which is not what we want.
			int maxLimitThatElasticsearchWillAccept = MAX_RESULT_WINDOW_SIZE;
			if ( offset != null ) {
				maxLimitThatElasticsearchWillAccept -= offset;
			}
			return maxLimitThatElasticsearchWillAccept;
		}
	}

	private String doExplain(URLEncodedString encodedIndexName, String id) {
		URLEncodedString elasticsearchId = URLEncodedString.fromString(
				searchContext.toElasticsearchId( sessionContext.getTenantIdentifier(), id )
		);

		ElasticsearchWork<ExplainResult> work = workFactory.explain( encodedIndexName, elasticsearchId, payload )
				.routingKeys( routingKeys )
				.build();

		ExplainResult explainResult = Futures.unwrappedExceptionJoin( queryOrchestrator.submit( work ) );
		return searchContext.getUserFacingGson().toJson( explainResult.getJsonObject() );
	}
}
