package de.htw.ds.sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import de.htw.ds.TypeMetadata;


/**
 * <p>Single threaded sorter implementation that collects elements in a list
 * and sorts them using the underlying merge sort implementation of lists.
 * Note that this implementation implies that such a sorter cannot
 * scale its workload over more than one processor core, and additionally
 * all elements are stored within the RAM of a single process.</p>
 * @param E the element type to be sorted in naturally ascending order
 */
@TypeMetadata(copyright="2010-2012 Sascha Baumeister, all rights reserved", version="0.2.1", authors="Sascha Baumeister")
public final class StreamSingleThreadSorter<E extends Comparable<E>> implements StreamSorter<E> {
	private final List<E> elements;
	private int readIndex;


	/**
	 * Creates a new instance in WRITE state.
	 */
	public StreamSingleThreadSorter() {
		this.elements = new ArrayList<E>();
		this.readIndex = -1;
	}


	/**
	 * {@inheritDoc}
	 */
	public synchronized void reset() {
		this.elements.clear();
		this.readIndex = -1;
	}


	/**
	 * {@inheritDoc}
	 * @throws NullPointerException if the given element is <tt>null</tt>
	 * @throws IllegalStateException if the sorter is not in WRITE state
	 */
	public synchronized void write(final E element) {
		if (this.readIndex != -1) throw new IllegalStateException();
		if (element == null) throw new NullPointerException();
		this.elements.add(element);
	}


	/**
	 * {@inheritDoc}
	 * @throws IllegalStateException if the sorter is not in WRITE state
	 */
	public synchronized void sort() {
		if (this.readIndex != -1) throw new IllegalStateException();
		if (!this.elements.isEmpty()) {
			Collections.sort(this.elements);
			this.readIndex = 0;
		}
	}


	/**
	 * {@inheritDoc}
	 * @throws IllegalStateException if the sorter is not in READ state
	 */
	public synchronized E read() {
		if (this.readIndex == -1) throw new IllegalStateException();

		final E result = this.elements.get(this.readIndex++);
		if (this.readIndex == this.elements.size()) this.readIndex = -1;
		return result;
	}


	/**
	 * {@inheritDoc}
	 */
	public State getState() {
		return this.readIndex == -1 ? State.WRITE : State.READ;
	}
}