/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.bridge.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.PropertyBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;

@PropertyBinding(binder = @PropertyBinderRef(type = org.hibernate.search.integrationtest.showcase.library.bridge.MultiKeywordStringBridge.Binder.class))
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Documented
@Repeatable(MultiKeywordStringBinding.List.class)
public @interface MultiKeywordStringBinding {

	String fieldName();

	String separatorPattern() default
			org.hibernate.search.integrationtest.showcase.library.bridge.MultiKeywordStringBridge.SEPARATOR_PATTERN_DEFAULT;

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@Documented
	@interface List {
		MultiKeywordStringBinding[] value();
	}
}
