package de.htw.ds.sync;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import de.htw.ds.TypeMetadata;


/**
 * <i>polling = zyklisches Abarbeiten!</i>
 * 
 * <p>Demonstrates thread processing and thread re-synchronization using a runnable
 * in conjunction with polling. Note the need to take care of InterruptedExceptions.
 * Also note the need of a short sleep phase during polling, and it's implications
 * for latency!</p>
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class ResyncThreadByPolling {
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
		
		//eine Thread-Collection erstellen
		final Set<Thread> threads = new HashSet<>();
		final Reference<Throwable> exceptionReference = new Reference<>();

		for (int index = 0; index < threadCount; ++index) {
			final Runnable runnable = new Runnable() {
				public void run() {
					try {
						final int sleepMillies = RANDOMIZER.nextInt(threadPeriod * SECOND);
						Thread.sleep(sleepMillies);
					} catch (final Throwable exception) {
						exceptionReference.put(exception);
					}
				}
			};

			final Thread thread = new Thread(runnable);
			threads.add(thread);	//Thread zur Thread-Sammlung hinzufügen
			thread.start();
		}

		System.out.println("Resynchronising Java thread(s)... ");
		
		/*
		* Solange Thread-Sammlung nicht leer ist, abwechselnd:
		*  -Main-Thread wartet eine Milisec
		*  -alle Threads durchgehen und diejenigen aus der Sammlung schmeißen, die inzwischen beendet sind
		*/
		while (!threads.isEmpty()) {
			// sleep a millisecond to prevent CPU from running at 100% needlessly!
			try { Thread.sleep(1); } catch (final InterruptedException exception) {}

			final Iterator<Thread> iterator = threads.iterator();
			while (iterator.hasNext()) {
				final Thread thread = iterator.next();
				if (!thread.isAlive()) iterator.remove();	//Thread schon beendet?
			}
		}
		
		//---?
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