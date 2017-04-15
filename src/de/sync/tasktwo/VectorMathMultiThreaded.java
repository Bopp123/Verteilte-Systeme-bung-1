package de.sync.tasktwo;

import java.util.Arrays;

public class VectorMathMultiThreaded {
	
	
	
	
	private static class Calculator implements Runnable{
		
		
		private final double[] resultAdd;
		private final double[][] resultMux;
		private final double[] left;
		private final double[] right;
		private final boolean adding;
		private final int start;
		
		

		public Calculator(double[] left, double[] right,int start,  double[] resultAdd,double[][] resultMux, boolean adding) {
			super();
			this.left = left;
			this.right = right;
			this.resultAdd = resultAdd;
			this.resultMux = resultMux;
			this.adding = adding;
			this.start= start;
		}
		
		
		
		public double[] getResultAdd() {
			return resultAdd;
		}



		public double[][] getResultMux() {
			return resultMux;
		}

		/**
		 * Sums two vectors within a single thread.
		 * @param left the first operand
		 * @param right the second operand
		 * @return the resulting vector
		 * @throws NullPointerException if one of the given parameters is {@code null}
		 * @throws IllegalArgumentException if the given parameters do not share the same length
		 */
		static public void add (final double[] left, final double[] right, final double[] result, int start) {
			if (left.length != right.length) throw new IllegalArgumentException();
			for (int x = 0; x < left.length; ++x) {
				result[x + start] = left[x] + right[x];
			}
		}
		/**
		 * Multiplexes two vectors within a single thread.
		 * @param left the first operand
		 * @param right the second operand
		 * @return the resulting matrix
		 * @throws NullPointerException if one of the given parameters is {@code null}
		 */
		static public void mux (final double[] left, final double[] right, final double[][] result, int start ) {
			for (int x = 0; x < left.length; ++x) {
				for (int rightIndex = 0; rightIndex < right.length; ++rightIndex) {
					result[x][start + rightIndex] = left[ x] * right[rightIndex];
				}
			}
		}


		@Override
		public void run() {
			if(adding){
				 add(this.left, this.right, this.resultAdd, this.start);
			}else{
				 mux(this.left, this.right, this.resultMux, this.start);
			}

			
		}
		
	}


	/**
	 * Runs both vector summation and vector multiplexing for demo purposes.
	 * @param args the argument array
	 */
	static public void main (final String[] args) {
		final int size = args.length == 0 ? 3000 : Integer.parseInt(args[0]);
	
		// initialize operand vectors
		final double[] a = new double[size], b = new double[size];
		for (int index = 0; index < size; ++index) {
			a[index] = index + 1.0;
			b[index] = index + 2.0;
		}
		int resultHash = 0;

		// Warm-up phase to force hot-spot translation of byte-code into machine code, code-optimization, etc!
		// Output of resultHash prevents VM from over-optimizing the warmup-phase (by complete removal), which
		// happens to code that does not compute something in loops that is not used outside of it.
		for (int loop = 0; loop < 30000; ++loop) {
			double[] c = addMulti(a, b);
			resultHash ^= c.hashCode();

			double[][] d = muxMulti(a, b);
			resultHash ^= d.hashCode();
		}
		System.out.format("warm-up phase ended with result hash %d\n", resultHash);
	
		System.out.format("Computation is performed on %s processors\n", Runtime.getRuntime().availableProcessors());
		final long timestamp0 = System.currentTimeMillis();
		for (int loop = 0; loop < 10000; ++loop) {
			final double[] sum = addMulti(a, b);
			resultHash ^= sum.hashCode();
		}
		final long timestamp1 = System.currentTimeMillis();
		for (int loop = 0; loop < 10000; ++loop) {
			final double[][] mux = muxMulti(a, b);
			resultHash ^= mux.hashCode();
		}
		final long timestamp2 = System.currentTimeMillis();
		System.out.format("timing phase ended with result hash %d\n", resultHash);

		System.out.format("a + b took %.4fms to compute.\n", (timestamp1 - timestamp0) * 0.0001);
		System.out.format("a x b took %.4fms to compute.\n", (timestamp2 - timestamp1) * 0.0001);
		if (size <= 100) {
			final double[] sum = addMulti(a, b);
			final double[][] mux = muxMulti(a, b);
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


	private static double[][] muxMulti(double[] a, double[] b) {
		double[][] resultMux = new double[a.length][b.length];
		int proccesCount  = Runtime.getRuntime().availableProcessors();
		Thread[] threads = new Thread[proccesCount];
		float nDouble = a.length/ (float) proccesCount;
		int n = (int) Math.ceil(nDouble);
		for (int i = 0; i < proccesCount; i++) {
			int start = i*n;
			int end = i*n + n;
			if(end>= a.length) end = a.length;
			double[] subA = Arrays.copyOfRange(a, start , end);
			double[] subB = Arrays.copyOfRange(b, start , end);
			Calculator muxCalc = new Calculator(a, subB, start, null, resultMux, false);
			Thread thread = new Thread(muxCalc);
			threads[i] = thread;
			thread.start();
		}
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return resultMux;
	}


	private static double[] addMulti(double[] a, double[] b) {
		double[] resultAdd = new double[a.length];
		int proccesCount  = Runtime.getRuntime().availableProcessors();
		Thread[] threads = new Thread[proccesCount];
		float nDouble = a.length/ (float) proccesCount;
		int n = (int) Math.ceil(nDouble);
		for (int i = 0; i < proccesCount; i++) {
			int start = i*n;
			int end = i*n + n;
			if(end>= a.length) end = a.length;
			double[] subA = Arrays.copyOfRange(a, start , end);
			double[] subB = Arrays.copyOfRange(b, start , end);
			Calculator addCalc = new Calculator(subA, subB, start, resultAdd, null, true);
			Thread thread = new Thread(addCalc);
			threads[i] = thread;
			thread.start();
		}
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return resultAdd;
	}


	

}
