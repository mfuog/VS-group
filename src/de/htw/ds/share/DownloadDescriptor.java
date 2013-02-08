package de.htw.ds.share;

import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Path;
import de.sb.javase.TypeMetadata;


/**
 * <p>This class models download task status information for a single
 * download task. It maintains both the basic file information, and the
 * chunk count information. Additionally, it can estimate chunk ratios.</p>
 */
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class DownloadDescriptor implements ContentDescriptor {

	private final BigInteger contentHash;
	private final long contentLength;
	private final Path filePath;
	private volatile int acquirableChunkCount;
	private volatile int acquiredChunkCount;
	private volatile int committedChunkCount;
	private volatile boolean closed;


	/**
	 * Protected no-arg constructor is required for JAX-B marshaling.
	 */
	protected DownloadDescriptor() {
		super();

		this.contentHash = null;
		this.contentLength = 0;
		this.filePath = null;
		this.closed = false;
		this.acquirableChunkCount = 0;
		this.acquiredChunkCount = 0;
		this.committedChunkCount = 0;
	}


	/**
	 * Creates a new instance
	 * @param contentHash the content hash
	 * @param contentLength the content length
	 * @param filePath the file path
	 * @throws NullPointerException if any of the given arguments is <tt>null</tt>
	 * @throws IllegalArgumentException if the given content length is negative
	 */
	public DownloadDescriptor(final BigInteger contentHash, final long contentLength, final Path filePath) {
		super();
		if (contentHash == null | filePath == null) throw new NullPointerException();
		if (contentLength < 0) throw new IllegalArgumentException();

		this.contentHash = contentHash;
		this.contentLength = contentLength;
		this.filePath = filePath;
		this.closed = false;
		this.acquirableChunkCount = 0;
		this.acquiredChunkCount = 0;
		this.committedChunkCount = 0;
	}


	/**
	 * Returns the content hash.
	 * @return the content hash
	 */
	public BigInteger getContentHash() {
		return this.contentHash;
	}


	/**
	 * Returns the content length.
	 * @return the content length
	 */
	public long getContentLength() {
		return this.contentLength;
	}


	/**
	 * Returns the file path.
	 * @return the file path
	 */
	public Path getFilePath() {
		return this.filePath;
	}


	/**
	 * Returns the closed property.
	 * @return whether this download task is finished or not
	 */
	public boolean isClosed() {
		return this.closed;
	}


	/**
	 * Sets the closed property.
	 * @param closed whether this download task is finished or not
	 */
	public void setClosed(final boolean closed) {
		this.closed = closed;
	}


	/**
	 * Returns the acquirable chunk count.
	 * @return the number of acquirable chunks
	 * @see #getChunkCount()
	 */
	public int getAcquirableChunkCount() {
		return this.acquirableChunkCount;
	}


	/**
	 * Sets the acquirable chunk count.
	 * @param acquirableChunkCount the number of acquirable chunks
	 * @throws IllegalArgumentException if the given chunk count is strictly negative
	 */
	public void setAcquirableChunkCount(final int acquirableChunkCount) {
		if (acquirableChunkCount < 0) throw new IllegalArgumentException();
		this.acquirableChunkCount = acquirableChunkCount;
	}


	/**
	 * Returns the acquired chunk count.
	 * @return the number of acquired chunks
	 * @see #getChunkCount()
	 */
	public int getAcquiredChunkCount() {
		return this.acquiredChunkCount;
	}


	/**
	 * Sets the acquired chunk count.
	 * @param acquiredChunkCount the number of acquirable chunks
	 * @throws IllegalArgumentException if the given chunk count is strictly negative
	 */
	public void setAcquiredChunkCount(final int acquiredChunkCount) {
		if (acquiredChunkCount < 0) throw new IllegalArgumentException();
		this.acquiredChunkCount = acquiredChunkCount;
	}


	/**
	 * Returns the committed chunk count.
	 * @return the number of chunks committed
	 * @see #getChunkCount()
	 */
	public int getCommittedChunkCount() {
		return this.committedChunkCount;
	}


	/**
	 * Sets the committed chunk count.
	 * @param committedChunkCount the number of committed chunks
	 * @throws IllegalArgumentException if the given chunk count is strictly negative
	 */
	public void setCommittedChunkCount(final int committedChunkCount) {
		if (committedChunkCount < 0) throw new IllegalArgumentException();
		this.committedChunkCount = committedChunkCount;
	}


	/**
	 * Returns the total number of chunks which is the sum of the
	 * three basic chunk counts for acquirable, committed, and
	 * processing chunks.
	 * @return the total chunk count
	 * @see #getAcquirableChunkCount()
	 * @see #getAcquiredChunkCount()
	 * @see #getCommittedChunkCount()
	 */
	public int getChunkCount() {
		return this.acquirableChunkCount + this.committedChunkCount + this.acquiredChunkCount;
	}


	/**
	 * Returns the ratio of acquirable chunks to total chunks.
	 * @return the acquirable chunk ratio within range [0, 1]
	 * @see #getAcquirableChunkCount()
	 */
	public double getAcquirableChunkRatio() {
		return this.acquirableChunkCount / (double) this.getChunkCount();
	}


	/**
	 * Returns the ratio of acquired chunks to total chunks.
	 * @return the acquired chunk ratio within range [0, 1]
	 * @see #getAcquiredChunkCount()
	 */
	public double getAcquiredChunkRatio() {
		return this.acquiredChunkCount / (double) this.getChunkCount();
	}


	/**
	 * Returns the ratio of committed chunks to total chunks.
	 * @return the committed chunk ratio within range [0, 1]
	 * @see #getCommittedChunkCount()
	 */
	public double getCommittedChunkRatio() {
		return this.committedChunkCount / (double) this.getChunkCount();
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final StringWriter charSink = new StringWriter();
		charSink.write("{hash=");
		charSink.write(FileShareResources.toHexString(this.contentHash));
		charSink.write(", length=");
		charSink.write(Long.toString(this.contentLength));
		charSink.write(", path=");
		charSink.write(this.filePath.toString());
		charSink.write("}");
		return charSink.toString();
	}
}