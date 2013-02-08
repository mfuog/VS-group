package de.htw.ds.shop;

public class PreciseRethrowTest {
	public void test() {
		try {
			// Connector-Operation
			// commit()
		} catch (final Exception exception) {
			// rollback()
			throw exception;
			// -> precise rethrow ersetzt:
			// if (exception instanceof Error) throw (Error) exception;
			// if (exception instanceof RuntimeException) throw (RuntimeException) exception;
			// throw new AssertionError();
		}
	}
}
