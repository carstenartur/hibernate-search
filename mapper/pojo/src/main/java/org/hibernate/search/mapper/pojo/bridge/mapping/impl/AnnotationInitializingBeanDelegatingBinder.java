/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.pojo.bridge.binding.MarkerBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingKeyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.reflect.impl.GenericTypeContext;
import org.hibernate.search.util.common.reflect.impl.ReflectionUtils;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A binder that upon binding retrieves a delegate binder from the bean provider,
 * initializes it using a pre-defined annotation, and then delegates to that binder.
 *
 * @param <A> The type of annotations accepted by the delegate binder.
 */
@SuppressWarnings("rawtypes") // Clients cannot provide a level of guarantee stronger than raw types
public final class AnnotationInitializingBeanDelegatingBinder<A extends Annotation>
		implements TypeBinder<A>, PropertyBinder<A>, RoutingKeyBinder<A>, MarkerBinder<A> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanReference<?> delegateReference;

	private A annotation;

	public AnnotationInitializingBeanDelegatingBinder(BeanReference<?> delegateReference) {
		this.delegateReference = delegateReference;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[delegateReference=" + delegateReference + ", annotation=" + annotation + "]";
	}

	@Override
	public void initialize(A annotation) {
		// Delay initialization to the bind() call
		this.annotation = annotation;
	}

	@Override
	public void bind(TypeBindingContext context) {
		try ( BeanHolder<? extends TypeBinder> delegateHolder =
				createDelegate( context.getBeanResolver(), TypeBinder.class ) ) {
			@SuppressWarnings("unchecked") // Checked using reflection in createDelegate
			TypeBinder<A> castedDelegate = delegateHolder.get();
			castedDelegate.initialize( annotation );
			castedDelegate.bind( context );
		}
	}

	@Override
	public void bind(PropertyBindingContext context) {
		try ( BeanHolder<? extends PropertyBinder> delegateHolder =
				createDelegate( context.getBeanResolver(), PropertyBinder.class ) ) {
			@SuppressWarnings("unchecked") // Checked using reflection in createDelegate
			PropertyBinder<A> castedDelegate = delegateHolder.get();
			castedDelegate.initialize( annotation );
			castedDelegate.bind( context );
		}
	}

	@Override
	public void bind(RoutingKeyBindingContext context) {
		try ( BeanHolder<? extends RoutingKeyBinder> delegateHolder =
				createDelegate( context.getBeanResolver(), RoutingKeyBinder.class ) ) {
			@SuppressWarnings("unchecked") // Checked using reflection in createDelegate
			RoutingKeyBinder<A> castedDelegate = delegateHolder.get();
			castedDelegate.initialize( annotation );
			castedDelegate.bind( context );
		}
	}

	@Override
	public void bind(MarkerBindingContext context) {
		try ( BeanHolder<? extends MarkerBinder> delegateHolder =
				createDelegate( context.getBeanResolver(), MarkerBinder.class ) ) {
			@SuppressWarnings("unchecked") // Checked using reflection in createDelegate
			MarkerBinder<A> castedDelegate = delegateHolder.get();
			castedDelegate.initialize( annotation );
			castedDelegate.bind( context );
		}
	}

	private <B> BeanHolder<? extends B> createDelegate(BeanResolver beanResolver, Class<B> expectedType) {
		BeanHolder<? extends B> delegateHolder = delegateReference.asSubTypeOf( expectedType ).resolve( beanResolver );
		try {
			B delegate = delegateHolder.get();
			Class<?> annotationType = annotation.annotationType();
			GenericTypeContext bridgeTypeContext = new GenericTypeContext( delegate.getClass() );
			Class<?> binderAnnotationType = bridgeTypeContext.resolveTypeArgument( expectedType, 0 )
					.map( ReflectionUtils::getRawType )
					.orElseThrow( () -> new AssertionFailure(
							"Could not auto-detect the annotation type accepted by binder '"
									+ delegate + "'."
									+ " There is a bug in Hibernate Search, please report it."
					) );
			if ( !binderAnnotationType.isAssignableFrom( annotationType ) ) {
				throw log.invalidAnnotationTypeForBinder( delegate, annotationType );
			}

			return delegateHolder;
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( delegateHolder );
			throw e;
		}
	}

}
