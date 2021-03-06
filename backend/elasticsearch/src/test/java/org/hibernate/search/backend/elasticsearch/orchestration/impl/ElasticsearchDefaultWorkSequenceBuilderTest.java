/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkSequenceBuilder.BulkResultExtractionStep;
import org.hibernate.search.backend.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResultItemExtractor;
import org.hibernate.search.engine.common.spi.ContextualErrorHandler;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.ExceptionMatcherBuilder;

import org.junit.Before;
import org.junit.Test;

import org.easymock.EasyMockSupport;


@SuppressWarnings({"unchecked", "rawtypes"}) // Raw types are the only way to mock parameterized types with EasyMock
public class ElasticsearchDefaultWorkSequenceBuilderTest extends EasyMockSupport {

	private ElasticsearchRefreshableWorkExecutionContext contextMock;
	private Supplier<ElasticsearchRefreshableWorkExecutionContext> contextSupplierMock;
	private ContextualErrorHandler errorHandlerMock;
	private Supplier<ContextualErrorHandler> errorHandlerSupplierMock;

	@Before
	public void initMocks() {
		contextMock = createStrictMock( ElasticsearchRefreshableWorkExecutionContext.class );
		contextSupplierMock = createStrictMock( Supplier.class );
		errorHandlerMock = createStrictMock( ContextualErrorHandler.class );
		errorHandlerSupplierMock = createStrictMock( Supplier.class );
	}

	@Test
	public void simple() {
		ElasticsearchWork<Object> work1 = work( 1 );
		BulkableElasticsearchWork<Object> work2 = bulkableWork( 2 );

		Object work1Result = new Object();
		Object work2Result = new Object();

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<Object> work1Future = new CompletableFuture<>();
		CompletableFuture<Object> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> refreshFuture = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work1FutureFromSequenceBuilder;
		CompletableFuture<Object> work2FutureFromSequenceBuilder;

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, errorHandlerSupplierMock );
		verifyAll();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		work1FutureFromSequenceBuilder = builder.addNonBulkExecution( work1 );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();

		resetAll();
		replayAll();
		work2FutureFromSequenceBuilder = builder.addNonBulkExecution( work2 );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();

		resetAll();
		replayAll();
		CompletableFuture<Void> sequenceFuture = builder.build();
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( work1.execute( contextMock ) ).andReturn( (CompletableFuture) work1Future );
		replayAll();
		previousFuture.complete( null );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( work2.execute( contextMock ) ).andReturn( (CompletableFuture) work2Future );
		replayAll();
		work1Future.complete( work1Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for refresh
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( contextMock.executePendingRefreshes() ).andReturn( refreshFuture );
		replayAll();
		work2Future.complete( work2Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for refresh
		assertThat( work2FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for refresh
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		refreshFuture.complete( null );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isSuccessful( work1Result );
		assertThat( work2FutureFromSequenceBuilder ).isSuccessful( work2Result );
		assertThat( sequenceFuture ).isSuccessful( (Void) null );
	}

	@Test
	public void bulk() {
		ElasticsearchWork<Object> work1 = work( 1 );
		BulkableElasticsearchWork<Object> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Object> work3 = bulkableWork( 3 );
		ElasticsearchWork<Object> work4 = work( 4 );
		ElasticsearchWork<BulkResult> bulkWork = work( 5 );

		Object work1Result = new Object();
		Object work2Result = new Object();
		Object work3Result = new Object();
		Object work4Result = new Object();
		BulkResult bulkResultMock = createStrictMock( BulkResult.class );
		BulkResultItemExtractor bulkItemResultExtractorMock = createStrictMock( BulkResultItemExtractor.class );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Object> work1Future = new CompletableFuture<>();
		CompletableFuture<Object> work2Future = new CompletableFuture<>();
		CompletableFuture<Object> work3Future = new CompletableFuture<>();
		CompletableFuture<Object> work4Future = new CompletableFuture<>();
		CompletableFuture<Void> refreshFuture = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work1FutureFromSequenceBuilder;
		CompletableFuture<Object> work2FutureFromSequenceBuilder;
		CompletableFuture<Object> work3FutureFromSequenceBuilder;
		CompletableFuture<Object> work4FutureFromSequenceBuilder;

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, errorHandlerSupplierMock );
		verifyAll();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		work1FutureFromSequenceBuilder = builder.addNonBulkExecution( work1 );
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture );
		work2FutureFromSequenceBuilder = extractionStep.add( work2, 0 );
		work3FutureFromSequenceBuilder = extractionStep.add( work3, 1 );
		work4FutureFromSequenceBuilder = builder.addNonBulkExecution( work4 );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();

		resetAll();
		replayAll();
		CompletableFuture<Void> sequenceFuture = builder.build();
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( work1.execute( contextMock ) ).andReturn( (CompletableFuture) work1Future );
		replayAll();
		previousFuture.complete( null );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		work1Future.complete( work1Result );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for the refresh
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkWork.execute( contextMock ) ).andReturn( (CompletableFuture) bulkResultFuture );
		replayAll();
		bulkWorkFuture.complete( bulkWork );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkResultMock.withContext( contextMock ) ).andReturn( bulkItemResultExtractorMock );
		expect( bulkItemResultExtractorMock.extract( work2, 0 ) ).andReturn( work2Future );
		expect( bulkItemResultExtractorMock.extract( work3, 1 ) ).andReturn( work3Future );
		replayAll();
		bulkResultFuture.complete( bulkResultMock );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		work3Future.complete( work3Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( work4.execute( contextMock ) ).andReturn( (CompletableFuture) work4Future );
		replayAll();
		work2Future.complete( work2Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( contextMock.executePendingRefreshes() ).andReturn( refreshFuture );
		replayAll();
		work4Future.complete( work4Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		refreshFuture.complete( null );
		verifyAll();
		assertThat( work2FutureFromSequenceBuilder ).isSuccessful( work1Result );
		assertThat( work2FutureFromSequenceBuilder ).isSuccessful( work2Result );
		assertThat( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		assertThat( work4FutureFromSequenceBuilder ).isSuccessful( work4Result );
		assertThat( sequenceFuture ).isSuccessful( (Void) null );
	}

	@Test
	public void newSequenceOnReset() {
		BulkableElasticsearchWork<Void> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture1 = new CompletableFuture<>();
		CompletableFuture<?> previousFuture2 = new CompletableFuture<>();
		// We'll never complete this future, so as to check that work 2 is executed in a different sequence
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2refreshFuture = new CompletableFuture<>();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, errorHandlerSupplierMock );
		verifyAll();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replayAll();
		builder.init( previousFuture1 );
		verifyAll();

		resetAll();
		replayAll();
		builder.addNonBulkExecution( work1 );
		verifyAll();

		resetAll();
		replayAll();
		CompletableFuture<Void> sequence1Future = builder.build();
		verifyAll();
		assertThat( sequence1Future ).isPending();

		resetAll();
		expect( work1.execute( contextMock ) ).andReturn( (CompletableFuture) work1Future );
		replayAll();
		previousFuture1.complete( null );
		verifyAll();
		assertThat( sequence1Future ).isPending();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replayAll();
		builder.init( previousFuture2 );
		verifyAll();
		assertThat( sequence1Future ).isPending();

		resetAll();
		replayAll();
		builder.addNonBulkExecution( work2 );
		verifyAll();
		assertThat( sequence1Future ).isPending();

		resetAll();
		replayAll();
		CompletableFuture<Void> sequence2Future = builder.build();
		verifyAll();
		assertThat( sequence1Future ).isPending();
		assertThat( sequence2Future ).isPending();

		resetAll();
		expect( work2.execute( contextMock ) ).andReturn( (CompletableFuture) work2Future );
		replayAll();
		previousFuture2.complete( null );
		verifyAll();
		assertThat( sequence1Future ).isPending();
		assertThat( sequence2Future ).isPending();

		resetAll();
		expect( contextMock.executePendingRefreshes() ).andReturn( sequence2refreshFuture );
		replayAll();
		work2Future.complete( null );
		verifyAll();
		assertThat( sequence1Future ).isPending();
		assertThat( sequence2Future ).isPending();

		resetAll();
		replayAll();
		sequence2refreshFuture.complete( null );
		verifyAll();
		assertThat( sequence1Future ).isPending();
		assertThat( sequence2Future ).isSuccessful( (Void) null );
	}

	@Test
	public void error_work() {
		ElasticsearchWork<Object> work0 = work( 0 );
		ElasticsearchWork<Void> work1 = work( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );

		Object work0Result = new Object();

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<Object> work0Future = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> refreshFuture = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work0FutureFromSequenceBuilder;
		CompletableFuture<Void> work1FutureFromSequenceBuilder;
		CompletableFuture<Void> work2FutureFromSequenceBuilder;

		MyException exception = new MyException();

		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		expect( work0.execute( contextMock ) ).andReturn( (CompletableFuture) work0Future );
		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, errorHandlerSupplierMock );
		builder.init( previousFuture );
		work0FutureFromSequenceBuilder = builder.addNonBulkExecution( work0 );
		work1FutureFromSequenceBuilder = builder.addNonBulkExecution( work1 );
		work2FutureFromSequenceBuilder = builder.addNonBulkExecution( work2 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyAll();
		assertThat( work0FutureFromSequenceBuilder ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( work1.execute( contextMock ) ).andReturn( (CompletableFuture) work1Future );
		replayAll();
		work0Future.complete( work0Result );
		verifyAll();
		assertThat( work0FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for refresh
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		errorHandlerMock.markAsFailed( work1, exception );
		errorHandlerMock.markAsSkipped( work2 );
		expect( contextMock.executePendingRefreshes() ).andReturn( refreshFuture );
		replayAll();
		work1Future.completeExceptionally( exception );
		verifyAll();
		assertThat( work0FutureFromSequenceBuilder ).isPending();
		// Errors must be propagated to the individual work futures ASAP
		assertThat( work1FutureFromSequenceBuilder ).isFailed( exception );
		// Subsequent works that haven't been executed must get a specific exception
		assertThat( work2FutureFromSequenceBuilder ).isFailed(
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( "operation was skipped due to the failure of a previous work in the same workset" )
						.causedBy( exception ).build()
		);
		// But the sequence future must wait for the refresh to happen
		assertThat( sequenceFuture ).isPending();

		resetAll();
		errorHandlerMock.handle();
		replayAll();
		refreshFuture.complete( null );
		verifyAll();
		// Works that happened before the error must be considered as successful if the refresh is successful
		assertThat( work0FutureFromSequenceBuilder ).isSuccessful( work0Result );
		// Errors MUST NOT be propagated to the sequence future if they've been handled successfully
		assertThat( sequenceFuture ).isSuccessful();
	}

	@Test
	public void error_bulk_work() {
		BulkableElasticsearchWork<Void> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Void> work3 = bulkableWork( 3 );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<Void> refreshFuture = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Void> work1FutureFromSequenceBuilder;
		CompletableFuture<Void> work2FutureFromSequenceBuilder;
		CompletableFuture<Void> work3FutureFromSequenceBuilder;

		MyException exception = new MyException();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, errorHandlerSupplierMock );
		verifyAll();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture );
		work1FutureFromSequenceBuilder = extractionStep.add( work1, 0 );
		work2FutureFromSequenceBuilder = extractionStep.add( work2, 1 );
		work3FutureFromSequenceBuilder = extractionStep.add( work3, 2 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		errorHandlerMock.markAsFailed( work1, exception );
		errorHandlerMock.markAsFailed( work2, exception );
		errorHandlerMock.markAsFailed( work3, exception );
		expect( contextMock.executePendingRefreshes() ).andReturn( refreshFuture );
		replayAll();
		bulkWorkFuture.completeExceptionally( exception );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isFailed( exception );
		assertThat( work1FutureFromSequenceBuilder ).isFailed( exception );
		assertThat( work2FutureFromSequenceBuilder ).isFailed( exception );
		assertThat( work3FutureFromSequenceBuilder ).isFailed( exception );
		assertThat( sequenceFuture ).isPending();

		resetAll();
		errorHandlerMock.addThrowable( exception );
		errorHandlerMock.handle();
		replayAll();
		refreshFuture.complete( null );
		verifyAll();
		// Errors MUST NOT be propagated if they've been handled
		assertThat( sequenceFuture ).isSuccessful();
	}

	@Test
	public void error_bulk_result() {
		BulkableElasticsearchWork<Void> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Void> work3 = bulkableWork( 3 );
		ElasticsearchWork<BulkResult> bulkWork = work( 4 );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Void> refreshFuture = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Void> work1FutureFromSequenceBuilder;
		CompletableFuture<Void> work2FutureFromSequenceBuilder;
		CompletableFuture<Void> work3FutureFromSequenceBuilder;

		MyException exception = new MyException();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, errorHandlerSupplierMock );
		verifyAll();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture );
		work1FutureFromSequenceBuilder = extractionStep.add( work1, 0 );
		work2FutureFromSequenceBuilder = extractionStep.add( work2, 1 );
		work3FutureFromSequenceBuilder = extractionStep.add( work3, 2 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkWork.execute( contextMock ) ).andReturn( (CompletableFuture) bulkResultFuture );
		replayAll();
		bulkWorkFuture.complete( bulkWork );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		errorHandlerMock.markAsFailed( work1, exception );
		errorHandlerMock.markAsFailed( work2, exception );
		errorHandlerMock.markAsFailed( work3, exception );
		expect( contextMock.executePendingRefreshes() ).andReturn( refreshFuture );
		replayAll();
		bulkResultFuture.completeExceptionally( exception );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isFailed( exception );
		assertThat( work1FutureFromSequenceBuilder ).isFailed( exception );
		assertThat( work2FutureFromSequenceBuilder ).isFailed( exception );
		assertThat( work3FutureFromSequenceBuilder ).isFailed( exception );
		assertThat( sequenceFuture ).isPending();

		resetAll();
		errorHandlerMock.addThrowable( exception );
		errorHandlerMock.handle();
		replayAll();
		refreshFuture.complete( null );
		verifyAll();
		// Errors MUST NOT be propagated if they've been handled
		assertThat( sequenceFuture ).isSuccessful();
	}

	@Test
	public void error_bulk_resultExtraction_singleFailure() {
		BulkableElasticsearchWork<Object> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Object> work3 = bulkableWork( 3 );
		BulkableElasticsearchWork<Void> work4 = bulkableWork( 4 );
		ElasticsearchWork<BulkResult> bulkWork = work( 5 );

		Object work1Result = new Object();
		Object work3Result = new Object();
		BulkResult bulkResultMock = createStrictMock( BulkResult.class );
		BulkResultItemExtractor bulkResultItemExtractorMock = createStrictMock( BulkResultItemExtractor.class );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Object> work1Future = new CompletableFuture<>();
		CompletableFuture<Object> work3Future = new CompletableFuture<>();
		CompletableFuture<Void> refreshFuture = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work1FutureFromSequenceBuilder;
		CompletableFuture<Void> work2FutureFromSequenceBuilder;
		CompletableFuture<Object> work3FutureFromSequenceBuilder;
		CompletableFuture<Void> work4FutureFromSequenceBuilder;

		MyRuntimeException exception = new MyRuntimeException();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, errorHandlerSupplierMock );
		verifyAll();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture );
		work1FutureFromSequenceBuilder = extractionStep.add( work1, 0 );
		work2FutureFromSequenceBuilder = extractionStep.add( work2, 1 );
		work3FutureFromSequenceBuilder = extractionStep.add( work3, 2 );
		work4FutureFromSequenceBuilder = builder.addNonBulkExecution( work4 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkWork.execute( contextMock ) ).andReturn( (CompletableFuture) bulkResultFuture );
		replayAll();
		bulkWorkFuture.complete( bulkWork );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkResultMock.withContext( contextMock ) ).andReturn( bulkResultItemExtractorMock );
		expect( bulkResultItemExtractorMock.extract( work1, 0 ) ).andReturn( work1Future );
		expect( bulkResultItemExtractorMock.extract( work2, 1 ) ).andThrow( exception );
		expect( bulkResultItemExtractorMock.extract( work3, 2 ) ).andReturn( work3Future );
		errorHandlerMock.markAsFailed( work2, exception );
		replayAll();
		bulkResultFuture.complete( bulkResultMock );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isFailed( exception );
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		work1Future.complete( work1Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for refresh
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		errorHandlerMock.markAsSkipped( work4 );
		expect( contextMock.executePendingRefreshes() ).andReturn( refreshFuture );
		replayAll();
		work3Future.complete( work3Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for refresh
		assertThat( work4FutureFromSequenceBuilder ).isFailed(
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( "operation was skipped due to the failure of a previous work in the same workset" )
						.causedBy( exception ).build()
		);
		assertThat( sequenceFuture ).isPending();

		resetAll();
		errorHandlerMock.handle();
		replayAll();
		refreshFuture.complete( null );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isSuccessful( work1Result );
		assertThat( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		// Errors MUST NOT be propagated if they've been handled
		assertThat( sequenceFuture ).isSuccessful();
	}

	@Test
	public void error_bulk_resultExtraction_multipleFailures() {
		BulkableElasticsearchWork<Void> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Void> work3 = bulkableWork( 3 );
		ElasticsearchWork<BulkResult> bulkWork = work( 4 );

		BulkResult bulkResultMock = createStrictMock( BulkResult.class );
		BulkResultItemExtractor bulkItemResultExtractorMock = createStrictMock( BulkResultItemExtractor.class );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Void> refreshFuture = new CompletableFuture<>();

		MyRuntimeException exception1 = new MyRuntimeException();
		MyRuntimeException exception2 = new MyRuntimeException();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, errorHandlerSupplierMock );
		verifyAll();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replayAll();
		builder.init( previousFuture );
		verifyAll();


		resetAll();
		replayAll();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture );
		extractionStep.add( work1, 0 );
		extractionStep.add( work2, 1 );
		builder.addNonBulkExecution( work3 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkWork.execute( contextMock ) ).andReturn( (CompletableFuture) bulkResultFuture );
		replayAll();
		bulkWorkFuture.complete( bulkWork );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkResultMock.withContext( contextMock ) ).andReturn( bulkItemResultExtractorMock );
		expect( bulkItemResultExtractorMock.extract( work1, 0 ) ).andThrow( exception1 );
		expect( bulkItemResultExtractorMock.extract( work2, 1 ) ).andThrow( exception2 );
		errorHandlerMock.markAsFailed( work1, exception1 );
		errorHandlerMock.markAsFailed( work2, exception2 );
		errorHandlerMock.markAsSkipped( work3 );
		expect( contextMock.executePendingRefreshes() ).andReturn( refreshFuture );
		replayAll();
		bulkResultFuture.complete( bulkResultMock );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( sequenceFuture ).isPending();

		resetAll();
		errorHandlerMock.handle();
		replayAll();
		refreshFuture.complete( null );
		verifyAll();
		// Errors MUST NOT be propagated if they've been handled
		assertThat( sequenceFuture ).isSuccessful();
	}


	@Test
	public void error_bulk_resultExtraction_future_singleFailure() {
		BulkableElasticsearchWork<Object> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Object> work3 = bulkableWork( 3 );
		BulkableElasticsearchWork<Void> work4 = bulkableWork( 4 );
		ElasticsearchWork<BulkResult> bulkWork = work( 5 );

		Object work1Result = new Object();
		Object work3Result = new Object();
		BulkResult bulkResultMock = createStrictMock( BulkResult.class );
		BulkResultItemExtractor bulkResultItemExtractorMock = createStrictMock( BulkResultItemExtractor.class );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Object> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();
		CompletableFuture<Object> work3Future = new CompletableFuture<>();
		CompletableFuture<Void> refreshFuture = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work1FutureFromSequenceBuilder;
		CompletableFuture<Void> work2FutureFromSequenceBuilder;
		CompletableFuture<Object> work3FutureFromSequenceBuilder;
		CompletableFuture<Void> work4FutureFromSequenceBuilder;

		MyException exception = new MyException();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, errorHandlerSupplierMock );
		verifyAll();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture );
		work1FutureFromSequenceBuilder = extractionStep.add( work1, 0 );
		work2FutureFromSequenceBuilder = extractionStep.add( work2, 1 );
		work3FutureFromSequenceBuilder = extractionStep.add( work3, 2 );
		work4FutureFromSequenceBuilder = builder.addNonBulkExecution( work4 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkWork.execute( contextMock ) ).andReturn( (CompletableFuture) bulkResultFuture );
		replayAll();
		bulkWorkFuture.complete( bulkWork );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkResultMock.withContext( contextMock ) ).andReturn( bulkResultItemExtractorMock );
		expect( bulkResultItemExtractorMock.extract( work1, 0 ) ).andReturn( work1Future );
		expect( bulkResultItemExtractorMock.extract( work2, 1 ) ).andReturn( work2Future );
		expect( bulkResultItemExtractorMock.extract( work3, 2 ) ).andReturn( work3Future );
		replayAll();
		bulkResultFuture.complete( bulkResultMock );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		replayAll();
		work1Future.complete( work1Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending(); // Still waiting for the refresh
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		errorHandlerMock.markAsFailed( work2, exception );
		replayAll();
		work2Future.completeExceptionally( exception );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isFailed( exception );
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( work4FutureFromSequenceBuilder ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		errorHandlerMock.markAsSkipped( work4 );
		expect( contextMock.executePendingRefreshes() ).andReturn( refreshFuture );
		replayAll();
		work3Future.complete( work3Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending(); // Still waiting for the refresh
		assertThat( work4FutureFromSequenceBuilder ).isFailed(
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( "operation was skipped due to the failure of a previous work in the same workset" )
						.causedBy( exception ).build()
		);
		assertThat( sequenceFuture ).isPending();

		resetAll();
		errorHandlerMock.handle();
		replayAll();
		refreshFuture.complete( null );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isSuccessful( work1Result );
		assertThat( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		// Errors MUST NOT be propagated if they've been handled
		assertThat( sequenceFuture ).isSuccessful();
	}

	@Test
	public void error_bulk_resultExtraction_future_multipleFailures() {
		BulkableElasticsearchWork<Void> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Void> work3 = bulkableWork( 3 );
		ElasticsearchWork<BulkResult> bulkWork = work( 4 );

		BulkResult bulkResultMock = createStrictMock( BulkResult.class );
		BulkResultItemExtractor bulkResultItemExtractorMock = createStrictMock( BulkResultItemExtractor.class );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<ElasticsearchWork<BulkResult>> bulkWorkFuture = new CompletableFuture<>();
		CompletableFuture<BulkResult> bulkResultFuture = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> work2Future = new CompletableFuture<>();
		CompletableFuture<Void> refreshFuture = new CompletableFuture<>();

		MyException exception1 = new MyException();
		MyException exception2 = new MyException();

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, errorHandlerSupplierMock );
		verifyAll();

		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		replayAll();
		builder.init( previousFuture );
		verifyAll();

		resetAll();
		replayAll();
		CompletableFuture<BulkResult> sequenceBuilderBulkResultFuture = builder.addBulkExecution( bulkWorkFuture );
		BulkResultExtractionStep extractionStep = builder.addBulkResultExtraction( sequenceBuilderBulkResultFuture );
		extractionStep.add( work1, 0 );
		extractionStep.add( work2, 1 );
		builder.addNonBulkExecution( work3 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkWork.execute( contextMock ) ).andReturn( (CompletableFuture) bulkResultFuture );
		replayAll();
		bulkWorkFuture.complete( bulkWork );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isPending();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		expect( bulkResultMock.withContext( contextMock ) ).andReturn( bulkResultItemExtractorMock );
		expect( bulkResultItemExtractorMock.extract( work1, 0 ) ).andReturn( work1Future );
		expect( bulkResultItemExtractorMock.extract( work2, 1 ) ).andReturn( work2Future );
		replayAll();
		bulkResultFuture.complete( bulkResultMock );
		verifyAll();
		assertThat( sequenceBuilderBulkResultFuture ).isSuccessful( bulkResultMock );
		assertThat( sequenceFuture ).isPending();

		resetAll();
		errorHandlerMock.markAsFailed( work2, exception2 );
		replayAll();
		work2Future.completeExceptionally( exception2 );
		verifyAll();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		errorHandlerMock.markAsFailed( work1, exception1 );
		errorHandlerMock.markAsSkipped( work3 );
		expect( contextMock.executePendingRefreshes() ).andReturn( refreshFuture );
		replayAll();
		work1Future.completeExceptionally( exception1 );
		verifyAll();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		errorHandlerMock.handle();
		replayAll();
		refreshFuture.complete( null );
		verifyAll();
		// Errors MUST NOT be propagated if they've been handled
		assertThat( sequenceFuture ).isSuccessful();
	}

	@Test
	public void error_handler() {
		ElasticsearchWork<Void> work1 = work( 1 );
		BulkableElasticsearchWork<Void> work2 = bulkableWork( 2 );

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> previousFuture = new CompletableFuture<>();
		CompletableFuture<Void> work1Future = new CompletableFuture<>();
		CompletableFuture<Void> refreshFuture = new CompletableFuture<>();

		MyException exception = new MyException();
		IllegalStateException handlerException = new IllegalStateException();

		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		expect( work1.execute( contextMock ) ).andReturn( (CompletableFuture) work1Future );
		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, errorHandlerSupplierMock );
		builder.init( previousFuture );
		builder.addNonBulkExecution( work1 );
		builder.addNonBulkExecution( work2 );
		CompletableFuture<Void> sequenceFuture = builder.build();
		previousFuture.complete( null );
		assertThat( sequenceFuture ).isPending();

		resetAll();
		errorHandlerMock.markAsFailed( work1, exception );
		errorHandlerMock.markAsSkipped( work2 );
		expect( contextMock.executePendingRefreshes() ).andReturn( refreshFuture );
		replayAll();
		work1Future.completeExceptionally( exception );
		verifyAll();
		assertThat( sequenceFuture ).isPending();

		resetAll();
		errorHandlerMock.handle();
		expectLastCall().andThrow( handlerException );
		replayAll();
		refreshFuture.complete( null );
		verifyAll();
		// Errors MUST be propagated if they originated from the handler (critical failure)
		assertThat( sequenceFuture ).isFailed( handlerException );
	}

	/**
	 * Test that, when a sequence follows another one,
	 * but the first sequence is still executing when we start building the second one,
	 * everything works fine.
	 * <p>
	 * We used to have problems related to instance variables we referred to from lambdas:
	 * as these variables were reset when we started building the second sequence,
	 * the execution of the first sequence was relying on the wrong data,
	 * and in the worst case could even deadlock.
	 */
	@Test
	public void intertwinedSequenceExecution() {
		BulkableElasticsearchWork<Object> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<Object> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<Object> work3 = bulkableWork( 3 );

		Object work1Result = new Object();
		Object work2Result = new Object();
		Object work3Result = new Object();

		// Futures returned by mocks: we will complete them
		CompletableFuture<?> sequence1PreviousFuture = new CompletableFuture<>();
		CompletableFuture<?> sequence2PreviousFuture = new CompletableFuture<>();
		CompletableFuture<Object> work1Future = new CompletableFuture<>();
		CompletableFuture<Object> work2Future = new CompletableFuture<>();
		CompletableFuture<Object> work3Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence1RefreshFuture = new CompletableFuture<>();
		CompletableFuture<Void> sequence2RefreshFuture = new CompletableFuture<>();

		// Futures returned by the sequence builder: we will test them
		CompletableFuture<Object> work1FutureFromSequenceBuilder;
		CompletableFuture<Object> work2FutureFromSequenceBuilder;
		CompletableFuture<Object> work3FutureFromSequenceBuilder;

		replayAll();
		ElasticsearchWorkSequenceBuilder builder = new ElasticsearchDefaultWorkSequenceBuilder(
				contextSupplierMock, errorHandlerSupplierMock );
		verifyAll();

		// Build and start the first sequence and simulate a long-running first work
		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		expect( work1.execute( contextMock ) ).andReturn( (CompletableFuture) work1Future );
		replayAll();
		builder.init( sequence1PreviousFuture );
		work1FutureFromSequenceBuilder = builder.addNonBulkExecution( work1 );
		work2FutureFromSequenceBuilder = builder.addNonBulkExecution( work2 );
		CompletableFuture<Void> sequence1Future = builder.build();
		sequence1PreviousFuture.complete( null );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( sequence1Future ).isPending();

		// Meanwhile, build and start the second sequence
		resetAll();
		expect( contextSupplierMock.get() ).andReturn( contextMock );
		expect( errorHandlerSupplierMock.get() ).andReturn( errorHandlerMock );
		expect( work3.execute( contextMock ) ).andReturn( (CompletableFuture) work3Future );
		replayAll();
		builder.init( sequence2PreviousFuture );
		work3FutureFromSequenceBuilder = builder.addNonBulkExecution( work3 );
		CompletableFuture<Void> sequence2Future = builder.build();
		sequence2PreviousFuture.complete( null );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending();
		assertThat( work2FutureFromSequenceBuilder ).isPending();
		assertThat( sequence1Future ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequence2Future ).isPending();

		// Then simulate the end of the first and second works
		resetAll();
		expect( work2.execute( contextMock ) ).andReturn( (CompletableFuture) work2Future );
		expect( contextMock.executePendingRefreshes() ).andReturn( sequence1RefreshFuture );
		replayAll();
		work1Future.complete( work1Result );
		work2Future.complete( work2Result );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for refresh
		assertThat( work2FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for refresh
		assertThat( sequence1Future ).isPending();
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequence2Future ).isPending();

		// Then simulate the end of the refresh for the first sequence
		resetAll();
		replayAll();
		sequence1RefreshFuture.complete( null );
		verifyAll();
		assertThat( work1FutureFromSequenceBuilder ).isSuccessful( work1Result );
		// This used to fail because we didn't refer to the refresh future from the right sequence
		assertThat( work2FutureFromSequenceBuilder ).isSuccessful( work2Result );
		assertThat( sequence1Future ).isSuccessful( (Void) null );
		assertThat( work3FutureFromSequenceBuilder ).isPending();
		assertThat( sequence2Future ).isPending();

		// Then simulate the end of the third work
		resetAll();
		expect( contextMock.executePendingRefreshes() ).andReturn( sequence2RefreshFuture );
		replayAll();
		work3Future.complete( null );
		verifyAll();
		assertThat( work3FutureFromSequenceBuilder ).isPending(); // Still pending, waiting for refresh
		assertThat( sequence2Future ).isPending();

		// Then simulate the end of the refresh for the second sequence
		resetAll();
		replayAll();
		sequence2RefreshFuture.complete( null );
		verifyAll();
		assertThat( work3FutureFromSequenceBuilder ).isSuccessful( work3Result );
		assertThat( sequence2Future ).isSuccessful( (Void) null );
	}

	private <T> ElasticsearchWork<T> work(int index) {
		ElasticsearchWork<T> mock = createStrictMock( "work" + index, ElasticsearchWork.class );
		return mock;
	}

	private <T> BulkableElasticsearchWork<T> bulkableWork(int index) {
		BulkableElasticsearchWork<T> mock = createStrictMock( "bulkableWork" + index, BulkableElasticsearchWork.class );
		return mock;
	}

	private static class MyException extends Exception {
	}

	private static class MyRuntimeException extends RuntimeException {
	}
}
