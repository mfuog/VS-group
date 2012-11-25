package de.htw.ds.sort;

import de.htw.ds.TypeMetadata;


/**
 * <p>Interface describing stream sorters that can sort tremendous amounts of objects
 * without necessarily loading them all into storage. The elements are sorted into
 * ascending order, depending on their natural ordering.</p>
 * <p>Note that stream sorters are state based objects! In <tt>WRITE</tt> state only
 * write(), sort(), or reset() messages are permitted. The sort() message switches
 * a sorter into <tt>READ</tt> state. After this, only read() or reset() messages are
 * permitted until the final sorted element has been read, which switches the sorter
 * back into <tt>WRITE</tt> state.</p>
 * @param E the element type to be sorted in naturally ascending order
 */
@TypeMetadata(copyright="2010-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public interface StreamSorter<E extends Comparable<E>> {
	public static enum State { WRITE, READ }

	/**
	 * Clears the receiver's sorting storage and resets it into <tt>WRITE</tt> state,
	 * regardless of the state it's currently in.
	 */
	void reset();


	/**
	 * Writes the given element to it's sorting storage.
	 * @param element the element to be stored
	 * @throws NullPointerException if the given element is <tt>null</tt>
	 * @throws IllegalStateException if the sorter is not in <tt>WRITE</tt> state,
	 *    or if there is a problem with the underlying data
	 */
	void write(E element);//E ist ein Formparameter, wir werden hier typischerweise String verwenden


	/**
	 * Sorts the elements in sorting storage, and switches the receiver into
	 * <tt>READ</tt> state if there is any element to be read after sorting.
	 * @throws IllegalStateException if the sorter is not in <tt>WRITE</tt> state,
	 *    or if there is a problem with the underlying data
	 */
	void sort();


	/**
	 * Returns the next element from sorted storage, and switches the receiver into
	 * <tt>WRITE</tt> state when the last element is returned.
	 * @return the next element in sort order
	 * @throws IllegalStateException if the sorter is not in <tt>READ</tt> state,
	 *    or if there is a problem with the underlying data
	 */
	E read();


	/**
	 * Returns the current state.
	 * @return the state
	 * @throws IllegalStateException if there is a problem with the underlying data
	 */
	public State getState();
}