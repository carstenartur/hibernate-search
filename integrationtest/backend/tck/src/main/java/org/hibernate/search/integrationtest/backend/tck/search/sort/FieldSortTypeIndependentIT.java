/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests related to behavior independent from the field type
 * for sorts by field value.
 * <p>
 * Behavior that is specific to the field type is tested elsewhere,
 * e.g. {@link FieldSortBaseIT} and {@link FieldSortUnsupportedTypesIT}.
 */
public class FieldSortTypeIndependentIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	public void unknownField() {
		StubMappingScope scope = index.createScope();

		String absoluteFieldPath = "unknownField";

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( absoluteFieldPath ) )
				.toQuery()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unknown field",
						absoluteFieldPath,
						index.name()
				);
	}

	@Test
	public void objectField_nested() {
		StubMappingScope scope = index.createScope();

		String absoluteFieldPath = index.binding().nestedObject.relativeFieldName;

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( absoluteFieldPath ) )
				.toQuery()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Cannot use 'sort:field' on field '" + absoluteFieldPath + "'" );
	}

	@Test
	public void objectField_flattened() {
		StubMappingScope scope = index.createScope();

		String absoluteFieldPath = index.binding().flattenedObject.relativeFieldName;

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( absoluteFieldPath ) )
				.toQuery()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Cannot use 'sort:field' on field '" + absoluteFieldPath + "'" );
	}

	private static class IndexBinding {
		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			flattenedObject = new ObjectMapping( root, "flattenedObject", ObjectStructure.FLATTENED );
			nestedObject = new ObjectMapping( root, "nestedObject", ObjectStructure.NESTED );
		}
	}

	private static class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectStructure structure) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
			self = objectField.toReference();
		}
	}
}
