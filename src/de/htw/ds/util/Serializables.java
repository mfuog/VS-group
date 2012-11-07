package de.htw.ds.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import de.htw.ds.TypeMetadata;


/**
 * <p>Static helper class simplifying object serialization/deserialization, and cloning of
 * serializable classes. Additionally, this helper class modifies object stream I/O exception
 * handling to adjust object serialization to modern exception handling standards - therefore
 * causing API use to be more streamlined, more consistent, and less error prone. This is
 * achieved by eliminating all checked I/O exceptions that cannot happen in this design, and
 * additionally by retrowing all checked exceptions that usually point to programming or
 * algorithmic errors as unchecked exceptions.</p>
 * <p>Note that modern design makes very selective and careful use of checked exceptions.
 * An exception should be checked only if BOTH of the following apply:<ul>
 * <li>The problem is usually not caused by programming error or reasons unrelated to
 * the code. Consider why NullPointerException or OutOfMemoryError are unchecked.</li>
 * <li>There is a good chance the problem can be meaningfully remedied in a catch block,
 * i.e. in other ways than simply displaying the problem to an end user. Consider why the
 * more modern JAX-WS API (WebServices) does NOT force any service method to declare
 * RemoteException, unlike the older Java-RMI standard.</li></ul>
 * Increased severity of a problem is NOT a reason to check an exception, that's what
 * unchecked exceptions and errors are for! The possibility to create checked exceptions
 * exists to force programmers to cure the mild cases, those easily overlooked but at the
 * same time often curable. That's why InterruptedException is rightfully a checked
 * exception, while parts of the original object stream API design is simply bad
 * practice when it comes to checked exceptions.</p>
 */
@TypeMetadata(copyright="2010-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class Serializables {

	/**
	 * Private constructor preventing instantiation.
	 */
	private Serializables() {
		super();
	}


	/**
	 * Serializes the given objects and returns the resulting data. Note that the given
	 * objects may be null, but not the given variable arguments list.
	 * @param objects the objects
	 * @return the serialized data
	 * @throws NullPointerException if the given variable arguments list is null
	 * @throws NotSerializableException if one of the given objects, or one of it's direct
	 *    or indirect fields is expected to be serializable, but is not
	 */
	public static byte[] serializeObjects(final Serializable... objects) throws NotSerializableException {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
				for (final Serializable object : objects) {
					oos.writeObject(object);
				}
				return bos.toByteArray();
			}
		} catch (final NotSerializableException exception) {
			throw exception;
		} catch (final InvalidClassException exception) {
			throw new AssertionError(); // cannot happen within writeObject() because serializable classes are no longer expected to possess accessible no-arg constuctors.
		} catch (final IOException exception) {
			throw new AssertionError(); // cannot happen because byte streams don't generate any IO exceptions
		}
	}


	/**
	 * Deserializes the given data and returns the resulting objects. Note that this
	 * method simplifies object stream exception handling by indicating incompatible
	 * serialVersionUIDs as ClassNotFoundException (similar logic to unreadable files
	 * causing FileNotFoundException), and both corrupted serialization headers and
	 * unexpected primitive data as IllegalArgumentException (because both point to
	 * messed up data).
	 * @param data the serialized data
	 * @param offset the offset
	 * @param length the length
	 * @return the deserialized objects
	 * @throws NullPointerException if the given byte array is null
	 * @throws IndexOutOfBoundsException if deserialization would cause access of data outside array bounds
	 * @throws IllegalArgumentException if the given data does not represent a valid serialized sequence of objects
	 * @throws ClassNotFoundException if a class is required that cannot be loaded, or has a serialVersionUID
	 *    incompatible to it's equivalent represented in the data
	 */
	public static Serializable[] deserializeObjects(final byte[] data, int offset, int length) throws ClassNotFoundException {
		if (offset < 0 || length < 0 || offset + length > data.length) throw new IndexOutOfBoundsException();

		try (ByteArrayInputStream bis = new ByteArrayInputStream(data, offset, length)) {
			final List<Serializable> result = new ArrayList<>();

			try (ObjectInputStream ois = new ObjectInputStream(bis)) {
				while(true) {
					result.add((Serializable) ois.readObject());
				}
			} catch (final EOFException exception) {
				// loop exit condition when the bytes array is truncated
			}

			return result.toArray(new Serializable[result.size()]);
		} catch (final InvalidClassException exception) {
			throw new ClassNotFoundException(exception.getMessage(), exception);
		} catch (final StreamCorruptedException exception) {
			throw new IllegalArgumentException(exception);
		} catch (final OptionalDataException exception) {
			throw new IllegalArgumentException(exception);
		} catch (final IOException exception) { 
			throw new AssertionError(); // cannot happen because byte streams don't generate any IO exceptions
		}
	}


	/**
	 * Deserializes the given data and returns the first object contained within it.
	 * This is a convenience method for use with serialized data containing single
	 * objects. Note that this method simplifies object stream exception handling by
	 * indicating incompatible serialVersionUIDs as ClassNotFoundException (similar
	 * logic to unreadable files causing FileNotFoundException), and both corrupted
	 * serialization headers and unexpected primitive data as IllegalArgumentException
	 * (because both point to messed up data).
	 * @param data the data
	 * @param offset the offset
	 * @param length the length
	 * @return the deserialized object
	 * @throws NullPointerException if the given byte array is null
	 * @throws IndexOutOfBoundsException if deserialization would cause access of data outside array bounds
	 * @throws NoSuchElementException if there is no serialized object in the given data
	 * @throws IllegalArgumentException if the given data does not represent a valid serialized sequence of objects
	 * @throws ClassNotFoundException if a class is required that cannot be loaded, or has a serialVersionUID
	 *    incompatible to it's equivalent represented in the data
	 */
	public static Serializable deserializeObject(final byte[] data, int offset, int length) throws ClassNotFoundException {
		final Serializable[] objects = Serializables.deserializeObjects(data, offset, length);
		if (objects.length == 0) throw new NoSuchElementException();
		return objects[0];
	}


	/**
	 * Clones the given object using serialization followed by immediate deserialization.
	 * @param <T> the object type
	 * @param object the object to be cloned, or null
	 * @return the clone, or null
	 * @throws NotSerializableException if the given object is not serializable
	 */
	@SuppressWarnings("unchecked")
	public static final <T extends Serializable> T clone(final T object) throws NotSerializableException {
		final byte[] bytes = Serializables.serializeObjects(object);
		try {
			return (T) Serializables.deserializeObject(bytes, 0, bytes.length);
		} catch (final ClassNotFoundException exception) {
			throw new AssertionError(); // cannot happen because we serialized the data within the same thread 
		}
	}
}