package io.github.kaiso.relmongo.exception;

public class RelMongoInvalidApiUsageException extends RuntimeException {

	public RelMongoInvalidApiUsageException(String string, Exception e) {
		super(string, e);
	}

	public RelMongoInvalidApiUsageException(String string) {
		super(string);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
