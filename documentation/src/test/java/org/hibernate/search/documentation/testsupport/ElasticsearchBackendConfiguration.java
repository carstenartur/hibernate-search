/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.testsupport;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.rule.MappingSetupHelper;
import org.hibernate.search.util.impl.integrationtest.elasticsearch.rule.TestElasticsearchClient;

import org.junit.rules.TestRule;

public class ElasticsearchBackendConfiguration extends AbstractDocumentationBackendConfiguration {

	// This will be used in a later commit
	protected final TestElasticsearchClient testElasticsearchClient = new TestElasticsearchClient();

	@Override
	public String toString() {
		return "elasticsearch";
	}

	@Override
	public Optional<TestRule> getTestRule() {
		return Optional.of( testElasticsearchClient );
	}

	@Override
	public <C extends MappingSetupHelper<C, ?, ?>.AbstractSetupContext> C setupWithName(C setupContext,
			String backendName, TestConfigurationProvider configurationProvider) {
		return setupContext
				.withBackendProperties(
						backendName,
						getBackendProperties( configurationProvider, "backend-elasticsearch" )
				)
				.withBackendProperty(
						backendName,
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						new ElasticsearchSimpleMappingAnalysisConfigurer()
				);
	}
}
