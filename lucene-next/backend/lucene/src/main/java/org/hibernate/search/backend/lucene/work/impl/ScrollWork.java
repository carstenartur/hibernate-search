/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;

public class ScrollWork<ER> implements ReadWork<ER> {

	private final LuceneSearcher<?, ER> searcher;

	private final ScoreDoc after;
	private final int limit;
	private final int totalHitCountThreshold;

	ScrollWork(LuceneSearcher<?, ER> searcher, ScoreDoc after, int limit, int totalHitCountThreshold) {
		this.after = after;
		this.limit = limit;
		this.searcher = searcher;
		this.totalHitCountThreshold = totalHitCountThreshold;
	}

	@Override
	public ER execute(ReadWorkExecutionContext context) {
		try {
			IndexSearcher indexSearcher = context.createSearcher();

			return searcher.scroll( indexSearcher, context.getIndexReaderMetadataResolver(), after, limit,
					totalHitCountThreshold );
		}
		catch (IOException e) {
			throw QueryLog.INSTANCE.ioExceptionOnQueryExecution( searcher.getLuceneQueryForExceptions(), e.getMessage(),
					context.getEventContext(), e );
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "searcher=" ).append( searcher )
				.append( ", limit=" ).append( limit )
				.append( "]" );
		return sb.toString();
	}
}
