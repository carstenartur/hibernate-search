/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.spi;

import java.io.IOException;

public interface DirectoryProvider extends AutoCloseable {

	/**
	 * @param context The initialization context, giving access to configuration and environment.
	 */
	void initialize(DirectoryProviderInitializationContext context);

	/**
	 * Release any resource currently held by the {@link DirectoryProvider}.
	 * <p>
	 * Per-directory resources do not have to be released here,
	 * as they will be released by calls to {@link DirectoryHolder#close()}.
	 * <p>
	 * After this method has been called, the result of calling any other method on the same instance is undefined.
	 *
	 * @throws RuntimeException If an error occurs while releasing resources.
	 */
	@Override
	default void close() {
	}

	/**
	 * Creates a {@link DirectoryHolder} for a given name, allocating internal resources (filesystem directories, ...)
	 * as necessary.
	 * <p>
	 * The provided index names are raw and do not take into account the limitations of the internal representation
	 * of indexes. If some characters cannot be used in a given {@link DirectoryProvider},
	 * this provider is expected to escape characters as necessary using an encoding scheme assigning
	 * a unique representation to each index name,
	 * so as to avoid two index names to be encoded into identical internal representations.
	 * Lower-casing the index name, for example, is not an acceptable encoding scheme,
	 * as two index names differing only in case could end up using the same directory.
	 *
	 * @param context The creation context, giving access to configuration and environment.
	 * @return The directory to use for that index name
	 * @throws IOException If an error occurs while initializing the directory.
	 */
	DirectoryHolder createDirectory(DirectoryCreationContext context) throws IOException;

}
