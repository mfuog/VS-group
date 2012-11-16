package de.htw.ds.sync;

import java.util.Arrays;
import de.htw.ds.TypeMetadata;


/**
 * <p>Demonstrator for single threading vector arithmetics based on double arrays. Note that
 * of all available processor cores within a system, this implementation is only capable of
 * using one!</p>
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="1.0.0", authors="Sascha Baumeister")
public final class VectorMathLinearMine {
	private static final int PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors();


	/**
	 * Sums two vectors within a single thread.
	 * @param leftOperand the first operand
	 * @param rightOperand the second operand
	 * @return the resulting vector
	 * @throws NullPointerException if one of the given parameters is null
	 * @throws IllegalArgumentException if the given parameters do not share the same length
	 */
	public static double[] add(final double[] leftOperand, final double[] rightOperand) {
		if (leftOperand.length != rightOperand.length) throw new IllegalArgumentException();
		final double[] result = new double[leftOperand.length];
		for (int index = 0; index < leftOperand.length; ++index) {
			result[index] = leftOperand[index] + rightOperand[index];
		}
		return result;
	}


	/**
	 * Multiplexes two vectors within a single thread.
	 * @param leftOperand the first operand
	 * @param rightOperand the second operand
	 * @return the resulting matrix
	 * @throws NullPointerException if one of the given parameters is null
	 */
	public static double[][] mux(final double[] leftOperand, final double[] rightOperand) {
		final double[][] result = new double[leftOperand.length][rightOperand.length];
		for (int leftIndex = 0; leftIndex < leftOperand.length; ++leftIndex) {
			for (int rightIndex = 0; rightIndex < rightOperand.length; ++rightIndex) {
				result[leftIndex][rightIndex] = leftOperand[leftIndex] * rightOperand[rightIndex];
			}
		}
		return result;
	}


	/**
	 * Runs both vector summation and vector multiplexing for demo purposes.
	 * @param args the argument array
	 */
	public static void main(final String[] args) {
		//use args as dimension for demo vectors a and b. if none given, use 10
		final int dimension = args.length == 0 ? 10 : Integer.parseInt(args[0]);

		//create demo vectors a and b and fill them with values
		final double[] a = new double[dimension], b = new double[dimension];
		for (int index = 0; index < dimension; ++index) {
			a[index] = index + 1.0;
			b[index] = index + 2.0;
		}
		System.out.format("Computation is performed on %s processor core(s):\n", PROCESSOR_COUNT);

		//adding a and b
		final long timestamp0 = System.currentTimeMillis();
		final double[] sum = add(a, b);
		final long timestamp1 = System.currentTimeMillis();
		System.out.format("a + b took %sms to compute.\n", timestamp1 - timestamp0);

		//multiplexing a and b
		final long timestamp2 = System.currentTimeMillis();
		final double[][] mux = mux(a, b);
		final long timestamp3 = System.currentTimeMillis();
		System.out.format("a x b took %sms to compute.\n", timestamp3 - timestamp2);

		System.out.format("dimension: %s \n", dimension);
		
		//print out results
		if (dimension <= 100) {
			System.out.print("a = ");
			System.out.println(Arrays.toString(a));
			System.out.print("b = ");
			System.out.println(Arrays.toString(b));
			System.out.print("a + b = ");
			System.out.println(Arrays.toString(sum));
			System.out.print("a x b = [");
			for (int index = 0; index < mux.length; ++index) {
				System.out.print(Arrays.toString(mux[index]));
			}
			System.out.println("]");
		}
	}
}