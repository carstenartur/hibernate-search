/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import org.hibernate.search.mapper.javabean.CloseableJavaBeanMapping;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.session.SearchSessionBuilder;
import org.hibernate.search.mapper.javabean.mapping.context.impl.JavaBeanMappingContext;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.javabean.session.impl.JavaBeanSearchSession;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingImplementor;

public class JavaBeanMappingImpl extends AbstractPojoMappingImplementor<JavaBeanMapping> implements CloseableJavaBeanMapping {

	private final JavaBeanMappingContext mappingContext;
	private final JavaBeanTypeContextContainer typeContextContainer;

	JavaBeanMappingImpl(PojoMappingDelegate mappingDelegate, JavaBeanTypeContextContainer typeContextContainer) {
		super( mappingDelegate );
		this.mappingContext = new JavaBeanMappingContext();
		this.typeContextContainer = typeContextContainer;
	}

	@Override
	public JavaBeanMapping toConcreteType() {
		return this;
	}

	@Override
	public SearchSession createSession() {
		return createSearchManagerBuilder().build();
	}

	@Override
	public SearchSessionBuilder createSessionWithOptions() {
		return createSearchManagerBuilder();
	}

	private SearchSessionBuilder createSearchManagerBuilder() {
		return new JavaBeanSearchSession.JavaBeanSearchSessionBuilder(
				getDelegate(), mappingContext, typeContextContainer
		);
	}
}
