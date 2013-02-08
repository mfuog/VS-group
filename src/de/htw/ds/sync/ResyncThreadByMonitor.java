package de.htw.ds.sync;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import de.sb.javase.Reference;
import de.sb.javase.TypeMetadata;


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
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class ResyncThreadByMonitor {
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
		final Object monitor = new Object();
		final Reference<Throwable> exceptionReference = new Reference<>();

		synchronized(monitor) {
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
							synchronized(monitor) { monitor.notify(); }
						}
					}
				};

				new Thread(runnable).start();
			}

			System.out.println("Resynchronising Java thread(s)... ");
			// Note that resynchronization using Java monitors (or other basic lock/unlock
			// mechanisms) is intrinsically unreliable due to a possible race condition, even
			// if done correctly: The second of two fast occurring monitor notifications may
			// be lost whenever it's thread is faster in acquiring the monitor's sync-lock
			// than this thread is in re-acquiring it after unblocking due to the first
			// notification!
			//    The problem is that if the second notification thread is already waiting
			// for the monitor's sync-lock when this thread is notified, they both have a 50%
			// chance to obtain said sync-lock first. If the winner is the second notification
			// thread, then it's (second) notification will reach the monitor before this
			// thread had a chance to call wait() again, which in turn will cause this
			// resynchronization attempt to deadlock!
			//    This is the prime reason why thread resynchronization using Thread.join(),
			// future.get(), or indebtedSemaphore.acquire() is much preferred over simple locks!
			for (int index = 0; index < childThreadCount; ++index) { 
				try {
					monitor.wait();
				} catch (final InterruptedException interrupt) {
					// CHOOSING to abort resynchronization, and as a natural consequence this thread. (see class comment!)
					// Note that interrupting a resynchronization is always a delicate affair, as the remainder
					// of the interrupted thread implicitly depends on the resynchronization to have taken place!
					throw new ThreadDeath();
				}
			}
		}

		final Throwable exception = exceptionReference.get();
		if (exception instanceof Error) throw (Error) exception;
		if (exception instanceof RuntimeException) throw (RuntimeException) exception;
		// CHOOSING to terminate all threads if any child thread is interrupted (see class comment!)
		if (exception instanceof InterruptedException) System.exit(-1);
		assert exception == null;

		System.out.format("Java thread(s) resynchronized after %sms.\n", System.currentTimeMillis() - timestamp);
	}
}