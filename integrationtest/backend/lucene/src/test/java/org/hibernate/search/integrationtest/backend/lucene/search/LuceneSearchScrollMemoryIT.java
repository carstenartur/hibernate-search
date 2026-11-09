/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.ProjectionExtractContext;
import org.hibernate.search.backend.lucene.search.projection.impl.ProjectionRequestContext;
import org.hibernate.search.backend.lucene.search.projection.impl.ProjectionTransformContext;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubSession;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.similarities.ClassicSimilarity;

class LuceneSearchScrollMemoryIT {

	private static final int CHUNK_SIZE = 16;
	private static final Instant TIME = Instant.parse( "2026-01-01T00:00:00.123456789Z" );
	private static final String ROUTE = "selected";

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	static Stream<Arguments> cases() {
		return Stream.of( 257, 1025 ).flatMap( count -> Stream.of( false, true )
				.flatMap( customSimilarity -> Stream.of( "score", "score-time-id", "time-id" )
						.map( sort -> Arguments.of( count, customSimilarity, sort ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("cases")
	void boundedPagesPreserveSnapshot(int documentCount, boolean customSimilarity, String sortKind) {
		var setup = setupHelper.start().withIndex( index ).withMultiTenancy()
				.withBackendProperty( "multi_tenancy.strategy", "discriminator" );
		if ( customSimilarity ) {
			setup.withBackendProperty( "analysis.configurer",
					(LuceneAnalysisConfigurer) context -> context.similarity( new ClassicSimilarity() ) );
		}
		var mapping = setup.setup();
		StubSession session = mapping.session( "tenant-1" );
		var indexer = index.bulkIndexer( session, true );
		for ( int i = 0; i < documentCount; i++ ) {
			int number = i;
			indexer.add( Integer.toString( number ), ROUTE, document -> writeDocument( document, number ) );
		}
		indexer.add( "wrong-route", "other", document -> writeDocument( document, -1 ) ).join();
		index.bulkIndexer( mapping.session( "tenant-2" ), true )
				.add( "0", ROUTE, document -> writeDocument( document, -2 ) ).join();

		StubMappingScope scope = index.createScope();
		var f = scope.projection();
		SearchProjection<List<?>> projection = f.composite( f.field( "id", String.class ), f.score(),
				f.field( "time", Instant.class ), f.field( "nullable", String.class ) ).toProjection();
		SearchSort sort = switch ( sortKind ) {
			case "score" -> scope.sort().score().toSort();
			case "score-time-id" -> scope.sort().composite().add( scope.sort().score() )
					.add( scope.sort().field( "time" ).desc() ).add( scope.sort().field( "id" ) ).toSort();
			default -> scope.sort().composite().add( scope.sort().field( "time" ).desc() )
					.add( scope.sort().field( "id" ) ).toSort();
		};
		LuceneSearchQuery<List<?>> referenceQuery = query( scope, session, projection, sort );
		List<List<?>> expected = referenceQuery.fetchAllHits();
		assertThat( expected ).hasSize( documentCount );
		assertThat( expected ).anySatisfy( hit -> assertThat( hit.get( 2 ) ).isEqualTo( TIME ) );
		assertThat( expected ).anySatisfy( hit -> assertThat( hit.get( 3 ) ).isNull() );

		ObservedProjection<List<?>> observed = new ObservedProjection<>( projection );
		List<List<?>> actual = new ArrayList<>();
		try ( var scroll = query( scope, session, observed, sort ).scroll( CHUNK_SIZE ) ) {
			actual.addAll( scroll.next().hits() );
			var plan = index.createIndexingPlan( session );
			plan.delete( referenceProvider( Integer.toString( documentCount - 1 ), ROUTE ) );
			plan.addOrUpdate( referenceProvider( "0", ROUTE ), document -> writeDocument( document, -3 ) );
			plan.add( referenceProvider( "late", ROUTE ), document -> writeDocument( document, -4 ) );
			plan.execute( OperationSubmitter.blocking() ).join();

			for ( var chunk = scroll.next(); chunk.hasHits(); chunk = scroll.next() ) {
				assertThat( chunk.hits() ).hasSizeLessThanOrEqualTo( CHUNK_SIZE );
				assertThat( chunk.total().hitCount() ).isEqualTo( documentCount );
				actual.addAll( chunk.hits() );
			}
		}
		assertThat( actual ).containsExactlyElementsOf( expected );
		assertThat( referenceQuery.fetchAllHits() ).isNotEqualTo( expected );
		assertThat( observed.indexReader.get().getRefCount() ).as( "closed scroll reader" ).isZero();
		// Observe the real TopDocs used during projection extraction, without a heap-size or GC assumption.
		assertThat( observed.largestPage.get() ).as( "retained ranked hits for %s documents", documentCount )
				.isBetween( 1, CHUNK_SIZE * 4 );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void closeReleasesReaderAfterEarlyTermination(boolean projectionFailure) {
		setupHelper.start().withIndex( index ).setup();
		var indexer = index.bulkIndexer();
		for ( int i = 0; i <= CHUNK_SIZE; i++ ) {
			int number = i;
			indexer.add( Integer.toString( number ), document -> writeDocument( document, number ) );
		}
		indexer.join();
		var scope = index.createScope();
		ObservedProjection<String> observed = new ObservedProjection<>( scope.projection().field( "id", String.class )
				.toProjection() );
		try ( var scroll = scope.query().select( observed )
				.where( f -> f.match().field( "text" ).matching( "value" ) ).scroll( CHUNK_SIZE ) ) {
			if ( projectionFailure ) {
				observed.transformFailure = new IllegalStateException( "injected projection failure" );
				assertThatThrownBy( scroll::next ).isSameAs( observed.transformFailure );
			}
			else {
				assertThat( scroll.next().hits() ).hasSize( CHUNK_SIZE );
			}
		}
		assertThat( observed.indexReader.get().getRefCount() ).as( "closed scroll reader" ).isZero();
	}

	private LuceneSearchQuery<List<?>> query(StubMappingScope scope, StubSession session,
			SearchProjection<List<?>> projection, SearchSort sort) {
		return scope.query( session ).extension( LuceneExtension.get() ).select( projection )
				.where( f -> f.match().field( "text" ).matching( "value" ) )
				.routing( ROUTE ).sort( sort ).toQuery();
	}

	private void writeDocument(DocumentElement document, int number) {
		document.addValue( index.binding().id, Integer.toString( number ) );
		document.addValue( index.binding().text, "value ".repeat( Math.floorMod( number, 3 ) + 1 )
				+ "filler ".repeat( Math.floorMod( number, 5 ) ) );
		document.addValue( index.binding().time, number % 11 == 0 ? null : TIME.plusSeconds( number % 7 ) );
		document.addValue( index.binding().nullable, number % 3 == 0 ? null : "value-" + number );
	}

	private static class IndexBinding {
		final IndexFieldReference<String> id;
		final IndexFieldReference<String> text;
		final IndexFieldReference<Instant> time;
		final IndexFieldReference<String> nullable;

		IndexBinding(IndexSchemaElement root) {
			id = root.field( "id", f -> f.asString().projectable( Projectable.YES ).sortable( Sortable.YES ) ).toReference();
			text = root.field( "text", f -> f.asString().analyzer( "standard" ) ).toReference();
			time = root.field( "time", f -> f.asInstant().projectable( Projectable.YES ).sortable( Sortable.YES ) )
					.toReference();
			nullable = root.field( "nullable", f -> f.asString().projectable( Projectable.YES ) ).toReference();
		}
	}

	private static class ObservedProjection<T> implements LuceneSearchProjection<T> {
		private final LuceneSearchProjection<T> delegate;
		private final AtomicInteger largestPage = new AtomicInteger();
		private final AtomicReference<IndexReader> indexReader = new AtomicReference<>();
		private RuntimeException transformFailure;

		ObservedProjection(SearchProjection<T> delegate) {
			this.delegate = (LuceneSearchProjection<T>) delegate;
		}

		@Override
		public Set<String> indexNames() {
			return delegate.indexNames();
		}

		@Override
		public Extractor<?, T> request(ProjectionRequestContext context) {
			return observe( delegate.request( context ) );
		}

		private <E> Extractor<E, T> observe(Extractor<E, T> extractor) {
			return new Extractor<>() {
				@Override
				public Values<E> values(ProjectionExtractContext context) {
					largestPage.accumulateAndGet( context.collectorExecutionContext().topDocs().scoreDocs.length, Math::max );
					indexReader.set( context.collectorExecutionContext().getIndexSearcher().getIndexReader() );
					return extractor.values( context );
				}

				@Override
				public T transform(LoadingResult<?> loadingResult, E extractedData, ProjectionTransformContext context) {
					if ( transformFailure != null ) {
						throw transformFailure;
					}
					return extractor.transform( loadingResult, extractedData, context );
				}
			};
		}
	}
}
