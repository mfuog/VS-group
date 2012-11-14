package de.htw.ds.sync.myrtha;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.NoSuchElementException;

import de.htw.ds.TypeMetadata;


/**
 * <p>This class defines thread-safe iteration operations, similar to the ones available in the SmallTalk language.</p>
 */
@TypeMetadata(copyright="2010-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class Iterators {
	/**
	 * <p>Element processors are invoked whenever a collection element has
	 * to be processed.</p>
	 * @param <E> the element type
	 * @param <R> the type of the processing result
	 */
	public static interface ElementProcessor<E, R> {
		R process (E element);
	}


	/**
	 * Iterates over all elements of the given collection in a thread-safe manner,
	 * processing each element with the given element processor.
	 * @param <E> the element type
	 * @param collection the collection
	 * @param elementProcessor the element processor
	 */
	public static <E> void iterate (final Collection<E> collection, final ElementProcessor<E, ?> elementProcessor) {
		synchronized(collection) {
			for (final E element : collection) {
				elementProcessor.process(element);
			}
		}
	}


	/**
	 * Iterates over all elements of the given collection in a thread-safe manner,
	 * returning the first element for which the given element processor returns true.
	 * @param <E> the element type
	 * @param collection the collection
	 * @param elementProcessor the element processor
	 * @return the detected element
	 * @throws NoSuchElementException if no element exists that processes to true
	 */
	public static <E> E detect (final Collection<E> collection, final ElementProcessor<E, Boolean> iterateBlock) throws NoSuchElementException {
		synchronized(collection) {
			for (final E element : collection) {
				if(iterateBlock.process(element)) {
					return element;
				}
			}
			throw new NoSuchElementException();
		}
	}


	/**
	 * Iterates over all elements of the given collection in a thread-safe manner,
	 * returning the processing results in another collection of the same kind.
	 * @param <E> the element type
	 * @param <R> the resulting element type
	 * @param collection the element collection
	 * @param elementProcessor the element processor
	 * @return the processing results as another collection of the same kind
	 */
	public static <E,R> Collection<R> collect (final Collection<E> collection, final ElementProcessor<E, R> elementProcessor) {
		synchronized(collection) {
			@SuppressWarnings("unchecked")
			final Collection<R> result = (Collection<R>) Iterators.createEmptyInstance(collection);
			for (final E element : collection) {
				result.add(elementProcessor.process(element));
			}
			return result;
		}
	}


	/**
	 * Iterates over all elements of the given collection in a thread-safe manner,
	 * returning those elements in another collection of the same kind for which the
	 * given element processor returns true.
	 * @param <E> the element type
	 * @param collection the element collection
	 * @param elementProcessor the element processor
	 * @return the elements processing to true as another collection of the same kind
	 */
	public static <E> Collection<E> select (final Collection<E> collection, final ElementProcessor<E, Boolean> elementProcessor) {
		synchronized(collection) {
			final Collection<E> result = Iterators.createEmptyInstance(collection);
			for (final E element : collection) {
				if(elementProcessor.process(element)) {
					result.add(element);
				}
			}
			return result;
		}
	}


	/**
	 * Creates an empty collection of the same kind as the given one, using
	 * the Java Reflection API.
	 * @param <E> the element type
	 * @param collection the collection
	 * @return an empty collection of the same kind as the given one
	 */
	private static <E> Collection<E> createEmptyInstance(final Collection<E> collection) {
		try {
			@SuppressWarnings("unchecked")
			final Constructor<Collection<E>> constructor = (Constructor<Collection<E>>) collection.getClass().getConstructor();
			constructor.setAccessible(true);
			final Collection<E> result = constructor.newInstance();
			return result;
		} catch (final NoSuchMethodException exception) {
			throw new IllegalArgumentException();
		} catch (final InstantiationException exception) {
			throw new IllegalArgumentException();
		} catch (IllegalAccessException e) {
			throw new AssertionError();
		} catch (InvocationTargetException exception) {
			if (exception.getCause() instanceof Error) throw (Error) exception.getCause();
			if (exception.getCause() instanceof RuntimeException) throw (RuntimeException) exception.getCause();
			throw new AssertionError();
		}
	}


	/**
	 * Private constructor prevents instantiation.
	 */
	private Iterators() {
		super();
	}
}