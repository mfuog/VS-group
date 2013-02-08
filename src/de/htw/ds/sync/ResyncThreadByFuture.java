package de.htw.ds.sync;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import de.sb.javase.TypeMetadata;


/**
 * <p>Demonstrates thread processing and thread resynchronization using a future, and
 * a situationally correct way of handling {@link InterruptedException}. Note that there
 * is no "golden" way to handle thread interruption which is always correct, i.e. the
 * handling must always take circumstances into account! However, an interruption MUST
 * always be handled immediately, in one or more of the following ways:</p><ul>
 * <li>ignoring or just logging it:<ul>
 *     <li>if the interrupted thread or it's process will end soon anyways</li>
 *     <li>OR if the interrupted thread is a service/monitor thread that other threads
 *         depend on, and which must therefore not be stopped before it's natural
 *         end</li></ul></li>
 * <li>re-interruption of the interrupted thread (or it's parent thread) at a later
 *     time using {@link Thread#interrupt()}:<ul>
 *     <li>if the interruption doesn't reappear immediately (interruptible loops)</li>
 *     <li>AND if interruptible operations follow later (otherwise the thread will not
 *         be interrupted again before it's natural end)</li></ul></li>
 * <li>terminating the interrupted process using {@link System#exit(int)}:<ul>
 *     <li>if process termination is safe with regard to other processes</li>
 *     <li>AND such behavior would be acceptable to process users</li></ul></li>
 * <li>terminating the interrupted thread by throwing {@link ThreadDeath}:<ul>
 *     <li>if it is safe that a method caller may catch <tt>ThreadDeath</tt> without
 *         rethrowing it (not recommended but can happen), thereby preventing thread
 *         termination</li>
 *     <li>AND if thread termination is safe with regard to open resources, which the use of
 *         {@link AutoCloseable#close()} in finally-blocks or try-with-resource blocks should
 *         always ensure anyways, because other errors like {@link OutOfMemoryError} can
 *         occur at any time</li>
 *     <li>AND if thread termination is safe with regard to other threads, i.e. other threads
 *         do not depend on the interrupted thread running until it's natural end</li>
 *         </ul></li>
 * <li>simply declaring {@link InterruptedException} in the methods throws clause:<ul>
 *     <li>ONLY allowed if the method provides system level functionality, or is the
 *         <tt>main()</tt> method of a process whose other threads don't depend on the
 *         main thread to end naturally!</li>
 *     <li>REASON: Higher call stacks have less and less chance to determine a correct way
 *         of handling the exception, which in effect would cause the exception declaration
 *         to spread like cancer throughout an API without any benefit! If you want to
 *         terminate the thread or process, throw {@link ThreadDeath} or use
 *         {@link System#exit(int)} instead. The main reason <tt>InterruptedException</tt>
 *         was introduced into Java was to force the immediate (!) caller of a blocking method
 *         to CHOOSE one of the possibilities in handling the interruption described above.
 *         Before that, system level methods threw <tt>ThreadDeath</tt>, which did not force
 *         programmers to THINK first about the implications of premature thread termination,
 *         and which subsequently caused a lot of problems when circumstances where not safe
 *         for this (see above).</li></ul></li></ul>
 * <p>Note that futures do not need any references to communicate a result back to
 * the originator thread. Also note the ability to return any kind of exception
 * to be originator thread by catching ExecutionExceptions.</p>
 */
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class ResyncThreadByFuture {
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
		final Set<RunnableFuture<Integer>> futures = new HashSet<>();

		for (int index = 0; index < childThreadCount; ++index) {
			final Callable<Integer> callable = new Callable<Integer>() {
				public Integer call() throws InterruptedException {
					final int maximumDelay = (int) TimeUnit.SECONDS.toMillis(maximumChildTreadDelay);
					final int delay = RANDOMIZER.nextInt(maximumDelay);
					Thread.sleep(delay);
					return delay;
				}
			};

			final RunnableFuture<Integer> future = new FutureTask<>(callable);
			futures.add(future);
			new Thread(future).start();
		}

		System.out.println("Resynchronising Java thread(s)... ");
		for (final RunnableFuture<Integer> future : futures) {
			try {
				// CHOOSING to totally ignore thread interruption (see class comment!)
				// Note that interrupting a resynchronization is always a delicate affair, as the remainder
				// of the interrupted thread implicitly depends on the resynchronization to have taken place!
				while (true) {
					try { future.get(); break; } catch (final InterruptedException interrupt) {}
				}
			} catch (final ExecutionException exception) {
				final Throwable cause = exception.getCause();
				if (cause instanceof Error) throw (Error) cause;
				if (cause instanceof RuntimeException) throw (RuntimeException) cause;
				// CHOOSING to interrupt parent thread after child interruption and resynchronization (see class comment!)
				if (cause instanceof InterruptedException) throw new ThreadDeath();
				throw new AssertionError();
			}
		}
		System.out.format("Java thread(s) resynchronized after %sms.\n", System.currentTimeMillis() - timestamp);
	}
}