/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkExecutor;
import org.hibernate.search.engine.mapper.session.context.spi.DetachedSessionContextImplementor;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.IdentifierMappingImplementor;
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;

/**
 * @param <I> The identifier type for the mapped entity type.
 * @param <E> The entity type mapped to the index.
 * @param <D> The document type for the index.
 */
public interface PojoWorkIndexedTypeContext<I, E, D extends DocumentElement> {

	Class<E> getJavaClass();

	IdentifierMappingImplementor<I, E> getIdentifierMapping();

	Supplier<E> toEntitySupplier(AbstractPojoSessionContextImplementor sessionContext, Object entity);

	DocumentReferenceProvider toDocumentReferenceProvider(AbstractPojoSessionContextImplementor sessionContext,
			I identifier, Supplier<E> entitySupplier);

	PojoDocumentContributor<D, E> toDocumentContributor(Supplier<E> entitySupplier,
			AbstractPojoSessionContextImplementor sessionContext);

	boolean requiresSelfReindexing(Set<String> dirtyPaths);

	void resolveEntitiesToReindex(PojoReindexingCollector collector, PojoRuntimeIntrospector runtimeIntrospector,
			Supplier<E> entitySupplier, Set<String> dirtyPaths);

	PojoIndexedTypeWorkPlan<I, E, D> createWorkPlan(AbstractPojoSessionContextImplementor sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy);

	PojoTypeDocumentWorkExecutor<I, E, D> createDocumentWorkExecutor(
			AbstractPojoSessionContextImplementor sessionContext,
			DocumentCommitStrategy commitStrategy);

	IndexWorkExecutor createWorkExecutor(DetachedSessionContextImplementor sessionContext);

}
