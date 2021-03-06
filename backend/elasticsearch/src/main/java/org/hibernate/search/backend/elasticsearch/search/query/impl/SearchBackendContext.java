/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchScopeModel;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.DocumentReferenceExtractorHelper;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionBackendContext;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;

/**
 * An interface with knowledge of the backend internals,
 * able to create components related to work execution.
 * <p>
 * Note this interface exists mainly to more cleanly pass information
 * from the backend to the various search-related components.
 * If we just passed the backend to the various search-related components,
 * we would have a cyclic dependency.
 * If we passed all the components held by the backend to the various search-related components,
 * we would end up with methods with many parameters.
 */
public interface SearchBackendContext {

	DocumentReferenceExtractorHelper getDocumentReferenceExtractorHelper();

	SearchProjectionBackendContext getSearchProjectionBackendContext();

	ElasticsearchSearchContext createSearchContext(MappingContextImplementor mappingContext,
			ElasticsearchScopeModel scopeModel);

	<H> ElasticsearchSearchQueryBuilder<H> createSearchQueryBuilder(
			ElasticsearchSearchContext searchContext,
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<?, ?> loadingContextBuilder,
			ElasticsearchSearchProjection<?, H> rootProjection);

}
