package de.htw.ds.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

import de.htw.ds.TypeMetadata;


/**
 * <p>Class loader extending it's parent class loader by loading classes from a
 * given jar-file. Stacking multiple instances of this class results in support
 * for multiple jar-files. Instances takes ownership of their jar-files while
 * they exist, and may optionally remove said jar-files after use.</p>
 */
@TypeMetadata(copyright="2009-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class JarFileLoader extends ClassLoader {

	private final JarFile jarFile;


	/**
	 * Creates a new instance that will load classes from it's parent loader,
	 * and if not found from class files within the given jar-file. If delete
	 * is <tt>true</tt>, the class loader will delete it's jar-file once it
	 * has been read, which is recommended with temporary libraries because
	 * these tend to be notoriously hard to delete after use. Otherwise, the
	 * jar-file will continue to exist after use.
	 * @param parent the optional parent class loader or <tt>null</tt>
	 * @param jarPath the path of the jar-file to load classes from
	 * @param delete whether or not to delete the jar-file after use
	 * @throws NullPointerException if the given jar-file is <tt>null</tt>
	 * @throws IOException if there's a problem opening the jar-file
	 */
	public JarFileLoader(final ClassLoader parent, final Path jarPath, final boolean delete) throws IOException {
		super(parent);

		final int mode = delete ? ZipFile.OPEN_READ | ZipFile.OPEN_DELETE : ZipFile.OPEN_READ;
		this.jarFile = new JarFile(jarPath.toFile(), false, mode);
	}


	/**
	 * Finds the class with the specified <a href="#name">binary name</a>.
	 * This method will be invoked by the {@link #loadClass <tt>loadClass</tt>}
	 * method after checking the parent class loader for the requested class.
	 * @param name the <a href="#name">binary name</a> of the class
	 * @return the resulting <tt>Class</tt> object
	 * @throws ClassFormatError if the class file is malformed or too large
	 * @throws ClassNotFoundException if the class could file not be found
	 */
	@Override
	protected Class<?> findClass(final String name) throws ClassNotFoundException, ClassFormatError {
		final JarEntry jarEntry = this.jarFile.getJarEntry(name.replace('.', '/') + ".class");

		if (jarEntry == null || jarEntry.isDirectory() || jarEntry.getSize() < 0) throw new ClassNotFoundException(name);
		if (jarEntry.getSize() > Integer.MAX_VALUE) throw new ClassFormatError(name + " too large.");

		final ByteArrayOutputStream sink = new ByteArrayOutputStream((int) jarEntry.getSize());
		try (InputStream source = this.jarFile.getInputStream(jarEntry)) {
			final byte[] buffer = new byte[0x10000];
			for (int bytesRead = source.read(buffer); bytesRead != -1; bytesRead = source.read(buffer)) {
				sink.write(buffer, 0, bytesRead);
			}
		} catch (final IOException exception) {
			throw new ClassFormatError(name + " is broken.");
		}

		final byte[] classBytes = sink.toByteArray();
		return this.defineClass(name, classBytes, 0, classBytes.length);
	}
}