/**
 * Copyright (C) 2014 Benoit Lacelle (benoit.lacelle@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package blasd.apex.shared.thread;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class TestApexExecutorsHelper {
	@Test
	public void testNewExecutorService() {
		ApexExecutorsHelper.newSingleThreadExecutor("test", 3, ApexExecutorsHelper.TIMEOUT_POLICY_1_HOUR).shutdown();
	}

	@Test
	public void testVeryLargeNumberOfThreads() {
		ApexExecutorsHelper.newShrinkableFixedThreadPool(Integer.MAX_VALUE, "test").shutdown();
	}

	@Test
	public void testNewScheduledExecutorService() {
		ApexExecutorsHelper.newShrinkableScheduledThreadPool("test", ApexExecutorsHelper.TIMEOUT_POLICY_1_HOUR)
				.shutdown();
	}

	@Test
	public void testRejectOfferWithTimeoutPolicy() {
		// TODO
	}

	// We fail adding many tasks with a listener on a bounded queue, even with timeout policy
	@Test(expected = RuntimeException.class)
	public void testBoundedQueueManytasks() {
		// Small thread, small queue and wait policy(abort policy would reject right away as we submit many tasks)
		ListeningExecutorService es = ApexExecutorsHelper.newShrinkableFixedThreadPool(2,
				"test",
				2,
				ApexExecutorsHelper.makeRejectedExecutionHandler(1, TimeUnit.SECONDS));

		for (int i = 0; i < 1000; i++) {
			ListenableFuture<Object> future = es.submit(new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					Thread.sleep(1);
					return new Object();
				}
			});

			future.addListener(new Runnable() {

				@Override
				public void run() {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}, es);
		}

		MoreExecutors.shutdownAndAwaitTermination(es, 1, TimeUnit.MINUTES);
	}

	@Test
	public void testManyTasksBigPool() throws InterruptedException {
		ListeningExecutorService es = ApexExecutorsHelper.newShrinkableFixedThreadPool("Test",
				1000,
				ApexExecutorsHelper.makeRejectedExecutionHandler(1, TimeUnit.MINUTES));

		List<Supplier<Object>> runnables = new ArrayList<>();
		for (int i = 0; i < 10 * 1000; i++) {
			runnables.add(() -> {
				int coucou = 2;

				coucou *= coucou;

				return null;
			});
		}

		ApexExecutorsHelper.invokeAll(runnables, es);

		es.shutdown();
		es.awaitTermination(1, TimeUnit.MINUTES);
	}

	@Test
	public void testManyTasksSmallPool() throws InterruptedException {
		ListeningExecutorService es = ApexExecutorsHelper.newShrinkableFixedThreadPool("Test");

		List<Supplier<Object>> runnables = new ArrayList<>();
		for (int i = 0; i < 10 * 1000; i++) {
			runnables.add(() -> {
				int coucou = 2;

				coucou *= coucou;

				return null;
			});
		}

		ApexExecutorsHelper.invokeAll(runnables, es);

		es.shutdown();
		es.awaitTermination(1, TimeUnit.MINUTES);
	}

	@Test
	public void testExecuteAllRunnables() throws InterruptedException {
		ListeningExecutorService es = ApexExecutorsHelper.newShrinkableFixedThreadPool("Test");

		List<Runnable> runnables = new ArrayList<>();
		for (int i = 0; i < 10 * 1000; i++) {
			runnables.add(new Runnable() {

				@Override
				public void run() {
					int coucou = 2;

					coucou *= coucou;
				}
			});
		}

		ApexExecutorsHelper.executeAllRunnable(runnables, es);
	}

	// Test not invokeAll methods, which does NOT stream tasks
	@Test
	public void testNotStream() throws InterruptedException {
		final int maxSize =
				3 * ApexExecutorsHelper.DEFAULT_ACTIVE_TASKS * ApexExecutorsHelper.DEFAULT_PARTITION_TASK_SIZE;

		{
			final Set<Integer> materialized = Sets.newConcurrentHashSet();
			Iterator<Integer> iterable = new Iterator<Integer>() {
				AtomicInteger currentIndex = new AtomicInteger();

				@Override
				public boolean hasNext() {
					return currentIndex.get() < maxSize;
				}

				@Override
				public Integer next() {
					int output = currentIndex.getAndIncrement();

					materialized.add(output);

					return output;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};

			// Split in block of size
			// ApexExecutorsHelper.DEFAULT_SPLIT_TASK_SIZE
			Iterator<? extends Runnable> runnables = ApexExecutorsHelper.partitions(iterable, param -> {
				// Not in stream: all have been materialized very soon
				Assert.assertEquals(maxSize, materialized.size());
			});

			// Preparing Runnable in a Collection DO materialize the whole
			// initial iterator
			List<Runnable> runnablesAsCollection = Lists.newArrayList(runnables);
			Assert.assertEquals(maxSize, materialized.size());

			// Of course, executing is far too late
			List<? extends ListenableFuture<?>> futures = ApexExecutorsHelper.invokeAllRunnable(runnablesAsCollection,
					MoreExecutors.newDirectExecutorService(),
					1,
					TimeUnit.MINUTES);
			Assert.assertEquals(maxSize, materialized.size());
			Assert.assertEquals(maxSize / ApexExecutorsHelper.DEFAULT_PARTITION_TASK_SIZE, futures.size());
		}
	}

}
