/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.elasticsearch.dialect;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

import com.google.gson.JsonObject;

@SuppressWarnings("deprecation") // We use Paths.DOC on purpose
public class Elasticsearch5TestDialect implements ElasticsearchTestDialect {

	@Override
	public boolean isEmptyMappingPossible() {
		return true;
	}

	@Override
	public URLEncodedString getTypeKeywordForNonMappingApi() {
		return Paths.DOC;
	}

	@Override
	public Optional<URLEncodedString> getTypeNameForMappingApi() {
		return Optional.of( Paths.DOC );
	}

	@Override
	public Boolean getIncludeTypeNameParameterForMappingApi() {
		return null;
	}

	@Override
	public List<String> getAllLocalDateDefaultMappingFormats() {
		return Arrays.asList( "yyyy-MM-dd", "yyyyyyyyy-MM-dd" );
	}

	@Override
	public void setTemplatePattern(JsonObject object, String pattern) {
		object.addProperty( "template", pattern );
	}

	@Override
	public boolean isGeoPointIndexNullAsPossible() {
		return false;
	}

	@Override
	public boolean worksFineWithStrictGraterThanRangedQueriesOnScaledFloatField() {
		return false;
	}
}
