/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.array;

import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

/**
 * An abstract base for tests dealing with automatic indexing based on Hibernate ORM entity events
 * and involving an array.
 */
public abstract class AbstractAutomaticIndexingArrayIT<TIndexed, TArray, TIndexField> {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	private final ArrayModelPrimitives<TIndexed, TArray, TIndexField> primitives;

	AbstractAutomaticIndexingArrayIT(ArrayModelPrimitives<TIndexed, TArray, TIndexField> primitives) {
		this.primitives = primitives;
	}

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectSchema( primitives.getIndexName(), b -> b
				.field( "serializedArray", primitives.getExpectedIndexFieldType(),
						b2 -> b2.multiValued( true ) )
				.field( "elementCollectionArray", primitives.getExpectedIndexFieldType(),
						b2 -> b2.multiValued( true ) ) );

		setupContext.withAnnotatedTypes( primitives.getIndexedClass() );
	}

	@Test
	public void serializedArray_replaceArray() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TArray array1 = primitives.newArray( 2 );
			primitives.setElement( array1, 0, 0 );
			primitives.setElement( array1, 1, 1 );
			primitives.setSerializedArray( entity1, array1 );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b.field( "serializedArray",
							primitives.getExpectedIndexFieldValue( array1, 0 ),
							primitives.getExpectedIndexFieldValue( array1, 1 ) ) );
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing the array
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TArray array2 = primitives.newArray( 3 );
			primitives.setElement( array2, 0, 1 );
			primitives.setElement( array2, 1, 2 );
			primitives.setElement( array2, 2, 3 );
			primitives.setSerializedArray( entity1, array2 );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b.field( "serializedArray",
							primitives.getExpectedIndexFieldValue( array2, 0 ),
							primitives.getExpectedIndexFieldValue( array2, 1 ),
							primitives.getExpectedIndexFieldValue( array2, 2 ) ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void serializedArray_replaceElement() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TArray array = primitives.newArray( 2 );
			primitives.setElement( array, 0, 0 );
			primitives.setElement( array, 1, 1 );
			primitives.setSerializedArray( entity1, array );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b.field( "serializedArray",
							primitives.getExpectedIndexFieldValue( array, 0 ),
							primitives.getExpectedIndexFieldValue( array, 1 ) ) );
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing an element in the array
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TArray array = primitives.getSerializedArray( entity1 );
			primitives.setElement( array, 1, 2 );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b.field( "serializedArray",
							primitives.getExpectedIndexFieldValue( array, 0 ),
							primitives.getExpectedIndexFieldValue( array, 1 ) ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void elementCollectionArray_replaceArray() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TArray array1 = primitives.newArray( 2 );
			primitives.setElement( array1, 0, 0 );
			primitives.setElement( array1, 1, 1 );
			primitives.setElementCollectionArray( entity1, array1 );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b.field( "elementCollectionArray",
							primitives.getExpectedIndexFieldValue( array1, 0 ),
							primitives.getExpectedIndexFieldValue( array1, 1 ) ) );
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing the array
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TArray array2 = primitives.newArray( 3 );
			primitives.setElement( array2, 0, 1 );
			primitives.setElement( array2, 1, 2 );
			primitives.setElement( array2, 2, 3 );
			primitives.setElementCollectionArray( entity1, array2 );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b.field( "elementCollectionArray",
							primitives.getExpectedIndexFieldValue( array2, 0 ),
							primitives.getExpectedIndexFieldValue( array2, 1 ),
							primitives.getExpectedIndexFieldValue( array2, 2 ) ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void elementCollectionArray_replaceElement() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TArray array = primitives.newArray( 2 );
			primitives.setElement( array, 0, 0 );
			primitives.setElement( array, 1, 1 );
			primitives.setElementCollectionArray( entity1, array );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b.field( "elementCollectionArray",
							primitives.getExpectedIndexFieldValue( array, 0 ),
							primitives.getExpectedIndexFieldValue( array, 1 ) ) );
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing an element in the array
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TArray array = primitives.getElementCollectionArray( entity1 );
			primitives.setElement( array, 1, 2 );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b.field( "elementCollectionArray",
							primitives.getExpectedIndexFieldValue( array, 0 ),
							primitives.getExpectedIndexFieldValue( array, 1 ) ) );
		} );
		backendMock.verifyExpectationsMet();
	}

}
