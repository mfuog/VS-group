package de.htw.ds.share;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.SocketAddress;
import de.sb.javase.io.SocketAddress.ConversionMode;
import de.sb.javase.util.BucketSet;
import de.sb.javase.util.PivotFilter;
import de.sb.javase.util.PivotIndexer;


/**
 * <p>This abstract class defines socket address filters for use by the
 * monitor threads, allowing them to contact small subsets of available
 * peers for their tasks. This becomes important once the number of available
 * peers grows larger, as contacting everyone for everything quickly
 * becomes impossible.
 * @param <P> the pivot type
 */
@TypeMetadata(copyright="2011-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public abstract class SocketAddressFilter<P> implements PivotFilter<SocketAddress,P> {
	private static final Random RANDOMIZER = new Random();

	/**
	 * <p>Returns a new socket-address filter based on file-hash distance and k-buckets.
	 * The given bucket cardinality and capacity specify the dimensions of the k-bucket
	 * matrix to be used for filtering.</p>
	 * <p>Such file-hash based filters allow file monitors to contact only a small subset
	 * of their known peers whenever they require meta information about a given file-hash.
	 * This is because source file monitors will statistically more often place a file's
	 * meta-information on peers nearer to a given file's hash, which is also where target
	 * file descriptors will look for additional file sources during downloads.</p>
	 * <p>k-Bucket based filtering has the advantage over simpler k-sorter based algorithms
	 * that source file monitors will also disperse file meta-information to some peers
	 * with a farther distance from the given file's hash. This is mandatory for meta-data
	 * queries (the ones that return the file-hashes), as these need to perform a blind
	 * search over random peers.</p>
	 * @param bucketCardinality the number of k-buckets within each bucket-set
	 * @param bucketCapacity the maximum number of elements (k) within each k-bucket
	 * @return the socket address filter
	 * @throws IllegalArgumentException if the cardinality or capacity is negative
	 */
	public static SocketAddressFilter<BigInteger> newFileHashBasedFilter(final int bucketCardinality, final int bucketCapacity) {
		if (bucketCardinality <= 0 | bucketCapacity <= 0) throw new IllegalArgumentException();

		return new SocketAddressFilter<BigInteger>() {
			public SocketAddress[] filter(final SocketAddress[] socketAddresses, final BigInteger contentHash) {
				final PivotIndexer<SocketAddress> pivotIndexer = new FileHashBasedSocketAddressIndexer(contentHash, bucketCardinality);
				final Set<SocketAddress> result = new BucketSet<SocketAddress>(pivotIndexer, bucketCapacity, false);
				result.addAll(Arrays.asList(socketAddresses));
				return result.toArray(new SocketAddress[result.size()]);
			}
		};
	}



	/**
	 * <p>Returns a new socket-address filter based on timestamp distance. The given capacity 
	 * specifies the maximum number of socket addresses returned by the filter.</p>
	 * <p>Such timestamp based filters allow peer monitors to contact only a small subset
	 * of their known peers whenever they require an update of their peer collection.
	 * This is because peer monitors place register own service address on systems they ask
	 * for peer addresses. Therefore, if all use the same filter, systems with service addresses
	 * nearer a given timestamp will be more likely to be asked for peer addresses, and therefore
	 * also be more likely to know a lot of peers. As time progresses, the set of preferably
	 * selected peers will slowly change, ensuring that the file share network cannot simply be
	 * sabotaged by stopping or attacking certain peers.</p>
	 * @param capacity the maximum number of socket addresses returned by each filtering attempt
	 * @return the socket address filter
	 */
	public static SocketAddressFilter<Long> newTimestampBasedFilter(final int capacity) {
		// TODO: Aufgabe 3
		return newRandomFilter(capacity);
	}


	/**
	 * <p>Returns a new random based socket-address filter that returns at most
	 * <tt>capacity</tt> randomly selected socket addresses.</p>
	 * @return the socket address filter
	 */
	public static <P> SocketAddressFilter<P> newRandomFilter(final int capacity) {
		return new SocketAddressFilter<P>() {
			public SocketAddress[] filter(final SocketAddress[] socketAddresses, final P pivot) {
				if (socketAddresses.length == 0) return socketAddresses;

				final Set<SocketAddress> result = new HashSet<SocketAddress>();
				for (int loop = 0; loop < capacity; ++loop) {
					final int index = RANDOMIZER.nextInt(socketAddresses.length);
					result.add(socketAddresses[index]);
				}
				return result.toArray(new SocketAddress[0]);
			}
		};
	}


	/**
	 * <p>Returns a new dummy socket-address filter that returns all
	 * elements it receives for filtering.</p>
	 * @return the socket address filter
	 */
	public static <P> SocketAddressFilter<P> newNullFilter() {
		return new SocketAddressFilter<P>() {
			public SocketAddress[] filter(final SocketAddress[] socketAddresses, final P pivot) {
				return socketAddresses;
			}
		};
	}


	/**
	 * Instances of this inner class assign an index to a given socket address,
	 * based on comparison with a file hash. The resulting indices are
	 * guaranteed to be within range [-1, cardinality - 1].
	 */
	@SuppressWarnings("serial")
	private static final class FileHashBasedSocketAddressIndexer implements PivotIndexer<SocketAddress> {

		private final BigInteger contentHash;
		private final int cardinality;
		private final BigInteger scale;


		/**
		 * Creates a new pivot indexer based on the given file hash
		 * and cardinality
		 * @param file hash the file hash used as an equivalent to a pivotal socket address 
		 * @param cardinality the cardinality of the index calculation, i.e. the
		 *    maximum index + 1
		 * @throws NullPointerException if the given file hash is <tt>null</tt>
		 * @throws IllegalArgumentException if the given file hash is strictly negative, or
		 *    if the given cardinality is negative
		 */
		public FileHashBasedSocketAddressIndexer(final BigInteger contentHash, final int cardinality) {
			super();
			if (contentHash == null) throw new NullPointerException();
			if (contentHash.signum() < 0 | cardinality <= 0) throw new IllegalArgumentException();

			this.contentHash = contentHash;
			this.cardinality = cardinality;
			this.scale = BigInteger.ONE.shiftLeft(SocketAddress.BINARY_IP6_SOCKET_ADDRESS_LENGTH << 3);
		}


		/**
		 * Returns the socket address equivalent of this indexer's
		 * file hash as pivot element!
		 */
		public SocketAddress getPivotElement() {
			return new SocketAddress(this.contentHash.toByteArray());
		}


		/**
		 * {@inheritDoc}
		 */
		public int getCardinality() {
			return this.cardinality;
		}			


		/**
		 * Calculates an index for the given socket-address based on it's XOR distance to
		 * the filter's file hash. Converts each socket-address into a binary IPv6
		 * socket-address, which by design feature the same bit-length as the file hashes,
		 * 144 bits. IPv4-mapped IPv6 addresses are used in case a socket-address is based
		 * on IPv4. Interprets each binary IPv6 socket-address as an unsigned virtual file
		 * hash, and calculates the XOR-distance of this to the given file-hash. Returns
		 * the index by calculating "cardinality * XOR-distance / scale" with lower
		 * indices indicating smaller XOR-distance. <tt>-1</tt> is returned if the given
		 * object is not a socket address, or if the socket address cannot be resolved
		 * into an IPv6 binary address.
		 * @param object the socket address
		 * @return an index within range [-1, cardinality - 1]
		 */
		public int getIndex(final Object object) {
			final byte[] binarySocketAddress;
			try {
				binarySocketAddress = ((SocketAddress) object).toBinarySocketAddress(ConversionMode.FORCE_IP6);
			} catch (final Exception exception) {
				return -1;
			}

			final BigInteger unsignedBinarySocketAddress = new BigInteger(1, binarySocketAddress);
			final BigInteger xorDistance = this.contentHash.xor(unsignedBinarySocketAddress);
			final BigInteger cardinality = BigInteger.valueOf(this.cardinality);
			return xorDistance.multiply(cardinality).divide(this.scale).intValue();
		}
	}
}