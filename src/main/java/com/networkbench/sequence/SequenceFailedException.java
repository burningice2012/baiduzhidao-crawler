/**
 * Copyright 2007, NetworkBench Systems Corp.
 */
package com.networkbench.sequence;

/**
 * @author BurningIce
 *
 */
public class SequenceFailedException extends Exception {
	private static final long serialVersionUID = -2538976960654524685L;

	public SequenceFailedException() {
		super();
	}

	public SequenceFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public SequenceFailedException(String message) {
		super(message);
	}

	public SequenceFailedException(Throwable cause) {
		super(cause);
	}

}
