package de.htw.ds.sync.frau_r;

import java.util.Random;
import de.htw.ds.TypeMetadata;


/**
 * <p>Demonstrates thread processing and thread re-synchronization using a runnable in conjunction
 * with (out-dated) monitor signaling. Note the need to take care of InterruptedExceptions.
 * Also note that any Object can be used as a synchronization monitor between threads!</p>
 * <p>Especially note the problem arising from monitors not behaving symmetrically: If
 * one of the new threads notify's it's monitor before thread can send the corresponding
 * <tt>wait()</tt> message, the main thread will deadlock. This can already happen while
 * resynchronizing a single thread, but is almost guaranteed to happen with more than one.</p>
 * <p>Monitors are extremely efficient for synchronization purposes. However, they're pretty
 * cumbersome and error-prone for re-synchronization purposes, and therefore primarily used
 * to implement other types of re-synchronization mechanisms, like semaphores.</p>
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class ResyncThreadByMonitor {
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
		final Object monitor = new Object();
		final Reference<Throwable> exceptionReference = new Reference<>();

		synchronized(monitor) {
			for (int index = 0; index < threadCount; ++index) {
				final Runnable runnable = new Runnable() {
					public void run() {
						try {
							final int sleepMillies = RANDOMIZER.nextInt(threadPeriod * SECOND);
							Thread.sleep(sleepMillies);
						} catch (final Throwable exception) {
							exceptionReference.put(exception);
						} finally {
							synchronized(monitor) { monitor.notify(); }
						}
					}
				};

				new Thread(runnable).start();
			}

			System.out.println("Resynchronising Java thread(s)... ");
			for (int index = 0; index < threadCount; ++index) {
				while (true) {
					// Note that this mechanism is totally unreliable in case multiple
					// threads notify the monitor at the same time, for example due to
					// an exception that occurs rapidly in all child threads!
					try { monitor.wait(); break; } catch (final InterruptedException exception) {}
				}
			}
		}

		final Throwable exception = exceptionReference.get();
		if (exception != null) {
			if (exception instanceof Error) throw (Error) exception;
			if (exception instanceof RuntimeException) throw (RuntimeException) exception;
			if (exception instanceof InterruptedException) throw (InterruptedException) exception;
			throw new AssertionError();
		}
		System.out.format("Java thread(s) resynchronized after %sms.\n", System.currentTimeMillis() - timestamp);
	}
}