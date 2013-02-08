package de.htw.ds.sync;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import de.sb.javase.Reference;
import de.sb.javase.TypeMetadata;


/**
 * <p>Demonstrates thread processing and thread re-synchronization using a runnable
 * in conjunction with polling. Note the need to take care of InterruptedExceptions.
 * Also note the need of a short sleep phase during polling, and it's implications
 * for latency!</p>
 */
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class ResyncThreadByPolling {
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
		final Set<Thread> threads = new HashSet<>();
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
					}
				}
			};

			final Thread thread = new Thread(runnable);
			threads.add(thread);
			thread.start();
		}

		System.out.println("Resynchronising Java thread(s)... ");
		while (!threads.isEmpty()) {
			final Iterator<Thread> iterator = threads.iterator();
			while (iterator.hasNext()) {
				if (!iterator.next().isAlive()) iterator.remove();
			}

			// MUST sleep at least a bit to prevent CPU from running at 100% while
			// polling. Even with a short 1ms nap, the thread has a good chance to
			// check the child thread status with pretty much every time slice
			// it get's from the systems task scheduler, while still not starving
			// the system's other tasks for CPU time, apart from needlessly heating
			// up the CPU!
			try {
				Thread.sleep(1);
			} catch (final InterruptedException interrupt) {
				// CHOSING to terminate thread if interrupted (see class comment!)
				// Note that interrupting a resynchronization is always a delicate affair, as the remainder
				// of the interrupted thread implicitly depends on the resynchronization to have taken place!
				throw new ThreadDeath();
			}

		}

		final Throwable exception = exceptionReference.get();
		if (exception instanceof Error) throw (Error) exception;
		if (exception instanceof RuntimeException) throw (RuntimeException) exception;
		if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
		assert exception == null;

		System.out.format("Java thread(s) resynchronized after %sms.\n", System.currentTimeMillis() - timestamp);
	}
}