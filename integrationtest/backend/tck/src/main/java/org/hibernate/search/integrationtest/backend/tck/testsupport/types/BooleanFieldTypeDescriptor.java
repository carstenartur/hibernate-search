/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexingExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class BooleanFieldTypeDescriptor extends FieldTypeDescriptor<Boolean> {

	BooleanFieldTypeDescriptor() {
		super( Boolean.class );
	}

	@Override
	public Optional<IndexingExpectations<Boolean>> getIndexingExpectations() {
		return Optional.of( new IndexingExpectations<>(
				true,
				false,
				// This is ugly, but we test it on purpose
				new Boolean( true ),
				new Boolean( false )
		) );
	}

	@Override
	public Optional<MatchPredicateExpectations<Boolean>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				true, false
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<Boolean>> getRangePredicateExpectations() {
		// Tested separately in BooleanSortAndRangePredicateIT, because we can only use two values
		return Optional.empty();
	}

	@Override
	public ExistsPredicateExpectations<Boolean> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				true, false
		);
	}

	@Override
	public Optional<FieldSortExpectations<Boolean>> getFieldSortExpectations() {
		// Tested separately in BooleanSortAndRangePredicateIT, because we can only use two values
		return Optional.empty();
	}

	@Override
	public Optional<FieldProjectionExpectations<Boolean>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				true, false, true
		) );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Boolean>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				true, false
		) );
	}
}
