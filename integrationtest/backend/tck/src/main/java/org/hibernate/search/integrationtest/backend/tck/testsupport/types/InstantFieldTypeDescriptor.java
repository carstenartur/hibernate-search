/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexingExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class InstantFieldTypeDescriptor extends FieldTypeDescriptor<Instant> {

	InstantFieldTypeDescriptor() {
		super( Instant.class );
	}

	@Override
	public Optional<IndexingExpectations<Instant>> getIndexingExpectations() {
		List<Instant> values = new ArrayList<>();
		LocalDateTimeFieldTypeDescriptor.getValuesForIndexingExpectations().forEach( localDateTime -> {
			values.add( localDateTime.atOffset( ZoneOffset.UTC ).toInstant() );
		} );
		values.add( Instant.EPOCH );
		return Optional.of( new IndexingExpectations<>( values ) );
	}

	@Override
	public Optional<MatchPredicateExpectations<Instant>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				Instant.parse( "1980-10-11T10:15:30.00Z" ),
				Instant.parse( "1984-10-07T10:15:30.00Z" )
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<Instant>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				// Indexed
				Instant.parse( "2018-02-01T10:15:30.00Z" ),
				Instant.parse( "2018-03-01T10:15:30.00Z" ),
				Instant.parse( "2018-04-01T10:15:30.00Z" ),
				// Values around what is indexed
				Instant.parse( "2018-02-15T10:15:30.00Z" ),
				Instant.parse( "2018-03-15T10:15:30.00Z" )
		) );
	}

	@Override
	public ExistsPredicateExpectations<Instant> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				Instant.EPOCH, Instant.parse( "2018-02-01T10:15:30.00Z" )
		);
	}

	@Override
	public Optional<FieldSortExpectations<Instant>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
				// Indexed
				Instant.parse( "2018-02-01T10:15:30.00Z" ),
				Instant.parse( "2018-03-01T10:15:30.00Z" ),
				Instant.parse( "2018-04-01T10:15:30.00Z" ),
				// Values around what is indexed
				Instant.parse( "2018-01-01T10:15:30.00Z" ),
				Instant.parse( "2018-02-15T10:15:30.00Z" ),
				Instant.parse( "2018-03-15T10:15:30.00Z" ),
				Instant.parse( "2018-05-01T10:15:30.00Z" )
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<Instant>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				Instant.parse( "2018-02-01T10:15:30.00Z" ),
				Instant.parse( "2018-03-01T10:15:30.00Z" ),
				Instant.parse( "2018-04-01T10:15:30.00Z" )
		) );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Instant>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				Instant.EPOCH, Instant.parse( "2018-02-01T10:15:30.00Z" )
		) );
	}
}
