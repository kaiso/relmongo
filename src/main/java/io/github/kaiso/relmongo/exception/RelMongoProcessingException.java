package io.github.kaiso.relmongo.exception;

public class RelMongoProcessingException extends RuntimeException {

    public RelMongoProcessingException(String string) {
        super(string);
    }

    public RelMongoProcessingException(Exception e) {
		super(e);
	}

	/**
     * 
     */
    private static final long serialVersionUID = 1L;

}
