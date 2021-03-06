/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl;

/**
 * A context allowing the definition of named analyzers and normalizers in a Lucene backend.
 *
 * @deprecated Use {@link org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext}
 * in your analysis configurer,
 * and avoid chaining multiple analyzer definitions in one statement
 * ({@code context.analyzer(...)....analyzer(...)...;}):
 * use multiple statements instead
 * ({@code context.analyzer(...).... ; context.analyzer(...)...;})
 */
@Deprecated
public interface LuceneAnalysisDefinitionContainerContext {

	/**
	 * Start a new analyzer definition.
	 *
	 * @param name The name used to reference this analyzer in Hibernate Search.
	 * @return The initial step of a DSL where the analyzer can be defined.
	 */
	LuceneAnalyzerTypeStep analyzer(String name);

	/**
	 * Start a new normalizer definition.
	 *
	 * @param name The name used to reference this normalizer in Hibernate Search.
	 * @return The initial step of a DSL where the normalizer can be defined.
	 */
	LuceneNormalizerTypeStep normalizer(String name);

}
