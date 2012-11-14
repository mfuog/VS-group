<<<<<<< HEAD
package de.htw.ds.sync;
=======
package de.htw.ds.sync.myrtha;
>>>>>>> e8571f9152b9780ec464505e18d7fb7142f2743a

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import de.htw.ds.TypeMetadata;


/**
 * <p>Demonstrates thread processing and thread resynchronization using a future.
 * Note that futures do not need any references to communicate a result back to
 * the originator thread. Also note the ability to return any kind of exception
 * to be originator thread by catching ExecutionExceptions.</p>
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class ResyncThreadByFuture {
	private static final int SECOND = 1000;
	private static final Random RANDOMIZER = new Random();

	/**
	 * Application entry point. The arguments must be a child thread count
	 * and the number of seconds the child threads should take for processing.
	 * @param args the arguments
	 * @throws IndexOutOfBoundsException if no thread count is passed
	 * @throws NumberFormatException if the given thread count is not an integral number
	 * @throws IllegalArgumentException if the given thread count is negative, or if
	 *    the given thread period is strictly negative
	 * @throws InterruptedException if a thread is interrupted while blocking
	 */
	public static void main(final String[] args) throws InterruptedException {
		final int threadCount = Integer.parseInt(args[0]);
		final int threadPeriod = Integer.parseInt(args[1]);
		resync(threadCount, threadPeriod);
	}


	/**
	 * Starts threadCount child threads and resynchronizes them, displaying the
	 * time it took for the longest running child to end.
	 * @throws IllegalArgumentException if the given thread count is negative, or if
	 *    the given thread period is strictly negative
	 * @throws InterruptedException if a child thread is interrupted while blocking
	 */
	private static void resync(final int threadCount, final int threadPeriod) throws InterruptedException {
		if (threadCount <= 0) throw new IllegalArgumentException();
		final long timestamp = System.currentTimeMillis();

		System.out.format("Starting %s Java thread(s)...\n", threadCount);
		final Set<RunnableFuture<Integer>> futures = new HashSet<>();

		for (int index = 0; index < threadCount; ++index) {
			final Callable<Integer> callable = new Callable<Integer>() {
				public Integer call() throws InterruptedException {
					final int sleepMillies = RANDOMIZER.nextInt(threadPeriod * SECOND);
					Thread.sleep(sleepMillies);
					return sleepMillies;
				}
			};

			final RunnableFuture<Integer> future = new FutureTask<>(callable);
			futures.add(future);
			new Thread(future).start();
		}

		System.out.println("Resynchronising Java thread(s)... ");
		for (final RunnableFuture<Integer> future : futures) {
			try {
				while (true) {
					try { future.get(); break; } catch (final InterruptedException exception) {}
				}
			} catch (final ExecutionException exception) {
				if (exception.getCause() instanceof Error) throw (Error) exception.getCause();
				if (exception.getCause() instanceof RuntimeException) throw (RuntimeException) exception.getCause();
				if (exception.getCause() instanceof InterruptedException) throw (InterruptedException) exception.getCause();
				throw new AssertionError();
			}
		}
		System.out.format("Java thread(s) resynchronized after %sms.\n", System.currentTimeMillis() - timestamp);
	}
}