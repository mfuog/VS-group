package de.htw.ds.util;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import de.htw.ds.TypeMetadata;


/**
 * <p>Instances of this class manage elements that are divided into equally sized
 * k-buckets using a pivot indexer, effectively forming a matrix of elements
 * with a fixed number of columns, and a maximum number of rows. Elements are
 * assigned to buckets using an pivot indexer, which itself contains the one
 * element that divides the others into their respective buckets. The pivot
 * element is either always part of the bucket-set, or never. This implies that
 * it cannot be added or removed.</p>
 * <p>Once a bucket reaches it's capacity, it will not accept more elements until
 * space is freed up by removing an element. The idea behind a k-bucket-set is
 * that it provides an easy way to distinguish between "important" and
 * "less important" elements, with the indexer assigning "important" elements into
 * less crowded buckets. Therefore, when adding "important" elements, these have
 * a much better chance not to be spilled (i.e. rejected from being added) than
 * "less important" elements that are assigned to crowded buckets.</p>
 * @param <E> the element type
 * @see #getPivotIndexer()
 * @see #toArray(int)
 */
@TypeMetadata(copyright="2009-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class BucketSet<E> extends AbstractSet<E> implements Cloneable, Serializable {
	private static final long serialVersionUID = -7040303224602680438L;

	/**
	 * Instances of this interface are responsible to calculate the
	 * index of the k-bucket each element is to be placed in.
	 * @param <E> the element type
	 */
	public static interface PivotIndexer<E> extends Serializable {

		/**
		 * Returns the pivot element.
		 * @return the pivot element
		 */
		E getPivotElement();

		/**
		 * Returns the cardinality of the index calculations, i.e. the maximum
		 * index + 1, which is equal to the bucket count in a k-bucket.
		 * @return the cardinality (bucket count)
		 */
		int getCardinality();

		/**
		 * Returns the index calculated for the given object, or -1 if the
		 * object is either null, the pivot element, or cannot be indexed.
		 * @param object an object
		 * @return an index within range [-1, cardinality[
		 */
		int getIndex(Object object);
	}

	private final boolean containsPivotElement;
	private final PivotIndexer<E> pivotIndexer;
	private final E[][] buckets;
	private int size;


	/**
	 * Public constructor.
	 * @param pivotIndexer the indexer assigning elements to k-buckets
	 * @param bucketCapacity the maximum number of elements (k) within each k-bucket
	 * @param containsPivotElement <tt>true</tt> if the bucket-set always contains
	 *    the pivot element, <tt>false</tt> if it never does
	 * @throws NullPointerException if the given indexer is <tt>null</tt>
	 * @throws IllegalArgumentException if the given bucket capacity is negative
	 */
	@SuppressWarnings("unchecked")
	public BucketSet(final PivotIndexer<E> pivotIndexer, final int bucketCapacity, final boolean containsPivotElement) {
		super();
		if (bucketCapacity <= 0) throw new IllegalArgumentException();

		this.containsPivotElement = containsPivotElement;
		this.buckets = (E[][]) new Object[pivotIndexer.getCardinality()][bucketCapacity];
		this.pivotIndexer = pivotIndexer;
		this.size = containsPivotElement ? 1 : 0;
	}


	/**
	 * Private constructor for cloning this final class despite final fields.
	 * @param pivotIndexer the indexer assigning elements to k-buckets
	 * @param buckets the k-buckets
	 * @param size the number of elements contained
	 * @param containsPivotElement <tt>true</tt> if the set always contains the pivot
	 *    element, <tt>false</tt> if it never does
	 * @throws NullPointerException if one of the given arguments is <tt>null</tt>
	 * @throws IllegalArgumentException if the cardinality of the indexer doesn't match the
	 *    number of buckets, or if the given size is strictly negative
	 */
	private BucketSet(final PivotIndexer<E> indexer, final E[][] buckets, final int size, final boolean containsPivotElement) {
		super();
		if (buckets.length != indexer.getCardinality() | size < 0) throw new IllegalArgumentException();

		this.containsPivotElement = containsPivotElement;
		this.buckets = buckets;
		this.pivotIndexer = indexer;
		this.size = size;
	}


	/**
	 * Returns a clone of the receiver.
	 */
	@Override
	public BucketSet<E> clone() {
		return new BucketSet<E>(this.pivotIndexer, this.buckets.clone(), this.size, this.containsPivotElement);
	}


	/**
	 * Returns the pivot indexer.
	 * @return the indexer assigning elements to k-buckets
	 */
	public PivotIndexer<E> getPivotIndexer() {
		return this.pivotIndexer;
	}


	/**
	 * Returns the bucket count, which equals the pivot-indexer's cardinality.
	 * @return the number of available buckets
	 */
	public int getBucketCount() {
		return this.buckets.length;
	}


	/**
	 * Returns the bucket capacity.
	 * @return the maximum number of elements (k) within each k-bucket
	 */
	public int getBucketCapacity() {
		return this.buckets[0].length;
	}


	/**
	 * Return <tt>true</tt> if the set always contains the pivot
	 *    element, <tt>false</tt> if it never does.
	 * @return <tt>true</tt> if the set always contains the pivot
	 *    element, <tt>false</tt> if it never does
	 */
	public boolean containsPivotElement() {
		return this.containsPivotElement;
	}


	/**
	 * Returns <tt>true</tt> if the receiver contains the given object in one of it's
	 * k-buckets, or as it's pivot element if the latter is contained.
	 * @param object an object or <tt>null</tt>
	 * @return <tt>true</tt> if the object is contained
	 */
	@Override
	public boolean contains(final Object object) {
		if (object == null) return false;
		if (this.containsPivotElement && this.pivotIndexer.getPivotElement().equals(object)) return true;

		final int bucketIndex = this.pivotIndexer.getIndex(object);
		if (bucketIndex < 0) return false;

		final E[] bucket = this.buckets[bucketIndex];
		for (int index = 0; index < bucket.length; ++index) {
			if (bucket[index] == null) return false;
			if (bucket[index].equals(object)) return true;
		}
		return false;
	}


	/**
	 * Returns <tt>true</tt> if the given element was added to one of the receiver's
	 * k-buckets, <tt>false</tt> otherwise. Neither <tt>null</tt> nor the pivot
	 * element can be added successfully. Other elements are added only if the
	 * element's bucket hasn't reached maximum capacity, the bucket-set doesn't
	 * already contain 2^31-1 elements, and the element isn't already included.
	 * @param element the new element
	 * @return <tt>true</tt> if this collection changed as a result of the call
	 */
	@Override
	public boolean add(final E element) {
		if (element == null || this.size == Integer.MAX_VALUE) return false;
		final int bucketIndex = this.pivotIndexer.getIndex(element);
		if (bucketIndex < 0) return false;
		final E[] bucket = this.buckets[bucketIndex];

		synchronized (bucket) {
			for (int index = 0; index < bucket.length; ++index) {
				if (bucket[index] == null) {
					bucket[index] = element;
					this.size += 1;
					return true;
				}
				if (bucket[index].equals(element)) return false;
			}
		}
		return false;
	}


	/**
	 * Returns true if the given object was successfully removed from the
	 * receiver. False is returned either if the object isn't contained, or if
	 * it equals the receiver's pivot element, which cannot be removed.
	 * @param object the object to be removed, or <tt>null</tt>
	 * @return <tt>true</tt> if this collection changed as a result of the call
	 */
	@Override
	public boolean remove(final Object object) {
		if (object == null) return false;
		final int bucketIndex = this.pivotIndexer.getIndex(object);
		if (bucketIndex < 0) return false;
		final E[] bucket = this.buckets[bucketIndex];

		synchronized (bucket) {
			for (int index = 0; index < bucket.length; ++index) {
				if (bucket[index] == null) return false;
				if (bucket[index].equals(object)) {
					System.arraycopy(bucket, index + 1, bucket, index, bucket.length - index - 1);
					bucket[bucket.length - 1] = null;
					this.size -= 1;
					return true;
				}
			}
		}

		return false;
	}


	/**
	 * Removes all the receiver's elements from all k-buckets. Note that the
	 * pivot element cannot be removed, therefore it is not affected by this
	 * operation!
	 */
	@Override
	public void clear() {
		synchronized (this.buckets) {
			for (final E[] bucket : this.buckets) {
				Arrays.fill(bucket, null);
			}
			this.size = this.containsPivotElement ? 1 : 0;
		}
	}


	/**
	 * Returns an iterator over all the elements of this set. The elements are
	 * returned in a predefined order: The pivot element is first (if it is
	 * contained), then the elements of the first k-bucket in their respective
	 * order of addition, then that of the next bucket, and so on. Note that the
	 * resulting iterator supports the remove operation, except for the pivot element.
	 * @return an iterator
	 * @see java.util.Set#iterator()
	 * @see #remove(Object)
	 * @see #removeAll(Collection)
	 * @see #retainAll(Collection)
	 */
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			private boolean pivotElementAvailable = BucketSet.this.containsPivotElement;
			private int currentBucketIndex = 0;
			private int currentElementIndex = -1;

			public boolean hasNext() {
				if (this.pivotElementAvailable) return true;

				int elementIndex = this.currentElementIndex + 1;
				if (elementIndex < BucketSet.this.buckets[this.currentBucketIndex].length) {
					final E element = BucketSet.this.buckets[this.currentBucketIndex][elementIndex];
					if (element != null) return true;
				}

				for (int bucketIndex = this.currentBucketIndex + 1; bucketIndex < BucketSet.this.buckets.length; ++bucketIndex) {
					final E element = BucketSet.this.buckets[bucketIndex][0];
					if (element != null) return true;
				}

				return false;
			}

			public E next() {
				if (this.pivotElementAvailable) {
					this.pivotElementAvailable = false;
					return BucketSet.this.pivotIndexer.getPivotElement();
				}

				int elementIndex = this.currentElementIndex + 1;
				if (elementIndex < BucketSet.this.buckets[this.currentBucketIndex].length) {
					final E element = BucketSet.this.buckets[this.currentBucketIndex][elementIndex];
					if (element != null) {
						this.currentElementIndex = elementIndex;
						return element;
					}
				}

				for (int bucketIndex = this.currentBucketIndex + 1; bucketIndex < BucketSet.this.buckets.length; ++bucketIndex) {
					final E element = BucketSet.this.buckets[bucketIndex][0];
					if (element != null) {
						this.currentBucketIndex = bucketIndex;
						this.currentElementIndex = 0;
						return element;
					}
				}

				throw new NoSuchElementException();
			}

			public void remove() {
				final E[] bucket = BucketSet.this.buckets[this.currentBucketIndex];
				if (this.currentElementIndex == -1 || bucket[this.currentElementIndex] == null) throw new IllegalStateException();
				System.arraycopy(bucket, this.currentElementIndex + 1, bucket, this.currentElementIndex, bucket.length - this.currentElementIndex - 1);
				bucket[bucket.length - 1] = null;
				BucketSet.this.size -= 1;
				this.currentElementIndex -= 1;
			}
		};
	}


	/**
	 * Retains only the elements in this collection that are contained in the
	 * specified collection, plus the pivot element. In other words, removes
	 * from this collection all of its elements that are not contained in the
	 * specified collection, and that are not equal to the pivot element.
	 * @param collection collection containing elements to be retained in this
	 *    collection
	 * @return <tt>true</tt> if this collection changed as a result of the call
	 * @see #remove(Object)
	 * @see #contains(Object)
	 */
	@Override
	public boolean retainAll(final Collection<?> collection) {
		boolean modified = false;
		final Iterator<E> iterator = this.iterator();
		if (this.containsPivotElement) iterator.next(); // prevent trying to remove pivot element
		while (iterator.hasNext()) {
			if (!collection.contains(iterator.next())) {
				iterator.remove();
				modified = true;
			}
		}
		return modified;
	}


	/**
	 * Removes from this set all of its elements that are contained in the
	 * specified collection, except the pivot element. If the specified
	 * collection is also a set, this operation effectively modifies this
	 * set so that its value is the <i>asymmetric set difference</i> of the
	 * two sets, with the exception of the pivot element which may be
	 * contained in both.
	 * @param collection collection containing elements to be removed from this set
	 * @return <tt>true</tt> if this set changed as a result of the call
	 * @see #remove(Object)
	 * @see #contains(Object)
	 */
	@Override
	public boolean removeAll(Collection<?> collection) {
		boolean modified = false;

		final Iterator<E> iterator = this.iterator();
		if (this.containsPivotElement) iterator.next(); // prevent trying to remove pivot element
		while (iterator.hasNext()) {
			if (collection.contains(iterator.next())) {
				iterator.remove();
				modified = true;
			}
		}
		return modified;
	}


	/**
	 * Returns the number of elements contained, possibly including the pivot
	 * element. Note that because the pivot element cannot be removed, the
	 * result is guaranteed to be strictly positive in case it is contained.
	 * @return the number of elements
	 * @see java.util.Set#size()
	 */
	public int size() {
		return this.size;
	}


	/**
	 * Returns a new array containing the elements of the k-bucket with the
	 * given index. The collection is copied into an array to both avoid thread
	 * synchronization problems, and to prevent other classes from modifying the
	 * k-buckets directly. Note that the index -1 addresses the virtual
	 * one-element pivot bucket, containing the pivot element.
	 * @param bucketIndex  the k-bucket index
	 * @return an array of elements
	 * @throws IndexOutOfBoundsException if the given index is out of range
	 *    [-1; bucketCardinality[
	 */
	@SuppressWarnings("unchecked")
	public E[] toArray(final int bucketIndex) {
		final E[] result;

		if (bucketIndex == -1) {
			result = (E[]) new Object[] { this.pivotIndexer.getPivotElement() };
		} else {
			final E[] bucket = this.buckets[bucketIndex];
			synchronized (this) {
				int elementCount = 0;
				while (elementCount < bucket.length	&& bucket[elementCount] != null) {
					elementCount += 1;
				}

				result = (E[]) new Object[elementCount];
				System.arraycopy(bucket, 0, result, 0, elementCount);
			}
		}

		return result;
	}
}