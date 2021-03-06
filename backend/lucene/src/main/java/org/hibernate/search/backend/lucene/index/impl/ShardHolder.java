/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.index.spi.ShardingStrategy;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.ReadIndexManagerContext;
import org.hibernate.search.backend.lucene.lowlevel.reader.spi.IndexReaderHolder;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.execution.impl.WorkExecutionIndexManagerContext;
import org.hibernate.search.engine.backend.index.spi.IndexManagerStartContext;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;

class ShardHolder implements Closeable, ReadIndexManagerContext, WorkExecutionIndexManagerContext {

	private final IndexManagerBackendContext backendContext;
	private final LuceneIndexModel model;

	private BeanHolder<? extends ShardingStrategy> shardingStrategyHolder;
	private final Map<String, Shard> shards = new LinkedHashMap<>();
	private final List<LuceneWriteWorkOrchestrator> writeOrchestrators = new ArrayList<>();

	ShardHolder(IndexManagerBackendContext backendContext, LuceneIndexModel model) {
		this.backendContext = backendContext;
		this.model = model;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[indexName=" + model.getIndexName() + "]";
	}

	void start(IndexManagerStartContext startContext) {
		ConfigurationPropertySource propertySource = startContext.getConfigurationPropertySource();

		try {
			ShardingStrategyInitializationContextImpl initializationContext =
					new ShardingStrategyInitializationContextImpl(
							backendContext,
							model,
							startContext,
							propertySource.withMask( "sharding" )
					);
			this.shardingStrategyHolder = initializationContext.create( shards );

			if ( startContext.getFailureCollector().hasFailure() ) {
				// At least one shard failed; abort.
				return;
			}

			for ( Shard shard : shards.values() ) {
				writeOrchestrators.add( shard.getWriteOrchestrator() );
			}
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.pushAll( shards.values() );
			shards.clear();
			writeOrchestrators.clear();
			throw e;
		}
	}

	@Override
	public void close() throws IOException {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.pushAll( Shard::close, shards.values() );
			shards.clear();
			writeOrchestrators.clear();
		}
	}

	@Override
	public String getIndexName() {
		return model.getIndexName();
	}

	@Override
	public void openIndexReaders(Set<String> routingKeys, Collection<IndexReaderHolder> readerCollector)
			throws IOException {
		Collection<Shard> enabledShards = toShards( routingKeys );
		for ( Shard shard : enabledShards ) {
			readerCollector.add( shard.openReader() );
		}
	}

	@Override
	public LuceneWriteWorkOrchestrator getWriteOrchestrator(String documentId, String routingKey) {
		return toShard( documentId, routingKey ).getWriteOrchestrator();
	}

	@Override
	public Collection<LuceneWriteWorkOrchestrator> getAllWriteOrchestrators() {
		return writeOrchestrators;
	}

	public List<Shard> getShardsForTests() {
		return new ArrayList<>( shards.values() );
	}

	private Collection<Shard> toShards(Set<String> routingKeys) {
		if ( shardingStrategyHolder == null || routingKeys.isEmpty() ) {
			// No sharding or no routing key => target all shards
			return shards.values();
		}

		Set<String> shardIdentifiers = shardingStrategyHolder.get().toShardIdentifiers( routingKeys );

		Collection<Shard> enabledShards = new HashSet<>();
		for ( String shardId : shardIdentifiers ) {
			enabledShards.add( shards.get( shardId ) );
		}
		return enabledShards;
	}

	private Shard toShard(String documentId, String routingKey) {
		if ( shardingStrategyHolder == null ) {
			// Sharding is disabled: there's only one shard
			return shards.values().iterator().next();
		}

		String shardId = shardingStrategyHolder.get().toShardIdentifier( documentId, routingKey );
		return shards.get( shardId );
	}
}
