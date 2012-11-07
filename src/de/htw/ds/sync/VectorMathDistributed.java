package de.htw.ds.sync;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import de.htw.ds.TypeMetadata;


/**
 * <p>Demonstrator for vector processing arithmetics distributed into multiple threads.
 * Thread re-synchronization is based on signaling, using an indebted semaphore. Note
 * that this implementation is (in principle) capable of using all available processor
 * cores within a given system. Scaling efficiency depends on the operating system
 * type, available RAM bandwidth, processor type (physical cores or virtual cores), number of
 * processor cores, and even processor type!</p>
 * <p>In the standard implementation, child threads are freshly instantiated whenever one is needed,
 * causing overhead for thread construction and destruction. This is usually not an issue with the
 * add() and muxHeapOriented() implementations, as the number of threads involved is relatively low.
 * However, it is most often an issue with the muxRowOriented() implementation, a disadvantage that
 * can be remedied using managed threads alternatively! You can try this by uncommenting the
 * relevant alternative lines.</p>
 * <p>Also note that professional implementations of the vector processing pattern include
 * an additional single threaded implementation that is transparently used whenever the
 * parameter space indicates multi-threading not to be efficient because of excessive costs!
 * This is realized within the mux() method in PERFECT mode.</p>
 * <p>Finally, note that Java 7 introduces an additional class called ForkJoinPool, that can be
 * used alternatively to ThreadPoolExecutor. It additionally offers worker re-synchronization,
 * thereby removing the need to use indebted semaphores. However, the price is that such a
 * ForkJoinPool cannot be used in a static way, because worker re-synchronization is wrecked
 * once the pool is reused concurrently in multiple threads!</p>
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="1.0.0", authors="Sascha Baumeister")
public final class VectorMathDistributed {
	private static final int PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors();
	private static enum MuxDistributionMode { SECTOR, STRIPE, ROW, PERFECT }
	private static final MuxDistributionMode MUX_IMPLEMENTATION = MuxDistributionMode.SECTOR;
	private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(PROCESSOR_COUNT);


	/**
	 * Sums two vectors, distributing the work load into as many new child threads as there
	 * are processor cores within a given system. Note that the added cost of thread
	 * construction and destruction is higher than the gain of distributing the work for
	 * practically any vector size.
	 * @param leftOperand the first operand
	 * @param rightOperand the second operand
	 * @return the resulting vector
	 * @throws NullPointerException if one of the given parameters is null
	 * @throws IllegalArgumentException if the given parameters do not share the same length
	 */
	public static double[] add(final double[] leftOperand, final double[] rightOperand) {
		if (leftOperand.length != rightOperand.length) throw new IllegalArgumentException();
		final double[] result = new double[leftOperand.length];

		final int sectorWidth = leftOperand.length / PROCESSOR_COUNT;
		final int sectorThreshold = leftOperand.length % PROCESSOR_COUNT;
		final Semaphore indebtedSemaphore = new Semaphore(1 - PROCESSOR_COUNT);

		for (int threadIndex = 0; threadIndex < PROCESSOR_COUNT; ++threadIndex) {
			final int startIndex = threadIndex * sectorWidth + (threadIndex < sectorThreshold ? threadIndex : sectorThreshold);
			final int stopIndex  = startIndex  + sectorWidth + (threadIndex < sectorThreshold ? 1 : 0);
			final Runnable runnable = new Runnable() {
				public void run() {
					try {
						for (int index = startIndex; index < stopIndex; ++index) {
							result[index] = leftOperand[index] + rightOperand[index];
						}
					} finally {
						indebtedSemaphore.release();
					}
				}
			};
			// EXECUTOR_SERVICE.execute(runnable);							// uncomment for managed thread alternative!
			new Thread(runnable).start();									// comment for managed thread alternative!
		}

		indebtedSemaphore.acquireUninterruptibly();
		return result;
	}


	/**
	 * Multiplexes two vectors, potentially distributing the work load into as many new child
	 * threads as there are processor cores within a given system. In PERFECT mode, work
	 * distribution is avoided whenever the result would be too small for distribution to be
	 * cost effective for at least a dual-core system.
	 */
	public static double[][] mux(final double[] leftOperand, final double[] rightOperand) {
		switch (MUX_IMPLEMENTATION) {
			case SECTOR: return muxSector(leftOperand, rightOperand);
			case STRIPE: return muxStripe(leftOperand, rightOperand);
			case ROW: return muxRow(leftOperand, rightOperand);
			default: return (PROCESSOR_COUNT > 1) && (leftOperand.length * rightOperand.length > 1000000)
				? muxSector(leftOperand, rightOperand)
				: muxSimple(leftOperand, rightOperand);
		}
	}


	/**
	 * Multiplexes two vectors within a single thread.
	 * @param leftOperand the first operand
	 * @param rightOperand the second operand
	 * @return the resulting matrix
	 * @throws NullPointerException if one of the given parameters is null
	 */
	private static double[][] muxSimple(final double[] leftOperand, final double[] rightOperand) {
		final double[][] result = new double[leftOperand.length][rightOperand.length];
		for (int leftIndex = 0; leftIndex < leftOperand.length; ++leftIndex) {
			for (int rightIndex = 0; rightIndex < rightOperand.length; ++rightIndex) {
				result[leftIndex][rightIndex] = leftOperand[leftIndex] * rightOperand[rightIndex];
			}
		}
		return result;
	}


	/**
	 * Multiplexes two vectors, distributing the work load into as many new child threads as
	 * there are processor cores within a given system. This algorithm uses one thread per
	 * processor core, calculating sectors of result rows within each thread. Note that the
	 * added cost of thread construction and destruction is higher than the gain of distributing
	 * the work for smaller results!
	 * @param leftOperand the first operand
	 * @param rightOperand the second operand
	 * @return the resulting matrix
	 * @throws NullPointerException if one of the given parameters is null
	 */
	private static double[][] muxSector(final double[] leftOperand, final double[] rightOperand) {
		final double[][] result = new double[leftOperand.length][rightOperand.length];

		final int sectorWidth = leftOperand.length / PROCESSOR_COUNT;
		final int sectorThreshold = leftOperand.length % PROCESSOR_COUNT;
		final Semaphore indebtedSemaphore = new Semaphore(1 - PROCESSOR_COUNT);

		for (int threadIndex = 0; threadIndex < PROCESSOR_COUNT; ++threadIndex) {
			final int startIndex = threadIndex * sectorWidth + (threadIndex < sectorThreshold ? threadIndex : sectorThreshold);
			final int stopIndex  = startIndex  + sectorWidth + (threadIndex < sectorThreshold ? 1 : 0);

			final Runnable runnable = new Runnable() {
				public void run() {
					try {
						for (int leftIndex = startIndex; leftIndex < stopIndex; ++leftIndex) {
							for (int rightIndex = 0; rightIndex < rightOperand.length; ++rightIndex) {
								result[leftIndex][rightIndex] = leftOperand[leftIndex] * rightOperand[rightIndex];
							}
						}
					} finally {
						indebtedSemaphore.release();
					}
				}
			};
			// EXECUTOR_SERVICE.execute(runnable);						// uncomment for managed thread alternative!
			new Thread(runnable).start();								// comment for managed thread alternative!
		}

		indebtedSemaphore.acquireUninterruptibly();
		return result;
	}


	/**
	 * Multiplexes two vectors, distributing the work load into as many new child threads as
	 * there are processor cores within a given system. This algorithm uses one thread per
	 * processor core, calculating stripes of rows (for example 0, 4, 8,...) within within
	 * each thread. Note that the added cost of thread construction and destruction is higher
	 * than the gain of distributing the work for smaller results! Also note that striped
	 * calculation of results can cause a large increase of memory cache collisions on some
	 * processor architectures!
	 * @param leftOperand the first operand
	 * @param rightOperand the second operand
	 * @return the resulting matrix
	 * @throws NullPointerException if one of the given parameters is null
	 */
	private static double[][] muxStripe(final double[] leftOperand, final double[] rightOperand) {
		final double[][] result = new double[leftOperand.length][rightOperand.length];

		final Semaphore indebtedSemaphore = new Semaphore(1 - PROCESSOR_COUNT);

		for (int threadIndex = 0; threadIndex < PROCESSOR_COUNT; ++threadIndex) {
			final int startIndex = threadIndex;

			final Runnable runnable = new Runnable() {
				public void run() {
					try {
						for (int leftIndex = startIndex; leftIndex < leftOperand.length; leftIndex += PROCESSOR_COUNT) {
							for (int rightIndex = 0; rightIndex < rightOperand.length; ++rightIndex) {
								result[leftIndex][rightIndex] = leftOperand[leftIndex] * rightOperand[rightIndex];
							}
						}
					} finally {
						indebtedSemaphore.release();
					}
				}
			};
			// EXECUTOR_SERVICE.execute(runnable);						// uncomment for managed thread alternative!
			new Thread(runnable).start();								// comment for managed thread alternative!
		}

		indebtedSemaphore.acquireUninterruptibly();
		return result;
	}


	/**
	 * Multiplexes two vectors, distributing the work load into as many new child threads as
	 * there are matrix rows to be processed. This algorithm uses one thread per result row.
	 * Demonstrates the use of a normal semaphore to prevent more child threads to be started
	 * at the same time than there are processor cores within a given system. Note that the added
	 * cost of thread construction and destruction is higher than the gain of distributing the
	 * work for practically any result size because of the sheer number of threads created.
	 * However, this is NOT the case when using managed threads - which also doesn't require the
	 * additional semaphore! 
	 * @param leftOperand the first operand
	 * @param rightOperand the second operand
	 * @return the resulting matrix
	 * @throws NullPointerException if one of the given parameters is null
	 */
	private static double[][] muxRow(final double[] leftOperand, final double[] rightOperand) {
		final double[][] result = new double[leftOperand.length][rightOperand.length];

		final Semaphore indebtedSemaphore = new Semaphore(1 - leftOperand.length);
		// final Semaphore gateSemaphore = new Semaphore(PROCESSOR_COUNT);		// uncomment for unmanaged thread alternative!

		for (int threadIndex = 0; threadIndex < leftOperand.length; ++threadIndex) {
			final int leftIndex = threadIndex;
			final Runnable runnable = new Runnable() {
				public void run() {
					try {
						for (int rightIndex = 0; rightIndex < rightOperand.length; ++rightIndex) {
							result[leftIndex][rightIndex] = leftOperand[leftIndex] * rightOperand[rightIndex];
						}
					} finally {
						// gateSemaphore.release();						// uncomment for unmanaged thread alternative!
						indebtedSemaphore.release();
					}
				}
			};

			EXECUTOR_SERVICE.execute(runnable);							// comment for managed thread alternative!
			// gateSemaphore.acquireUninterruptibly();					// uncomment for managed thread alternative!
			// new Thread(runnable).start();							// uncomment for managed thread alternative!
		}

		indebtedSemaphore.acquireUninterruptibly();
		return result;
	}


	/**
	 * Runs both vector summation and vector multiplexing for demo purposes.
	 * @param args the argument array
	 */
	public static void main(final String[] args) {
		final int dimension = args.length == 0 ? 10 : Integer.parseInt(args[0]);

		final double[] a = new double[dimension], b = new double[dimension];
		for (int index = 0; index < dimension; ++index) {
			a[index] = index + 1.0;
			b[index] = index + 2.0;
		}
		System.out.format("Computation is performed on %s processor core(s):\n", PROCESSOR_COUNT);

		final long timestamp0 = System.currentTimeMillis();
		final double[] sum = add(a, b);
		final long timestamp1 = System.currentTimeMillis();
		System.out.format("a + b took %sms to compute.\n", timestamp1 - timestamp0);

		final long timestamp2 = System.currentTimeMillis();
		final double[][] mux = mux(a, b);
		final long timestamp3 = System.currentTimeMillis();
		System.out.format("a x b took %sms to compute.\n", timestamp3 - timestamp2);

		if (dimension <= 100) {
			System.out.print("a = ");
			System.out.println(Arrays.toString(a));
			System.out.print("b = ");
			System.out.println(Arrays.toString(b));
			System.out.print("a + b = ");
			System.out.println(Arrays.toString(sum));
			System.out.print("a x b = [");
			for (int index = 0; index < mux.length; ++index) {
				System.out.print(Arrays.toString(mux[index]));
			}
			System.out.println("]");
		}
	}
}