/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.searchdsl.projection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.ElasticsearchBackendConfiguration;
import org.hibernate.search.documentation.testsupport.LuceneBackendConfiguration;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmAutomaticIndexingSynchronizationStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.common.impl.EntityReferenceImpl;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Explanation;

@RunWith(Parameterized.class)
public class ProjectionDslIT {

	private static final int ASIMOV_ID = 1;
	private static final int MARTINEZ_ID = 2;

	private static final String BOOK_INDEX_NAME = Book.class.getName();
	private static final int BOOK1_ID = 1;
	private static final int BOOK2_ID = 2;
	private static final int BOOK3_ID = 3;
	private static final int BOOK4_ID = 4;

	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendConfigurations() {
		return BackendConfigurations.simple().toArray();
	}

	@Rule
	public OrmSetupHelper setupHelper;

	private BackendConfiguration backendConfiguration;

	private EntityManagerFactory entityManagerFactory;

	public ProjectionDslIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = OrmSetupHelper.withSingleBackend( backendConfiguration );
		this.backendConfiguration = backendConfiguration;
	}

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						HibernateOrmAutomaticIndexingSynchronizationStrategyName.SEARCHABLE
				)
				.setup( Book.class, Author.class, EmbeddableGeoPoint.class );
		initData();
	}

	@Test
	public void entryPoint() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::entryPoint-lambdas[]
			SearchSession searchSession = Search.session( entityManager );

			List<String> result = searchSession.search( Book.class ) // <1>
					.asProjection( f -> f.field( "title", String.class ) ) // <2>
					.predicate( f -> f.matchAll() )
					.fetchHits(); // <3>
			// end::entryPoint-lambdas[]
			assertThat( result ).containsExactlyInAnyOrder(
					entityManager.getReference( Book.class, BOOK1_ID ).getTitle(),
					entityManager.getReference( Book.class, BOOK2_ID ).getTitle(),
					entityManager.getReference( Book.class, BOOK3_ID ).getTitle(),
					entityManager.getReference( Book.class, BOOK4_ID ).getTitle()
			);
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::entryPoint-objects[]
			SearchSession searchSession = Search.session( entityManager );

			SearchScope<Book> scope = searchSession.scope( Book.class );

			List<String> result = scope.search()
					.asProjection( scope.projection().field( "title", String.class )
							.toProjection() )
					.predicate( scope.predicate().matchAll().toPredicate() )
					.fetchHits();
			// end::entryPoint-objects[]
			assertThat( result ).containsExactlyInAnyOrder(
					entityManager.getReference( Book.class, BOOK1_ID ).getTitle(),
					entityManager.getReference( Book.class, BOOK2_ID ).getTitle(),
					entityManager.getReference( Book.class, BOOK3_ID ).getTitle(),
					entityManager.getReference( Book.class, BOOK4_ID ).getTitle()
			);
		} );
	}

	@Test
	public void documentReference() {
		withinSearchSession( searchSession -> {
			// tag::documentReference[]
			List<DocumentReference> hits = searchSession.search( Book.class )
					.asProjection( f -> f.documentReference() )
					.predicate( f -> f.matchAll() )
					.fetchHits();
			// end::documentReference[]
			SearchHitsAssert.assertThat( hits ).hasDocRefHitsAnyOrder(
					BOOK_INDEX_NAME,
					String.valueOf( BOOK1_ID ),
					String.valueOf( BOOK2_ID ),
					String.valueOf( BOOK3_ID ),
					String.valueOf( BOOK4_ID )
			);
		} );
	}

	@Test
	public void reference() {
		withinSearchSession( searchSession -> {
			// tag::reference[]
			List<EntityReference> hits = searchSession.search( Book.class )
					.asProjection( f -> f.entityReference() )
					.predicate( f -> f.matchAll() )
					.fetchHits();
			// end::reference[]
			assertThat( hits ).containsExactlyInAnyOrder(
					new EntityReferenceImpl( Book.class, BOOK1_ID ),
					new EntityReferenceImpl( Book.class, BOOK2_ID ),
					new EntityReferenceImpl( Book.class, BOOK3_ID ),
					new EntityReferenceImpl( Book.class, BOOK4_ID )
			);
		} );
	}

	@Test
	public void entity() {
		withinSearchSession( searchSession -> {
			// tag::entity[]
			List<Book> hits = searchSession.search( Book.class )
					.asProjection( f -> f.entity() )
					.predicate( f -> f.matchAll() )
					.fetchHits();
			// end::entity[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrder(
					session.getReference( Book.class, BOOK1_ID ),
					session.getReference( Book.class, BOOK2_ID ),
					session.getReference( Book.class, BOOK3_ID ),
					session.getReference( Book.class, BOOK4_ID )
			);
		} );
	}

	@Test
	public void field() {
		withinSearchSession( searchSession -> {
			// tag::field[]
			List<Genre> hits = searchSession.search( Book.class )
					.asProjection( f -> f.field( "genre", Genre.class ) )
					.predicate( f -> f.matchAll() )
					.fetchHits();
			// end::field[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrder(
					session.getReference( Book.class, BOOK1_ID ).getGenre(),
					session.getReference( Book.class, BOOK2_ID ).getGenre(),
					session.getReference( Book.class, BOOK3_ID ).getGenre(),
					session.getReference( Book.class, BOOK4_ID ).getGenre()
			);
		} );

		withinSearchSession( searchSession -> {
			// tag::field-noType[]
			List<Object> hits = searchSession.search( Book.class )
					.asProjection( f -> f.field( "genre" ) )
					.predicate( f -> f.matchAll() )
					.fetchHits();
			// end::field-noType[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrder(
					session.getReference( Book.class, BOOK1_ID ).getGenre(),
					session.getReference( Book.class, BOOK2_ID ).getGenre(),
					session.getReference( Book.class, BOOK3_ID ).getGenre(),
					session.getReference( Book.class, BOOK4_ID ).getGenre()
			);
		} );

		withinSearchSession( searchSession -> {
			// tag::field-noProjectionConverter[]
			List<String> hits = searchSession.search( Book.class )
					.asProjection( f -> f.field(
							"genre", String.class, ValueConvert.NO
					) )
					.predicate( f -> f.matchAll() )
					.fetchHits();
			// end::field-noProjectionConverter[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrder(
					session.getReference( Book.class, BOOK1_ID ).getGenre().name(),
					session.getReference( Book.class, BOOK2_ID ).getGenre().name(),
					session.getReference( Book.class, BOOK3_ID ).getGenre().name(),
					session.getReference( Book.class, BOOK4_ID ).getGenre().name()
			);
		} );
	}

	@Test
	public void score() {
		withinSearchSession( searchSession -> {
			// tag::score[]
			List<Float> hits = searchSession.search( Book.class )
					.asProjection( f -> f.score() )
					.predicate( f -> f.match().onField( "title" )
							.matching( "robot dawn" ) )
					.fetchHits();
			// end::score[]
			assertThat( hits )
					.hasSize( 2 )
					.allSatisfy(
							score -> assertThat( score ).isGreaterThan( 0.0f )
					);
		} );
	}

	@Test
	public void distance() {
		withinSearchSession( searchSession -> {
			// tag::distance[]
			GeoPoint center = GeoPoint.of( 47.506060, 2.473916 );
			SearchResult<Double> result = searchSession.search( Author.class )
					.asProjection( f -> f.distance( "placeOfBirth", center ) )
					.predicate( f -> f.matchAll() )
					.fetch(); // <3>
			// end::distance[]
			assertThat( result.getHits() )
					.hasSize( 2 )
					.allSatisfy(
							distance -> assertThat( distance ).isBetween( 1_000_000.0, 10_000_000.0 )
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::distance-unit[]
			GeoPoint center = GeoPoint.of( 47.506060, 2.473916 );
			SearchResult<Double> result = searchSession.search( Author.class )
					.asProjection( f -> f.distance( "placeOfBirth", center )
							.unit( DistanceUnit.KILOMETERS ) )
					.predicate( f -> f.matchAll() )
					.fetch(); // <3>
			// end::distance-unit[]
			assertThat( result.getHits() )
					.hasSize( 2 )
					.allSatisfy(
							distance -> assertThat( distance ).isBetween( 1_000.0, 10_000.0 )
					);
		} );
	}

	@Test
	public void composite() {
		withinSearchSession( searchSession -> {
			// tag::composite-customObject[]
			List<MyPair<String, Genre>> hits = searchSession.search( Book.class )
					.asProjection( f -> f.composite( // <1>
							MyPair::new, // <2>
							f.field( "title", String.class ), // <3>
							f.field( "genre", Genre.class ) // <4>
					) )
					.predicate( f -> f.matchAll() )
					.fetchHits(); // <5>
			// end::composite-customObject[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrder(
					new MyPair<>(
							session.getReference( Book.class, BOOK1_ID ).getTitle(),
							session.getReference( Book.class, BOOK1_ID ).getGenre()
					),
					new MyPair<>(
							session.getReference( Book.class, BOOK2_ID ).getTitle(),
							session.getReference( Book.class, BOOK2_ID ).getGenre()
					),
					new MyPair<>(
							session.getReference( Book.class, BOOK3_ID ).getTitle(),
							session.getReference( Book.class, BOOK3_ID ).getGenre()
					),
					new MyPair<>(
							session.getReference( Book.class, BOOK4_ID ).getTitle(),
							session.getReference( Book.class, BOOK4_ID ).getGenre()
					)
			);
		} );

		withinSearchSession( searchSession -> {
			// tag::composite-list[]
			List<List<?>> hits = searchSession.search( Book.class )
					.asProjection( f -> f.composite( // <1>
							f.field( "title", String.class ), // <2>
							f.field( "genre", Genre.class ) // <3>
					) )
					.predicate( f -> f.matchAll() )
					.fetchHits(); // <4>
			// end::composite-list[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrder(
					Arrays.asList(
							session.getReference( Book.class, BOOK1_ID ).getTitle(),
							session.getReference( Book.class, BOOK1_ID ).getGenre()
					),
					Arrays.asList(
							session.getReference( Book.class, BOOK2_ID ).getTitle(),
							session.getReference( Book.class, BOOK2_ID ).getGenre()
					),
					Arrays.asList(
							session.getReference( Book.class, BOOK3_ID ).getTitle(),
							session.getReference( Book.class, BOOK3_ID ).getGenre()
					),
					Arrays.asList(
							session.getReference( Book.class, BOOK4_ID ).getTitle(),
							session.getReference( Book.class, BOOK4_ID ).getGenre()
					)
			);
		} );
	}

	@Test
	public void lucene() {
		Assume.assumeTrue( backendConfiguration instanceof LuceneBackendConfiguration );

		withinSearchSession( searchSession -> {
			// tag::lucene-document[]
			List<Document> hits = searchSession.search( Book.class )
					.extension( LuceneExtension.get() )
					.asProjection( f -> f.document() )
					.predicate( f -> f.matchAll() )
					.fetchHits();
			// end::lucene-document[]
			assertThat( hits ).hasSize( 4 );
		} );

		withinSearchSession( searchSession -> {
			// tag::lucene-explanation[]
			List<Explanation> hits = searchSession.search( Book.class )
					.extension( LuceneExtension.get() )
					.asProjection( f -> f.explanation() )
					.predicate( f -> f.matchAll() )
					.fetchHits();
			// end::lucene-explanation[]
			assertThat( hits ).hasSize( 4 );
		} );
	}

	@Test
	public void elasticsearch() {
		Assume.assumeTrue( backendConfiguration instanceof ElasticsearchBackendConfiguration );

		withinSearchSession( searchSession -> {
			// tag::elasticsearch-source[]
			List<String> hits = searchSession.search( Book.class )
					.extension( ElasticsearchExtension.get() )
					.asProjection( f -> f.source() )
					.predicate( f -> f.matchAll() )
					.fetchHits();
			// end::elasticsearch-source[]
			assertThat( hits ).hasSize( 4 );
		} );

		withinSearchSession( searchSession -> {
			// tag::elasticsearch-explanation[]
			List<String> hits = searchSession.search( Book.class )
					.extension( ElasticsearchExtension.get() )
					.asProjection( f -> f.explanation() )
					.predicate( f -> f.matchAll() )
					.fetchHits();
			// end::elasticsearch-explanation[]
			assertThat( hits ).hasSize( 4 );
		} );
	}

	private void withinSearchSession(Consumer<SearchSession> action) {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			action.accept( searchSession );
		} );
	}

	private void initData() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			Author isaacAsimov = new Author();
			isaacAsimov.setId( ASIMOV_ID );
			isaacAsimov.setFirstName( "Isaac" );
			isaacAsimov.setLastName( "Asimov" );
			isaacAsimov.setPlaceOfBirth( EmbeddableGeoPoint.of( 53.976177, 32.158627 ) );

			Author aLeeMartinez = new Author();
			aLeeMartinez.setId( MARTINEZ_ID );
			aLeeMartinez.setFirstName( "A. Lee" );
			aLeeMartinez.setLastName( "Martinez" );
			aLeeMartinez.setPlaceOfBirth( EmbeddableGeoPoint.of( 31.814315, -106.475524 ) );

			Book book1 = new Book();
			book1.setId( BOOK1_ID );
			book1.setTitle( "I, Robot" );
			book1.setGenre( Genre.SCIENCE_FICTION );
			book1.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book1 );

			Book book2 = new Book();
			book2.setId( BOOK2_ID );
			book2.setTitle( "The Caves of Steel" );
			book2.setGenre( Genre.SCIENCE_FICTION );
			book2.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book2 );

			Book book3 = new Book();
			book3.setId( BOOK3_ID );
			book3.setTitle( "The Robots of Dawn" );
			book3.setGenre( Genre.SCIENCE_FICTION );
			book3.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book3 );

			Book book4 = new Book();
			book4.setId( BOOK4_ID );
			book4.setTitle( "The Automatic Detective" );
			book4.setGenre( Genre.CRIME_FICTION );
			book4.getAuthors().add( aLeeMartinez );
			aLeeMartinez.getBooks().add( book3 );

			entityManager.persist( isaacAsimov );
			entityManager.persist( aLeeMartinez );

			entityManager.persist( book1 );
			entityManager.persist( book2 );
			entityManager.persist( book3 );
			entityManager.persist( book4 );
		} );
	}

	private static class MyPair<T1, T2> {
		private final T1 first;
		private final T2 second;

		MyPair(T1 first, T2 second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == null || !MyPair.class.equals( obj.getClass() ) ) {
				return false;
			}
			MyPair<?, ?> other = (MyPair<?, ?>) obj;
			return Objects.equals( first, other.first ) && Objects.equals( second, other.second );
		}

		@Override
		public int hashCode() {
			return Objects.hash( first, second );
		}
	}
}
