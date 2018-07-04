package io.github.kaiso.relmongo.exception;

public class RelMongoConfigurationException extends RuntimeException {

	public RelMongoConfigurationException(String string, Exception e) {
		super(string, e);
	}

	public RelMongoConfigurationException(String string) {
		super(string);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
