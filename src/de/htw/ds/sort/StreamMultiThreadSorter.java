package de.htw.ds.sort;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import de.sb.javase.TypeMetadata;


/**
 * <p>Multi threaded merge sorter implementation that distributes elements evenly
 * over two child sorters, sorts them separately using two separate threads,
 * and then merges the two sorted children's elements during read requests.
 * Note that this implementation is able to scale its workload over two processor
 * cores, and even more if such sorters are stacked. However, all elements are
 * still stored within the RAM of a single process.</p>
 * @param E the element type to be sorted in naturally ascending order
 */
@TypeMetadata(copyright="2010-2013 Sascha Baumeister, all rights reserved", version="0.2.1", authors="Sascha Baumeister")
public final class StreamMultiThreadSorter<E extends Comparable<E>> implements StreamSorter<E> {
	private static enum InternalState { WRITE_LEFT, WRITE_RIGHT, READ }

	private final StreamSorter<E> leftChild, rightChild;
	private E leftCache, rightCache;
	private InternalState internalState;


	/**
	 * Creates a new instance that is based on two child sorters.
	 * @param leftChild the left child
	 * @param rightChild the right child
	 * @throws NullPointerException if one of the given children is <tt>null</tt>
	 */
	public StreamMultiThreadSorter(final StreamSorter<E> leftChild, final StreamSorter<E> rightChild) {
		if (leftChild == null || rightChild == null) throw new NullPointerException();

		this.leftChild = leftChild;
		this.rightChild = rightChild;
		this.internalState = InternalState.WRITE_LEFT;
	}


	/**
	 * {@inheritDoc}
	 */
	public synchronized void reset() {
		this.leftChild.reset();
		this.rightChild.reset();
		this.leftCache = null;
		this.rightCache = null;
		this.internalState = InternalState.WRITE_LEFT;
	}


	/**
	 * {@inheritDoc}
	 * @throws NullPointerException if the given element is <tt>null</tt>
	 * @throws IllegalStateException if the sorter is not in <tt>WRITE</tt> state,
	 *    or if there is a problem with the underlying data
	 */
	public synchronized void write(final E element) {
		switch (this.internalState) {
			case WRITE_LEFT: {
				this.leftChild.write(element);
				this.internalState = InternalState.WRITE_RIGHT;
				break;
			}
			case WRITE_RIGHT: {
				this.rightChild.write(element);
				this.internalState = InternalState.WRITE_LEFT;
				break;
			}
			default: {
				throw new IllegalStateException();
			}
		}
	}


	/**
	 * {@inheritDoc}
	 * @throws IllegalStateException if the sorter is not in <tt>WRITE</tt>
	 *    state, or if there is a problem with the underlying data
	 */
	public synchronized void sort() {
		if (this.internalState == InternalState.READ) throw new IllegalStateException();

		final RunnableFuture<E> leftFuture = new FutureTask<E>(new SortProcessor<E>(this.leftChild));
		final RunnableFuture<E> rightFuture = new FutureTask<E>(new SortProcessor<E>(this.rightChild));
		new Thread(leftFuture, "left-child").start();
		new Thread(rightFuture, "right-child").start();

		try {
			this.leftCache = leftFuture.get();
			this.rightCache = rightFuture.get();
		} catch (final InterruptedException interrupt) {
			throw new ThreadDeath();
		} catch (final ExecutionException exception) {
			final Throwable cause = exception.getCause();
			if (cause instanceof Error) throw (Error) cause;
			if (cause instanceof RuntimeException) throw (RuntimeException) cause;
			throw new AssertionError();
		}

		if (this.leftCache != null | this.rightCache != null) {
			this.internalState = InternalState.READ;
		}
	}


	/**
	 * {@inheritDoc}
	 * @throws IllegalStateException if the sorter is not in <tt>READ</tt> state,
	 *    or if there is a problem with the underlying data
	 */
	public synchronized E read() {
		if (this.internalState != InternalState.READ) throw new IllegalStateException();

		final E result;
		if (this.leftCache == null) {
			result = this.nextRightElement();
		} else if (this.rightCache == null) {
			result = this.nextLeftElement();
		} else if (this.leftCache.compareTo(this.rightCache) >= 0) {
			result = this.nextRightElement();
		} else {
			result = this.nextLeftElement();
		}

		if (this.leftCache == null & this.rightCache == null) {
			this.internalState = InternalState.WRITE_LEFT;
		}

		return result;
	}


	/**
	 * {@inheritDoc}
	 */
	public State getState() {
		return this.internalState == InternalState.READ ? State.READ : State.WRITE;
	}


	/**
	 * Returns the next element of the left child, and replenishes it's cache.
	 * @return the left child's next element, or <tt>null</tt>
	 */
	private E nextLeftElement() {
		final E result = this.leftCache;
		try {
			this.leftCache = this.leftChild.read();
		} catch (final IllegalStateException exception) {
			this.leftCache = null;
		}
		return result;
	}


	/**
	 * Returns the next element of the right child, and replenishes it's cache.
	 * @return the right child's next element, or <tt>null</tt>
	 */
	private E nextRightElement() {
		final E result = this.rightCache;
		try {
			this.rightCache = this.rightChild.read();
		} catch (final IllegalStateException exception) {
			this.rightCache = null;
		}
		return result;
	}


	/**
	 * Inner callable class that allows multi-threaded sorting.
	 * @param <E> the element type to be sorted in naturally ascending order
	 */
	private static class SortProcessor<E extends Comparable<E>> implements Callable<E> {
		private final StreamSorter<E> streamSorter;

		public SortProcessor(final StreamSorter<E> streamSorter) {
			super();
			if (streamSorter == null) throw new NullPointerException();

			this.streamSorter = streamSorter;
		}

		public E call() {
			this.streamSorter.sort();

			try{
				return this.streamSorter.read();
			} catch (final IllegalStateException exception) {
				return null;
			}
		}
	}
}