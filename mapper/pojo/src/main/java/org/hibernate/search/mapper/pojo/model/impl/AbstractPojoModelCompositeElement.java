/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.PojoModelCompositeElement;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoPropertyAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @param <V> The type of the element, i.e. the type of values returned by accessors to this element.
 */
public abstract class AbstractPojoModelCompositeElement<V> implements PojoModelCompositeElement {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider;
	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, PojoModelNestedCompositeElement<V, ?>> properties = new LinkedHashMap<>();
	private PojoTypeAdditionalMetadata typeAdditionalMetadata;
	private boolean propertiesInitialized = false;

	private PojoElementAccessor<?> accessor;

	AbstractPojoModelCompositeElement(PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider) {
		this.typeAdditionalMetadataProvider = typeAdditionalMetadataProvider;
	}

	@Override
	@SuppressWarnings("unchecked") // The cast is checked using reflection
	public final <T> PojoElementAccessor<T> createAccessor(Class<T> requestedType) {
		if ( !isAssignableTo( requestedType ) ) {
			throw log.incompatibleRequestedType( getModelPathTypeNode().toUnboundPath(), requestedType );
		}
		return (PojoElementAccessor<T>) createAccessor();
	}

	@Override
	public PojoElementAccessor<?> createAccessor() {
		if ( accessor == null ) {
			accessor = doCreateAccessor();
		}
		return accessor;
	}

	@Override
	public boolean isAssignableTo(Class<?> clazz) {
		return getTypeModel().getRawType().isSubTypeOf( clazz );
	}

	@Override
	public PojoModelNestedCompositeElement<?, ?> property(String relativeFieldName) {
		return properties.computeIfAbsent( relativeFieldName, name -> {
			BoundPojoModelPathTypeNode<V> modelPathTypeNode = getModelPathTypeNode();
			PojoPropertyAdditionalMetadata additionalMetadata = getTypeAdditionalMetadata().getPropertyAdditionalMetadata( name );
			return new PojoModelNestedCompositeElement<>(
					this,
					modelPathTypeNode.property( name ),
					additionalMetadata,
					typeAdditionalMetadataProvider
			);
		} );
	}

	@Override
	public Stream<? extends PojoModelProperty> properties() {
		if ( !propertiesInitialized ) {
			// Populate all the known properties
			getTypeModel().getRawType().getAscendingSuperTypes()
					.flatMap( PojoRawTypeModel::getDeclaredProperties )
					.map( PojoPropertyModel::getName )
					.forEach( this::property );
			propertiesInitialized = true;
		}
		return properties.values().stream();
	}

	public boolean hasDependency() {
		return hasAccessor();
	}

	public boolean hasNonRootDependency() {
		for ( PojoModelNestedCompositeElement<V, ?> property : properties.values() ) {
			if ( property.hasAccessor() ) {
				return true;
			}
		}
		return false;
	}

	abstract PojoElementAccessor<V> doCreateAccessor();

	abstract BoundPojoModelPathTypeNode<V> getModelPathTypeNode();

	final boolean hasAccessor() {
		return accessor != null;
	}

	final void contributePropertyDependencies(PojoIndexingDependencyCollectorTypeNode<V> dependencyCollector) {
		for ( Map.Entry<String, PojoModelNestedCompositeElement<V, ?>> entry : properties.entrySet() ) {
			entry.getValue().contributeDependencies( dependencyCollector );
		}
	}

	private PojoTypeModel<V> getTypeModel() {
		return getModelPathTypeNode().getTypeModel();
	}

	private PojoTypeAdditionalMetadata getTypeAdditionalMetadata() {
		if ( typeAdditionalMetadata == null ) {
			typeAdditionalMetadata = typeAdditionalMetadataProvider.get( getModelPathTypeNode().getTypeModel().getRawType() );
		}
		return typeAdditionalMetadata;
	}
}
