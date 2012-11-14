<<<<<<< HEAD
package de.htw.ds.sync;
=======
package de.htw.ds.sync.myrtha;
>>>>>>> e8571f9152b9780ec464505e18d7fb7142f2743a

import de.htw.ds.TypeMetadata;


/**
 * Reusable class referencing a single object, or null;
 * @param <T> the object type
 */
@TypeMetadata(copyright="2010-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class Reference<T> {
	private T object = null;

	/**
	 * Returns the referenced object.
	 * @return the object
	 */
	public T get() {
		return this.object;
	}


	/**
	 * Sets the referenced object.
	 * @param object the object
	 */
	public void put(final T object) {
		this.object = object;
	}
}