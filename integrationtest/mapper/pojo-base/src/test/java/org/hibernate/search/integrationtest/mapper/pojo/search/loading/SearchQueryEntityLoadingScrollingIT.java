/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading;

import java.util.List;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubNextScrollWorkBehavior;

public class SearchQueryEntityLoadingScrollingIT extends SearchQueryEntityLoadingBaseIT {

	@Override
	protected <T2> List<T2> getHits(List<String> targetIndexes, SearchQuery<T2> query,
			List<DocumentReference> hitDocumentReferences) {
		backendMock.expectScrollObjects(
				targetIndexes,
				hitDocumentReferences.size(),
				b -> {}
		);

		backendMock.expectNextScroll( targetIndexes, StubNextScrollWorkBehavior
				.of( hitDocumentReferences.size(), hitDocumentReferences ) );

		backendMock.expectCloseScroll( targetIndexes );

		try ( SearchScroll<T2> scroll = query.scroll( hitDocumentReferences.size() ) ) {
			SearchScrollResult<T2> next = scroll.next();
			return next.hits();
		}
	}
}
