/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;

public class LuceneDeleteAllEntriesWork extends AbstractLuceneDeleteAllEntriesWork {

	public LuceneDeleteAllEntriesWork(String tenantId) {
		super( tenantId );
	}

	@Override
	protected long doDeleteDocuments(IndexWriterDelegator indexWriterDelegator, String tenantId) throws IOException {
		return indexWriterDelegator.deleteAll();
	}
}
