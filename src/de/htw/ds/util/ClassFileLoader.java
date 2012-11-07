package de.htw.ds.util;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;

import de.htw.ds.TypeMetadata;


/**
 * <p>Class loader extending it's parent class loader by reading
 * class files from a given context directory.</p>
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class ClassFileLoader extends ClassLoader {

	private final Path contextPath;


	/**
	 * Creates a new instance that will load classes from it's parent loader,
	 * and if not found from class files within the given context directory
	 * or one of it's children.
	 * @param parent the optional parent class loader, or <tt>null</tt>
	 * @param contextPath the context directory to load class files from
	 * @throws NullPointerException if the given context path is <tt>null</tt>
	 * @throws NotDirectoryException if the given context path is not a directory
	 */
	public ClassFileLoader(final ClassLoader parent, final Path contextPath) throws NotDirectoryException {
		super(parent);

		if (!Files.isDirectory(contextPath)) throw new NotDirectoryException(contextPath.toString());
		this.contextPath = contextPath;
	}


	/**
	 * Finds the class with the specified <a href="#name">binary name</a>.
	 * This method will be invoked by the {@link #loadClass <tt>loadClass</tt>}
	 * method after checking the parent class loader for the requested class.
	 * @param name the <a href="#name">binary name</a> of the class
	 * @return the resulting <tt>Class</tt> object
	 * @throws ClassFormatError if the class file is malformed or too large
	 * @throws ClassNotFoundException if the class file could not be found
	 */
	@Override
	protected Class<?> findClass(final String name) throws ClassNotFoundException, ClassFormatError {
		final String separator = this.contextPath.getFileSystem().getSeparator();
		final Path classFilePath = this.contextPath.resolve(name.replace(".", separator) + ".class");

		try {
			final byte[] classBytes = Files.readAllBytes(classFilePath);
			return this.defineClass(name, classBytes, 0, classBytes.length);
		} catch (final NoSuchFileException | AccessDeniedException exception) {
			throw new ClassNotFoundException(name);
		} catch (final IOException exception) {
			throw new ClassFormatError(name + " is broken.");
		} catch (final OutOfMemoryError exception) {
			throw new ClassFormatError(name + " is too large.");
		}
	}
}