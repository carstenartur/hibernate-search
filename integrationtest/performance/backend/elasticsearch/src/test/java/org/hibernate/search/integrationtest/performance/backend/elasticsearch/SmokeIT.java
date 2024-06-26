/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.performance.backend.elasticsearch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchTestHostConnectionConfiguration;
import org.hibernate.search.util.impl.test.SystemHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * Test that JMH benchmarks work correctly on a very short run.
 * <p>
 * This may not work correctly when run from the IDE.
 * <p>
 * See README to know how to run the benchmark from the command line to obtain more reliable results.
 */
public class SmokeIT {
	private final List<SystemHelper.SystemPropertyRestorer> toClose = new ArrayList<>();

	@Before
	public void setupConnectionInfo() {
		Map<String, String> connectionInfo = new LinkedHashMap<>();
		ElasticsearchTestHostConnectionConfiguration.get().addToBackendProperties( connectionInfo );
		connectionInfo.forEach( (key, value) -> {
			if ( value != null ) {
				toClose.add( SystemHelper.setSystemProperty( key, value ) );
			}
		} );
	}

	@After
	public void restoreSystemProperties() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( SystemHelper.SystemPropertyRestorer::close, toClose );
		}
	}

	@Test
	public void test() throws RunnerException {
		Options opts = new OptionsBuilder()
				.include( ".*" )
				.warmupIterations( 0 )
				.measurementIterations( 1 )
				.measurementTime( TimeValue.seconds( 1 ) )
				.param(
						"configuration",
						// Overriding read timeout to avoid failures on some super slow machines (Mac?)
						"read_timeout=120000",
						"read_timeout=120000&max_connections_per_route=30"
				)
				.param( "initialIndexSize", "100" )
				.param( "batchSize", "10" )
				.param( "maxResults", "10" )
				.shouldFailOnError( true )
				.forks( 0 ) // To simplify debugging; Remember this implies JVM parameters via @Fork won't be applied.
				.build();

		new Runner( opts ).run();
	}

}
