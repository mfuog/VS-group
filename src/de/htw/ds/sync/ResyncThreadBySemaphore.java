package de.htw.ds.sync;

import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import de.sb.javase.Reference;
import de.sb.javase.TypeMetadata;


/**
 * <p>Demonstrates thread processing and thread re-synchronization using a runnable
 * in conjunction with semaphore signaling. </p>
 * <p>Especially note that semaphores do not require extra synchronization when compared to
 * monitors, as ticket release may happen before the main thread tries to acquire a ticket,
 * without provoking deadlocks!</p>
 */
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class ResyncThreadBySemaphore {
	private static final Random RANDOMIZER = new Random();


	/**
	 * Application entry point. The arguments must be a child thread count,
	 * and the maximum number of seconds the child threads should take for
	 * processing.
	 * @param args the arguments
	 * @throws IndexOutOfBoundsException if less than two arguments are passed
	 * @throws NumberFormatException if any of the given arguments is not an
	 *     integral number
	 * @throws IllegalArgumentException if any of the arguments is negative
	 */
	public static void main(final String[] args) {
		final int childThreadCount = Integer.parseInt(args[0]);
		final int maximumChildTreadDelay = Integer.parseInt(args[1]);
		resync(childThreadCount, maximumChildTreadDelay);
	}


	/**
	 * Starts child threads and resynchronizes them, displaying the time it took
	 * for the longest running child to end.
	 * @param childThreadCount the number of child threads
	 * @param maximumChildTreadDelay the maximum delay within each child thread
	 *    before completion, in seconds
	 * @throws IllegalArgumentException if any of the arguments is negative
	 */
	private static void resync(final int childThreadCount, final int maximumChildTreadDelay) {
		if (childThreadCount < 0 | maximumChildTreadDelay < 0) throw new IllegalArgumentException();
		final long timestamp = System.currentTimeMillis();

		System.out.format("Starting %s Java thread(s)...\n", childThreadCount);
		final Semaphore indebtedSemaphore = new Semaphore(1 - childThreadCount);
		final Reference<Throwable> exceptionReference = new Reference<>();

		for (int index = 0; index < childThreadCount; ++index) {
			final Runnable runnable = new Runnable() {
				public void run() {
					try {
						final int maximumDelay = (int) TimeUnit.SECONDS.toMillis(maximumChildTreadDelay);
						final int delay = RANDOMIZER.nextInt(maximumDelay);
						Thread.sleep(delay);
					} catch (final Throwable exception) {
						exceptionReference.put(exception);
					} finally {
						indebtedSemaphore.release();
					}
				}
			};

			new Thread(runnable).start();
		}

		System.out.println("Resynchronising Java thread(s)... ");
		// CHOOSING to (implicitly) re-interrupt interrupted thread AFTER resynchronization (see class comment!)
		// Note that interrupting a resynchronization is always a delicate affair, as the remainder
		// of the interrupted thread implicitly depends on the resynchronization to have taken place!
		indebtedSemaphore.acquireUninterruptibly();

		final Throwable exception = exceptionReference.get();
		if (exception instanceof Error) throw (Error) exception;
		if (exception instanceof RuntimeException) throw (RuntimeException) exception;
		if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
		assert exception == null;

		System.out.format("Java thread(s) resynchronized after %sms.\n", System.currentTimeMillis() - timestamp);
	}
}