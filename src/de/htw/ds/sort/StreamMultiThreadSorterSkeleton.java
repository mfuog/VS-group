package de.htw.ds.sort;

import de.htw.ds.TypeMetadata;


/**
 * <p>Multi threaded merge sorter implementation that distributes elements evenly
 * over two child sorters, sorts them separately using two separate threads,
 * and then merges the two sorted children's elements during read requests.
 * Note that this implementation is able to scale its workload over two processor
 * cores, and even more if such sorters are stacked!</p>
 * @param E the element type to be sorted in naturally ascending order
 */
@TypeMetadata(copyright="2010-2012 Sascha Baumeister, all rights reserved", version="0.2.1", authors="Sascha Baumeister")
public final class StreamMultiThreadSorterSkeleton<E extends Comparable<E>> implements StreamSorter<E> {
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
	public StreamMultiThreadSorterSkeleton(final StreamSorter<E> leftChild, final StreamSorter<E> rightChild) {
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
	 */
	public void sort() {
		if (this.internalState == InternalState.READ) throw new IllegalStateException();

		// TODO: Scale this implementation so that the two sort requests and associated
		// reads are distributed into two separate threads, which should be advantageous on
		// Multi-Core nodes. Code this keeping in mind that the sort method may throw
		// runtime exceptions or errors. Therefore, make sure you can access both the result
		// of the read() operation (for the caches) and a possible exception (for a rethrow)
		// after resynchronisation. Implement the method using either an indebted semaphore
		// and shared references, or using futures.
		this.leftChild.sort();
		try {
			this.leftCache = this.leftChild.read();
		} catch (final IllegalStateException exception) {
			this.leftCache = null;
		}

		this.rightChild.sort();
		try {
			this.rightCache = this.rightChild.read();
		} catch (final IllegalStateException exception) {
			this.rightCache = null;
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
}