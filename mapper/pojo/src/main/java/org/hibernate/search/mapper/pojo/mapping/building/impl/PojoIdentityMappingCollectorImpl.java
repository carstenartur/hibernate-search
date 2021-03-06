/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundRoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.IdentifierMappingImplementor;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.PropertyIdentifierMapping;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.ProvidedStringIdentifierMapping;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.RoutingKeyBridgeRoutingKeyProvider;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.RoutingKeyProvider;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.processing.building.impl.PojoIdentityMappingCollector;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class PojoIdentityMappingCollectorImpl<E> implements PojoIdentityMappingCollector {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoRawTypeModel<E> typeModel;
	private final PojoMappingHelper mappingHelper;
	private final IndexManagerBuildingState<?> indexManagerBuildingState;

	private final boolean implicitProvidedId;
	private final BoundPojoModelPathPropertyNode<?, ?> entityIdPropertyPath;

	IdentifierMappingImplementor<?, E> identifierMapping;
	Optional<PojoPropertyModel<?>> documentIdSourceProperty;
	RoutingKeyProvider<E> routingKeyProvider;

	PojoIdentityMappingCollectorImpl(PojoRawTypeModel<E> typeModel,
			PojoTypeAdditionalMetadata typeAdditionalMetadata,
			PojoMappingHelper mappingHelper,
			IndexManagerBuildingState<?> indexManagerBuildingState,
			boolean implicitProvidedId) {
		this.typeModel = typeModel;
		this.mappingHelper = mappingHelper;
		this.indexManagerBuildingState = indexManagerBuildingState;
		this.implicitProvidedId = implicitProvidedId;

		Optional<String> entityIdPropertyName = typeAdditionalMetadata.getEntityTypeMetadata()
				.orElseThrow( () -> log.missingEntityTypeMetadata( typeModel ) )
				.getEntityIdPropertyName();
		if ( entityIdPropertyName.isPresent() ) {
			this.entityIdPropertyPath = BoundPojoModelPath.root( typeModel ).property( entityIdPropertyName.get() );
		}
		else {
			this.entityIdPropertyPath = null;
		}
	}

	void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( IdentifierMappingImplementor::close, identifierMapping );
			closer.push( RoutingKeyProvider::close, routingKeyProvider );
		}
	}

	@Override
	public <T> void identifierBridge(BoundPojoModelPathPropertyNode<?, T> modelPath,
			IdentifierBinder binder) {
		BoundIdentifierBridge<T> boundIdentifierBridge = mappingHelper.getIndexModelBinder()
				.bindIdentifier( indexManagerBuildingState.getIndexedEntityBindingContext(), modelPath, binder );
		PojoPropertyModel<T> propertyModel = modelPath.getPropertyModel();
		this.identifierMapping = new PropertyIdentifierMapping<>(
				propertyModel.getTypeModel().getRawType().getCaster(),
				propertyModel.getHandle(),
				boundIdentifierBridge.getBridgeHolder()
		);
		this.documentIdSourceProperty = Optional.of( propertyModel );
	}

	@Override
	public <T> BoundRoutingKeyBridge<T> routingKeyBridge(BoundPojoModelPathTypeNode<T> modelPath,
			RoutingKeyBinder<?> binder) {
		BoundRoutingKeyBridge<T> boundRoutingKeyBridge = mappingHelper.getIndexModelBinder()
				.bindRoutingKey( indexManagerBuildingState.getIndexedEntityBindingContext(), modelPath, binder );
		this.routingKeyProvider = new RoutingKeyBridgeRoutingKeyProvider<>( boundRoutingKeyBridge.getBridgeHolder() );
		return boundRoutingKeyBridge;
	}

	void applyDefaults() {
		if ( identifierMapping == null ) {
			// Assume a provided ID if requested
			if ( implicitProvidedId ) {
				identifierMapping = ProvidedStringIdentifierMapping.get();
				documentIdSourceProperty = Optional.empty();
			}
			// Fall back to the entity ID if possible
			else if ( entityIdPropertyPath != null ) {
				identifierBridge( entityIdPropertyPath, null );
			}
			else {
				throw log.missingIdentifierMapping( typeModel );
			}
		}

		if ( routingKeyProvider == null ) {
			routingKeyProvider = RoutingKeyProvider.alwaysNull();
		}
	}

}
